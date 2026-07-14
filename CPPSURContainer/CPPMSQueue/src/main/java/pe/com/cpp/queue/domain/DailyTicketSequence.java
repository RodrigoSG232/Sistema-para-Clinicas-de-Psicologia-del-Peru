package pe.com.cpp.queue.domain;
import java.time.LocalDate; import jakarta.persistence.*;
@Entity @Table(name="daily_ticket_sequence") public class DailyTicketSequence {
 @Id @Column(name="operational_date") private LocalDate operationalDate;
 @Column(name="next_value",nullable=false) private int nextValue;
 @Column(name="active_ticket_id") private Long activeTicketId;
 protected DailyTicketSequence(){} public DailyTicketSequence(LocalDate date){operationalDate=date;nextValue=1;}
 public int takeNext(){return nextValue++;} public void activate(Long id){if(activeTicketId!=null)throw new IllegalStateException("Ya existe un ticket en atención");activeTicketId=id;} public void clear(Long id){if(!id.equals(activeTicketId))throw new IllegalStateException("El ticket no es el turno actualmente atendido");activeTicketId=null;}
 public LocalDate getOperationalDate(){return operationalDate;} public int getNextValue(){return nextValue;} public Long getActiveTicketId(){return activeTicketId;}
}
