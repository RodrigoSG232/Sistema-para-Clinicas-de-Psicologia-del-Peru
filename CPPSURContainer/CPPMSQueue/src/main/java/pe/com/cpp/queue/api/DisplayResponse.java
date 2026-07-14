package pe.com.cpp.queue.api;
import java.util.List;
public record DisplayResponse(TicketResponse current,List<TicketResponse> next){}
