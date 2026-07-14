package pe.com.cpp.queue.api;
import java.net.URI; import java.util.List; import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import pe.com.cpp.queue.domain.TicketStatus; import pe.com.cpp.queue.service.QueueService;
@RestController @RequestMapping("/api/queue") public class QueueController {
 private final QueueService service; public QueueController(QueueService service){this.service=service;}
 @PostMapping("/tickets") ResponseEntity<TicketResponse> issue(){TicketResponse t=service.issue();return ResponseEntity.created(URI.create("/api/queue/tickets/"+t.id())).body(t);}
 @PostMapping("/tickets/appointments/{appointmentId}") ResponseEntity<TicketResponse> issueForAppointment(@PathVariable Integer appointmentId,@RequestParam Integer patientId){TicketResponse t=service.issueForAppointment(appointmentId,patientId);return ResponseEntity.created(URI.create("/api/queue/tickets/"+t.id())).body(t);}
 @GetMapping("/tickets/appointments/{appointmentId}") TicketResponse byAppointment(@PathVariable Integer appointmentId){return service.byAppointment(appointmentId);}
 @GetMapping("/tickets/today") List<TicketResponse> today(){return service.todayTickets();}
 @GetMapping("/tickets") List<TicketResponse> byStatus(@RequestParam TicketStatus status){return service.byStatus(status);}
 @GetMapping("/tickets/current") ResponseEntity<TicketResponse> current(){TicketResponse t=service.current();return t==null?ResponseEntity.noContent().build():ResponseEntity.ok(t);}
 @PatchMapping("/tickets/{id}/call") TicketResponse call(@PathVariable Long id){return service.call(id);}
 @PatchMapping("/tickets/{id}/finish") TicketResponse finish(@PathVariable Long id){return service.finish(id);}
 @PatchMapping("/tickets/{id}/appointment") TicketResponse attach(@PathVariable Long id,@jakarta.validation.Valid @RequestBody TicketAppointmentRequest request){return service.attach(id,request.appointmentId(),request.patientId());}
 @GetMapping("/public/display") DisplayResponse display(){return service.display();}
}
