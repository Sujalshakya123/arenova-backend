package com.arenova.respositories;

import com.arenova.entities.Event;
import com.arenova.entities.EventChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventChatMessageRepository extends JpaRepository<EventChatMessage, Long> {

    List<EventChatMessage> findByEventOrderBySentAtDesc(Event event, Pageable pageable);
}
