package com.cattlescan.systemassistant.service;

import java.time.LocalDate;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cattlescan.systemassistant.entity.ChatMessage;
import com.cattlescan.systemassistant.model.ApiResponse;
import com.cattlescan.systemassistant.repository.ChatMessageRepository;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Service
public class OpenAIAssistantClient {

    @Value("${openai.api.key}")
    private String apiKey;

    private final OkHttpClient httpClient = new OkHttpClient();
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
    public ApiResponse askAssistant(String userId, String userMessage, String context) {
        try {
            // --- Load conversation history from DB ---
            List<ChatMessage> history = chatMessageRepository.findByUserIdOrderByTimestampAsc(userId);
            JSONArray conversationHistory = buildConversationJSONArray(history);

            // --- Add user message including context ---
            JSONObject userMsgObj = new JSONObject();
            userMsgObj.put("role", "user");
            userMsgObj.put("content", "Context:\n" + context + "\n\nQuestion:\n" + userMessage);
            conversationHistory.put(userMsgObj);

            // --- Save user message in DB ---
            //chatMessageRepository.save(new ChatMessage(userId, "user", userMessage));

            // --- Build JSON payload for OpenAI API ---
            JSONObject payload = new JSONObject();
            payload.put("model", "gpt-4o-mini");
            payload.put("messages", conversationHistory);

            // --- Add function/tool definitions ---
            JSONArray functions = functionConfigService.getFunctions();
            JSONArray tools = buildToolsJSONArray(functions);
            payload.put("tools", tools);

            // --- Call OpenAI API ---
            RequestBody body = RequestBody.create(MediaType.parse("application/json"), payload.toString());
            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseStr = response.body().string();
                JSONObject responseJson = new JSONObject(responseStr);

                // --- Handle OpenAI response (including function calls) ---
                return handleOpenAIResponse(userId, responseJson);
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
    private ApiResponse handleOpenAIResponse(String userId, JSONObject responseJson) {
        JSONArray choices = responseJson.optJSONArray("choices");
        if (choices != null && choices.length() > 0) {
            JSONObject messageObj = choices.getJSONObject(0).optJSONObject("message");
            if (messageObj != null) {
                String assistantContent = messageObj.optString("content", "No answer.");

                // Save assistant message in DB
                chatMessageRepository.save(new ChatMessage(userId, "assistant", assistantContent));

                // Process tool calls if any
                if (messageObj.has("tool_calls")) {
                    JSONArray toolCalls = messageObj.getJSONArray("tool_calls");
                    for (int i = 0; i < toolCalls.length(); i++) {
                        JSONObject tool = toolCalls.getJSONObject(i);
                        JSONObject fnObj = tool.getJSONObject("function");
                        String name = fnObj.getString("name");
                        JSONObject args = new JSONObject(fnObj.getString("arguments"));

                        if ("get_cow_calving_status".equals(name)) {
                            int cowId = args.optInt("cowId", -1);
                            int farmId = args.optInt("farmId", -1);
                            String date = args.optString("date", LocalDate.now().toString());
                            String time = args.optString("time", "unknown");

                            String result = getCowCalvingStatus(cowId, farmId, date, time);

                            // Save tool response in DB
                            //chatMessageRepository.save(new ChatMessage(userId, "assistant", result));
                            return new ApiResponse(result);
                        }
                    }
                }

                return new ApiResponse(assistantContent);
            }
        }
        return new ApiResponse("Empty response from model.");
    }

    /**
     * Dummy function to return cow calving status.
     * Replace this with actual API or database logic.
     */
    private String getCowCalvingStatus(int cowId, int farmId, String date, String time) {
        return String.format("Cow #%d did not calve on %s at %s due to low activity (Farm #%d).",
                cowId, date, time, farmId);
    }

    /**
     * Reset conversation history for a user.
     */
    @Transactional
    public void resetConversation(String userId) {
        chatMessageRepository.deleteByUserId(userId);
    }
}
