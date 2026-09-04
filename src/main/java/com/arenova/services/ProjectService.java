package com.arenova.services;

import com.arenova.dtos.CreateProjectRequest;
import com.arenova.dtos.ProjectDTO;
import org.apache.coyote.BadRequestException;

import java.util.List;

public interface ProjectService {
    List<ProjectDTO> getMyProjects();

    ProjectDTO getMyProjectById(Long id);

    ProjectDTO createProject(CreateProjectRequest request) throws BadRequestException;

    ProjectDTO updateProject(Long id, CreateProjectRequest request) throws BadRequestException;

    void deleteProject(Long id);
}
