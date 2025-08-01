package com.example.hotelwebhook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HotelWebhookApplication {

    public static void main(String[] args) {
        SpringApplication.run(HotelWebhookApplication.class, args);
    }

}
