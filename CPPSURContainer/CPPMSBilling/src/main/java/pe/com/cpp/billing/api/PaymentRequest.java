package pe.com.cpp.billing.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PaymentRequest(
        @NotBlank(message = "El medio de pago es obligatorio") String medioPago,
        String tipo,
        @Size(max = 100, message = "El usuario de caja no debe superar 100 caracteres") String cajero) {
}
