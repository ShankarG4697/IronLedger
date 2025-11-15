package com.ironledger.wallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class IronledgerApplication {

	public static void main(String[] args) {
		SpringApplication.run(IronledgerApplication.class, args);
	}

}
