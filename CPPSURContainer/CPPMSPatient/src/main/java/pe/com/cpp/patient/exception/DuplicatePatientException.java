package pe.com.cpp.patient.exception;

public class DuplicatePatientException extends RuntimeException {

    public DuplicatePatientException(String message) {
        super(message);
    }
}
