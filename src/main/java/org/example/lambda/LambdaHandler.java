/*package org.example.lambda;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.ChatRequest;
import java.util.Map;
import java.util.HashMap;
public class LambdaHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        context.getLogger().log("Lambda received event: " + event);
        String receivedText = "No message received";
        try {
            if ("OPTIONS".equals(event.get("httpMethod"))) {
                return getCorsResponse();
            }
            if (event.containsKey("body")) {
                String requestBody = (String) event.get("body");
                if (requestBody != null && !requestBody.isEmpty()) {
                    ChatRequest chatRequest = objectMapper.readValue(requestBody, ChatRequest.class);
                    receivedText = chatRequest.getText();
                }
            }
        } catch (Exception e) {
            context.getLogger().log("Error parsing request body: " + e.getMessage());
        }
        context.getLogger().log("Received message: " + receivedText);
        return createResponse(200, "{\"message\": \"Received: " + receivedText + "\"}");
    }
    private Map<String, Object> getCorsResponse() {
        return createResponse(200, "");
    }
    private Map<String, Object> createResponse(int statusCode, String body) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type");

        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", statusCode);
        response.put("headers", headers);
        response.put("body", body);

        return response;
    }
}*/
package org.example.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.ChatRequest;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.HashMap;

import software.amazon.awssdk.core.SdkBytes;
import org.json.JSONObject;

public class LambdaHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        context.getLogger().log("Lambda received event: " + event);

        String receivedText = "No message received"; // Default response
        String bedrockResponse = "No AI response";

        try {
            if ("OPTIONS".equals(event.get("httpMethod"))) {
                return getCorsResponse();
            }

            if (event.containsKey("body")) {
                String requestBody = (String) event.get("body");
                if (requestBody != null && !requestBody.isEmpty()) {
                    ChatRequest chatRequest = objectMapper.readValue(requestBody, ChatRequest.class);
                    receivedText = chatRequest.getInputText();
                    bedrockResponse = callAmazonBedrock(receivedText, context);
                }
            }
        } catch (Exception e) {
            context.getLogger().log("Error processing request: " + e.getMessage());
        }
        context.getLogger().log("Received message: " + receivedText);
        context.getLogger().log("Bedrock AI Response: " + bedrockResponse);
        return createResponse(200, "{\"message\": \"AI: " + bedrockResponse + "\"}");
    }

    private String callAmazonBedrock(String userMessage, Context context) {
        try (BedrockRuntimeClient bedrockClient = BedrockRuntimeClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build()) {
            String modelInput = "{ \"inputText\": \"" + userMessage + "\"}";
            SdkBytes sdkBytes = SdkBytes.fromUtf8String(modelInput);

            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId("amazon.titan-text-premier-v1:0")
                    .body(sdkBytes)
                    .build();
            InvokeModelResponse response = bedrockClient.invokeModel(request);
            String responseBody =  new String(response.body().asByteArray(), StandardCharsets.UTF_8);
            JSONObject jsonResponse = new JSONObject(responseBody);
            return jsonResponse.getJSONArray("results")
                    .getJSONObject(0)
                    .getString("outputText");
        } catch (Exception e) {
            context.getLogger().log("Bedrock API call failed: " + e.getMessage());
            return "Error fetching AI response";
        }
    }

    private Map<String, Object> getCorsResponse() {
        return createResponse(200, "");
    }

    private Map<String, Object> createResponse(int statusCode, String body) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type");

        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", statusCode);
        response.put("headers", headers);
        response.put("body", body);

        return response;
    }
}

