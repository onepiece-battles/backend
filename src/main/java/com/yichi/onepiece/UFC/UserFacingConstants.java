package com.yichi.onepiece.UFC;

import com.yichi.onepiece.common.Holder;
import com.yichi.onepiece.model.I18nText;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
@ConfigurationProperties(prefix = "user")
@PropertySource(value = "classpath:user-facing-constants.yaml",
factory = YamlPropertySourceFactory.class)
public class UserFacingConstants {
    private static final Holder<UserFacingConstants> defaultConstantsHolder = new Holder<>();
    private static final ThreadLocal<UserFacingConstants> holder = new ThreadLocal<>();

    private static AtomicLong threadLocalSize;

    private Map<String, Constant> constantMap;

    public static UserFacingConstants get() {
        var constants = holder.get();
        if (constants == null) {
            constants = defaultConstantsHolder.get();
            holder.set(constants);
        }
        return constants;
    }

    public static Constant getConstant(String key) {
        return Objects.requireNonNull(get().constantMap.get(key), key+" is not initialized");
    }

    public Map<String, Constant> getConstantMap() {
        return constantMap;
    }
    public void setConstantsMap(Map<String, Constant> constantMap) {
        this.constantMap = constantMap;
        for (var entry: this.constantMap.entrySet()) {

        }
    }

    public static class Constant {
        private String key;
        private I18nText displayName;

        public String getKey(){
            return key;
        }

        public I18nText getDisplayName() {
            return displayName;
        }
    }

    public static void clear() {
        holder.remove();
        if (threadLocalSize!=null) {
            threadLocalSize.decrementAndGet();
        }
    }

    @Override
    public String toString() {
        return String.join("\n", constantMap.entrySet().toString());
    }
}
