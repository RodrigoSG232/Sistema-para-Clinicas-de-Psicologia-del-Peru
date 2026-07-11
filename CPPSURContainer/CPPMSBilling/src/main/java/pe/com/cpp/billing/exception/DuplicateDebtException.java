package pe.com.cpp.billing.exception;

public class DuplicateDebtException extends RuntimeException {
    public DuplicateDebtException(String message) {
        super(message);
    }
}
