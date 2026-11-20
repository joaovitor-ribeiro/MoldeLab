package com.moldelab.modules.measurements;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "measurement_profiles")
public class MeasurementProfile {

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 2000)
    private String notes;

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(name = "measurements_json", nullable = false)
    private String measurementsJson;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected MeasurementProfile() {}

    public MeasurementProfile(UUID id, String name, String notes, String measurementsJson, OffsetDateTime now) {
        this.id = id;
        this.name = name;
        this.notes = notes;
        this.measurementsJson = measurementsJson;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String name, String notes, String measurementsJson, OffsetDateTime now) {
        this.name = name;
        this.notes = notes;
        this.measurementsJson = measurementsJson;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getNotes() { return notes; }
    public String getMeasurementsJson() { return measurementsJson; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
