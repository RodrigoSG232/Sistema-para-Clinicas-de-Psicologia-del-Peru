package pe.com.cpp.clinical.api;
import java.time.LocalDate; import org.springframework.format.annotation.DateTimeFormat; import org.springframework.web.bind.annotation.*; import pe.com.cpp.clinical.service.ProductivityService;
@RestController @RequestMapping("/api/clinical/productivity") public class ProductivityController {
 private final ProductivityService service; public ProductivityController(ProductivityService service){this.service=service;}
 @GetMapping public ProductivityReportResponse report(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to){return service.report(from,to);}
}
