package com.kwcapstone.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    String[] accessURL = { "http://localhost:8080", "https://localhost:8080",
                            "http://localhost:5173", "https://localhost:5173",
                            "https://moaba.vercel.app", "https://www.moaba.site",
                            "https://moaba.site",
                            "http://3.39.11.168:8080", "https://3.39.11.168:8080",
                            "http://localhost:3000", "https://localhost:3000"};

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(accessURL)
                .allowedMethods("GET", "POST", "PATCH", "DELETE", "PUT", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3000)
                .allowCredentials(true);
    }
}
