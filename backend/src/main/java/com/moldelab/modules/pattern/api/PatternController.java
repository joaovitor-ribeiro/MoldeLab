package com.moldelab.modules.pattern.api;

import com.moldelab.modules.export.PaperSize;
import com.moldelab.modules.export.PdfPatternRenderer;
import com.moldelab.modules.measurements.api.MeasurementKey;
import com.moldelab.modules.measurements.api.Measurements;
import com.moldelab.modules.pattern.domain.GarmentCatalog;
import com.moldelab.modules.pattern.domain.GarmentType;
import com.moldelab.modules.pattern.domain.ParamSpec;
import com.moldelab.modules.pattern.domain.Pattern;
import com.moldelab.modules.pattern.domain.PatternEngine;
import com.moldelab.modules.pattern.domain.SeamSettings;
import com.moldelab.shared.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PatternController {

    public record SeamRequest(
            @DecimalMin("0") @DecimalMax("5") Double seamCm,
            @DecimalMin("0") @DecimalMax("8") Double hemCm,
            @DecimalMin("0") @DecimalMax("5") Double finishCm) {

        SeamSettings toSettings() {
            SeamSettings d = SeamSettings.DEFAULT;
            return new SeamSettings(
                    seamCm == null ? d.seamMm() : seamCm * 10,
                    hemCm == null ? d.hemMm() : hemCm * 10,
                    finishCm == null ? d.finishMm() : finishCm * 10);
        }
    }

    public record DraftRequest(
            @NotNull GarmentType garmentType,
            @NotNull @Valid Measurements measurements,
            Map<String, Double> params,
            @Valid SeamRequest seams,
            @DecimalMin("50") @DecimalMax("320") Double fabricWidthCm) {}

    public record PdfRequest(
            @NotNull @Valid DraftRequest draft,
            PaperSize paper,
            @Size(max = 80) String title,
            @Size(max = 120) String profileName,
            Boolean showSeamLine) {}

    public record RequiredMeasurement(String key, String jsonName, String label) {}

    public record CatalogItem(GarmentType type, String label, String description,
                              List<RequiredMeasurement> requiredMeasurements, List<ParamSpec> params) {}

    private final GarmentCatalog catalog;
    private final PatternEngine engine;
    private final PdfPatternRenderer pdf;

    public PatternController(GarmentCatalog catalog, PatternEngine engine, PdfPatternRenderer pdf) {
        this.catalog = catalog;
        this.engine = engine;
        this.pdf = pdf;
    }

    @GetMapping("/catalog")
    public List<CatalogItem> catalog() {
        return catalog.all().stream().map(d -> new CatalogItem(d.type(), d.type().label(), d.type().description(),
                d.requiredMeasurements().stream()
                        .map(k -> new RequiredMeasurement(k.name(), k.jsonName(), k.label())).toList(),
                d.params())).toList();
    }

    @PostMapping("/patterns/draft")
    public Pattern draft(@Valid @RequestBody DraftRequest req) {
        return run(req);
    }

    @PostMapping(value = "/patterns/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@Valid @RequestBody PdfRequest req) {
        Pattern pattern = run(req.draft());
        String title = req.title() == null || req.title().isBlank() ? pattern.garmentLabel() : req.title().trim();
        byte[] bytes = pdf.render(pattern, new PdfPatternRenderer.Options(
                req.paper() == null ? PaperSize.A4 : req.paper(), title, req.profileName(),
                req.showSeamLine() == null || req.showSeamLine()));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fileName(title) + ".pdf", StandardCharsets.UTF_8).build().toString())
                .body(bytes);
    }

    private Pattern run(DraftRequest req) {
        try {
            SeamSettings seams = req.seams() == null ? SeamSettings.DEFAULT : req.seams().toSettings();
            double fabric = req.fabricWidthCm() == null ? 1500 : req.fabricWidthCm() * 10;
            return engine.draft(req.garmentType(), req.measurements(), req.params(), seams, fabric);
        } catch (PatternEngine.InvalidMeasurementsException e) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("missing", e.missing().stream().map(MeasurementKey::jsonName).toList());
            details.put("outOfRange", e.outOfRange().stream().map(MeasurementKey::jsonName).toList());
            throw ApiException.business("INVALID_MEASUREMENTS", e.getMessage(), details);
        }
    }

    static String fileName(String title) {
        String ascii = Normalizer.normalize(title, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = ascii.replaceAll("[^A-Za-z0-9]+", "-").replaceAll("(^-|-$)", "").toLowerCase();
        return slug.isEmpty() ? "molde" : slug;
    }
}
