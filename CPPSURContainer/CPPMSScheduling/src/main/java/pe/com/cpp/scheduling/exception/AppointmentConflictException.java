package pe.com.cpp.scheduling.exception;

public class AppointmentConflictException extends RuntimeException {
    public AppointmentConflictException(String message) {
        super(message);
    }
}
