package com.arenova.services.impl;

import com.arenova.dtos.ContactRequest;
import com.arenova.services.ContactService;
import com.arenova.services.EmailService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final EmailService emailService;

    @Override
    public void submitContact(ContactRequest request) throws BadRequestException {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Name is required.");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new BadRequestException("Email is required.");
        }
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new BadRequestException("Message is required.");
        }

        emailService.sendContactMessage(
                request.getName().trim(),
                request.getEmail().trim(),
                request.getSubject() != null ? request.getSubject().trim() : "General Question",
                request.getMessage().trim()
        );
    }
}
