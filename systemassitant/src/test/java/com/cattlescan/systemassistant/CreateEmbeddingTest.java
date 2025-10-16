package com.cattlescan.systemassistant;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.cattlescan.systemassistant.embedding.SystemEmbedding;


@RunWith(SpringRunner.class)
@SpringBootTest
public class CreateEmbeddingTest {

	
	public static final Logger logger = LoggerFactory.getLogger(CreateEmbeddingTest.class);
	
	@Autowired
	private SystemEmbedding systemEmbedding;
	
	@Test
	public void embedPdfs() {		
		try {
			systemEmbedding.processPdfFile(new File("C:\\Users\\serge\\OneDrive\\Documents\\System description\\Calving prediction.pdf"));
		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
	}

}
