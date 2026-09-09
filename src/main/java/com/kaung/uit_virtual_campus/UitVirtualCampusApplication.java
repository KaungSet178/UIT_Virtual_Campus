package com.kaung.uit_virtual_campus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UitVirtualCampusApplication {

    public static void main(String[] args) {
        SpringApplication.run(UitVirtualCampusApplication.class, args);
    }

}
