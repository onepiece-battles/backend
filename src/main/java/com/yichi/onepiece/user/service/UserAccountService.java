package com.yichi.onepiece.user.service;


import com.aliyuncs.utils.Base64Helper;
import com.yichi.onepiece.configuration.ChattingWebSocketHandler;
import com.yichi.onepiece.exception.ExceptionUtils;
import com.yichi.onepiece.exception.FileStorageException;
import com.yichi.onepiece.iam.model.*;
import com.yichi.onepiece.iam.service.RoleService;
import com.yichi.onepiece.image.service.ImageService;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.model.*;
import com.yichi.onepiece.user.model.*;
import com.yichi.onepiece.user_facing_constants.UserFacingConstants;
import com.yichi.onepiece.user_facing_constants.UserFacingConstantsKey;
import com.yichi.onepiece.utils.EncryptHelper;
import com.yichi.onepiece.utils.JwtHelper;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.yichi.onepiece.constants.OnepieceConstants.SUCCESS;
import static com.yichi.onepiece.constants.OnepieceConstants.VALID_EMAIL_PATTERN;
import static com.yichi.onepiece.user_facing_constants.UserFacingConstantsKey.*;

@Service
public class UserAccountService {

    private final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final AuthenticationManager authenticationManager;
    private final TokenDao tokenDao;
    private final UserDao userDao;
    private final RoleDao roleDao;
    private final UserRoleDao userRoleDao;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final ImageService imageService;
    private final ActionRecordService actionRecordService;
    private final ChattingWebSocketHandler chattingWebSocketHandler;

    public UserAccountService(AuthenticationManager authenticationManager, TokenDao tokenDao, UserDao userDao, RoleDao roleDao, UserRoleDao userRoleDao, RoleService roleService, PasswordEncoder passwordEncoder, ImageService imageService, ActionRecordService actionRecordService, ChattingWebSocketHandler chattingWebSocketHandler) {
        this.authenticationManager = authenticationManager;
        this.tokenDao = tokenDao;
        this.userDao = userDao;
        this.roleDao = roleDao;
        this.userRoleDao = userRoleDao;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
        this.imageService = imageService;
        this.actionRecordService = actionRecordService;
        this.chattingWebSocketHandler = chattingWebSocketHandler;
    }

    /**
     * Request to register into system
     *
     * @param nickName nickname
     * @param password password
     * @param username email address as username
     * @param confirmedPassword confirmed password
     * @param onepieceLocale the locale information (i18n information) based on FE language
     * @return register verification: key of DUPLICATE_USER_NAME/PASSWORD_DIFFERENT/REGISTER_SUCCESS
     */
    public String register(
            String nickName,
            String password,
            String username,
            String confirmedPassword,
            OnepieceLocale onepieceLocale,
            @Nullable UserRole userRole)  {
        if (!userDao.listByNickName(nickName).isEmpty()) {
            return UserFacingConstantsKey.DUPLICATE_USER_NAME;
        }
        if (!userDao.listByEmailAddress(username).isEmpty()) {
            return UserFacingConstantsKey.EMAIL_HAS_BEEN_REGISTERED;
        }
        if (!username.matches(VALID_EMAIL_PATTERN)) {
            return UserFacingConstantsKey.INVALID_EMAIL_ADDRESS;
        }
        if (!password.equals(confirmedPassword)) {
            return UserFacingConstantsKey.PASSWORD_CONFIRMATION;
        }
        UserEntity user;
        try {
            var defaultImage =  new ClassPathResource("/images/user_default_profile.png").getContentAsByteArray();
            var encryptedPassword = passwordEncoder.encode(password);
            var uri = imageService.uploadImage(Base64Helper.encode(defaultImage));
            user = UserEntityBuilder.newBuilder()
                    .setNickname(nickName)
                    .setAccountStatus(AccountStatus.LOGGED_OUT)
                    .setEncryptedPassword(encryptedPassword)
                    .setUsername(username)
                    .setEmailVerified(false)
                    .setLocale(onepieceLocale)
                    .setLoginRole(null)
                    .setLoginRoleId(null)
                    .setImageUri(uri)
                    .setProfileImage(ArrayUtils.toObject(defaultImage))
                    .build();
        } catch (IOException e) {
            throw new RuntimeException();
        } catch (FileStorageException e) {
            throw new RuntimeException(e);
        }
        userDao.insert(user);
        // new user are default to be created by NIKA
        var actionRecord = actionRecordService.createWithUserAndRole(user, UserRole.NIKA);
        var updatedUser =
                user.toBuilder()
                        .setCreationRecordId(actionRecord.getId())
                        .setModificationRecordId(actionRecord.getId())
                        .build();
        userDao.update(updatedUser);
        // newly registered users are default to have role: "NORMAL".
        var roleEntities = roleDao.listRoleByRoleNames(List.of(UserRole.NORMAL));
        if (userRole!=null) {
            roleEntities = roleDao.listRoleByRoleNames(List.of(userRole));
        }
        // make nika account to be onepiecebattles@163.com
        if (username.equals("onepiecebattles@163.com")) {
            roleEntities = roleDao.listRoleByRoleNames(
                    List.of(UserRole.NIKA, UserRole.ADMIN, UserRole.NORMAL, UserRole.VISITOR));
        }
        roleEntities.forEach(
                roleEntity ->
                        userRoleDao.insert(
                                UserRoleEntityBuilder.newBuilder()
                                        .setUserId(user.getId())
                                        .setRoleId(roleEntity.getId())
                                        .build()));
        return SUCCESS;
    }


    public LoginResponse login(LoginRequest request) {
        logger.info("start login");
        var email = request.getEmail();
        var password = request.getPassword();
        var userDetail = userDao.getUserByUsername(email);
        I18nText verification;
        if (userDetail.isEmpty()) {
            verification = UserFacingConstants.getConstant(REGISTER_FIRST_BEFORE_LOGIN).getDisplayName();
            return new LoginResponse().verification(verification);
        }
        // Authenticate username and password with registered information.
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
        } catch (Exception e) {
            verification = UserFacingConstants.getConstant(WRONG_PASSWORD).getDisplayName();
            return new LoginResponse().verification(verification);
        }

        // generate jwt token and refresh token for this login action
        var user = userDetail.get();
        var jwtToken = JwtHelper.generateAccessToken(user);
        var jwtRefreshToken = JwtHelper.generateRefreshToken(user);

        // revoke all user tokens
        tokenDao.revokeAllUserTokensByUserId(user.getId());
        // add new token
        TokenEntity tokenEntity =
                TokenEntityBuilder.newBuilder()
                        .setUserId(user.getId())
                        .setTokenType(TokenType.BEARER)
                        .setTokenValue(jwtToken)
                        .setExpired(false)
                        .setRevoked(false)
                        .build();
        tokenDao.insert(tokenEntity);

        // update user login role id
        var roles = roleDao.listByUserId(user.getId());
        var roleEntity = getHighestPermissionRole(roles);
        user.setAccountStatus(AccountStatus.LOGGED_IN);
        user.setLoginRole(roleEntity.getName());
        user.setLoginRoleId(roleEntity.getId());
        userDao.update(user);
        var resourcePermissionMaps = roleService.getPermissionMapByRoleName(user.getLoginRole());
        var tokens =
                tokenDao.listTokenByUserId(user.getId()).stream().map(TokenEntity::getTokenValue).toList();
        var roleIds = userRoleDao.getByUserId(user.getId());
        var roleNames =
                roleDao.batchFindById(roleIds).stream()
                        .map(RoleEntity::getName)
                        .collect(Collectors.toList());
        return new LoginResponse()
                .verification(null)
                .accessToken(jwtToken)
                .refreshToken(jwtRefreshToken)
                .user(
                        UserConverter.INSTANCE
                                .toUser(user)
                                .profileImage(EncryptHelper.base64Encode(ArrayUtils.toPrimitive(user.getProfileImage())))
                                .loginRoleResourcesPermissionMap(resourcePermissionMaps)
                                .userRoles(roleNames)
                                .tokens(tokens));
    }

    public LogoutResponse logout(UUID userId) {
        chattingWebSocketHandler.disconnectUser(userId);
        tokenDao.revokeAllUserTokensByUserId(userId);
        var userOptional = userDao.findById(userId);
        if (userOptional.isEmpty()) {
            return new LogoutResponse().logoutResult(NO_USER_FOUND);
        }
        var user = userOptional.get();
        var signedOutUser = user.toBuilder().setAccountStatus(AccountStatus.LOGGED_OUT).build();
        userDao.update(signedOutUser);
        return new LogoutResponse().logoutResult(SUCCESS).userName(user.getUsername());
    }

    private RoleEntity getHighestPermissionRole(List<RoleEntity> roles) {
        if (roles.isEmpty()) {
            throw ExceptionUtils.notFound("no role assigned to user").get();
        }
        Map<UserRole, RoleEntity> roleNameToRole = roles.stream()
                .collect(Collectors.toMap(RoleEntity::getName, Function.identity()));
        var selectedRole = Stream.of(UserRole.NIKA, UserRole.ADMIN, UserRole.NORMAL, UserRole.VISITOR).filter(
                roleNameToRole::containsKey
        ).findFirst().orElseThrow();
        return roleNameToRole.get(selectedRole);
    }


}
