package com.moldelab.modules.measurements;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MeasurementProfileRepository extends JpaRepository<MeasurementProfile, UUID> {
    List<MeasurementProfile> findAllByOrderByUpdatedAtDesc();
}
