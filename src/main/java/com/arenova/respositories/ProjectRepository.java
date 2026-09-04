package com.arenova.respositories;

import com.arenova.entities.Project;
import com.arenova.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByOrganizerOrderByUpdatedAtDesc(User organizer);

    Optional<Project> findByIdAndOrganizer(Long id, User organizer);
}
