package pe.com.cpp.clinical.api;
import java.net.URI; import org.springframework.http.ResponseEntity; import org.springframework.web.bind.annotation.*; import jakarta.validation.Valid; import pe.com.cpp.clinical.service.DischargeService;
@RestController @RequestMapping("/api/clinical/processes/{processId}") public class DischargeController {
 private final DischargeService service; public DischargeController(DischargeService service){this.service=service;}
 @PostMapping("/discharge") public ResponseEntity<DischargeReportResponse> discharge(@PathVariable Integer processId,@Valid @RequestBody DischargeRequest request){DischargeReportResponse response=service.discharge(processId,request);return ResponseEntity.created(URI.create("/api/clinical/processes/"+processId+"/discharge-report")).body(response);}
 @GetMapping("/discharge-report") public DischargeReportResponse report(@PathVariable Integer processId){return service.report(processId);}
}
