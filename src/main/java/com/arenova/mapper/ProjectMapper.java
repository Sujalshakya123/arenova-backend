package com.arenova.mapper;

import com.arenova.dtos.ProjectDTO;
import com.arenova.entities.Project;

public class ProjectMapper {

    public static ProjectDTO toDTO(Project project) {
        return ProjectDTO.builder()
                .id(project.getId())
                .name(project.getName())
                .plan(project.getPlan() != null ? project.getPlan() : "Free")
                .tournamentCount(
                        project.getTournamentCount() != null
                                ? project.getTournamentCount()
                                : 0
                )
                .updatedAt(project.getUpdatedAt())
                .organizerId(
                        project.getOrganizer() != null
                                ? project.getOrganizer().getId()
                                : null
                )
                .build();
    }
}
