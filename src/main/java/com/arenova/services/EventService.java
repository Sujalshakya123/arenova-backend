package com.arenova.services;

import com.arenova.dtos.CreateEventRequest;
import com.arenova.dtos.EventDTO;
import com.arenova.dtos.PlatformStatsDTO;
import org.apache.coyote.BadRequestException;

import java.util.List;

public interface EventService {

    EventDTO createEvent(CreateEventRequest request) throws BadRequestException;

    EventDTO updateEvent(Long id, CreateEventRequest request) throws BadRequestException;

    void deleteEvent(Long id);

    EventDTO getEventById(Long id);

    List<EventDTO> getEventsByProject(Long projectId);

    List<EventDTO> getMyEvents();

    /** Public browse: LIVE + COMPLETED (and any with null status treated as live). */
    List<EventDTO> getPublicEvents();

    /** Homepage stats: public events, player accounts, and ongoing bracket matches. */
    PlatformStatsDTO getPublicPlatformStats();

    EventDTO uploadDetailBanner(Long id, org.springframework.web.multipart.MultipartFile file)
            throws Exception;
}
