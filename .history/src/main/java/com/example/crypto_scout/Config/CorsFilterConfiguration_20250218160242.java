package com.example.crypto_scout.Config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CorsFilterConfiguration {

    @Bean
    public FilterRegistrationBean<SimpleCorsFilter> corsFilterRegistrationBean() {
        FilterRegistrationBean<SimpleCorsFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new SimpleCorsFilter());
        registrationBean.addUrlPatterns("/*"); // Apply to all URLs
        registrationBean.setOrder(0); // Set high precedence (lowest order value)
        return registrationBean;
    }
}
