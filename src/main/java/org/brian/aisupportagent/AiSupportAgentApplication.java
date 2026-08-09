package org.brian.aisupportagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AiSupportAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiSupportAgentApplication.class, args);
    }

}
