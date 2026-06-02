package com.dashboard.api.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.assertj.core.api.Assertions.assertThat;

class IpClienteResolverTest {

    @Test
    void usaCfConnectingIpQuandoForwardedHeadersSaoConfiaveis() {
        IpClienteResolver resolver = new IpClienteResolver(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.10");
        request.addHeader("CF-Connecting-IP", "203.0.113.10");
        request.addHeader("X-Forwarded-For", "198.51.100.20, 10.0.0.10");

        assertThat(resolver.resolver(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void ignoraForwardedHeadersQuandoNaoSaoConfiaveis() {
        IpClienteResolver resolver = new IpClienteResolver(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.10");
        request.addHeader("CF-Connecting-IP", "203.0.113.10");
        request.addHeader("X-Forwarded-For", "198.51.100.20, 10.0.0.10");

        assertThat(resolver.resolver(request)).isEqualTo("10.0.0.10");
    }
}
