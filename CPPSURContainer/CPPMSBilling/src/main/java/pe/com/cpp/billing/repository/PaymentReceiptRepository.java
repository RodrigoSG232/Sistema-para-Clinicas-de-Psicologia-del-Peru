package pe.com.cpp.billing.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.com.cpp.billing.domain.PaymentReceipt;

public interface PaymentReceiptRepository extends JpaRepository<PaymentReceipt, Integer> {
}
