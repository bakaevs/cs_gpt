package com.cattlescan.systemassistant.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cattlescan.systemassistant.entity.ChatMessage;
import com.cattlescan.systemassistant.model.ApiResponse;
import com.cattlescan.systemassistant.model.CustomerSupportAction;
import com.cattlescan.systemassistant.repository.ChatMessageRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Service
public class OpenAIAssistantClient {

    @Value("${openai.api.key}")
    private String apiKey;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
    	    .connectTimeout(30, TimeUnit.SECONDS)
    	    .writeTimeout(30, TimeUnit.SECONDS)
    	    .readTimeout(120, TimeUnit.SECONDS)
    	    .build();
    private final ChatMessageRepository chatMessageRepository;

    @Autowired
    private FunctionConfigService functionConfigService;

    @Autowired
    public OpenAIAssistantClient(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    /**
     * Send a message to OpenAI assistant and store conversation in DB.
     * Supports tool calls.
     */
 // --- In OpenAIAssistantClient ---
    public ApiResponse askAssistant(String userId, Long threadId, String userMessage, String context) {

        try {
            // ✅ 1 — Load conversation history ONLY for this thread
            List<ChatMessage> history =
                    chatMessageRepository.findByThreadIdOrderByCreatedAtAsc(threadId);

            JSONArray conversationHistory = buildConversationJSONArray(history);

            // ✅ 2 — Include context + user message as final message
            JSONObject userMsgObj = new JSONObject();
            userMsgObj.put("role", "user");
            userMsgObj.put("content", "Context:\n" + context + "\n\nQuestion:\n" + userMessage);
            conversationHistory.put(userMsgObj);

            // ✅ 3 — Build request payload
            JSONObject payload = new JSONObject();
            payload.put("model", "gpt-4o-mini");
            payload.put("messages", conversationHistory);

            // ✅ 4 — Add tools/functions if configured
            JSONArray functions = functionConfigService.getFunctions();
            JSONArray tools = buildToolsJSONArray(functions);
            payload.put("tools", tools);

            // ✅ 5 — Send request to OpenAI
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    payload.toString()
            );

            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {

                String responseStr = response.body().string();
                JSONObject responseJson = new JSONObject(responseStr);

                // ✅ 6 — Delegate handling (and saving messages)
                return handleOpenAIResponse(userId, threadId, responseJson);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse("Error calling Assistant API: " + e.getMessage());
        }
    }



    /**
     * Build conversation JSON array from DB messages.
     */
    private JSONArray buildConversationJSONArray(List<ChatMessage> history) {
        JSONArray conversation = new JSONArray();
        for (ChatMessage msg : history) {
            JSONObject obj = new JSONObject();
            obj.put("role", msg.getRole());
            obj.put("content", msg.getContent());
            conversation.put(obj);
        }
        return conversation;
    }

    /**
     * Wrap function definitions as tools for OpenAI.
     */
    private JSONArray buildToolsJSONArray(JSONArray functions) {
        JSONArray tools = new JSONArray();
        for (int i = 0; i < functions.length(); i++) {
            JSONObject fn = functions.getJSONObject(i);
            JSONObject tool = new JSONObject();
            tool.put("type", "function");
            tool.put("function", fn);
            tools.put(tool);
        }
        return tools;
    }

    /**
     * Handle OpenAI API response, including tool calls, and store assistant messages.
     */
    private ApiResponse handleOpenAIResponse(String userId, Long threadId, JSONObject responseJson) {

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setThreadId(threadId);
        apiResponse.setRaw(responseJson.toString());  // ✅ keep whole response for debugging

        JSONArray choices = responseJson.optJSONArray("choices");
        if (choices == null || choices.length() == 0) {
            apiResponse.setAnswer("Empty response from model.");
            return apiResponse;
        }

        JSONObject messageObj = choices.getJSONObject(0).optJSONObject("message");
        if (messageObj == null) {
            apiResponse.setAnswer("Empty response from model.");
            return apiResponse;
        }

        // ====================================================
        // ✅ 1) TOOL CALLS
        // ====================================================
        if (messageObj.has("tool_calls")) {

            JSONArray toolCalls = messageObj.getJSONArray("tool_calls");

            // For debugging or UI, store all tool calls in payload
            apiResponse.setPayload(toolCalls.toString());

            for (int i = 0; i < toolCalls.length(); i++) {

                JSONObject tool = toolCalls.getJSONObject(i);
                JSONObject fnObj = tool.getJSONObject("function");

                String functionName = fnObj.getString("name");
                JSONObject args = new JSONObject(fnObj.getString("arguments"));

                String alertType;

                switch (functionName) {
                    case "get_cow_calving_status":
                        alertType = "CIH";
                        break;

                    case "get_cow_heat_status":
                        alertType = "HEAT";
                        break;

                    case "check_animal_low_activity":
                        alertType = "LOW_ACT";
                        break;

                    default:
                        alertType = "UNKNOWN";
                        break;
                }


                int cowId = args.optInt("cowId", -1);
                int farmId = Integer.parseInt(userId.split("-")[1]);

                // ------------------------------
                // ✅ Parse date
                // ------------------------------
                String rawDate = args.optString("date", "");
                LocalDate parsedDate;

                try {
                    if (rawDate == null || rawDate.isBlank()) {
                        parsedDate = LocalDate.now();
                    } else {
                        parsedDate = LocalDate.parse(rawDate);

                        if (parsedDate.isBefore(LocalDate.now().minusMonths(6))) {
                            parsedDate = parsedDate.withYear(Year.now().getValue());
                        }
                    }
                } catch (DateTimeParseException e1) {
                    try {
                        String cleaned = rawDate.replaceAll("(?<=\\d)(st|nd|rd|th)", "").trim();
                        DateTimeFormatter fmt = new DateTimeFormatterBuilder()
                                .parseCaseInsensitive()
                                .appendPattern("MMM d")
                                .toFormatter(Locale.ENGLISH);
                        parsedDate = LocalDate.parse(cleaned, fmt)
                                .withYear(Year.now().getValue());
                    } catch (Exception e2) {
                        parsedDate = LocalDate.now();
                    }
                }

                String date = parsedDate.toString();
                String time = args.optString("time", "unknown");

                // ------------------------------
                // ✅ Run internal alert checker
                // ------------------------------
                String result = checkAlertStatus(cowId, farmId, date, time, alertType);

                apiResponse.setAnswer(result);
                apiResponse.setPayload(result);
                return apiResponse;
            }
        }

        // ====================================================
        // ✅ 2) NORMAL ASSISTANT MESSAGE
        // ====================================================
        String assistantText = messageObj.optString("content", "No response.");
        apiResponse.setAnswer(assistantText);

        return apiResponse;
    }



    private String checkAlertStatus(int cowId, int farmId, String date, String time, String alert) {
    	OkHttpClient client = createUnsafeClient();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            // --- Prepare action object ---
            CustomerSupportAction action = new CustomerSupportAction();
            action.setAction("INVESTIGATE_ALERT_FAILURE");

            Map<String, Object> params = new HashMap<>();
            params.put("farm_id", farmId);
            params.put("animal_id", cowId);
            params.put("expected_event", alert);
            params.put("date", date);
            params.put("time", time);
            action.setParameters(params);

            // --- Convert to JSON ---
            String jsonBody = objectMapper.writeValueAsString(action);

            // --- Build request ---
            RequestBody body = RequestBody.create(MediaType.parse("application/json"), jsonBody);
            Request request = new Request.Builder()
                    .url("https://localhost:8443/data/investigateissue")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("api-key", "6ba571f3-85a4-4252-ba19-02bf5daae2ce")
                    .post(body)
                    .build();

            // --- Execute call ---
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No response body";
                    throw new IOException("Unexpected response code: " + response.code() + " - " + errorBody);
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                System.out.println("Response: " + responseBody);

                // Optionally, parse response JSON to extract result field
                try {
                    JsonNode jsonNode = objectMapper.readTree(responseBody);
                    if (jsonNode.has("result")) {
                        return jsonNode.get("result").asText();
                    }
                } catch (Exception e) {
                    System.err.println("Warning: unable to parse JSON response: " + e.getMessage());
                }

                // Default success message if no result field
                return String.format("Cow #%d on farm #%d checked for calving on %s at %s — no issues detected.",
                        cowId, farmId, date, time);
            }

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            System.err.println("Error converting object to JSON: " + e.getMessage());
            return "Error: failed to build request payload.";

        } catch (java.net.ConnectException e) {
            System.err.println("Connection error: unable to reach server — " + e.getMessage());
            return "Error: unable to connect to server.";

        } catch (IOException e) {
            System.err.println("I/O error while calling API: " + e.getMessage());
            return "Error: API call failed — " + e.getMessage();

        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return "Unexpected error occurred: " + e.getMessage();
        }
    }


    /**
     * Reset conversation history for a user.
     */
    @Transactional
    public void resetConversation(String userId) {
        chatMessageRepository.deleteByUserId(userId);
    }
    
    private OkHttpClient createUnsafeClient() {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[]{}; }
                }
            };

            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);
            return builder.build();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
