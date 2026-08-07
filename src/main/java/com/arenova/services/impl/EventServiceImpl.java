package com.arenova.services.impl;


import com.arenova.dtos.EventDTO;
import com.arenova.entities.Event;
import com.arenova.exceptions.ResourceNotFoundException;
import com.arenova.mapper.EventMapper;
import com.arenova.respositories.EventRepository;
import com.arenova.services.EventService;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class EventServiceImpl implements EventService {


    private final EventRepository eventRepository;

    private final ModelMapper modelMapper;


    @Override
    public EventDTO createEvent(EventDTO eventDTO) {
        Event event = EventMapper.toEntity(eventDTO);
        Event savedEvent = eventRepository.save(event);
        return EventMapper.toDTO(savedEvent);
    }

    @Override
    public EventDTO getEventById(Long id) {
        Event event = eventRepository.findById(id).
                orElseThrow(()-> new ResourceNotFoundException("Event ID Not Found")); //Entity value
        return EventMapper.toDTO(event);
    }

    @Override
    public EventDTO updateEvent(Long id, EventDTO eventDTO) {
        Event event = eventRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("Event ID NOT FOUND"));

        event.setTitle(eventDTO.getTitle());

        event.setMinCapacity(eventDTO.getMinCapacity());
        event.setMaxCapacity(eventDTO.getMaxCapacity());
        event.setCreatedAt(eventDTO.getCreatedAt());
        event.setMode(eventDTO.getMode());
        event.setPrizePool(eventDTO.getPrizePool());
        event.setEntry(eventDTO.getEntry());
        event.setDescription(eventDTO.getDescription());

        Event updatedEvent = eventRepository.save(event);
        return EventMapper.toDTO(updatedEvent);
    }

    @Override
    public void deleteEvent(Long id) {
        Event event = eventRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("Event ID NOT FOUND"));
        eventRepository.delete(event);
    }




}
