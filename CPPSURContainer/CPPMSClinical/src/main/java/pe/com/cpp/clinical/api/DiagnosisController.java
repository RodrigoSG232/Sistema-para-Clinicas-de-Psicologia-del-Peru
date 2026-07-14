package pe.com.cpp.clinical.api;
import java.net.URI; import java.util.List; import org.springframework.http.ResponseEntity; import org.springframework.web.bind.annotation.*; import jakarta.validation.Valid; import pe.com.cpp.clinical.service.DiagnosisService;
@RestController @RequestMapping("/api/clinical") public class DiagnosisController {
 private final DiagnosisService service; public DiagnosisController(DiagnosisService service){this.service=service;}
 @GetMapping("/diagnoses/cie10") public List<Cie10DiagnosisResponse> search(@RequestParam(defaultValue="") String q){return service.search(q);}
 @PostMapping("/processes/{processId}/hypotheses") public ResponseEntity<HypothesisResponse> register(@PathVariable Integer processId,@Valid @RequestBody HypothesisCreateRequest request){HypothesisResponse response=service.register(processId,request);return ResponseEntity.created(URI.create("/api/clinical/processes/"+processId+"/hypotheses/"+response.id())).body(response);}
 @GetMapping("/processes/{processId}/hypotheses") public List<HypothesisResponse> find(@PathVariable Integer processId){return service.findByProcess(processId);}
}
