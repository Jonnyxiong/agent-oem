package com.ucpaas.sms.config;

import static springfox.documentation.builders.PathSelectors.regex;

import org.springframework.context.annotation.Bean;

import com.google.common.collect.Sets;

import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Created by lpjLiu on 2017/7/22.
 */
@EnableSwagger2
public class SwaggerConfig {
    @Bean
    public Docket configSpringfoxDocketForAll() {
        return new Docket(DocumentationType.SWAGGER_2)
                .produces(Sets.newHashSet("application/json"))
                .consumes(Sets.newHashSet("application/json"))
                .protocols(Sets.newHashSet("http"/*, "https"*/))
                .forCodeGeneration(true)
                .select().paths(regex(".*"))
                .build()
                .apiInfo(apiInfo());
    }

    private ApiInfo apiInfo() {
        ApiInfo apiInfo = new ApiInfo(
                "OEM代理商 REST API文档",
                "OEM代理商 REST风格的可视化文档",
                "1.0.0",
                "http://localhost:8080/v2/api-docs",
                "liulipengju@ucpaas.com",
                "Apache License",
                "http://www.apache.org/licenses/LICENSE-2.0.html"
        );
        return apiInfo;
    }
}
