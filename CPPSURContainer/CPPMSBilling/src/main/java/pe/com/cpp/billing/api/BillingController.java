package pe.com.cpp.billing.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.com.cpp.billing.service.BillingService;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @PostMapping("/debts/from-appointment/{appointmentId}")
    public ResponseEntity<DebtResponse> createFromAppointment(@PathVariable Integer appointmentId) {
        DebtResponse debt = billingService.createFromAppointment(appointmentId);
        return ResponseEntity.created(URI.create("/api/billing/debts/" + debt.id())).body(debt);
    }

    @GetMapping("/debts")
    public List<DebtResponse> searchPending(
            @RequestParam(required = false) String patient,
            @RequestParam(required = false) String concept) {
        return billingService.searchPending(patient, concept);
    }

    @GetMapping("/debts/patient/{patientId}")
    public List<DebtResponse> findPendingByPatient(@PathVariable Integer patientId) {
        return billingService.findPendingByPatient(patientId);
    }

    @PostMapping("/payments/{debtId}")
    public PaymentResponse pay(@PathVariable Integer debtId,
            @Valid @RequestBody PaymentRequest request) {
        return billingService.pay(debtId, request);
    }

    @GetMapping("/receipts/{receiptId}")
    public ReceiptResponse findReceipt(@PathVariable Integer receiptId) {
        return billingService.findReceipt(receiptId);
    }
}
