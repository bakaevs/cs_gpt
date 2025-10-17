package com.cattlescan.systemassistant.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

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

	@RequestMapping(value = { "/" }, method = RequestMethod.GET)
	public ModelAndView home(HttpSession session, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("chat");
		
		return modelAndView;
	}
	
	
    @PostMapping("/process")
    public ResponseEntity<?> processQuestion(@RequestBody Map<String, String> payload) {
        try {
            String question = payload.get("question");
            String userId = payload.get("userId");
            ApiResponse response = assistantService.processQuestion(question, userId);

            // You can return toolCalls separately if needed
            Map<String, Object> result = new HashMap<>();
            result.put("answer", response.getAnswer());
            //result.put("toolCalls", response.getToolCalls()); // modify ApiResponse to include toolCalls

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing question: " + e.getMessage());
        }
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
