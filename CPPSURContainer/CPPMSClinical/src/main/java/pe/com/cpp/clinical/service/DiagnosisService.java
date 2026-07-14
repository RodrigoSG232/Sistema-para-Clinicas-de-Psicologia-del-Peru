package pe.com.cpp.clinical.service;
import java.time.Clock; import java.time.LocalDateTime; import java.util.*; import org.springframework.data.domain.PageRequest; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import pe.com.cpp.clinical.api.*; import pe.com.cpp.clinical.domain.*; import pe.com.cpp.clinical.exception.*; import pe.com.cpp.clinical.repository.*;
@Service public class DiagnosisService {
 private final Cie10DiagnosisRepository catalog; private final ClinicalHypothesisRepository hypotheses; private final HypothesisDiagnosisRepository assignments; private final TherapeuticProcessRepository processes; private final ClinicalSessionRepository sessions; private final Clock clock;
 @org.springframework.beans.factory.annotation.Autowired public DiagnosisService(Cie10DiagnosisRepository catalog,ClinicalHypothesisRepository hypotheses,HypothesisDiagnosisRepository assignments,TherapeuticProcessRepository processes,ClinicalSessionRepository sessions){this(catalog,hypotheses,assignments,processes,sessions,Clock.systemDefaultZone());}
 DiagnosisService(Cie10DiagnosisRepository catalog,ClinicalHypothesisRepository hypotheses,HypothesisDiagnosisRepository assignments,TherapeuticProcessRepository processes,ClinicalSessionRepository sessions,Clock clock){this.catalog=catalog;this.hypotheses=hypotheses;this.assignments=assignments;this.processes=processes;this.sessions=sessions;this.clock=clock;}
 @Transactional(readOnly=true) public List<Cie10DiagnosisResponse> search(String query){String normalized=query==null?"":query.trim();return catalog.search(normalized,PageRequest.of(0,20)).stream().map(this::toDiagnosis).toList();}
 @Transactional public HypothesisResponse register(Integer processId,HypothesisCreateRequest request){
  TherapeuticProcess process=processes.findById(processId).orElseThrow(()->new ResourceNotFoundException("Proceso terapéutico no encontrado"));
  if(!process.isActive())throw new BusinessRuleException("El proceso terapéutico está inactivo");
  if(process.getCurrentPhase()!=2)throw new BusinessRuleException("La hipótesis y el plan solo pueden registrarse durante la fase 2");
  ClinicalSession session=null;
  if(request.sessionId()!=null){session=sessions.findById(request.sessionId()).orElseThrow(()->new ResourceNotFoundException("Sesión clínica no encontrada"));if(!session.getProcess().getId().equals(processId))throw new BusinessRuleException("La sesión no pertenece al proceso terapéutico");if(session.getSessionPhase()!=2)throw new BusinessRuleException("La sesión vinculada debe pertenecer a la fase 2");}
  List<String> codes=request.diagnosisCodes().stream().map(String::trim).map(String::toUpperCase).distinct().toList();
  List<Cie10Diagnosis> diagnoses=catalog.findByCodeInAndActiveTrue(codes);
  if(diagnoses.size()!=codes.size()){Set<String> found=new HashSet<>();diagnoses.forEach(d->found.add(d.getCode()));String missing=codes.stream().filter(c->!found.contains(c)).findFirst().orElse("desconocido");throw new BusinessRuleException("Código CIE-10 no válido o inactivo: "+missing);}
  ClinicalHypothesis saved=hypotheses.save(new ClinicalHypothesis(process,session,request.hypothesis().trim(),request.therapeuticPlan().trim(),request.registeredBy().trim(),LocalDateTime.now(clock)));
  List<HypothesisDiagnosis> links=diagnoses.stream().map(d->new HypothesisDiagnosis(saved,d)).toList();assignments.saveAll(links);
  return toResponse(saved,links);
 }
 @Transactional(readOnly=true) public List<HypothesisResponse> findByProcess(Integer processId){if(!processes.existsById(processId))throw new ResourceNotFoundException("Proceso terapéutico no encontrado");return hypotheses.findByProcessIdOrderByRegisteredAtDesc(processId).stream().map(h->toResponse(h,assignments.findForHypothesis(h.getId()))).toList();}
 private Cie10DiagnosisResponse toDiagnosis(Cie10Diagnosis d){return new Cie10DiagnosisResponse(d.getCode(),d.getDescription());}
 private HypothesisResponse toResponse(ClinicalHypothesis h,List<HypothesisDiagnosis> links){return new HypothesisResponse(h.getId(),h.getProcess().getId(),h.getSession()==null?null:h.getSession().getId(),h.getPhase(),h.getHypothesis(),h.getTherapeuticPlan(),links.stream().map(a->toDiagnosis(a.getDiagnosis())).toList(),h.getRegisteredBy(),h.getRegisteredAt());}
}
