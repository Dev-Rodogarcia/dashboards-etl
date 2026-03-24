package com.dashboard.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration;

@SpringBootApplication(exclude = {
    LdapAutoConfiguration.class
})
public class DashboardApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(DashboardApiApplication.class, args);
    }
}
