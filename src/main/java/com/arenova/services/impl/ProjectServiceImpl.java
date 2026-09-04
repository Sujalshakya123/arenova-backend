package com.arenova.services.impl;

import com.arenova.dtos.CreateProjectRequest;
import com.arenova.dtos.ProjectDTO;
import com.arenova.entities.Project;
import com.arenova.entities.User;
import com.arenova.exceptions.ResourceNotFoundException;
import com.arenova.mapper.ProjectMapper;
import com.arenova.respositories.ProjectRepository;
import com.arenova.respositories.UserRepository;
import com.arenova.security.OrganizerAccessSupport;
import com.arenova.services.ProjectService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResourceNotFoundException("Not authenticated");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private User currentOrganizer() {
        User user = currentUser();
        OrganizerAccessSupport.requireActiveOrganizer(user);
        return user;
    }

    @Override
    public List<ProjectDTO> getMyProjects() {
        User organizer = currentOrganizer();
        return projectRepository.findByOrganizerOrderByUpdatedAtDesc(organizer)
                .stream()
                .map(ProjectMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ProjectDTO getMyProjectById(Long id) {
        User organizer = currentOrganizer();
        Project project = projectRepository.findByIdAndOrganizer(id, organizer)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        return ProjectMapper.toDTO(project);
    }

    @Override
    public ProjectDTO createProject(CreateProjectRequest request) throws BadRequestException {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Project name is required.");
        }

        User organizer = currentOrganizer();
        Project project = Project.builder()
                .name(request.getName().trim())
                .plan(
                        request.getPlan() != null && !request.getPlan().isBlank()
                                ? request.getPlan().trim()
                                : "Free"
                )
                .tournamentCount(0)
                .organizer(organizer)
                .build();

        return ProjectMapper.toDTO(projectRepository.save(project));
    }

    @Override
    public ProjectDTO updateProject(Long id, CreateProjectRequest request) throws BadRequestException {
        User organizer = currentOrganizer();
        Project project = projectRepository.findByIdAndOrganizer(id, organizer)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (request.getName() != null && !request.getName().isBlank()) {
            project.setName(request.getName().trim());
        }
        if (request.getPlan() != null && !request.getPlan().isBlank()) {
            project.setPlan(request.getPlan().trim());
        }

        return ProjectMapper.toDTO(projectRepository.save(project));
    }

    @Override
    public void deleteProject(Long id) {
        User organizer = currentOrganizer();
        Project project = projectRepository.findByIdAndOrganizer(id, organizer)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        projectRepository.delete(project);
    }
}
