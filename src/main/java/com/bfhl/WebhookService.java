package com.bfhl;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class WebhookService {

    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        this.restTemplate = new RestTemplateBuilder()
                .errorHandler(new DefaultResponseErrorHandler() {
                    @Override
                    public void handleError(ClientHttpResponse response) throws IOException {
                        System.err.println("HTTP Status: " + response.getStatusCode());
                        System.err.println("Response Body: " + new String(response.getBody().readAllBytes()));
                    }
                })
                .build();
        restTemplate.setRequestFactory(clientHttpRequestFactory());
    }

    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setBufferRequestBody(true);
        return factory;
    }

    public void process() {
        String registrationUrl = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("name", "John Doe");
        requestBody.put("regNo", "REG12347");
        requestBody.put("email", "john@example.com");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(registrationUrl, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                System.out.println("🔍 Full API Response: " + response.getBody());

                String webhookUrl = (String) response.getBody().get("webhookUrl");
                if (webhookUrl == null) {
                    webhookUrl = (String) response.getBody().get("webhook");
                }

                String accessToken = (String) response.getBody().get("accessToken");

                System.out.println("Received webhook URL: " + webhookUrl);
                System.out.println("Received accessToken: " + accessToken);

                if (webhookUrl != null && webhookUrl.startsWith("http")) {
                    submitSolution(webhookUrl, accessToken);
                } else {
                    System.err.println("Invalid or missing webhook URL.");
                }
            } else {
                System.err.println("Webhook generation failed");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void submitSolution(String webhookUrl, String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            Map<String, String> solution = new HashMap<>();
            String finalQuery = "SELECT p.AMOUNT AS SALARY, CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS NAME, " +
                    "TIMESTAMPDIFF(YEAR, e.DOB, CURDATE()) AS AGE, d.DEPARTMENT_NAME " +
                    "FROM PAYMENTS p " +
                    "JOIN EMPLOYEE e ON p.EMP_ID = e.EMP_ID " +
                    "JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID " +
                    "WHERE DAY(p.PAYMENT_TIME) != 1 " +
                    "ORDER BY p.AMOUNT DESC LIMIT 1";

            solution.put("finalQuery", finalQuery.trim());

            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(solution);

            System.out.println("Submitting to Webhook...");
            System.out.println("Auth Header: Bearer " + accessToken);
            System.out.println("Payload: " + json);
            System.out.println("Endpoint: " + webhookUrl);

            HttpEntity<String> request = new HttpEntity<>(json, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(webhookUrl, request, String.class);

            System.out.println("Solution submitted successfully. Response: " + response.getBody());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
