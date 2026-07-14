package pe.com.cpp.clinical.api;
import org.springframework.web.bind.annotation.*; import pe.com.cpp.clinical.service.DischargeService;
@RestController @RequestMapping("/api/clinical/patients/{patientId}") public class DischargeLookupController {
 private final DischargeService service; public DischargeLookupController(DischargeService service){this.service=service;}
 @GetMapping("/discharge-report/latest") public DischargeReportResponse latest(@PathVariable Integer patientId){return service.latestReportForPatient(patientId);}
}
