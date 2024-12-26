package com.kh.totalproject.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SwaggerConfig {
    // localhost:8111/swagger-ui/index.html
    @Bean
    public OpenAPI customOpenAPI(){
        return new OpenAPI()
                .info(new Info()
                        .title("Total Project API")
                        .version("v0")
                        .description("API Swagger"));
    }
//    @Bean
//    public OpenAPI customOpenAPI() {
//        return new OpenAPI()
//                .info(new Info()
//                        .title("Total Project API")
//                        .version("v1.0.0")
//                        .description("API Documentation with Swagger"))
//                .components(new Components()
//                        .addSecuritySchemes("Bearer Authentication",
//                                new SecurityScheme()
//                                        .type(SecurityScheme.Type.HTTP)
//                                        .scheme("bearer")
//                                        .bearerFormat("JWT")))
//                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"));
//    }
//    @Bean
//    public WebMvcConfigurer corsConfigurer() {
//        return new WebMvcConfigurer() {
//            @Override
//            public void addCorsMappings(CorsRegistry registry){
//                registry.addMapping("/v3/api-docs/**").allowedOrigins("*");
//                registry.addMapping("/swagger-ui/**").allowedOrigins("*");
//            }
//        };
//    }
}