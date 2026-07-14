package pe.com.cpp.clinical.api;
import java.net.URI; import java.util.List; import org.springframework.http.ResponseEntity; import org.springframework.web.bind.annotation.*; import jakarta.validation.Valid; import pe.com.cpp.clinical.service.ClinicalService;
@RestController @RequestMapping("/api/clinical")
public class ClinicalController {
 private final ClinicalService service; public ClinicalController(ClinicalService service){this.service=service;}
 @PostMapping("/patients/{patientId}/processes") public ResponseEntity<ProcessResponse> start(@PathVariable Integer patientId,@Valid @RequestBody ProcessCreateRequest request){ProcessResponse r=service.startProcess(patientId,request);return ResponseEntity.created(URI.create("/api/clinical/patients/"+patientId+"/processes/active")).body(r);}
 @PostMapping("/external/patients/{patientId}/processes") public ResponseEntity<ProcessResponse> startExternal(@PathVariable Integer patientId,@Valid @RequestBody ExternalProcessCreateRequest request){ProcessResponse r=service.startExternalProcess(patientId,request);return ResponseEntity.ok(r);}
 @GetMapping("/patients/{patientId}/processes/active") public ProcessResponse active(@PathVariable Integer patientId){return service.activeProcess(patientId);}
 @PatchMapping("/processes/{processId}/phase") public ProcessResponse phase(@PathVariable Integer processId,@Valid @RequestBody PhaseUpdateRequest request){return service.changePhase(processId,request);}
 @PostMapping("/sessions") public ResponseEntity<SessionResponse> session(@Valid @RequestBody SessionCreateRequest request){SessionResponse r=service.registerSession(request);return ResponseEntity.created(URI.create("/api/clinical/sessions/"+r.id())).body(r);}
 @PostMapping("/external/sessions") public ResponseEntity<SessionResponse> externalSession(@Valid @RequestBody ExternalSessionCreateRequest request){return ResponseEntity.ok(service.registerExternalSession(request));}
 @GetMapping("/processes/{processId}/sessions") public List<SessionResponse> sessions(@PathVariable Integer processId){return service.sessionsForProcess(processId);}
 @GetMapping("/sessions/{sessionId}") public SessionResponse session(@PathVariable Integer sessionId){return service.session(sessionId);}
}
