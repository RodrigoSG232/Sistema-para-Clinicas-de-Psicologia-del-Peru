package pe.com.cpp.scheduling.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.com.cpp.scheduling.domain.Psychologist;

public interface PsychologistRepository extends JpaRepository<Psychologist, Integer> {
    List<Psychologist> findByActiveTrueOrderByLastNamesAscFirstNamesAsc();
    List<Psychologist> findBySpecialtyIdAndActiveTrueOrderByLastNamesAscFirstNamesAsc(Integer specialtyId);
}
