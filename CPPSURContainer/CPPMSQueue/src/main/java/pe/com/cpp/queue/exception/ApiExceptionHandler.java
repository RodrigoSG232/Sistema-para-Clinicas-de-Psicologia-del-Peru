package pe.com.cpp.queue.exception;
import java.util.Map; import org.springframework.dao.*; import org.springframework.http.*; import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException; import org.springframework.web.bind.annotation.*;
@RestControllerAdvice public class ApiExceptionHandler {
 @ExceptionHandler(ResourceNotFoundException.class) ResponseEntity<Map<String,String>> notFound(ResourceNotFoundException e){return ResponseEntity.status(404).body(Map.of("error",e.getMessage()));}
 @ExceptionHandler({QueueConflictException.class,CannotAcquireLockException.class,PessimisticLockingFailureException.class}) ResponseEntity<Map<String,String>> conflict(RuntimeException e){return ResponseEntity.status(409).body(Map.of("error",e instanceof QueueConflictException?e.getMessage():"La cola fue modificada concurrentemente; intente nuevamente"));}
 @ExceptionHandler(MethodArgumentTypeMismatchException.class) ResponseEntity<Map<String,String>> invalid(MethodArgumentTypeMismatchException e){return ResponseEntity.badRequest().body(Map.of("error","Estado de ticket inválido"));}
}
