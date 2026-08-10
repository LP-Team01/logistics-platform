package com.logistics.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "gateway.security")
public record SecurityRuleProperties(
    List<String> permitAll,
    List<Rule> rules
) {

    public record Rule(
        String path,
        List<String> methods,
        List<UserRole> roles
    ) {
    }
}
