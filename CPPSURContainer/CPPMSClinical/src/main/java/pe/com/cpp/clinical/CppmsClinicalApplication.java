package pe.com.cpp.clinical;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;

@SpringBootApplication
public class CppmsClinicalApplication {
    public static void main(String[] args) { SpringApplication.run(CppmsClinicalApplication.class, args); }
    @Bean @LoadBalanced RestClient.Builder loadBalancedRestClientBuilder(@Value("${internal.api.key:}") String internalApiKey) {
        RestClient.Builder builder = RestClient.builder();
        if (!internalApiKey.isBlank()) builder.defaultHeader("X-Internal-Api-Key", internalApiKey);
        return builder;
    }
}
