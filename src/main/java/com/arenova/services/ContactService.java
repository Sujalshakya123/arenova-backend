package com.arenova.services;

import com.arenova.dtos.ContactRequest;
import org.apache.coyote.BadRequestException;

public interface ContactService {
    void submitContact(ContactRequest request) throws BadRequestException;
}
