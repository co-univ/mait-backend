package com.coniv.mait;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MaitApplication {

	public static void main(String[] args) {
		SpringApplication.run(MaitApplication.class, args);
	}

}
