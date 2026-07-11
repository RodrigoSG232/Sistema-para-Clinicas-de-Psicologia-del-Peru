package pe.com.cpp.scheduling;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@SpringBootApplication
public class CppmsSchedulingApplication {

    public static void main(String[] args) {
        SpringApplication.run(CppmsSchedulingApplication.class, args);
    }

    @Bean
    Clock applicationClock() {
        return Clock.system(ZoneId.of("America/Lima"));
    }

    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }
}
