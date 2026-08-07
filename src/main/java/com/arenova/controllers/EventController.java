package com.arenova.controllers;

import com.arenova.dtos.EventDTO;
import com.arenova.dtos.UserDTO;
import com.arenova.services.EventService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<EventDTO> createEvent(@RequestBody EventDTO eventDTO){
//        EventDTO savedEvent = eventService.createEvent(eventDTO);
        return ResponseEntity.status(HttpStatus.OK).body(eventService.createEvent(eventDTO));
    }

    @GetMapping("{id}")
    public ResponseEntity<EventDTO>  getEventById(@PathVariable() Long id){
        EventDTO event = eventService.getEventById(id);
        return new ResponseEntity<>(event, HttpStatus.FOUND);
    }

    @PutMapping("{id}")
    public ResponseEntity<EventDTO> updateEvent(@PathVariable Long id, @RequestBody EventDTO eventDTO){
        EventDTO event= eventService.updateEvent(id, eventDTO);
        return new ResponseEntity<>(event , HttpStatus.OK);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<EventDTO> deleteUser(@PathVariable long id){
        eventService.deleteEvent(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }


}
