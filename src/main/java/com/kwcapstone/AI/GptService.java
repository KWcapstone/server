package com.kwcapstone.AI;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class GptService {
    //필드로 선언해줘야함 -> open api 호출할 때 ㅎ필요한 요청 틀임
    private final WebClient openAiWebClient;
    private final GptConfig gptConfig;

    //토큰 계산하는 함수
    public int estimateMaxTokens(String promptText){
        int wordCound = promptText.trim().split("\\s+").length;
        int estimatedTokens = (int) (wordCound * 1.4);
        if(wordCound > 500) {
            estimatedTokens += 200;
        }

        return Math.min(estimatedTokens, 1000);
    }

    //gpt에게 요청하는 call를 만들어줘야할 것 같음
    // open ai 는 max_tokens 생성할 때 최대 토큰 수를 으미함
    // prompt 가 응답 모두 토큰으로 계산되어 얼마나 길게 나올 수 있는지를 제한함
    //prompt 는 누적된 회의 스크립트 전체 텍스트
    public String callOpenAI(String prompt) {
        int maxTokens = estimateMaxTokens(prompt);

        String promptMessage = """
            다음 텍스트를 요약해줘.
            응답은 %d 토큰 이내로 끝나도록 핵심만 간결하게 정리해줘.
            요약본만 대답해주면 돼. "더 필요하신거 있으신가요" 과 같은 답변 이어서 하지마.
            """.formatted(maxTokens);

        Map<String, Object> requestBody = Map.of(
                "model", gptConfig.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", promptMessage),
                        Map.of("role", "user", "content", prompt) //원문 내용
                ),
                "temperature", 0.3, //창의성(무작위성)을 조절하는 파라미터
                "max_tokens", maxTokens
        );

        // block()을 써서 동기적으로 받기
        return openAiWebClient.post()
                .uri("/chat/completions") // gpt 한테 보내는 엔드포인트
                .bodyValue(requestBody)  //요청 본문 담을 데이터
                .retrieve() // 응답을 받아오는 단계 -> 비동기 응답 객체로 흐름이 바뀜
                .bodyToMono(Map.class)  // JSON으로 받음
                .map(response -> {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return message.get("content").toString().trim();
                })
                .onErrorResume(e -> Mono.just("Error: " + e.getMessage()))
                .block(); // block은 동기식으로 기다리기 (필요 시 비동기 방식으로 분리 가능)
    }
}
