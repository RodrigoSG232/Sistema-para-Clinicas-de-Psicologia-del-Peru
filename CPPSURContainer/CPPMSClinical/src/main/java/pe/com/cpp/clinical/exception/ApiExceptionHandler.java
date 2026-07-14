package pe.com.cpp.clinical.exception;
import java.util.*; import org.springframework.dao.DataIntegrityViolationException; import org.springframework.http.*; import org.springframework.web.bind.MethodArgumentNotValidException; import org.springframework.web.bind.annotation.*;
@RestControllerAdvice public class ApiExceptionHandler {
 @ExceptionHandler(ResourceNotFoundException.class) ResponseEntity<Map<String,String>> notFound(ResourceNotFoundException e){return ResponseEntity.status(404).body(Map.of("error",e.getMessage()));}
 @ExceptionHandler({DuplicateClinicalRecordException.class,DataIntegrityViolationException.class}) ResponseEntity<Map<String,String>> conflict(RuntimeException e){return ResponseEntity.status(409).body(Map.of("error",e instanceof DuplicateClinicalRecordException?e.getMessage():"El registro clínico ya existe"));}
 @ExceptionHandler(BusinessRuleException.class) ResponseEntity<Map<String,String>> business(BusinessRuleException e){return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));}
 @ExceptionHandler(ExternalServiceException.class) ResponseEntity<Map<String,String>> external(ExternalServiceException e){return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error",e.getMessage()));}
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<Map<String,String>> validation(MethodArgumentNotValidException e){Map<String,String> errors=new LinkedHashMap<>();e.getBindingResult().getFieldErrors().forEach(x->errors.putIfAbsent(x.getField(),x.getDefaultMessage()));return ResponseEntity.badRequest().body(errors);}
}
