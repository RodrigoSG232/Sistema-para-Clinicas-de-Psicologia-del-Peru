package pe.com.cpp.billing.api;

public record PaymentResponse(
        ReceiptResponse comprobante,
        String numeroComprobante,
        String mensaje) {
}
