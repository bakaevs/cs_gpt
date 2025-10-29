package com.cattlescan.systemassistant;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.cattlescan.systemassistant.model.ApiResponse;
import com.cattlescan.systemassistant.service.AssistantService;


@RunWith(SpringRunner.class)
@SpringBootTest
public class ProcessQuestionTest {
	
	
	public static final Logger logger = LoggerFactory.getLogger(ProcessQuestionTest.class);
	
    @Autowired
    private AssistantService assistantService;

	//@Test
	public void testConversation() {
		try {
			String question =  "Why Cow 1457 did not generate the alert on October 14th, 12 pm?";
			//String question = "How many hours of data needed to predict calving?";
			ApiResponse apiResponse = assistantService.processQuestion(question, "1", 2l);
			logger.info(apiResponse.getAnswer());
			
			
		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
	}
	
	
	@Test
	public void resetConversation() {
		try {
			assistantService.resetConversation("1");
			
		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
	}
	

}
