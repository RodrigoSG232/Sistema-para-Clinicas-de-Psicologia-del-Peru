package pe.com.cpp.clinical.service;

import java.time.Clock; import java.time.LocalDate; import java.time.LocalDateTime; import java.util.List;
import org.springframework.dao.DataIntegrityViolationException; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import pe.com.cpp.clinical.api.*; import pe.com.cpp.clinical.client.*; import pe.com.cpp.clinical.domain.*; import pe.com.cpp.clinical.exception.*; import pe.com.cpp.clinical.repository.*;

@Service
public class ClinicalService {
 private final TherapeuticProcessRepository processes; private final InitialInterviewRepository interviews; private final ClinicalSessionRepository sessions; private final PatientClient patients; private final SchedulingClient scheduling; private final Clock clock;
 @org.springframework.beans.factory.annotation.Autowired public ClinicalService(TherapeuticProcessRepository processes,InitialInterviewRepository interviews,ClinicalSessionRepository sessions,PatientClient patients,SchedulingClient scheduling){this(processes,interviews,sessions,patients,scheduling,Clock.systemDefaultZone());}
 ClinicalService(TherapeuticProcessRepository processes,InitialInterviewRepository interviews,ClinicalSessionRepository sessions,PatientClient patients,SchedulingClient scheduling,Clock clock){this.processes=processes;this.interviews=interviews;this.sessions=sessions;this.patients=patients;this.scheduling=scheduling;this.clock=clock;}

 @Transactional
 public ProcessResponse startProcess(Integer patientId,ProcessCreateRequest request){
  if(processes.findByPatientIdAndActiveTrue(patientId).isPresent())throw new DuplicateClinicalRecordException("El paciente ya tiene un proceso terapéutico activo");
  PatientSnapshot patient=patients.findById(patientId); if(!Boolean.TRUE.equals(patient.activo()))throw new BusinessRuleException("El paciente está inactivo");
  AppointmentSnapshot appointment=scheduling.findById(request.appointmentId()); validateAppointment(appointment,patientId,request.psychologistId(),"EN_CONSULTA");
  TherapeuticProcess process=new TherapeuticProcess(patient.id(),patient.nombreCompleto(),patient.dni(),patient.numeroHistoria(),appointment.psicologoId(),appointment.psicologo(),LocalDate.now(clock),request.observaciones(),request.appointmentId());
  try{process=processes.save(process);InitialInterviewRequest input=request.entrevista();InitialInterview interview=interviews.save(new InitialInterview(process,input.motivoConsulta(),input.antecedentesPersonales(),input.antecedentesFamiliares(),input.observacionesIniciales(),LocalDateTime.now(clock)));return toResponse(process,interview);}catch(DataIntegrityViolationException e){throw new DuplicateClinicalRecordException("El paciente ya tiene un proceso terapéutico activo");}
 }
 @Transactional
 public ProcessResponse startExternalProcess(Integer patientId,ExternalProcessCreateRequest request){
  var active=processes.findByPatientIdAndActiveTrue(patientId);
  if(active.isPresent())return toResponse(active.get(),interviews.findByProcessId(active.get().getId()).orElse(null));
  var latest=processes.findFirstByPatientIdOrderByStartDateDescIdDesc(patientId);if(latest.isPresent()&&!latest.get().isActive()&&"ALTA".equals(latest.get().getStatus()))throw new BusinessRuleException("La historia clínica está cerrada por alta médica");
  TherapeuticProcess process=new TherapeuticProcess(patientId,request.patientName().trim(),request.patientDni().trim(),request.patientHistoryNumber().trim(),request.psychologistId(),request.psychologistName().trim(),LocalDate.now(clock),request.observaciones(),request.appointmentId());
  try{process=processes.save(process);InitialInterviewRequest input=request.entrevista();LocalDateTime now=LocalDateTime.now(clock);InitialInterview interview=interviews.save(new InitialInterview(process,input.motivoConsulta(),input.antecedentesPersonales(),input.antecedentesFamiliares(),input.observacionesIniciales(),now));ensureExternalInitialSession(process,request,now);return toResponse(process,interview);}catch(DataIntegrityViolationException e){TherapeuticProcess existing=processes.findByPatientIdAndActiveTrue(patientId).orElseThrow(()->new DuplicateClinicalRecordException("El paciente ya tiene un proceso terapéutico activo"));return toResponse(existing,interviews.findByProcessId(existing.getId()).orElse(null));}
 }
 @Transactional(readOnly=true) public ProcessResponse activeProcess(Integer patientId){TherapeuticProcess p=processes.findByPatientIdAndActiveTrue(patientId).orElseThrow(()->new ResourceNotFoundException("El paciente no tiene un proceso terapéutico activo"));return toResponse(p,interviews.findByProcessId(p.getId()).orElse(null));}
 @Transactional public ProcessResponse changePhase(Integer id,PhaseUpdateRequest request){TherapeuticProcess p=findProcess(id);if(!p.isActive())throw new BusinessRuleException("La historia clínica está cerrada por alta");p.changePhase(request.faseActual(),request.observaciones());return toResponse(processes.save(p),interviews.findByProcessId(id).orElse(null));}
 @Transactional
 public SessionResponse registerSession(SessionCreateRequest request){
  TherapeuticProcess process=findProcess(request.processId()); if(!process.isActive())throw new BusinessRuleException("El proceso terapéutico está inactivo");
  if(sessions.existsByAppointmentId(request.appointmentId()))throw new DuplicateClinicalRecordException("La cita ya tiene una sesión clínica");
  AppointmentSnapshot appointment=scheduling.findById(request.appointmentId());validateAppointment(appointment,process.getPatientId(),process.getPsychologistId(),"EN_CONSULTA");
  int phase=request.sessionPhase()==null?process.getCurrentPhase():request.sessionPhase();
  if(phase!=process.getCurrentPhase())throw new BusinessRuleException("La fase de la sesión debe coincidir con la fase actual del proceso");
  try{ClinicalSession saved=sessions.saveAndFlush(new ClinicalSession(process,request.appointmentId(),phase,request.evolution(),request.patientIndications(),request.registeredBy(),LocalDateTime.now(clock)));scheduling.markAttended(request.appointmentId());return toResponse(saved);}catch(DataIntegrityViolationException e){throw new DuplicateClinicalRecordException("La cita ya tiene una sesión clínica");}
 }
 @Transactional
 public SessionResponse registerExternalSession(ExternalSessionCreateRequest request){
  TherapeuticProcess process=findProcess(request.processId());if(!process.isActive())throw new BusinessRuleException("El proceso terapéutico está inactivo");
  var existing=sessions.findByAppointmentId(request.appointmentId());if(existing.isPresent()){if(!existing.get().getProcess().getId().equals(process.getId()))throw new DuplicateClinicalRecordException("La cita ya pertenece a otro proceso clínico");return toResponse(existing.get());}
  int phase=request.sessionPhase()==null?process.getCurrentPhase():request.sessionPhase();if(phase!=process.getCurrentPhase())throw new BusinessRuleException("La fase de la sesión debe coincidir con la fase actual del proceso");
  try{return toResponse(sessions.saveAndFlush(new ClinicalSession(process,request.appointmentId(),phase,request.evolution(),request.patientIndications(),request.registeredBy(),LocalDateTime.now(clock))));}catch(DataIntegrityViolationException e){ClinicalSession saved=sessions.findByAppointmentId(request.appointmentId()).orElseThrow(()->new DuplicateClinicalRecordException("La cita ya tiene una sesión clínica"));return toResponse(saved);}
 }
 @Transactional(readOnly=true) public List<SessionResponse> sessionsForProcess(Integer processId){findProcess(processId);return sessions.findByProcessIdOrderByRegisteredAtAscIdAsc(processId).stream().map(this::toResponse).toList();}
 @Transactional(readOnly=true) public SessionResponse session(Integer id){return toResponse(sessions.findById(id).orElseThrow(()->new ResourceNotFoundException("Sesión clínica no encontrada")));}
 private void ensureExternalInitialSession(TherapeuticProcess process,ExternalProcessCreateRequest request,LocalDateTime at){if(sessions.findByAppointmentId(request.appointmentId()).isPresent())return;InitialInterviewRequest input=request.entrevista();StringBuilder evolution=new StringBuilder("Motivo de consulta: ").append(input.motivoConsulta());if(input.antecedentesPersonales()!=null&&!input.antecedentesPersonales().isBlank())evolution.append("\n\nObservaciones clínicas: ").append(input.antecedentesPersonales());if(input.observacionesIniciales()!=null&&!input.observacionesIniciales().isBlank())evolution.append("\n\nResultado / hipótesis: ").append(input.observacionesIniciales());sessions.saveAndFlush(new ClinicalSession(process,request.appointmentId(),1,evolution.toString(),null,request.psychologistName().trim(),at));}
 private TherapeuticProcess findProcess(Integer id){return processes.findById(id).orElseThrow(()->new ResourceNotFoundException("Proceso terapéutico no encontrado"));}
 private void validateAppointment(AppointmentSnapshot a,Integer patientId,Integer psychologistId,String state){if(!patientId.equals(a.pacienteId()))throw new BusinessRuleException("La cita no corresponde al paciente");if(!psychologistId.equals(a.psicologoId()))throw new BusinessRuleException("La cita no corresponde al psicólogo");if(!state.equals(a.estado()))throw new BusinessRuleException("La cita debe estar en estado "+state);}
 private ProcessResponse toResponse(TherapeuticProcess p,InitialInterview i){InterviewResponse ir=i==null?null:new InterviewResponse(i.getId(),i.getReasonForConsultation(),i.getPersonalHistory(),i.getFamilyHistory(),i.getInitialObservations(),i.getRegisteredAt());return new ProcessResponse(p.getId(),p.getPatientId(),p.getPatientName(),p.getPatientDni(),p.getPatientHistoryNumber(),p.getPsychologistId(),p.getPsychologistName(),p.getCurrentPhase(),p.getStartDate(),p.getEndDate(),p.getObservations(),p.isActive(),p.getStatus(),ir);}
 private SessionResponse toResponse(ClinicalSession s){return new SessionResponse(s.getId(),s.getProcess().getId(),s.getAppointmentId(),s.getSessionPhase(),s.getEvolution(),s.getPatientIndications(),s.getRegisteredBy(),s.getRegisteredAt());}
}
