package com.moldelab.modules.projects;

import com.fasterxml.jackson.core.type.TypeReference;
import com.moldelab.modules.measurements.api.Measurements;
import com.moldelab.modules.pattern.api.PatternController.SeamRequest;
import com.moldelab.modules.pattern.domain.GarmentType;
import com.moldelab.shared.ApiException;
import com.moldelab.shared.JsonCodec;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Projetos salvos: modelo + parâmetros + medidas + (opcional) desenho/foto de referência. */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    static final int MAX_SKETCH_CHARS = 6_000_000; // ~4,5 MB de imagem em base64

    public record ProjectRequest(
            @NotBlank @Size(max = 120) String name,
            @NotNull GarmentType garmentType,
            UUID profileId,
            @NotNull @Valid Measurements measurements,
            Map<String, Double> params,
            @Valid SeamRequest seams,
            @Size(max = MAX_SKETCH_CHARS) @Pattern(regexp = "^data:image/(png|jpeg|webp);base64,[A-Za-z0-9+/=]+$",
                    message = "deve ser uma imagem PNG/JPEG/WEBP em data URL") String sketchDataUrl) {}

    public record ProjectSummary(UUID id, String name, GarmentType garmentType, boolean hasSketch, OffsetDateTime updatedAt) {}

    public record ProjectResponse(UUID id, String name, GarmentType garmentType, UUID profileId, Measurements measurements,
                                  Map<String, Double> params, SeamRequest seams, String sketchDataUrl,
                                  OffsetDateTime createdAt, OffsetDateTime updatedAt) {}

    private static final TypeReference<Map<String, Double>> PARAMS = new TypeReference<>() {};

    private final ProjectRepository repo;
    private final JsonCodec json;
    private final Clock clock;

    public ProjectController(ProjectRepository repo, JsonCodec json, Clock clock) {
        this.repo = repo;
        this.json = json;
        this.clock = clock;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<ProjectSummary> list() {
        return repo.findAllByOrderByUpdatedAtDesc().stream()
                .map(p -> new ProjectSummary(p.getId(), p.getName(), GarmentType.valueOf(p.getGarmentType()),
                        p.getSketchDataUrl() != null, p.getUpdatedAt()))
                .toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ProjectResponse get(@PathVariable UUID id) {
        return toResponse(find(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ProjectResponse create(@Valid @RequestBody ProjectRequest req) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        Project p = new Project(UUID.randomUUID(), now);
        apply(p, req, now);
        return toResponse(repo.save(p));
    }

    @PutMapping("/{id}")
    @Transactional
    public ProjectResponse update(@PathVariable UUID id, @Valid @RequestBody ProjectRequest req) {
        Project p = find(id);
        apply(p, req, OffsetDateTime.now(clock));
        return toResponse(p);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable UUID id) {
        repo.delete(find(id));
    }

    private void apply(Project p, ProjectRequest req, OffsetDateTime now) {
        SeamRequest seams = req.seams() == null ? new SeamRequest(null, null, null) : req.seams();
        p.apply(req.name().trim(), req.garmentType().name(), req.profileId(), json.write(req.measurements()),
                json.write(req.params() == null ? Map.of() : req.params()), json.write(seams), req.sketchDataUrl(), now);
    }

    private Project find(UUID id) {
        return repo.findById(id).orElseThrow(() -> ApiException.notFound("Projeto"));
    }

    private ProjectResponse toResponse(Project p) {
        return new ProjectResponse(p.getId(), p.getName(), GarmentType.valueOf(p.getGarmentType()), p.getProfileId(),
                json.read(p.getMeasurementsJson(), Measurements.class), json.read(p.getParamsJson(), PARAMS),
                json.read(p.getSeamsJson(), SeamRequest.class), p.getSketchDataUrl(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
