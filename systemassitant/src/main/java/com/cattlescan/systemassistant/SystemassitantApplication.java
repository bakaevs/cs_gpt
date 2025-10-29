package com.cattlescan.systemassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class SystemassitantApplication {

	public static void main(String[] args) {
		SpringApplication.run(SystemassitantApplication.class, args);
	}

}
