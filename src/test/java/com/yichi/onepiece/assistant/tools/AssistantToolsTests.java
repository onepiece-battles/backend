package com.yichi.onepiece.assistant.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yichi.onepiece.assistant.agent.AssistantLanguage;
import com.yichi.onepiece.assistant.agent.ToolContext;
import com.yichi.onepiece.assistant.agent.ToolResult;
import com.yichi.onepiece.assistant.dto.AssistantDto.ResourceLink;
import com.yichi.onepiece.character.service.CharacterService;
import com.yichi.onepiece.exception.PermissionDeniedException;
import com.yichi.onepiece.iam.service.IamService;
import com.yichi.onepiece.model.BattleExecution;
import com.yichi.onepiece.model.BattleListView;
import com.yichi.onepiece.model.CharacterFullView;
import com.yichi.onepiece.model.CharacterListView;
import com.yichi.onepiece.model.FruitListView;
import com.yichi.onepiece.model.FruitType;
import com.yichi.onepiece.model.GroupForCharacterView;
import com.yichi.onepiece.model.I18nText;
import com.yichi.onepiece.model.ListCharacterResponse;
import com.yichi.onepiece.model.Permission;
import com.yichi.onepiece.model.ResourceType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AssistantToolsTests {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ToolContext english = new ToolContext(UUID.randomUUID(), AssistantLanguage.EN_US);
    private CharacterService characterService;
    private IamService iamService;
    private CharacterTools characterTools;

    @BeforeMethod
    public void setUp() {
        characterService = mock(CharacterService.class);
        iamService = mock(IamService.class);
        characterTools = new CharacterTools(characterService, iamService);
    }

    @Test
    public void characterDetailMatchesTheShapeTheModelExpects() throws Exception {
        UUID luffyId = UUID.randomUUID();
        UUID lawId = UUID.randomUUID();
        UUID fruitId = UUID.randomUUID();
        UUID crewId = UUID.randomUUID();
        UUID battleId = UUID.randomUUID();
        when(characterService.listCharacters(any())).thenReturn(new ListCharacterResponse().characters(
                new ArrayList<>(List.of(new CharacterListView().id(luffyId).name(name("Luffy", "路飞"))))));
        var law = new com.yichi.onepiece.model.Character().id(lawId).name(name("Law", "罗"));
        when(characterService.getCharacterFullView(luffyId)).thenReturn(new CharacterFullView()
                .id(luffyId)
                .name(name("Luffy", "路飞"))
                .score(1200)
                .fruits(new ArrayList<>(List.of(new FruitListView().id(fruitId).name(name("Gomu Gomu no Mi", "橡胶果实")))))
                .groups(new ArrayList<>(List.of(new GroupForCharacterView().groupId(crewId).name(name("Straw Hats", "草帽海贼团")))))
                .battles(new ArrayList<>(List.of(new BattleListView()
                        .id(battleId)
                        .starterId(luffyId)
                        .receiver(law)
                        .battleExecution(new BattleExecution().winnerId(luffyId))))));

        ToolResult result = characterTools.characterDetail("Luffy", english);

        JsonNode json = objectMapper.readTree(result.content());
        assertThat(json.get("name").asText()).isEqualTo("Luffy");
        assertThat(json.get("combat_power").asInt()).isEqualTo(1200);
        assertThat(json.get("devil_fruits").get(0).get("name").asText()).isEqualTo("Gomu Gomu no Mi");
        assertThat(json.get("crews").get(0).get("id").asText()).isEqualTo(crewId.toString());
        assertThat(json.get("battles").get(0).get("result").asText()).isEqualTo("win");
        assertThat(json.get("battles").get(0).get("opponent").get("name").asText()).isEqualTo("Law");
        assertThat(result.resources()).extracting(ResourceLink::type)
                .containsExactly("character", "fruit", "group", "character", "battle");
        // The same permissions as the character detail endpoint.
        verify(iamService).checkPermissionOfCurrentAccount(ResourceType.BATTLE, Set.of(Permission.GET));
        verify(iamService).checkPermissionOfCurrentAccount(ResourceType.GROUP, Set.of(Permission.GET));
    }

    @Test
    public void namesFollowTheAnswerLanguage() throws Exception {
        when(characterService.listCharacters(any())).thenReturn(new ListCharacterResponse().characters(
                new ArrayList<>(List.of(new CharacterListView().id(UUID.randomUUID()).name(name("Luffy", "路飞"))))));

        ToolResult result = characterTools.searchCharacters("路飞", new ToolContext(UUID.randomUUID(), AssistantLanguage.ZH_CN));

        assertThat(objectMapper.readTree(result.content()).get("characters").get(0).get("name").asText()).isEqualTo("路飞");
    }

    @Test
    public void reportsACharacterThatDoesNotExist() {
        when(characterService.listCharacters(any())).thenReturn(new ListCharacterResponse().characters(new ArrayList<>()));

        ToolResult result = characterTools.characterDetail("Nobody", english);

        assertThat(result.content()).contains("\"found\":false");
        assertThat(result.resources()).isEmpty();
    }

    @Test
    public void checksPermissionsBeforeLookingAnythingUp() {
        doThrow(new PermissionDeniedException("Access Denied"))
                .when(iamService).checkPermissionOfCurrentAccount(eq(ResourceType.CHARACTER), any());

        assertThatThrownBy(() -> characterTools.searchCharacters("Luffy", english))
                .isInstanceOf(PermissionDeniedException.class);
        verifyNoInteractions(characterService);
    }

    @Test
    public void parsesFruitTypesInEnglishAndChinese() {
        assertThat(FruitTools.parseFruitType("Ancient Zoan")).isEqualTo(FruitType.ANCIENT_ZOAN);
        assertThat(FruitTools.parseFruitType("幻兽种")).isEqualTo(FruitType.MYTHICAL_ZOAN);
        assertThat(FruitTools.parseFruitType("logia")).isEqualTo(FruitType.LOGIA);
        assertThat(FruitTools.parseFruitType("banana")).isNull();
    }

    private static I18nText name(String english, String chinese) {
        return new I18nText().enUS(english).zhCN(chinese);
    }
}
