package pe.com.cpp.scheduling.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.com.cpp.scheduling.domain.Appointment;
import pe.com.cpp.scheduling.domain.AppointmentStatus;

public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {

    boolean existsByPsychologistIdAndAppointmentDateAndAppointmentTimeAndStatusNotIn(
            Integer psychologistId, LocalDate appointmentDate, LocalTime appointmentTime,
            Collection<AppointmentStatus> excludedStatuses);

    @Query("""
            select a.appointmentTime from Appointment a
            where a.psychologist.id = :psychologistId
              and a.appointmentDate = :date
              and a.status not in :excludedStatuses
            """)
    List<LocalTime> findOccupiedTimes(@Param("psychologistId") Integer psychologistId,
            @Param("date") LocalDate date,
            @Param("excludedStatuses") Collection<AppointmentStatus> excludedStatuses);

    List<Appointment> findByPatientIdOrderByCreatedAtDesc(Integer patientId);

    List<Appointment> findByAppointmentDateBetweenOrderByAppointmentDateAscAppointmentTimeAsc(
            LocalDate start, LocalDate end);

    List<Appointment> findByPsychologistIdAndAppointmentDateOrderByAppointmentTimeAsc(
            Integer psychologistId, LocalDate appointmentDate);

    Optional<Appointment> findByIdAndPsychologistId(Integer id, Integer psychologistId);
}
