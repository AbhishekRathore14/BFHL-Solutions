package com.bfhl;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BfhlSqlSolutionApplication implements CommandLineRunner {

    private final WebhookService webhookService;

    public BfhlSqlSolutionApplication(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    public static void main(String[] args) {
        SpringApplication.run(BfhlSqlSolutionApplication.class, args);
    }

    @Override
    public void run(String... args) {
        webhookService.process();
    }
}