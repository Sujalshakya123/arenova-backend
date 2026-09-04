package com.arenova.services;

import com.arenova.dtos.EventRegistrationDTO;
import com.arenova.dtos.RegisterEventRequest;
import com.arenova.dtos.RegisterEventResponseDTO;
import org.apache.coyote.BadRequestException;

import java.util.List;

public interface EventRegistrationService {

    RegisterEventResponseDTO registerForEvent(Long eventId, RegisterEventRequest request)
            throws BadRequestException;

    List<EventRegistrationDTO> getMyRegistrations();

    List<EventRegistrationDTO> getEventRegistrations(Long eventId);

    void withdrawRegistration(Long registrationId) throws BadRequestException;

    EventRegistrationDTO getMyRegistrationForEvent(Long eventId);

    EventRegistrationDTO approveRegistration(Long registrationId) throws BadRequestException;

    EventRegistrationDTO rejectRegistration(Long registrationId) throws BadRequestException;
}
