package com.oncetold.oncetold;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class OncetoldApplication {

    public static void main(String[] args) {
        SpringApplication.run(OncetoldApplication.class, args);
    }

}