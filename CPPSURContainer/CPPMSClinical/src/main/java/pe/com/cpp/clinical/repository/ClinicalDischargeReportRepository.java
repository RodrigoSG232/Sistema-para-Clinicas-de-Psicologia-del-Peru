package pe.com.cpp.clinical.repository;
import java.time.LocalDate; import java.util.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import pe.com.cpp.clinical.domain.ClinicalDischargeReport;
public interface ClinicalDischargeReportRepository extends JpaRepository<ClinicalDischargeReport,Integer>{
 Optional<ClinicalDischargeReport> findByProcessId(Integer processId); boolean existsByProcessId(Integer processId);
 @Query(value="""
  SELECT r.id AS "reportId", r.ticket_number AS "ticketNumber",
         p.patient_name AS "patientName", p.patient_history_number AS "patientHistoryNumber",
         p.psychologist_name AS "psychologistName", r.ticket_issued_at AS "ticketIssuedAt",
         r.discharged_at AS "dischargedAt",
         ROUND((EXTRACT(EPOCH FROM (r.discharged_at - r.ticket_issued_at)) / 3600.0)::numeric, 2)::double precision AS "efficiencyHours"
    FROM clinical_discharge_report r
    JOIN therapeutic_process p ON p.id = r.therapeutic_process_id
   WHERE r.ticket_issued_at IS NOT NULL
     AND CAST(r.discharged_at AS DATE) BETWEEN :fromDate AND :toDate
   ORDER BY r.discharged_at ASC, r.id ASC
  """,nativeQuery=true)
 List<ProductivityCaseProjection> findProductivityCases(@Param("fromDate") LocalDate fromDate,@Param("toDate") LocalDate toDate);
}
