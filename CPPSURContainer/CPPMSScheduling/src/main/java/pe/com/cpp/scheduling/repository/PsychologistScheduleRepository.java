package pe.com.cpp.scheduling.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.com.cpp.scheduling.domain.PsychologistSchedule;

public interface PsychologistScheduleRepository extends JpaRepository<PsychologistSchedule, Integer> {
    Optional<PsychologistSchedule> findByPsychologistIdAndDayOfWeekAndActiveTrue(
            Integer psychologistId, Integer dayOfWeek);
}
