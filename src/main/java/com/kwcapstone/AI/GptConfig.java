package com.kwcapstone.AI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GptConfig {
    @Value("${OPENAI_API_KEY}")
    private String secretKey;

    @Value("gpt-3.5-turbo")
    private String model;

    public String getSecretKey() {
        return secretKey;
    }

    public String getModel() {
        return model;
    }
}
