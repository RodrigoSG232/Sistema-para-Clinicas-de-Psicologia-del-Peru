package pe.com.cpp.scheduling.client;

public record PatientSnapshot(
        Integer id,
        String numeroHistoria,
        String dni,
        String nombres,
        String apellidos,
        String nombreCompleto,
        Boolean activo) {
}
