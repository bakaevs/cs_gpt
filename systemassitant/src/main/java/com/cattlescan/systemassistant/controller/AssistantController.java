package com.cattlescan.systemassistant.controller;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import com.cattlescan.systemassistant.model.ApiResponse;
import com.cattlescan.systemassistant.service.AssistantService;
import com.cattlescan.systemassistant.util.MarkdownConverter;

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

    /* ---------------------------------------------------------
       ✅ 1. Get list of threads for user
       --------------------------------------------------------- */
    @GetMapping("/threads")
    public ResponseEntity<?> getThreads(@RequestParam String userId) {

        return ResponseEntity.ok(assistantService.getThreads(userId));
    }


    /* ---------------------------------------------------------
       ✅ 2. Load messages for a thread
       --------------------------------------------------------- */
    @GetMapping("/thread/messages")
    public ResponseEntity<?> getThreadMessages(@RequestParam Long threadId) {

        return ResponseEntity.ok(assistantService.getMessagesForThread(threadId));
    }


    /* ---------------------------------------------------------
       ✅ 3. Create a new thread for a user
       --------------------------------------------------------- */
    @PostMapping("/thread/create")
    public Long createThread(@RequestParam String userId, @RequestParam String name) {
        return assistantService.createThread(userId, name);
    }


    /* ---------------------------------------------------------
       ✅ 4. Rename a thread
       --------------------------------------------------------- */
    @PostMapping("/thread/rename")
    public ResponseEntity<?> renameThread(
            @RequestParam Long threadId,
            @RequestParam String name) {

        assistantService.renameThread(threadId, name);
        return ResponseEntity.ok("Renamed");
    }


    /* ---------------------------------------------------------
       ✅ 5. Send a message in thread
       --------------------------------------------------------- */
    @PostMapping("/thread/send")
    public ResponseEntity<?> sendMessageInThread(@RequestBody Map<String, Object> body) throws IOException {

        Long threadId = Long.parseLong(body.get("threadId").toString());
        String userId = body.get("userId").toString();
        String question = body.get("question").toString();

        ApiResponse response = assistantService.processThreadQuestion(threadId, question, userId);
        response.setAnswer(MarkdownConverter.toHtml(response.getAnswer()));

        return ResponseEntity.ok(response);
    }
}
