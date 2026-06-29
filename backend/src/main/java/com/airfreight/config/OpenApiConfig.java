package com.airfreight.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("空运代理智能管理系统 API")
                        .version("1.0.0")
                        .description("基于15步空运代理工作流 + 财务客户对接 + 国外航空/海关/银行API对接的端到端管理系统")
                        .contact(new Contact()
                                .name("rain-fly")
                                .email("diaoyufei@hotmail.com"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")));
    }
}