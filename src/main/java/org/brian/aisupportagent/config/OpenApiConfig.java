package org.brian.aisupportagent.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI supportAgentOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Support Agent API")
                        .version("1.0.0")
                        .description("""
                                REST API for a retrieval-augmented customer support system. \
                                Administrators manage PDF knowledge documents, while \
                                authenticated users ask grounded questions and maintain \
                                conversation history.
                                """))
                .components(new Components().addSecuritySchemes(
                        BEARER_AUTH_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT access token returned by login or registration")
                ));
    }
}
