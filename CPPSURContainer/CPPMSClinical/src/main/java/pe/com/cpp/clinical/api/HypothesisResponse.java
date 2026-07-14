package pe.com.cpp.clinical.api;
import java.time.LocalDateTime; import java.util.List;
public record HypothesisResponse(Integer id,Integer processId,Integer sessionId,Integer phase,String hypothesis,String therapeuticPlan,List<Cie10DiagnosisResponse> diagnoses,String registeredBy,LocalDateTime registeredAt){}
