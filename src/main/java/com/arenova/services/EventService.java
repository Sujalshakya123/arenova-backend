package com.arenova.services;

import com.arenova.dtos.EventDTO;
import com.arenova.dtos.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
//@RequiredArgsConstructor
public interface EventService {

    //Create Event
    EventDTO createEvent(EventDTO eventDTO);

    //Update Event
    EventDTO updateEvent(Long id, EventDTO eventDTO);

    //Delete Event
    void deleteEvent(Long id);


    //Get event by id
    EventDTO getEventById(Long id);
}
