package pe.com.cpp.patient;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CppmsPatientApplication {

    public static void main(String[] args) {
        SpringApplication.run(CppmsPatientApplication.class, args);
    }

    @Bean
    Clock applicationClock() {
        return Clock.system(ZoneId.of("America/Lima"));
    }
}
