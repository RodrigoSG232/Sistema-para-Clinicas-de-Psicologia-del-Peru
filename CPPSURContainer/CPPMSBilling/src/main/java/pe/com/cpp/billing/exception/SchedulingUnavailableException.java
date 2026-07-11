package pe.com.cpp.billing.exception;

public class SchedulingUnavailableException extends ExternalServiceException {
    public SchedulingUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
