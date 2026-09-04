package com.arenova.services;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${arenova.contact.to:sujaruu10@gmail.com}")
    private String contactTo;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    public void sendOtp(String email, String otp) {

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setTo(email);

        message.setSubject(
                "Arenova Verification Code"
        );

        message.setText(
                "Your verification code is: "
                        + otp +
                        "\n\nExpires in 5 minutes."
        );

        mailSender.send(message);
    }

    public void sendPasswordResetOtp(String email, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Arenova Password Reset Code");
        message.setText(
                "Your password reset code is: "
                        + otp
                        + "\n\nExpires in 5 minutes.\n"
                        + "If you did not request this, you can ignore this email."
        );
        mailSender.send(message);
    }

    public void sendContactMessage(
            String fromName,
            String fromEmail,
            String subject,
            String messageBody
    ) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(contactTo);
        if (mailFrom != null && !mailFrom.isBlank()) {
            message.setFrom(mailFrom);
        }
        message.setReplyTo(fromEmail);
        message.setSubject("[Arenova Contact] " + subject);
        message.setText(
                "From: " + fromName + " <" + fromEmail + ">\n"
                        + "Subject: " + subject + "\n\n"
                        + messageBody
        );
        mailSender.send(message);
    }

}
