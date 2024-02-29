package com.yichi.onepiece.configuration;

import com.yichi.onepiece.quartz.QuartzJobFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

@Configuration
public class QuartzConfig {
    private final QuartzJobFactory quartzJobFactory;

    public QuartzConfig(QuartzJobFactory quartzJobFactory) {
        this.quartzJobFactory = quartzJobFactory;
    }


    // use customized scheduler to get spring bean when creating jobs
    @Bean
    public SchedulerFactoryBean schedulerFactory() {
        SchedulerFactoryBean bean = new SchedulerFactoryBean();
        bean.setJobFactory(quartzJobFactory);
        return bean;
    }
}
