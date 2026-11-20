package com.moldelab.modules.measurements;

import com.moldelab.modules.measurements.api.MeasurementKey;
import com.moldelab.modules.measurements.api.Measurements;
import com.moldelab.modules.measurements.api.StandardSizes;
import com.moldelab.shared.ApiException;
import com.moldelab.shared.JsonCodec;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class MeasurementController {

    public record MeasurementGuide(String key, String jsonName, String label, String howToMeasure, double minCm, double maxCm) {}

    public record ProfileRequest(@NotBlank @Size(max = 120) String name,
                                 @Size(max = 2000) String notes,
                                 @NotNull @Valid Measurements measurements) {}

    public record ProfileResponse(UUID id, String name, String notes, Measurements measurements,
                                  OffsetDateTime createdAt, OffsetDateTime updatedAt) {}

    private final MeasurementProfileRepository repo;
    private final JsonCodec json;
    private final Clock clock;

    public MeasurementController(MeasurementProfileRepository repo, JsonCodec json, Clock clock) {
        this.repo = repo;
        this.json = json;
        this.clock = clock;
    }

    @GetMapping("/measurements/guide")
    public List<MeasurementGuide> guide() {
        return Arrays.stream(MeasurementKey.values())
                .map(k -> new MeasurementGuide(k.name(), k.jsonName(), k.label(), k.howToMeasure(), k.minCm(), k.maxCm()))
                .toList();
    }

    @GetMapping("/measurements/standard-sizes")
    public List<StandardSizes.StandardSize> standardSizes() {
        return StandardSizes.all();
    }

    @GetMapping("/profiles")
    @Transactional(readOnly = true)
    public List<ProfileResponse> list() {
        return repo.findAllByOrderByUpdatedAtDesc().stream().map(this::toResponse).toList();
    }

    @GetMapping("/profiles/{id}")
    @Transactional(readOnly = true)
    public ProfileResponse get(@PathVariable UUID id) {
        return toResponse(find(id));
    }

    @PostMapping("/profiles")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ProfileResponse create(@Valid @RequestBody ProfileRequest req) {
        MeasurementProfile p = new MeasurementProfile(UUID.randomUUID(), req.name().trim(), req.notes(),
                json.write(req.measurements()), OffsetDateTime.now(clock));
        return toResponse(repo.save(p));
    }

    @PutMapping("/profiles/{id}")
    @Transactional
    public ProfileResponse update(@PathVariable UUID id, @Valid @RequestBody ProfileRequest req) {
        MeasurementProfile p = find(id);
        p.update(req.name().trim(), req.notes(), json.write(req.measurements()), OffsetDateTime.now(clock));
        return toResponse(p);
    }

    @DeleteMapping("/profiles/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable UUID id) {
        repo.delete(find(id));
    }

    private MeasurementProfile find(UUID id) {
        return repo.findById(id).orElseThrow(() -> ApiException.notFound("Perfil de medidas"));
    }

    private ProfileResponse toResponse(MeasurementProfile p) {
        return new ProfileResponse(p.getId(), p.getName(), p.getNotes(),
                json.read(p.getMeasurementsJson(), Measurements.class), p.getCreatedAt(), p.getUpdatedAt());
    }
}
