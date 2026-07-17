package com.dashboard.api.client.esl;

import java.util.Map;

record EslGraphqlRequest(
        String operationName,
        String query,
        Map<String, Object> variables
) {
}
