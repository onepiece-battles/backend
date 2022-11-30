package com.yichi.onepiece.Context;

import com.yichi.onepiece.UFC.UserFacingConstants;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RequestInterceptor {

    public RequestInterceptor(UserFacingConstants constants, MeterRegistry meterRegistry,
                              @Value("${com.yichi.onepiece.meters.cache_gauge_name:}") String gaugeName) {
        UserFacingConstants.init(constants, meterRegistry, gaugeName);
    }
}
