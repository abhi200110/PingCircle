package com.pingcircle.pingCircle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PingCircleApplication {

	public static void main(String[] args) {
		SpringApplication.run(PingCircleApplication.class, args);
	}

}
