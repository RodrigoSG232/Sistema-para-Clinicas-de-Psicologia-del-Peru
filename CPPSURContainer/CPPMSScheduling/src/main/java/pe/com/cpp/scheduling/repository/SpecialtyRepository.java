package pe.com.cpp.scheduling.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.com.cpp.scheduling.domain.Specialty;

public interface SpecialtyRepository extends JpaRepository<Specialty, Integer> {
    List<Specialty> findByActiveTrueOrderByName();
}
