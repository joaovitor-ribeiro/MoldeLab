package com.moldelab.modules.projects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "garment_type", nullable = false, length = 40)
    private String garmentType;

    @Column(name = "profile_id")
    private UUID profileId;

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(name = "measurements_json", nullable = false)
    private String measurementsJson;

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(name = "params_json", nullable = false)
    private String paramsJson;

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(name = "seams_json", nullable = false)
    private String seamsJson;

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(name = "sketch_data_url")
    private String sketchDataUrl;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Project() {}

    public Project(UUID id, OffsetDateTime now) {
        this.id = id;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void apply(String name, String garmentType, UUID profileId, String measurementsJson, String paramsJson,
                      String seamsJson, String sketchDataUrl, OffsetDateTime now) {
        this.name = name;
        this.garmentType = garmentType;
        this.profileId = profileId;
        this.measurementsJson = measurementsJson;
        this.paramsJson = paramsJson;
        this.seamsJson = seamsJson;
        this.sketchDataUrl = sketchDataUrl;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getGarmentType() { return garmentType; }
    public UUID getProfileId() { return profileId; }
    public String getMeasurementsJson() { return measurementsJson; }
    public String getParamsJson() { return paramsJson; }
    public String getSeamsJson() { return seamsJson; }
    public String getSketchDataUrl() { return sketchDataUrl; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
