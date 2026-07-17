package com.dashboard.api.client.esl;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.integration.esl")
public record EslGraphqlProperties(
        String graphqlUrl,
        String bearerToken,
        String corporationDocument,
        String requesterName,
        String requesterEmail,
        String requesterPhone,
        String requesterDepartment,
        String apiBaseUrl,
        String graphqlEndpoint
) {
}
