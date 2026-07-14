package pe.com.cpp.clinical.repository;
import java.util.List; import org.springframework.data.domain.Pageable; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import pe.com.cpp.clinical.domain.Cie10Diagnosis;
public interface Cie10DiagnosisRepository extends JpaRepository<Cie10Diagnosis,String>{
 @Query("select d from Cie10Diagnosis d where d.active=true and (:q='' or lower(d.code) like lower(concat('%',:q,'%')) or lower(d.description) like lower(concat('%',:q,'%'))) order by d.code") List<Cie10Diagnosis> search(@Param("q")String query,Pageable pageable);
 List<Cie10Diagnosis> findByCodeInAndActiveTrue(List<String> codes);
}
