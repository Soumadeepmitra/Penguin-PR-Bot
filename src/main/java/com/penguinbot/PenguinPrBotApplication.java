package com.penguinbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class PenguinPrBotApplication {
    private static final Logger log = LoggerFactory.getLogger(PenguinPrBotApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(PenguinPrBotApplication.class, args);
        log.info("🐧 Penguin PR Bot started successfully!");
    }
}
