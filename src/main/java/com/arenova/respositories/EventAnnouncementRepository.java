package com.arenova.respositories;

import com.arenova.entities.Event;
import com.arenova.entities.EventAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventAnnouncementRepository extends JpaRepository<EventAnnouncement, Long> {

    List<EventAnnouncement> findByEventInOrderByCreatedAtDesc(List<Event> events);
}
