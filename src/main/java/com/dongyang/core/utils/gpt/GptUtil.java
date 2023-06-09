package com.dongyang.core.utils.gpt;

import com.dongyang.core.controller.dto.request.GptRequestDto;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class GptUtil {
    @Value("${gpt.apikey}")
    private String API_KEY;

    @Value("${gpt.url}")
    private String API_URL;

    private final RestTemplate restTemplate = restTemplate();

    public String sendRequest(GptRequestDto requestDto) {
        try {
            return parseGPTResponseContent(requestToGPT(API_URL, createRequest(requestDto)));

        } catch (RestClientException | ParseException e) {
            throw new OpenAIException("OpenAI API 호출 중 오류가 발생하였습니다.", e);
        }
    }

    private HttpEntity<Map<String, Object>> createRequest(GptRequestDto requestDto) {
        // 질문에 대한 requestData 생성
        return new HttpEntity<>(
                createRequestBody(createChatMessages(requestDto)),
                createRequestHeader());
    }

    private List<ChatMessage> createChatMessages(GptRequestDto requestDto) {
        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(ChatMessage.of("user", requestDto.getQuestion()));

        return chatMessages;
    }

    private HttpHeaders createRequestHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(API_KEY);
        return headers;
    }

    private ResponseEntity<String> requestToGPT(String url, HttpEntity<Map<String, Object>> request) {
        // postForEntity
        // url: 요청 URL, request: requestData, String.class: 응답내용의 형태
        return restTemplate.postForEntity(url, request, String.class);
    }

    private String parseGPTResponseContent(ResponseEntity<String> response) throws ParseException {
        String body = response.getBody();

        JSONParser jsonParser = new JSONParser();
        JSONObject bodyJson = (JSONObject) jsonParser.parse(body);

        JSONArray choicesJson = (JSONArray) bodyJson.get("choices");

        JSONObject choiceJson = (JSONObject) choicesJson.get(0);
        JSONObject messageJson = (JSONObject) choiceJson.get("message");

        return messageJson.get("content").toString();
    }

    private Map<String, Object> createRequestBody(List<ChatMessage> chatMessages) {
        Map<String, Object> requestBody = new HashMap<>();

        // 권한, 요청내용 담기
        requestBody.put("messages", chatMessages);

        // 요청에 사용될 모델 설정
        requestBody.put("model", "gpt-3.5-turbo");

        // 완료시 생성할 최대 토큰수
        requestBody.put("max_tokens", 100);
        return requestBody;
    }


    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add((request, body, execution) -> {
            request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            request.getHeaders().setBearerAuth(API_KEY);
            return execution.execute(request, body);
        });
        restTemplate.setInterceptors(interceptors);

        return restTemplate;
    }

    public class OpenAIException extends RestClientException {
        public OpenAIException(String message) {
            super(message);
        }

        public OpenAIException(String message, Throwable cause) {
            super(message, cause);
        }

    }
}