package ch.bzz.controller;

import ch.bzz.generated.api.ProjectApi;
import ch.bzz.generated.model.LoginProject200Response;
import ch.bzz.generated.model.LoginRequest;
import ch.bzz.model.Project;
import ch.bzz.repository.ProjectRepository;
import ch.bzz.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class    ProjectApiController implements ProjectApi {

    private final ProjectRepository projectRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final JwtUtil jwtUtil;

    public ProjectApiController(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
        this.jwtUtil = new JwtUtil();
    }

    @Override
    public ResponseEntity<Void> createProject(LoginRequest loginRequest) {
        String projectName = loginRequest.getProjectName();
        if (!projectRepository.findByProjectName(projectName).isEmpty()) {
            return ResponseEntity.status(409).build();
        }
        String hashedPassword = encoder.encode(loginRequest.getPassword());
        loginRequest.setPassword(hashedPassword);
        loginRequest.setProjectName(projectName);
        projectRepository.save(new Project(projectName, hashedPassword));
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<LoginProject200Response> loginProject(LoginRequest loginRequest) {
        Project project = projectRepository.findByProjectName(loginRequest.getProjectName()).get(0);
        if (project != null && encoder.matches(loginRequest.getPassword(), project.getPasswordHash())) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(401).build();
    }
}
