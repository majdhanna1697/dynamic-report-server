package com.dynamic.report.configurations;

import com.dynamic.report.filters.TraceIdFilter;
import com.dynamic.report.services.RSAService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TraceIdFilterConfig {

    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilter(RSAService rsaService) {
        FilterRegistrationBean<TraceIdFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new TraceIdFilter(rsaService));
        registrationBean.addUrlPatterns("/*");
        return registrationBean;
    }
}
