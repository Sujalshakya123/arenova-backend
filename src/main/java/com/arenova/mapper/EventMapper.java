package com.arenova.mapper;


import com.arenova.dtos.EventDTO;
import com.arenova.entities.Event;

public class EventMapper {

    public static EventDTO toDTO (Event event){
        return new EventDTO(
                event.getId(),
                event.getTitle(),
                event.getMinCapacity(),
                event.getMaxCapacity(),
                event.getCreatedAt(),
                event.getMode(),
                event.getPrizePool(),
                event.getEntry(),
                event.getDescription()

        );
    }

    //Event DTO - Event Entity Convert

    public static Event toEntity(EventDTO eventDTO){
        return new Event(
                eventDTO.getId(),
                eventDTO.getTitle(),
                eventDTO.getMinCapacity(),
                eventDTO.getMaxCapacity(),
                eventDTO.getCreatedAt(),
                eventDTO.getMode(),
                eventDTO.getPrizePool(),
                eventDTO.getEntry(),
                eventDTO.getDescription()
        );
    }
}
