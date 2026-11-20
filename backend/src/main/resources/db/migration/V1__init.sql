CREATE TABLE measurement_profiles (
    id                UUID          PRIMARY KEY,
    name              VARCHAR(120)  NOT NULL,
    notes             VARCHAR(2000),
    measurements_json CLOB          NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE projects (
    id                UUID          PRIMARY KEY,
    name              VARCHAR(120)  NOT NULL,
    garment_type      VARCHAR(40)   NOT NULL,
    profile_id        UUID,
    measurements_json CLOB          NOT NULL,
    params_json       CLOB          NOT NULL,
    seams_json        CLOB          NOT NULL,
    sketch_data_url   CLOB,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_projects_profile FOREIGN KEY (profile_id) REFERENCES measurement_profiles (id) ON DELETE SET NULL
);

CREATE INDEX idx_projects_updated ON projects (updated_at DESC);
