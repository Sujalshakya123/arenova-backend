package com.arenova.respositories;

import com.arenova.dtos.enums.PaymentStatus;
import com.arenova.entities.EventRegistration;
import com.arenova.entities.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTransactionUuid(String transactionUuid);

    Optional<Payment> findFirstByRegistrationOrderByCreatedAtDesc(EventRegistration registration);

    Optional<Payment> findFirstByRegistrationAndStatus(EventRegistration registration, PaymentStatus status);

    List<Payment> findByRegistrationAndStatus(EventRegistration registration, PaymentStatus status);

    List<Payment> findAllByOrderByCreatedAtDesc();

    List<Payment> findByEvent_IdAndStatus(Long eventId, PaymentStatus status);

    List<Payment> findByEvent_IdOrderByCreatedAtDesc(Long eventId);
}
