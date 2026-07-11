package pe.com.cpp.patient.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.com.cpp.patient.domain.Patient;

public interface PatientRepository extends JpaRepository<Patient, Integer> {

    boolean existsByDni(String dni);

    Optional<Patient> findByDni(String dni);

    @Query("""
            select p from Patient p
            where lower(p.firstNames) like lower(concat('%', :query, '%'))
               or lower(p.lastNames) like lower(concat('%', :query, '%'))
               or p.dni like concat('%', :query, '%')
            order by p.lastNames, p.firstNames
            """)
    List<Patient> search(@Param("query") String query);
}
