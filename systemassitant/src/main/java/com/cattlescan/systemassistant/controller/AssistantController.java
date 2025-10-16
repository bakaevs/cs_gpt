package com.cattlescan.systemassistant.controller;

import java.io.IOException;

import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.cattlescan.systemassistant.model.ApiResponse;
import com.cattlescan.systemassistant.service.AssistantService;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantService assistantService;

    @Autowired
    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    /**
     * Sends a message from a user to the assistant and returns the response.
     *
     * Example JSON payload:
     * {
     *     "userId": "user123",
     *     "message": "Why did cow #76 not calve yesterday?",
     *     "functions": [ ... ] // JSON array of functions
     * }
     * @throws IOException 
     */
    @PostMapping("/message")
    public ApiResponse sendMessage(
            @RequestParam String userId,
            @RequestParam String message
    ) throws IOException {
        return assistantService.processQuestion(userId, message);
    }

    /**
     * Resets conversation history for a user.
     */
    @PostMapping("/reset")
    public ApiResponse resetConversation(@RequestParam String userId) {
        assistantService.resetConversation(userId);
        return new ApiResponse("Conversation history reset for user: " + userId);
    }
}
