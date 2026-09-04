package com.arenova.services;

import com.arenova.dtos.EventDTO;
import com.arenova.dtos.enums.EventStatus;
import org.apache.coyote.BadRequestException;

import java.util.List;

public interface AdminEventService {

    List<EventDTO> listEvents(EventStatus status);

    EventDTO approveEvent(Long id) throws BadRequestException;

    EventDTO rejectEvent(Long id) throws BadRequestException;

    EventDTO completeEvent(Long id) throws BadRequestException;
}
