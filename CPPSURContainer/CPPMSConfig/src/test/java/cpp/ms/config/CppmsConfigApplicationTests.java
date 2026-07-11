package cpp.ms.config;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CppmsConfigApplicationTests {
	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void servesGatewayConfigurationFromNativeRepository() {
		ResponseEntity<JsonNode> response = restTemplate.getForEntity(
				"/cpp-api-gateway/default", JsonNode.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().path("name").asText()).isEqualTo("cpp-api-gateway");
	}
}
