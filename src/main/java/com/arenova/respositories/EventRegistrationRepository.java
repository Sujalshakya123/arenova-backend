package com.arenova.respositories;

import com.arenova.dtos.enums.RegistrationStatus;
import com.arenova.entities.Event;
import com.arenova.entities.EventRegistration;
import com.arenova.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {

    List<EventRegistration> findByUserAndStatusOrderByRegisteredAtDesc(
            User user,
            RegistrationStatus status
    );

    List<EventRegistration> findByUserAndStatusInOrderByRegisteredAtDesc(
            User user,
            Collection<RegistrationStatus> statuses
    );

    List<EventRegistration> findByUserOrderByRegisteredAtDesc(User user);

    List<EventRegistration> findByEventAndStatusOrderByRegisteredAtDesc(
            Event event,
            RegistrationStatus status
    );

    List<EventRegistration> findByEventAndStatusInOrderByRegisteredAtDesc(
            Event event,
            Collection<RegistrationStatus> statuses
    );

    Optional<EventRegistration> findByEventAndUser(Event event, User user);

    long countByEventAndStatus(Event event, RegistrationStatus status);

    long countByEventAndStatusIn(Event event, Collection<RegistrationStatus> statuses);

    Optional<EventRegistration> findByIdAndUser(Long id, User user);

    long countByUser_Id(Long userId);

    List<EventRegistration> findAllByOrderByRegisteredAtDesc();
}
