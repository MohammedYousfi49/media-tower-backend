package com.mediatower.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
@EnableAsync
@SpringBootApplication
@EnableScheduling // << AJOUTEZ CETTE ANNOTATION

@EnableJpaRepositories(basePackages = "com.mediatower.backend.repository") // S'assurer que les repositories sont scannés
public class MediatowerBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MediatowerBackendApplication.class, args);
	}
}