package com.kwcapstone.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

//api 호출할 때 요청 방식 설정하는 파일
@Configuration
public class WebClientConfig {
    //gpt api 부를 때 필요한 api key
    @Value("${OPENAI_API_KEY}")
    private String openAiApiKey;

    //webcliet 객체를 직접 생성혹 스프링에 bean을 등록
    @Bean
    public WebClient openAiWebClient() {
        return WebClient.builder()
                .baseUrl("https://api.openai.com/v1") //요청의 기본 url
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer "+openAiApiKey) //요청할 때 넣을 headers opne api key
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE) //요청 본문이 json 형태로 넘어간다는 의미
                .build();
    }
}
