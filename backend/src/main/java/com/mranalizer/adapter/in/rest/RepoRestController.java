package com.mranalizer.adapter.in.rest;

import com.mranalizer.adapter.in.rest.dto.SavedRepoResponse;
import com.mranalizer.domain.model.SavedRepository;
import com.mranalizer.domain.port.in.ManageReposUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/repos")
public class RepoRestController {

    private final ManageReposUseCase manageReposUseCase;

    public RepoRestController(ManageReposUseCase manageReposUseCase) {
        this.manageReposUseCase = manageReposUseCase;
    }

    @GetMapping
    public ResponseEntity<List<SavedRepoResponse>> listRepos() {
        List<SavedRepoResponse> response = manageReposUseCase.getAll().stream()
                .map(SavedRepoResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<SavedRepoResponse> addRepo(@RequestBody Map<String, String> body) {
        String projectSlug = body.get("projectSlug");
        String provider = body.getOrDefault("provider", "github");
        SavedRepository saved = manageReposUseCase.add(projectSlug, provider);
        return ResponseEntity.ok(SavedRepoResponse.from(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRepo(@PathVariable Long id) {
        manageReposUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
