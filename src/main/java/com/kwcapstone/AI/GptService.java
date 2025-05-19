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
        int wordCount = promptText.trim().split("\\s+").length;
        int estimatedTokens = (int) (wordCount * 3.5);
        if(wordCount > 500) {
            estimatedTokens += 1000;
        }

        return Math.min(estimatedTokens, 1000);
    }

    //gpt에게 요청하는 call를 만들어줘야할 것 같음
    // open ai 는 max_tokens 생성할 때 최대 토큰 수를 으미함
    // prompt 가 응답 모두 토큰으로 계산되어 얼마나 길게 나올 수 있는지를 제한함
    //prompt 는 누적된 회의 스크립트 전체 텍스트
    public String callSummaryOpenAI(String prompt) {
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

    public String callMainOpenAI(String prompt) {
        int maxTokens = estimateMaxTokens(prompt);

        String promptMessage = """
            다음 텍스트는 현재 진행 중인 아이디에이션 과정의 스크립트야. 이의 주제를 파악하고 현재 아이디에이션에 대한 주요 키워드를 5개 알려줘.
            주요 키워드만 대답해주면 돼. "더 필요하신거 있으신가요" 과 같은 답변 이어서 하지마. JSON 배열로 추출해줘.
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

    public int estimateRecommendResponseTokens(String promptText) {
        int promptTokenEstimate = (int) (promptText.trim().split("\\s+").length*1.4);
        int totalMax = 4096;

        int remaining = totalMax - promptTokenEstimate - 100;

        return Math.min(Math.max(remaining, 100), 1000);
    }

    public String callRecommendedKeywords(String prompt) {
        int maxTokens = estimateRecommendResponseTokens(prompt);

        String promptMessage = """
                    아래 회의 스크립트는 아이디어 회의 중 일부야.
                    지금 논의된 주제를 바탕으로, 다음 회의에서 더 발전시켜볼 만한 창의적인 아이디어 키워드 5개를 추천해줘.
                    각 키워드는 새로운 아이디어 방향을 제시하는 것이어야 하고, 이전 내용을 반복하지 않아야 해.
                    키워드는 6글자 이내면 좋고, 쉼표로 구분해서 한 줄로만 출력해줘.
                    """;

        Map<String, Object> requestBody = Map.of(
                "model", gptConfig.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", promptMessage),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.3,
                "max_tokens", maxTokens
        );

        return openAiWebClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return message.get("content").toString().trim();
                })
                .onErrorResume(e -> Mono.just("Error: " + e.getMessage()))
                .block();
    }

    //주요 노드 추가하기
    public String callMindMapNode(String prompt){
        int maxTokens = estimateMaxTokens(prompt);

        String promptMessage = """
                다음 텍스트는 회의에서 논의된 내용이야.
                이 내용에서 주요 아이디어나 핵심 개념을 기준으로 마인드맵 노드를 구성하려고 해.
                
                각 노드는 1~2단어 또는 짧은 문장으로 구성되어야 하고,
                하나의 노드는 하나의 주제나 개념을 담고 있어야 해.
                
                총 5~7개의 마인드맵 노드를 뽑아서 **JSON 배열**로 보여줘.
                답변은 노드만 깔끔하게 출력하고, 설명은 필요 없어.
                예: ["프로젝트 일정", "기술 스택", "팀 구성", ...]
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
                .block(); // block은 동기식으로 기다리기 (필요 시 비동기 방식으로 분리 가능
    }
}
