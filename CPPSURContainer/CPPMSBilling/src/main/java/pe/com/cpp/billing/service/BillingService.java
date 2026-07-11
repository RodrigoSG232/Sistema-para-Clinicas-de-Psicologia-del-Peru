package pe.com.cpp.billing.service;

import static pe.com.cpp.billing.domain.ReceiptSequence.PAYMENT_RECEIPT_SEQUENCE;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.com.cpp.billing.api.DebtResponse;
import pe.com.cpp.billing.api.PaymentRequest;
import pe.com.cpp.billing.api.PaymentResponse;
import pe.com.cpp.billing.api.ReceiptResponse;
import pe.com.cpp.billing.client.AppointmentSnapshot;
import pe.com.cpp.billing.client.SchedulingClient;
import pe.com.cpp.billing.domain.Debt;
import pe.com.cpp.billing.domain.DebtStatus;
import pe.com.cpp.billing.domain.PaymentMethod;
import pe.com.cpp.billing.domain.PaymentReceipt;
import pe.com.cpp.billing.domain.ReceiptSequence;
import pe.com.cpp.billing.domain.ReceiptType;
import pe.com.cpp.billing.exception.BusinessRuleException;
import pe.com.cpp.billing.exception.DuplicateDebtException;
import pe.com.cpp.billing.exception.ResourceNotFoundException;
import pe.com.cpp.billing.repository.DebtRepository;
import pe.com.cpp.billing.repository.PaymentReceiptRepository;
import pe.com.cpp.billing.repository.ReceiptSequenceRepository;

@Service
public class BillingService {

    private final DebtRepository debtRepository;
    private final PaymentReceiptRepository receiptRepository;
    private final ReceiptSequenceRepository sequenceRepository;
    private final SchedulingClient schedulingClient;
    private final Clock clock;

    public BillingService(DebtRepository debtRepository,
            PaymentReceiptRepository receiptRepository,
            ReceiptSequenceRepository sequenceRepository,
            SchedulingClient schedulingClient,
            Clock clock) {
        this.debtRepository = debtRepository;
        this.receiptRepository = receiptRepository;
        this.sequenceRepository = sequenceRepository;
        this.schedulingClient = schedulingClient;
        this.clock = clock;
    }

    @Transactional
    public DebtResponse createFromAppointment(Integer appointmentId) {
        if (debtRepository.existsByAppointmentId(appointmentId)) {
            throw new DuplicateDebtException("La cita ya tiene una deuda asociada");
        }
        AppointmentSnapshot appointment = schedulingClient.findById(appointmentId);
        if (!"PENDIENTE_PAGO".equals(appointment.estado())) {
            throw new BusinessRuleException("Solo se puede generar deuda para una cita pendiente de pago");
        }
        if (appointment.monto() == null || appointment.monto().signum() <= 0) {
            throw new BusinessRuleException("La cita no contiene un monto válido");
        }

        Debt debt = new Debt(
                appointment.pacienteId(), appointment.paciente(), appointment.pacienteDni(),
                appointment.pacienteHc(), appointment.id(), "Gastos de cita",
                appointment.especialidad(), appointment.monto(), LocalDateTime.now(clock));
        return toResponse(debtRepository.save(debt));
    }

    @Transactional(readOnly = true)
    public List<DebtResponse> searchPending(String patient, String concept) {
        return debtRepository.search(DebtStatus.PENDING, trimToNull(patient), trimToNull(concept))
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<DebtResponse> findPendingByPatient(Integer patientId) {
        return debtRepository.findByPatientIdAndStatusOrderByCreatedAt(patientId, DebtStatus.PENDING)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public PaymentResponse pay(Integer debtId, PaymentRequest request) {
        Debt debt = debtRepository.findByIdForUpdate(debtId)
                .orElseThrow(() -> new ResourceNotFoundException("Deuda no encontrada"));
        if (debt.getStatus() == DebtStatus.PAID) {
            throw new DuplicateDebtException("Esta deuda ya fue pagada");
        }

        PaymentMethod paymentMethod = parsePaymentMethod(request.medioPago());
        ReceiptType receiptType = parseReceiptType(request.tipo());
        if (debt.getAppointmentId() != null) {
            schedulingClient.markAsPaid(debt.getAppointmentId());
        }

        ReceiptSequence sequence = sequenceRepository.findByNameForUpdate(PAYMENT_RECEIPT_SEQUENCE)
                .orElseThrow(() -> new IllegalStateException("No se configuró la secuencia de comprobantes"));
        int nextNumber = sequence.takeNextValue();
        String prefix = receiptType == ReceiptType.RECEIPT ? "B" : "F";
        String receiptNumber = prefix + "-" + String.format("%05d", nextNumber);

        debt.markAsPaid();
        PaymentReceipt receipt = new PaymentReceipt(
                receiptNumber, receiptType, debt, paymentMethod, debt.getAmount(),
                LocalDateTime.now(clock), trimToNull(request.cajero()));
        PaymentReceipt savedReceipt = receiptRepository.save(receipt);
        ReceiptResponse response = toResponse(savedReceipt);
        return new PaymentResponse(response, receiptNumber, "Pago registrado exitosamente");
    }

    @Transactional(readOnly = true)
    public ReceiptResponse findReceipt(Integer receiptId) {
        return receiptRepository.findById(receiptId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Comprobante no encontrado"));
    }

    private DebtResponse toResponse(Debt debt) {
        return new DebtResponse(debt.getId(), debt.getPatientId(), debt.getPatientName(),
                debt.getPatientDni(), debt.getPatientHistoryNumber(), debt.getAppointmentId(),
                debt.getConcept(), debt.getSpecialtyName(), debt.getAmount(),
                toExternalStatus(debt.getStatus()), debt.getCreatedAt());
    }

    private ReceiptResponse toResponse(PaymentReceipt receipt) {
        Debt debt = receipt.getDebt();
        return new ReceiptResponse(receipt.getId(), receipt.getReceiptNumber(),
                toExternalType(receipt.getType()), toExternalPaymentMethod(receipt.getPaymentMethod()),
                receipt.getAmountPaid(), receipt.getIssuedAt(), receipt.getCashierUsername(),
                debt.getId(), debt.getConcept(), toExternalStatus(debt.getStatus()), debt.getPatientId(),
                debt.getPatientName(), debt.getPatientDni(), debt.getSpecialtyName(), debt.getAppointmentId());
    }

    private PaymentMethod parsePaymentMethod(String value) {
        return switch (value.trim().toUpperCase()) {
            case "EFECTIVO" -> PaymentMethod.CASH;
            case "TARJETA" -> PaymentMethod.CARD;
            case "YAPE_PLIN" -> PaymentMethod.YAPE_PLIN;
            default -> throw new BusinessRuleException("Medio de pago no válido");
        };
    }

    private ReceiptType parseReceiptType(String value) {
        if (value == null || value.isBlank() || "BOLETA".equalsIgnoreCase(value)) {
            return ReceiptType.RECEIPT;
        }
        if ("FACTURA".equalsIgnoreCase(value)) {
            return ReceiptType.INVOICE;
        }
        throw new BusinessRuleException("Tipo de comprobante no válido");
    }

    private String toExternalStatus(DebtStatus status) {
        return status == DebtStatus.PENDING ? "PENDIENTE" : "PAGADA";
    }

    private String toExternalPaymentMethod(PaymentMethod method) {
        return switch (method) {
            case CASH -> "EFECTIVO";
            case CARD -> "TARJETA";
            case YAPE_PLIN -> "YAPE_PLIN";
        };
    }

    private String toExternalType(ReceiptType type) {
        return type == ReceiptType.RECEIPT ? "BOLETA" : "FACTURA";
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
