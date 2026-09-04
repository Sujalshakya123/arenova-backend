package com.arenova.services.impl;

import com.arenova.dtos.EventDTO;
import com.arenova.dtos.enums.EventStatus;
import com.arenova.entities.Event;
import com.arenova.entities.Project;
import com.arenova.exceptions.ResourceNotFoundException;
import com.arenova.mapper.EventMapper;
import com.arenova.respositories.EventRepository;
import com.arenova.respositories.ProjectRepository;
import com.arenova.services.AdminAccessService;
import com.arenova.services.AdminEventService;
import com.arenova.services.PrizePoolService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminEventServiceImpl implements AdminEventService {

    private final EventRepository eventRepository;
    private final ProjectRepository projectRepository;
    private final AdminAccessService adminAccessService;
    private final PrizePoolService prizePoolService;

    private EventDTO toEnrichedDto(Event event) {
        EventDTO dto = EventMapper.toDTO(event);
        prizePoolService.enrichEventDto(dto, event);
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventDTO> listEvents(EventStatus status) {
        adminAccessService.requireAdmin();
        List<Event> events = status != null
                ? eventRepository.findByStatusOrderByCreatedAtDesc(status)
                : eventRepository.findAllByOrderByCreatedAtDesc();
        return events.stream()
                .map(this::toEnrichedDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventDTO approveEvent(Long id) throws BadRequestException {
        adminAccessService.requireAdmin();
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BadRequestException("Only draft tournaments can be approved.");
        }
        event.setStatus(EventStatus.LIVE);
        if (event.getRegistrationOpen() == null) {
            event.setRegistrationOpen(true);
        }
        return toEnrichedDto(eventRepository.save(event));
    }

    @Override
    @Transactional
    public EventDTO rejectEvent(Long id) throws BadRequestException {
        adminAccessService.requireAdmin();
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BadRequestException("Only draft tournaments can be rejected.");
        }

        Project project = event.getProject();
        Long projectId = project != null ? project.getId() : null;
        eventRepository.delete(event);

        if (project != null && projectId != null) {
            int count = project.getTournamentCount() != null ? project.getTournamentCount() : 0;
            project.setTournamentCount(Math.max(0, count - 1));
            projectRepository.save(project);
        }

        // Entity is deleted — return a minimal snapshot for the client.
        return EventDTO.builder()
                .id(id)
                .title(event.getTitle())
                .status(EventStatus.DRAFT)
                .build();
    }

    @Override
    @Transactional
    public EventDTO completeEvent(Long id) throws BadRequestException {
        adminAccessService.requireAdmin();
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        if (event.getStatus() != EventStatus.LIVE) {
            throw new BadRequestException("Only live tournaments can be marked completed.");
        }
        event.setStatus(EventStatus.COMPLETED);
        event.setRegistrationOpen(false);
        return toEnrichedDto(eventRepository.save(event));
    }
}
