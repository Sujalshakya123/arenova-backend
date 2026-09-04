package com.arenova.respositories;

import com.arenova.dtos.enums.EventStatus;
import com.arenova.entities.Event;
import com.arenova.entities.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByProjectOrderByCreatedAtDesc(Project project);

    Optional<Event> findByIdAndProject(Long id, Project project);

    List<Event> findByStatusInOrderByCreatedAtDesc(List<EventStatus> statuses);

    List<Event> findByProject_Organizer_IdOrderByCreatedAtDesc(Long organizerId);

    List<Event> findAllByOrderByCreatedAtDesc();

    @Query("""
            SELECT e FROM Event e
            JOIN FETCH e.project p
            JOIN FETCH p.organizer
            WHERE p.organizer IS NOT NULL
            ORDER BY e.createdAt DESC
            """)
    List<Event> findAllWithOrganizerOrderByCreatedAtDesc();

    List<Event> findByStatusOrderByCreatedAtDesc(EventStatus status);

    long countByStatus(EventStatus status);

    long countByProject_Organizer_Id(Long organizerId);

    List<Event> findTop8ByOrderByCreatedAtDesc();
}
