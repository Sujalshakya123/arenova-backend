package com.arenova.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class EsewaSignatureUtil {

    private EsewaSignatureUtil() {
    }

    public static String sign(String message, String secretKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate eSewa signature", e);
        }
    }

    public static String requestSignature(String totalAmount, String transactionUuid, String productCode, String secretKey) {
        String message = "total_amount=" + totalAmount
                + ",transaction_uuid=" + transactionUuid
                + ",product_code=" + productCode;
        return sign(message, secretKey);
    }

    public static String callbackSignature(
            String transactionCode,
            String status,
            String totalAmount,
            String transactionUuid,
            String productCode,
            String signedFieldNames,
            String secretKey
    ) {
        String message = "transaction_code=" + transactionCode
                + ",status=" + status
                + ",total_amount=" + totalAmount
                + ",transaction_uuid=" + transactionUuid
                + ",product_code=" + productCode
                + ",signed_field_names=" + signedFieldNames;
        return sign(message, secretKey);
    }
}
