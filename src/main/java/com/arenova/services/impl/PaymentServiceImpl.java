package com.arenova.services.impl;

import com.arenova.config.EsewaProperties;
import com.arenova.dtos.EsewaPaymentInitDTO;
import com.arenova.dtos.EsewaVerifyRequest;
import com.arenova.dtos.PaymentReceiptDTO;
import com.arenova.dtos.enums.PaymentStatus;
import com.arenova.entities.EventRegistration;
import com.arenova.entities.Payment;
import com.arenova.entities.User;
import com.arenova.exceptions.ResourceNotFoundException;
import com.arenova.mapper.PaymentReceiptMapper;
import com.arenova.respositories.EventRegistrationRepository;
import com.arenova.respositories.PaymentRepository;
import com.arenova.respositories.UserRepository;
import com.arenova.services.PaymentService;
import com.arenova.util.EntryFeeUtil;
import com.arenova.util.EsewaSignatureUtil;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final EventRegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final EsewaProperties esewaProperties;
    private final RestClient.Builder restClientBuilder;

    @Override
    @Transactional
    public EsewaPaymentInitDTO initiateEsewaForRegistration(EventRegistration registration, int amountNpr) {
        if (amountNpr <= 0) {
            throw new IllegalArgumentException("Amount must be positive for eSewa");
        }

        markOpenPaymentsFailed(registration);
        return createEsewaInit(registration, amountNpr);
    }

    @Override
    @Transactional
    public EsewaPaymentInitDTO resumeEsewaPayment(Long registrationId) throws BadRequestException {
        User current = currentUser();
        EventRegistration registration = registrationRepository
                .findByIdAndUser(registrationId, current)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found"));

        if (registration.getStatus() != com.arenova.dtos.enums.RegistrationStatus.PENDING
                && registration.getStatus() != com.arenova.dtos.enums.RegistrationStatus.REGISTERED) {
            throw new BadRequestException("Payment can only be resumed for an active registration.");
        }

        String existing = paymentStatusForRegistration(registration);
        if ("COMPLETED".equalsIgnoreCase(existing)) {
            throw new BadRequestException("This registration is already paid.");
        }

        int fee = EntryFeeUtil.parseEntryFeeNpr(registration.getEvent().getEntry());
        if (fee <= 0) {
            throw new BadRequestException("This tournament has no entry fee.");
        }

        String method = registration.getPaymentMethod() != null
                ? registration.getPaymentMethod().trim().toLowerCase()
                : "";
        if (!method.isEmpty() && !"esewa".equals(method) && !"mock".equals(method)) {
            throw new BadRequestException("Resume payment is only available for eSewa.");
        }

        registration.setPaymentMethod("esewa");
        registrationRepository.save(registration);

        markOpenPaymentsFailed(registration);
        return createEsewaInit(registration, fee);
    }

    @Override
    @Transactional
    public Map<String, Object> verifyEsewaCallback(EsewaVerifyRequest request) throws BadRequestException {
        User current = currentUser();
        if (request == null || request.getData() == null || request.getData().isBlank()) {
            throw new BadRequestException("Missing eSewa callback data");
        }

        String json;
        try {
            byte[] decoded = Base64.getDecoder().decode(request.getData().trim());
            json = new String(decoded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BadRequestException("Invalid eSewa callback payload");
        }

        String status = jsonString(json, "status");
        String transactionUuid = jsonString(json, "transaction_uuid");
        String totalAmountRaw = jsonString(json, "total_amount");
        String totalAmount = normalizeAmount(totalAmountRaw);
        String productCode = jsonString(json, "product_code");
        String transactionCode = jsonString(json, "transaction_code");
        String signedFieldNames = jsonString(json, "signed_field_names");
        String callbackSignature = jsonString(json, "signature");

        if (transactionUuid == null || transactionUuid.isBlank()) {
            throw new BadRequestException("Missing transaction UUID");
        }

        Payment payment = paymentRepository.findByTransactionUuid(transactionUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if (!payment.getUser().getId().equals(current.getId())) {
            throw new ResourceNotFoundException("Payment not found");
        }

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            return resultMap(payment, true, "Payment already verified");
        }

        boolean signatureOk = false;
        if (callbackSignature != null && signedFieldNames != null && transactionCode != null
                && status != null && totalAmountRaw != null && productCode != null) {
            String expected = EsewaSignatureUtil.callbackSignature(
                    transactionCode,
                    status,
                    totalAmountRaw,
                    transactionUuid,
                    productCode,
                    signedFieldNames,
                    esewaProperties.getSecretKey()
            );
            signatureOk = expected.equals(callbackSignature);
            if (!signatureOk) {
                String expectedNorm = EsewaSignatureUtil.callbackSignature(
                        transactionCode,
                        status,
                        totalAmount,
                        transactionUuid,
                        productCode,
                        signedFieldNames,
                        esewaProperties.getSecretKey()
                );
                signatureOk = expectedNorm.equals(callbackSignature);
            }
        }

        boolean callbackComplete = "COMPLETE".equalsIgnoreCase(status);
        boolean apiComplete = confirmStatusWithEsewa(
                productCode != null ? productCode : esewaProperties.getMerchantCode(),
                payment.getAmount(),
                transactionUuid
        );

        if (!(callbackComplete && (apiComplete || signatureOk))) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new BadRequestException("eSewa payment was not completed or could not be verified");
        }

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setEsewaRefId(transactionCode);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        return resultMap(payment, true, "Payment verified successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public String paymentStatusForRegistration(EventRegistration registration) {
        if (registration == null) {
            return null;
        }
        return paymentRepository.findFirstByRegistrationOrderByCreatedAtDesc(registration)
                .map(p -> p.getStatus().name())
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentReceiptDTO getMyReceiptById(Long paymentId) {
        User current = currentUser();
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (payment.getUser() == null || !payment.getUser().getId().equals(current.getId())) {
            throw new ResourceNotFoundException("Payment not found");
        }
        return PaymentReceiptMapper.toReceipt(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentReceiptDTO getMyReceiptByRegistration(Long registrationId) {
        User current = currentUser();
        EventRegistration registration = registrationRepository
                .findByIdAndUser(registrationId, current)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found"));
        Payment payment = paymentRepository
                .findFirstByRegistrationOrderByCreatedAtDesc(registration)
                .orElseThrow(() -> new ResourceNotFoundException("No payment found for this registration"));
        return PaymentReceiptMapper.toReceipt(payment);
    }

    private boolean confirmStatusWithEsewa(String productCode, String totalAmount, String transactionUuid) {
        try {
            String url = UriComponentsBuilder
                    .fromUriString(esewaProperties.getStatusUrl())
                    .queryParam("product_code", productCode)
                    .queryParam("total_amount", totalAmount)
                    .queryParam("transaction_uuid", transactionUuid)
                    .build(true)
                    .toUriString();

            String body = restClientBuilder.build()
                    .get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            if (body == null || body.isBlank()) {
                return false;
            }
            return "COMPLETE".equalsIgnoreCase(jsonString(body, "status"));
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, Object> resultMap(Payment payment, boolean success, String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("success", success);
        map.put("message", message);
        map.put("paymentStatus", payment.getStatus().name());
        map.put("transactionUuid", payment.getTransactionUuid());
        map.put("registrationId", payment.getRegistration().getId());
        map.put("eventId", payment.getEvent().getId());
        map.put("amount", payment.getAmount());
        map.put("esewaRefId", payment.getEsewaRefId());
        return map;
    }

    private EsewaPaymentInitDTO createEsewaInit(EventRegistration registration, int amountNpr) {
        String amount = EntryFeeUtil.formatAmount(amountNpr);
        String transactionUuid = "ARN-" + registration.getId() + "-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        Payment payment = paymentRepository.save(
                Payment.builder()
                        .registration(registration)
                        .user(registration.getUser())
                        .event(registration.getEvent())
                        .amount(amount)
                        .method("esewa")
                        .status(PaymentStatus.INITIATED)
                        .transactionUuid(transactionUuid)
                        .build()
        );

        String productCode = esewaProperties.getMerchantCode();
        String signature = EsewaSignatureUtil.requestSignature(
                amount,
                payment.getTransactionUuid(),
                productCode,
                esewaProperties.getSecretKey()
        );

        return EsewaPaymentInitDTO.builder()
                .paymentUrl(esewaProperties.getPaymentUrl())
                .amount(amount)
                .taxAmount("0")
                .totalAmount(amount)
                .transactionUuid(payment.getTransactionUuid())
                .productCode(productCode)
                .productServiceCharge("0")
                .productDeliveryCharge("0")
                .successUrl(esewaProperties.getSuccessUrl())
                .failureUrl(esewaProperties.getFailureUrl())
                .signedFieldNames("total_amount,transaction_uuid,product_code")
                .signature(signature)
                .build();
    }

    private void markOpenPaymentsFailed(EventRegistration registration) {
        paymentRepository.findByRegistrationAndStatus(registration, PaymentStatus.INITIATED)
                .forEach(payment -> {
                    payment.setStatus(PaymentStatus.FAILED);
                    paymentRepository.save(payment);
                });
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new ResourceNotFoundException("Not authenticated");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private static String jsonString(String json, String field) {
        if (json == null || field == null) {
            return null;
        }
        Matcher matcher = Pattern.compile(
                "\"" + Pattern.quote(field) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\""
        ).matcher(json);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1).replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static String normalizeAmount(String amount) {
        if (amount == null) {
            return null;
        }
        String trimmed = amount.trim().replace(",", "");
        if (trimmed.matches("\\d+\\.0")) {
            return trimmed.substring(0, trimmed.indexOf('.')) + ".00";
        }
        if (!trimmed.contains(".")) {
            return trimmed + ".00";
        }
        return trimmed;
    }
}
