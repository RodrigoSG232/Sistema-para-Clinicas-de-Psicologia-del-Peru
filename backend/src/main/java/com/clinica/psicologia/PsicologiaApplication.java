package com.clinica.psicologia;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PsicologiaApplication {
    public static void main(String[] args) {
        System.out.println("HASH: " + new BCryptPasswordEncoder().encode("123"));
        SpringApplication.run(PsicologiaApplication.class, args);
    }
}
