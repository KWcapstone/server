package com.kwcapstone.AI;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    public int estimateMaxTokens(String promptText) {
        final int totalLimit = 4096;

        int promptTokens = promptText.trim().split("\\s+").length;
        int estimatedTokens = (int) (promptTokens * 1.5);

        // 응답에 할당할 수 있는 최대 토큰 수
        int remaining = totalLimit - estimatedTokens;

        // 안전한 최소 보장 범위 (100~1500 사이 제한)
        return Math.max(100, Math.min(remaining, 1500));
    }

    public int estimateMindMapMaxTokens(String promptText){
        final int totalLimit = 4096;

        int wordCount = promptText.trim().split("\\s+").length;
        int estimatedTokens = (int) (wordCount * 1.5);

        // 응답에 할당할 수 있는 최대 토큰 수
        int remaining = totalLimit - estimatedTokens;

        // 안전한 최소 보장 범위 (100~1500 사이 제한)
        return Math.max(100, Math.min(remaining, 1500));
    }

    //요약본
    public String callSummaryOpenAI(String prompt) {
        int maxTokens = estimateMaxTokens(prompt);

        String promptMessage = """
                아래 회의 스크립트는 아이디어 회의 중 일부야. \s
                회의 내용을 간결하게 요약해줘.
                
                응답은 반드시 아래 JSON 형식으로 출력해줘:
                {
                  "title": "핵심 주제를 대표하는 간결한 제목",
                  "content": "회의의 핵심 흐름과 논의된 주요 사항을 정리한 본문 내용"
                }
                
                주의사항:
                - title은 15자 이내로, 내용을 대표할 수 있는 요약 문구로 작성해줘.
                - content는 3~4문장 이내로 회의 핵심 내용을 압축해서 설명해줘.
                - '더 도와드릴까요?' 같은 멘트는 절대 포함하지 마.
                - 이 형식을 꼭꼭 지켜줘. Json 형식이며 필드는 꼭 title과 content여야 해.
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

        String fullPrompt = """
                    아래 회의 스크립트는 아이디어 회의 중 일부야.
                    지금 논의된 주제를 바탕으로, 다음 회의에서 더 발전시켜볼 만한 창의적이고, 새로운 아이디어 키워드 5개를 추천해줘. 제약사항은 다음과 같아.
                    1. 기존의 내용을 요약하지 않아야 함.
                    2. 이전 내용을 반복하지 않아야 함.
                    3. 키워드는 5글자 이내여야 함.
                    4. "더 필요하신거 있으신가요" 과 같은 답변 이어서 하면 안됨.
                    5. 반드시 JSON 배열로만 추출할 것. 예: ["React", "블록체인", "GPU", "그래픽AI", "클라우드"]
                    6. 스크립트 부족하다고 같은 추천 키워드만 보내지 마.
                    """ + prompt;

        Map<String, Object> requestBody = Map.of(
                "model", gptConfig.getModel(),
                "messages", List.of(
                        Map.of("role", "user", "content", fullPrompt)
                ),
                "temperature", 0.3,
                "max_tokens", maxTokens
        );

        return openAiWebClient.post()
                .uri("/chat/completions") // gpt 한테 보내는 엔드포인트
                .bodyValue(requestBody)  //요청 본문 담을 데이터
                .retrieve() // 응답을 받아오는 단계 -> 비동기 응답 객체로 흐름이 바뀜
                .bodyToMono(Map.class)  // JSON으로 받음
                .map(response -> {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String content = message.get("content").toString().trim();
                    return content;
                })
                .onErrorResume(e -> Mono.just("Error: " + e.getMessage()))
                .block();
    }

    //주요 노드 추가하기
    public String callMindMapNode(String prompt){
        int maxTokens = estimateMindMapMaxTokens(prompt);

        String promptMessage = """
                다음 스크립트는 회의에서 논의된 내용이야.
                이 내용에서 주요 아이디어나 핵심 개념을 기준으로 마인드맵 노드를 구성하려고 해.
                이때 내가 Json 구조를 같이 보냈다면 기존의 node 구조를 보낸거야.
                기존의 node 구조를 바탕으로 다음 텍스트에 맞는 노드들을 뻗어나가는 형식으로 해야해.
                기존의 node 구조의 틀과 내용을 크게 변경하면 안된다는 소리지.
                
                각 노드는 1~2단어 또는 짧은 문장으로 구성되어야 하고,
                하나의 노드는 하나의 주제나 개념을 담고 있어야 해.
               
                각 노드는 다음과 같은 정보가 필요해:
                - id: 노드 고유 ID (아무 값이나 string으로 설정해도 좋아)
                - label: 노드에 들어갈 핵심 키워드 또는 문장 요약
                - parentId: 부모 노드의 ID (루트 노드는 null)
                - position : 마인드맵을 React Flow에서 시각화할 때 사용할 x, y 좌표
                
                **주의할 점**
                    - position은 React Flow에서 사용되는 좌표야. x는 좌우, y는 상하 위치를 의미하고 숫자(px) 단위로 줘.
                    - 루트 노드는 (x=0, y=0)에서 시작하고, 자식 노드는 y 간격 50~70 정도씩 아래로 배치하고, 형제 노드는 x 간격 150 정도씩 떨어뜨려서 배치해줘.
                    - 물론 내가 말한 x, y 간격이 텍스트 길이 때문에 겹칠 것 같다면 너가 적당히 너무 멀지 않도록 간격 조절해줘. 
                    - 그리고 노드에서 또 뻗어나아가는 노드 가지들이 있을 텐데 그 각각의 가지 사이의 간격도 엄청 띄우지 않도록 해줘.
                    - 처음 노드 생성할 때 제일 중심이 되는 노드(input) 를 중심으로 밑으로만 자식 노드가 생성 되는 것이 아닌 중간 노드 위의 위치에도 자식 노드가 생성되도록 신경써줘. 
                    - 즉, 계층 구조를 시각적으로 표현하기 좋도록 적절한 위치 값을 계산해서 넣어줘.
                
                결과는 JSON 배열 형태로 줘. 예시는 다음과 같아:
                
                    [
                      { "id": "1", "label": "졸업작품 아이디에이션", "parentId": null, "position": { "x": 0, "y": 0 } },
                      { "id": "2", "label": "실시간 마인드맵", "parentId": "1", "position": { "x": 0, "y": 0 } },
                      { "id": "3", "label": "토레타 챌린지", "parentId": "1", "position": { "x": 0, "y": 0 } },
                      { "id": "4", "label": "구현 난이도", "parentId": "2", "position": { "x": 0, "y": 0 } }
                    ]
                
                    이렇게 계층 구조를 JSON 배열로 표현해줘.
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


    public String modifyMainOpenAI(String prompt) {
        int maxTokens = estimateMaxTokens(prompt);

        String promptMessage = """
            다음 텍스트는 현재 진행 중인 아이디에이션 과정에서 수정된 node야.
             Json 형태로 node 구조를 보냈는데 이를 보고 주제를 다시 파악하고 현재 아이디에이션에 대한 주요 키워드를 5개 알려줘.
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

    public String modifyRecommendedKeywords(String prompt) {
        int maxTokens = estimateRecommendResponseTokens(prompt);

        String fullPrompt = """
                    다음 텍스트는 현재 진행 중인 아이디에이션 과정에서 수정된 node야.
                    Json 형태로 node 구조를 보냈는데 이를 보고 주제를 파악해서, 다음 회의에서 더 발전시켜볼 만한 창의적이고, 새로운 아이디어 키워드 5개를 추천해줘. 제약사항은 다음과 같아.
                    1. 기존의 내용을 요약하지 않아야 함.
                    2. 이전 내용을 반복하지 않아야 함.
                    3. 키워드는 5글자 이내여야 함.
                    4. "더 필요하신거 있으신가요" 과 같은 답변 이어서 하면 안됨.
                5. 반드시 JSON 배열로만 추출할 것. 예: ["React", "블록체인", "GPU", "그래픽AI", "클라우드"]
                """ + prompt;

        Map<String, Object> requestBody = Map.of(
                "model", gptConfig.getModel(),
                "messages", List.of(
                        Map.of("role", "user", "content", fullPrompt)
                ),
                "temperature", 0.3,
                "max_tokens", maxTokens
        );

        return openAiWebClient.post()
                .uri("/chat/completions") // gpt 한테 보내는 엔드포인트
                .bodyValue(requestBody)  //요청 본문 담을 데이터
                .retrieve() // 응답을 받아오는 단계 -> 비동기 응답 객체로 흐름이 바뀜
                .bodyToMono(Map.class)  // JSON으로 받음
                .map(response -> {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String content = message.get("content").toString().trim();
                    return content;
                })
                .onErrorResume(e -> Mono.just("Error: " + e.getMessage()))
                .block();
    }
}
