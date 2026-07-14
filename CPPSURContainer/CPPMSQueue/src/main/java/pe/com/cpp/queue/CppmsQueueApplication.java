package pe.com.cpp.queue;
import java.time.Clock; import java.time.ZoneId; import org.springframework.boot.SpringApplication; import org.springframework.boot.autoconfigure.SpringBootApplication; import org.springframework.context.annotation.Bean;
@SpringBootApplication public class CppmsQueueApplication {
 public static void main(String[] args){SpringApplication.run(CppmsQueueApplication.class,args);}
 @Bean Clock limaClock(){return Clock.system(ZoneId.of("America/Lima"));}
}
