package pe.com.cpp.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.com.cpp.billing.api.DebtResponse;
import pe.com.cpp.billing.api.PaymentRequest;
import pe.com.cpp.billing.api.PaymentResponse;
import pe.com.cpp.billing.client.AppointmentSnapshot;
import pe.com.cpp.billing.client.SchedulingClient;
import pe.com.cpp.billing.domain.Debt;
import pe.com.cpp.billing.domain.DebtStatus;
import pe.com.cpp.billing.domain.PaymentReceipt;
import pe.com.cpp.billing.domain.ReceiptSequence;
import pe.com.cpp.billing.exception.DuplicateDebtException;
import pe.com.cpp.billing.repository.DebtRepository;
import pe.com.cpp.billing.repository.PaymentReceiptRepository;
import pe.com.cpp.billing.repository.ReceiptSequenceRepository;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock private DebtRepository debtRepository;
    @Mock private PaymentReceiptRepository receiptRepository;
    @Mock private ReceiptSequenceRepository sequenceRepository;
    @Mock private SchedulingClient schedulingClient;

    private BillingService billingService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-11T16:00:00Z"), ZoneId.of("America/Lima"));
        billingService = new BillingService(
                debtRepository, receiptRepository, sequenceRepository, schedulingClient, clock);
    }

    @Test
    void createsDebtUsingAppointmentSnapshot() {
        AppointmentSnapshot appointment = new AppointmentSnapshot(
                12, LocalDate.of(2026, 7, 13), LocalTime.of(10, 0), "PENDIENTE_PAGO",
                1, "Jose Martinez Vargas", 1, "Psicologia Clinica", new BigDecimal("80.00"),
                7, "Ana Torres", "76543210", "HC-0007", null);
        when(debtRepository.existsByAppointmentId(12)).thenReturn(false);
        when(schedulingClient.findById(12)).thenReturn(appointment);
        when(debtRepository.save(any(Debt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DebtResponse response = billingService.createFromAppointment(12);

        assertThat(response.citaId()).isEqualTo(12);
        assertThat(response.pacienteId()).isEqualTo(7);
        assertThat(response.estado()).isEqualTo("PENDIENTE");
        assertThat(response.monto()).isEqualByComparingTo("80.00");
        assertThat(response.creadoEn()).isEqualTo("2026-07-11T11:00:00");
        verify(schedulingClient).findById(12);
    }

    @Test
    void paysDebtAndMarksAppointmentAsPaid() {
        Debt debt = new Debt(7, "Ana Torres", "76543210", "HC-0007", 12,
                "Gastos de cita", "Psicologia Clinica", new BigDecimal("80.00"), null);
        ReceiptSequence sequence = mock(ReceiptSequence.class);
        when(debtRepository.findByIdForUpdate(5)).thenReturn(Optional.of(debt));
        when(sequenceRepository.findByNameForUpdate(ReceiptSequence.PAYMENT_RECEIPT_SEQUENCE))
                .thenReturn(Optional.of(sequence));
        when(sequence.takeNextValue()).thenReturn(9);
        when(receiptRepository.save(any(PaymentReceipt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = billingService.pay(
                5, new PaymentRequest("TARJETA", "BOLETA", "caja01"));

        assertThat(response.numeroComprobante()).isEqualTo("B-00009");
        assertThat(response.comprobante().medioPago()).isEqualTo("TARJETA");
        assertThat(response.comprobante().estadoDeuda()).isEqualTo("PAGADA");
        assertThat(response.comprobante().montoPagado()).isEqualByComparingTo("80.00");
        verify(schedulingClient).markAsPaid(12);
        verify(sequence).takeNextValue();
    }

    @Test
    void rejectsAlreadyPaidDebt() {
        Debt debt = mock(Debt.class);
        when(debt.getStatus()).thenReturn(DebtStatus.PAID);
        when(debtRepository.findByIdForUpdate(5)).thenReturn(Optional.of(debt));

        assertThatThrownBy(() -> billingService.pay(
                5, new PaymentRequest("EFECTIVO", "BOLETA", null)))
                .isInstanceOf(DuplicateDebtException.class)
                .hasMessage("Esta deuda ya fue pagada");
    }
}
