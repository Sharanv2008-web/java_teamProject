-- =============================================================================
-- Deep-Sea Oil & Gas Subsea Pipeline Integrity Diagnostics System
-- Project Assignment Set 30 - Student 2 (Database Host) Deliverable
-- PostgreSQL Variant: subsea_pipeline_schema_postgres.sql
-- Target Host: OCI VM 2 (Database Host) on Port 5432
-- =============================================================================

CREATE DATABASE subsea_db;
\c subsea_db;

DROP VIEW IF EXISTS v_pipeline_hazards;
DROP VIEW IF EXISTS v_segment_integrity_overview;
DROP TABLE IF EXISTS repair_orders CASCADE;
DROP TABLE IF EXISTS corrosion_audits CASCADE;
DROP TABLE IF EXISTS pig_inspections CASCADE;
DROP TABLE IF EXISTS pipe_segments CASCADE;

CREATE TABLE pipe_segments (
    segment_id              SERIAL PRIMARY KEY,
    segment_code            VARCHAR(30) NOT NULL UNIQUE,
    asset_category          VARCHAR(50) NOT NULL CHECK (asset_category IN ('HIGH-PRESSURE DEEPWATER PIPE', 'SHALLOW COASTAL TRUNK LINE', 'LIQUEFIED GAS FEEDER')),
    subsea_depth_meters     NUMERIC(8, 2) NOT NULL CHECK (subsea_depth_meters >= 0),
    wall_thickness_mm       NUMERIC(6, 2) NOT NULL CHECK (wall_thickness_mm > 0),
    fluid_type              VARCHAR(50) NOT NULL,
    maop_psi                NUMERIC(8, 2) NOT NULL CHECK (maop_psi > 0),
    max_wall_loss_cap_pct   NUMERIC(5, 2) NOT NULL CHECK (max_wall_loss_cap_pct > 0 AND max_wall_loss_cap_pct <= 100),
    installation_year       INT DEFAULT 2018,
    location_zone           VARCHAR(100) DEFAULT 'Gulf of Mexico - Block 42',
    registered_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pig_inspections (
    inspection_id               SERIAL PRIMARY KEY,
    segment_code                VARCHAR(30) NOT NULL REFERENCES pipe_segments(segment_code) ON UPDATE CASCADE ON DELETE RESTRICT,
    pig_run_tag                 VARCHAR(50) NOT NULL,
    ultrasonic_wall_loss_pct    NUMERIC(5, 2) NOT NULL CHECK (ultrasonic_wall_loss_pct >= 0 AND ultrasonic_wall_loss_pct <= 100),
    cathodic_protection_voltage NUMERIC(5, 3) NOT NULL,
    internal_pressure_psi       NUMERIC(8, 2) NOT NULL CHECK (internal_pressure_psi >= 0),
    odometer_distance_km        NUMERIC(8, 2) DEFAULT 0.00,
    raw_telemetry_line          TEXT,
    inspection_timestamp        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE corrosion_audits (
    audit_id                    SERIAL PRIMARY KEY,
    segment_code                VARCHAR(30) NOT NULL REFERENCES pipe_segments(segment_code) ON UPDATE CASCADE ON DELETE RESTRICT,
    inspection_id               INT NOT NULL REFERENCES pig_inspections(inspection_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    upstream_flow_bpd           NUMERIC(10, 2) NOT NULL,
    downstream_flow_bpd         NUMERIC(10, 2) NOT NULL,
    flow_pressure_drop_pct      NUMERIC(5, 2) NOT NULL,
    acoustic_leak_freq_khz      NUMERIC(6, 2) NOT NULL,
    hydrophone_noise_score      NUMERIC(5, 2) NOT NULL,
    environmental_corrosion_pct NUMERIC(5, 2) DEFAULT 0.00,
    anchor_drag_risk_score      NUMERIC(5, 2) DEFAULT 0.00,
    temp_variance_celsius       NUMERIC(5, 2) DEFAULT 0.00,
    pipeline_corrosion_index    NUMERIC(6, 2) NOT NULL,
    rupture_risk_factor         NUMERIC(6, 4) NOT NULL,
    composite_health_score      NUMERIC(5, 2) NOT NULL,
    integrity_grade             CHAR(1) NOT NULL CHECK (integrity_grade IN ('A', 'B', 'C', 'D')),
    operational_status          VARCHAR(60) NOT NULL,
    hazard_flag                 BOOLEAN NOT NULL DEFAULT FALSE,
    hazard_description          VARCHAR(255) DEFAULT 'NONE',
    audit_timestamp             TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE repair_orders (
    order_id                    SERIAL PRIMARY KEY,
    order_code                  VARCHAR(40) NOT NULL UNIQUE,
    segment_code                VARCHAR(30) NOT NULL REFERENCES pipe_segments(segment_code) ON UPDATE CASCADE ON DELETE RESTRICT,
    audit_id                    INT NOT NULL REFERENCES corrosion_audits(audit_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    integrity_grade             CHAR(1) NOT NULL,
    severity_band               VARCHAR(40) NOT NULL,
    recommended_action          VARCHAR(120) NOT NULL,
    pressure_reduction_pct      NUMERIC(5, 2) DEFAULT 0.00,
    target_operating_pressure   NUMERIC(8, 2) NOT NULL,
    order_status                VARCHAR(30) NOT NULL DEFAULT 'AUTHORIZED',
    authorized_by               VARCHAR(80) NOT NULL DEFAULT 'SYSTEM_CONTROLLER_VM1',
    created_at                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
