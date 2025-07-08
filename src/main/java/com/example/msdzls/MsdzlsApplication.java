package com.example.msdzls;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.example.msdzls.repository")
public class MsdzlsApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsdzlsApplication.class, args);
    }

}
