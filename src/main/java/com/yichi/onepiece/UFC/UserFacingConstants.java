package com.yichi.onepiece.UFC;

import com.yichi.onepiece.common.Holder;
import com.yichi.onepiece.model.I18nText;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
@ConfigurationProperties(prefix = "user")
@PropertySource(value = "classpath:user-facing-constants.yaml", factory = YamlPropertySourceFactory.class)
public class UserFacingConstants {
    private static final Holder<UserFacingConstants> defaultConstantsHolder = new Holder<>();
    private static final ThreadLocal<UserFacingConstants> holder = new ThreadLocal<>();
    private static AtomicLong threadLocalSize;
    private Map<String, Constant> constants;

    public static UserFacingConstants get() {
        var constants = holder.get();
        if (constants == null) {
            constants = defaultConstantsHolder.get();
            holder.set(constants);
            if (threadLocalSize != null) {
                threadLocalSize.incrementAndGet();
            }
        }
        if (constants != null) {
            return constants;
        }
        throw new RuntimeException("No UserFacingConstants found");
    }

    public static void init(UserFacingConstants constants, MeterRegistry meterRegistry, String cacheGaugeName) {
        defaultConstantsHolder.set(constants);
        threadLocalSize = meterRegistry.gauge(cacheGaugeName, Tags.of("collection_name", "UserFacingConstants.threadLocal"),
                new AtomicLong(0));
    }

    public static Constant getConstant(String key) {
        return Objects.requireNonNull(get().constants.get(key), key + " is not initialized");
    }

    // constant.key -> constant map
    public Map<String, Constant> getConstantMap() {
        return constants;
    }

    public void setConstants(Map<String, Constant> constants) {
        this.constants = constants;
        for (var entry : this.constants.entrySet()) {
            entry.getValue().setKey(entry.getKey());
        }
    }

    public static class Constant {
        private String key;
        private I18nText displayName;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public I18nText getDisplayName() {
            return displayName;
        }

        public void setDisplayName(I18nText displayName) {
            this.displayName = displayName;
        }
    }

    public static void clear() {
        holder.remove();
        if (threadLocalSize != null) {
            threadLocalSize.decrementAndGet();
        }
    }

    @Override
    public String toString() {
        return String.join("\n", constants.entrySet().toString());
    }
}
