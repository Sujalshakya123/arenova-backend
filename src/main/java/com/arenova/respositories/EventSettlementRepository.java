package com.arenova.respositories;

import com.arenova.entities.Event;
import com.arenova.entities.EventSettlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventSettlementRepository extends JpaRepository<EventSettlement, Long> {

    Optional<EventSettlement> findByEvent(Event event);

    Optional<EventSettlement> findByEvent_Id(Long eventId);

    List<EventSettlement> findAllByOrderByInitiatedAtDesc();
}
