package com.dashboard.api.client.esl;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EslGraphqlProperties.class)
public class EslGraphqlClientConfig {
}
