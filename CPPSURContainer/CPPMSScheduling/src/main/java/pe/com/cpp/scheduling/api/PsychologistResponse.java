package pe.com.cpp.scheduling.api;

public record PsychologistResponse(
        Integer id,
        String nombres,
        String apellidos,
        String nombreCompleto,
        String colegiatura,
        Integer especialidadId,
        String especialidad) {
}
