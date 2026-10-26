package com.brainserve.oysmail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class OysMailApplication {
    public static void main(String[] args) {
        SpringApplication.run(OysMailApplication.class, args);
    }
}
