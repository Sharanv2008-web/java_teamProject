-- =============================================================================
-- Deep-Sea Oil & Gas Subsea Pipeline Integrity Diagnostics System
-- Project Assignment Set 30 - Student 2 (Database Host) Deliverable
-- Database Schema: subsea_pipeline_schema.sql
-- Engine: MySQL 8.0+ / MariaDB 10.5+
-- Target Host: OCI VM 2 (Database Host) on Port 3306
-- =============================================================================

-- 1. Database Creation & Context
CREATE DATABASE IF NOT EXISTS subsea_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE subsea_db;

-- 2. Drop Tables if existing in reverse dependency order
DROP VIEW IF EXISTS v_pipeline_hazards;
DROP VIEW IF EXISTS v_segment_integrity_overview;
DROP TABLE IF EXISTS repair_orders;
DROP TABLE IF EXISTS corrosion_audits;
DROP TABLE IF EXISTS pig_inspections;
DROP TABLE IF EXISTS pipe_segments;

-- =============================================================================
-- TABLE 1: pipe_segments
-- Phased Incremental Data Entry: Phase 1 (Segment Registration)
-- Records Segment Code, Asset Category, Subsea Depth, Wall Thickness, Fluid, MAOP
-- =============================================================================
CREATE TABLE pipe_segments (
    segment_id              INT AUTO_INCREMENT PRIMARY KEY,
    segment_code            VARCHAR(30) NOT NULL UNIQUE COMMENT 'Format: PIPE-SUB-##### validated by regex',
    asset_category          ENUM('HIGH-PRESSURE DEEPWATER PIPE', 'SHALLOW COASTAL TRUNK LINE', 'LIQUEFIED GAS FEEDER') NOT NULL,
    subsea_depth_meters     DECIMAL(8, 2) NOT NULL COMMENT 'Depth in meters (sea-depth notes cleaned via trim)',
    wall_thickness_mm       DECIMAL(6, 2) NOT NULL COMMENT 'Nominal pipeline wall thickness in mm',
    fluid_type              VARCHAR(50) NOT NULL COMMENT 'Crude Oil, Liquefied Gas, Multiphase, Natural Gas',
    maop_psi                DECIMAL(8, 2) NOT NULL COMMENT 'Maximum Allowable Operating Pressure in PSI',
    max_wall_loss_cap_pct   DECIMAL(5, 2) NOT NULL COMMENT '30% for High-Pressure, 35% for Coastal, 25% for Gas Feeder',
    installation_year       INT DEFAULT 2018,
    location_zone           VARCHAR(100) DEFAULT 'Gulf of Mexico - Block 42',
    registered_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_depth_positive CHECK (subsea_depth_meters >= 0),
    CONSTRAINT chk_wall_thickness CHECK (wall_thickness_mm > 0),
    CONSTRAINT chk_maop_positive CHECK (maop_psi > 0),
    CONSTRAINT chk_wall_cap_positive CHECK (max_wall_loss_cap_pct > 0 AND max_wall_loss_cap_pct <= 100)
) ENGINE=InnoDB;

CREATE INDEX idx_segments_category ON pipe_segments(asset_category);
CREATE INDEX idx_segments_depth ON pipe_segments(subsea_depth_meters);

-- =============================================================================
-- TABLE 2: pig_inspections
-- Phased Incremental Data Entry: Phase 2 (Smart PIG Inspection Telemetry)
-- Records Ultrasonic Wall Loss (%), Cathodic Protection Voltage (-V), Internal Pressure (PSI)
-- =============================================================================
CREATE TABLE pig_inspections (
    inspection_id               INT AUTO_INCREMENT PRIMARY KEY,
    segment_code                VARCHAR(30) NOT NULL,
    pig_run_tag                 VARCHAR(50) NOT NULL COMMENT 'Extracted tag using substring, e.g. PIG-RUN-2026-X1',
    ultrasonic_wall_loss_pct    DECIMAL(5, 2) NOT NULL COMMENT 'Measured wall loss percentage (0.00% to 100.00%)',
    cathodic_protection_voltage DECIMAL(5, 3) NOT NULL COMMENT 'Measured Cathodic Protection Voltage (-V)',
    internal_pressure_psi       DECIMAL(8, 2) NOT NULL COMMENT 'Actual Operating Pressure in PSI',
    odometer_distance_km        DECIMAL(8, 2) DEFAULT 0.00 COMMENT 'Inspection distance along pipeline',
    raw_telemetry_line          TEXT COMMENT 'Original ingested telemetry string',
    inspection_timestamp        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_inspections_segment
        FOREIGN KEY (segment_code) REFERENCES pipe_segments(segment_code)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_wall_loss_range CHECK (ultrasonic_wall_loss_pct >= 0 AND ultrasonic_wall_loss_pct <= 100),
    CONSTRAINT chk_internal_pressure CHECK (internal_pressure_psi >= 0)
) ENGINE=InnoDB;

CREATE INDEX idx_inspections_segment ON pig_inspections(segment_code);
CREATE INDEX idx_inspections_tag ON pig_inspections(pig_run_tag);
CREATE INDEX idx_inspections_time ON pig_inspections(inspection_timestamp);

-- =============================================================================
-- TABLE 3: corrosion_audits
-- Phased Incremental Data Entry: Phase 3 (Flow Audit & Diagnostics Evaluation)
-- Records Flow Volume Up/Down (BPD), Acoustic Leak Frequency, Hydrophone Noise Score,
-- Environmental Corrosion, Anchor Drag Risk, Temp Variance, PCI, Rupture Risk, Grade, Hazard Flag
-- =============================================================================
CREATE TABLE corrosion_audits (
    audit_id                    INT AUTO_INCREMENT PRIMARY KEY,
    segment_code                VARCHAR(30) NOT NULL,
    inspection_id               INT NOT NULL,
    upstream_flow_bpd           DECIMAL(10, 2) NOT NULL COMMENT 'Upstream Flow in Barrels Per Day',
    downstream_flow_bpd         DECIMAL(10, 2) NOT NULL COMMENT 'Downstream Flow in Barrels Per Day',
    flow_pressure_drop_pct      DECIMAL(5, 2) NOT NULL COMMENT 'Calculated or measured flow pressure drop %',
    acoustic_leak_freq_khz      DECIMAL(6, 2) NOT NULL COMMENT 'Acoustic Leak Frequency (kHz)',
    hydrophone_noise_score      DECIMAL(5, 2) NOT NULL COMMENT 'Hydrophone Noise Score (0 to 100)',
    environmental_corrosion_pct DECIMAL(5, 2) DEFAULT 0.00 COMMENT 'Used in SHALLOW COASTAL TRUNK LINE (40% weight)',
    anchor_drag_risk_score      DECIMAL(5, 2) DEFAULT 0.00 COMMENT 'Used in SHALLOW COASTAL TRUNK LINE (20% weight)',
    temp_variance_celsius       DECIMAL(5, 2) DEFAULT 0.00 COMMENT 'Used in LIQUEFIED GAS FEEDER (30% weight)',
    
    -- Mathematical Formula 1: PCI = 100 - (Wall Loss % * 2.0 + (1.2 - Cathodic Voltage) * 50)
    pipeline_corrosion_index    DECIMAL(6, 2) NOT NULL COMMENT 'PCI score',
    
    -- Mathematical Formula 2: Rupture Risk Factor = (Operating Pressure / MAOP) * (Wall Loss % / 100)
    rupture_risk_factor         DECIMAL(6, 4) NOT NULL COMMENT 'Rupture Risk Factor',
    
    -- Asset-Specific Weighted Composite Score (0.0 to 100.0)
    composite_health_score      DECIMAL(5, 2) NOT NULL COMMENT 'Category weighted health score',
    
    -- Integrity Bands:
    -- Grade A: 90-100 Score -> Unrestricted Operation
    -- Stable Grade B: 75-89 Score -> Normal Service Flow
    -- Repair Required Grade C: 60-74 Score -> Maintenance Sleeve Ordered
    -- Shutdown Order: <60 Score or Hazard Flagged -> Emergency Line Closure
    integrity_grade             CHAR(1) NOT NULL COMMENT 'A, B, C, D (Shutdown)',
    operational_status          VARCHAR(60) NOT NULL COMMENT 'Action band or shutdown order',
    
    -- Critical Corrosion Filter:
    -- Wall Loss > Category Cap OR Cathodic Protection Voltage < 0.85V -> Flagged CRITICAL_CORROSION_HAZARD
    hazard_flag                 BOOLEAN NOT NULL DEFAULT FALSE,
    hazard_description          VARCHAR(255) DEFAULT 'NONE',
    audit_timestamp             TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_audits_segment
        FOREIGN KEY (segment_code) REFERENCES pipe_segments(segment_code)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_audits_inspection
        FOREIGN KEY (inspection_id) REFERENCES pig_inspections(inspection_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_integrity_grade CHECK (integrity_grade IN ('A', 'B', 'C', 'D'))
) ENGINE=InnoDB;

CREATE INDEX idx_audits_segment ON corrosion_audits(segment_code);
CREATE INDEX idx_audits_grade ON corrosion_audits(integrity_grade);
CREATE INDEX idx_audits_hazard ON corrosion_audits(hazard_flag);
CREATE INDEX idx_audits_pci ON corrosion_audits(pipeline_corrosion_index);

-- =============================================================================
-- TABLE 4: repair_orders
-- Subsea Repair Authorizations & Maintenance Orders
-- =============================================================================
CREATE TABLE repair_orders (
    order_id                    INT AUTO_INCREMENT PRIMARY KEY,
    order_code                  VARCHAR(40) NOT NULL UNIQUE COMMENT 'Format: REPAIR-ORD-#####',
    segment_code                VARCHAR(30) NOT NULL,
    audit_id                    INT NOT NULL,
    integrity_grade             CHAR(1) NOT NULL,
    severity_band               VARCHAR(40) NOT NULL COMMENT 'CRITICAL_EMERGENCY, MAINTENANCE_REQUIRED, MONITORING',
    recommended_action          VARCHAR(120) NOT NULL COMMENT 'Maintenance Sleeve Ordered, Emergency Line Closure, etc.',
    pressure_reduction_pct      DECIMAL(5, 2) DEFAULT 0.00 COMMENT 'Immediate mandatory pressure reduction %',
    target_operating_pressure   DECIMAL(8, 2) NOT NULL COMMENT 'New restricted safe pressure limit in PSI',
    order_status                VARCHAR(30) NOT NULL DEFAULT 'AUTHORIZED' COMMENT 'AUTHORIZED, DISPATCHED, COMPLETED',
    authorized_by               VARCHAR(80) NOT NULL DEFAULT 'SYSTEM_CONTROLLER_VM1',
    created_at                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_orders_segment
        FOREIGN KEY (segment_code) REFERENCES pipe_segments(segment_code)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_orders_audit
        FOREIGN KEY (audit_id) REFERENCES corrosion_audits(audit_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE INDEX idx_orders_segment ON repair_orders(segment_code);
CREATE INDEX idx_orders_status ON repair_orders(order_status);
CREATE INDEX idx_orders_severity ON repair_orders(severity_band);

-- =============================================================================
-- DATABASE VIEWS FOR AUDITING & REPORTING (Deliverable Verification)
-- =============================================================================

-- View 1: Active Critical Pipeline Hazards & Emergency Shutdown Orders
CREATE OR REPLACE VIEW v_pipeline_hazards AS
SELECT 
    ps.segment_code,
    ps.asset_category,
    ps.subsea_depth_meters,
    ps.fluid_type,
    pi.ultrasonic_wall_loss_pct,
    ps.max_wall_loss_cap_pct,
    pi.cathodic_protection_voltage,
    pi.internal_pressure_psi,
    ca.pipeline_corrosion_index AS pci,
    ca.rupture_risk_factor,
    ca.composite_health_score,
    ca.integrity_grade,
    ca.hazard_description,
    ro.order_code,
    ro.recommended_action,
    ro.pressure_reduction_pct,
    ro.target_operating_pressure
FROM pipe_segments ps
JOIN pig_inspections pi ON ps.segment_code = pi.segment_code
JOIN corrosion_audits ca ON pi.inspection_id = ca.inspection_id
LEFT JOIN repair_orders ro ON ca.audit_id = ro.audit_id
WHERE ca.hazard_flag = TRUE OR ca.integrity_grade = 'D';

-- View 2: Comprehensive Pipeline Integrity Master Overview
CREATE OR REPLACE VIEW v_segment_integrity_overview AS
SELECT 
    ps.segment_code,
    ps.asset_category,
    ps.subsea_depth_meters,
    ps.wall_thickness_mm,
    ps.maop_psi,
    pi.pig_run_tag,
    pi.ultrasonic_wall_loss_pct,
    pi.cathodic_protection_voltage,
    pi.internal_pressure_psi,
    ca.upstream_flow_bpd,
    ca.downstream_flow_bpd,
    ca.flow_pressure_drop_pct,
    ca.acoustic_leak_freq_khz,
    ca.pipeline_corrosion_index AS pci,
    ca.rupture_risk_factor,
    ca.composite_health_score,
    ca.integrity_grade,
    ca.operational_status,
    ca.hazard_flag,
    COALESCE(ro.order_code, 'N/A - NORMAL') AS repair_order_code,
    COALESCE(ro.recommended_action, 'No Repair Required') AS repair_action
FROM pipe_segments ps
LEFT JOIN pig_inspections pi ON ps.segment_code = pi.segment_code
LEFT JOIN corrosion_audits ca ON pi.inspection_id = ca.inspection_id
LEFT JOIN repair_orders ro ON ca.audit_id = ro.audit_id;

-- =============================================================================
-- SEED DATA INSERTION (Verification Baseline for Student 2)
-- =============================================================================

-- Phase 1 Registrations
INSERT INTO pipe_segments 
    (segment_code, asset_category, subsea_depth_meters, wall_thickness_mm, fluid_type, maop_psi, max_wall_loss_cap_pct, location_zone)
VALUES
    ('PIPE-SUB-10001', 'HIGH-PRESSURE DEEPWATER PIPE', 2150.50, 38.50, 'Crude Oil', 4200.00, 30.00, 'Green Canyon Ultra-Deep 800'),
    ('PIPE-SUB-10002', 'HIGH-PRESSURE DEEPWATER PIPE', 1890.00, 35.00, 'Crude Oil', 3900.00, 30.00, 'Mississippi Canyon Deep'),
    ('PIPE-SUB-20001', 'SHALLOW COASTAL TRUNK LINE',   145.20, 22.00, 'Multiphase', 1600.00, 35.00, 'Galveston Coastal Shelf'),
    ('PIPE-SUB-20002', 'SHALLOW COASTAL TRUNK LINE',    85.00, 20.00, 'Multiphase', 1450.00, 35.00, 'Mobile Bay Coastal Transition'),
    ('PIPE-SUB-30001', 'LIQUEFIED GAS FEEDER',        620.80, 28.00, 'Liquefied Gas', 2800.00, 25.00, 'Garden Banks Gas Corridor'),
    ('PIPE-SUB-30002', 'LIQUEFIED GAS FEEDER',        710.00, 26.50, 'Liquefied Gas', 2700.00, 25.00, 'Alaminos Canyon Gas Feeder');

-- Phase 2 PIG Inspection Records
INSERT INTO pig_inspections 
    (segment_code, pig_run_tag, ultrasonic_wall_loss_pct, cathodic_protection_voltage, internal_pressure_psi, odometer_distance_km, raw_telemetry_line)
VALUES
    ('PIPE-SUB-10001', 'PIG-TAG-DW-901', 12.50, 1.050, 3800.00, 45.20, 'TAG:PIG-TAG-DW-901|PIPE-SUB-10001|DEPTH:2150.50m|LOSS:12.50%|CP:-1.050V|PRES:3800.00PSI'),
    ('PIPE-SUB-10002', 'PIG-TAG-DW-902', 32.80, 0.820, 3650.00, 88.50, 'TAG:PIG-TAG-DW-902|PIPE-SUB-10002|DEPTH:1890.00m|LOSS:32.80%|CP:-0.820V|PRES:3650.00PSI'),
    ('PIPE-SUB-20001', 'PIG-TAG-CS-301', 18.00, 1.100, 1300.00, 15.60, 'TAG:PIG-TAG-CS-301|PIPE-SUB-20001|DEPTH: 145.20m |LOSS:18.00%|CP:-1.100V|PRES:1300.00PSI'),
    ('PIPE-SUB-20002', 'PIG-TAG-CS-302', 26.50, 0.980, 1250.00, 32.10, 'TAG:PIG-TAG-CS-302|PIPE-SUB-20002|DEPTH:  85.00m |LOSS:26.50%|CP:-0.980V|PRES:1250.00PSI'),
    ('PIPE-SUB-30001', 'PIG-TAG-GF-501',  8.40, 1.150, 2400.00, 22.40, 'TAG:PIG-TAG-GF-501|PIPE-SUB-30001|DEPTH: 620.80m |LOSS:08.40%|CP:-1.150V|PRES:2400.00PSI'),
    ('PIPE-SUB-30002', 'PIG-TAG-GF-502', 28.20, 0.840, 2550.00, 64.80, 'TAG:PIG-TAG-GF-502|PIPE-SUB-30002|DEPTH: 710.00m |LOSS:28.20%|CP:-0.840V|PRES:2550.00PSI');

-- Phase 3 Corrosion Audits & Calculations
-- Formula PCI = 100 - (Wall_Loss * 2.0 + (1.2 - CP) * 50)
-- Formula Rupture_Risk = (Operating_Pressure / MAOP) * (Wall_Loss / 100)
INSERT INTO corrosion_audits
    (segment_code, inspection_id, upstream_flow_bpd, downstream_flow_bpd, flow_pressure_drop_pct, acoustic_leak_freq_khz, hydrophone_noise_score, environmental_corrosion_pct, anchor_drag_risk_score, temp_variance_celsius, pipeline_corrosion_index, rupture_risk_factor, composite_health_score, integrity_grade, operational_status, hazard_flag, hazard_description)
VALUES
    -- PIPE-SUB-10001: Grade A (Healthy)
    -- PCI = 100 - (12.50*2 + (1.2 - 1.05)*50) = 100 - (25 + 7.5) = 67.5 -> Health score 92.5
    ('PIPE-SUB-10001', 1, 85000.00, 84850.00, 1.20, 14.50, 18.00, 2.00, 1.00, 1.20, 67.50, 0.1131, 92.50, 'A', 'Unrestricted Operation', FALSE, 'NONE'),

    -- PIPE-SUB-10002: CRITICAL HAZARD (Wall loss 32.8% > Cap 30%, CP 0.82V < 0.85V) -> Emergency Line Closure
    -- PCI = 100 - (32.8*2 + (1.2 - 0.82)*50) = 100 - (65.6 + 19) = 15.4 -> Shutdown
    ('PIPE-SUB-10002', 2, 82000.00, 76500.00, 8.50, 62.80, 85.00, 5.00, 2.00, 2.50, 15.40, 0.3070, 42.00, 'D', 'Emergency Line Closure', TRUE, 'CRITICAL_CORROSION_HAZARD: Wall loss 32.80% exceeds cap 30.00% & CP Voltage 0.820V < 0.85V threshold'),

    -- PIPE-SUB-20001: Grade B (Stable Normal Service Flow)
    ('PIPE-SUB-20001', 3, 45000.00, 44200.00, 3.20, 22.00, 28.00, 12.00, 8.00, 0.80, 59.00, 0.1463, 82.00, 'B', 'Normal Service Flow', FALSE, 'NONE'),

    -- PIPE-SUB-20002: Grade C (Repair Required - Maintenance Sleeve)
    ('PIPE-SUB-20002', 4, 43000.00, 40800.00, 6.10, 38.50, 52.00, 22.50, 14.00, 1.50, 36.00, 0.2285, 68.00, 'C', 'Maintenance Sleeve Ordered', FALSE, 'NONE'),

    -- PIPE-SUB-30001: Grade A (Healthy Gas Feeder)
    ('PIPE-SUB-30001', 5, 120000.00, 119800.00, 1.10, 12.00, 15.00, 1.50, 1.00, 1.00, 80.70, 0.0720, 94.00, 'A', 'Unrestricted Operation', FALSE, 'NONE'),

    -- PIPE-SUB-30002: CRITICAL HAZARD (Wall loss 28.2% > Gas Feeder Cap 25%, CP 0.84V < 0.85V)
    ('PIPE-SUB-30002', 6, 118000.00, 108500.00, 9.80, 71.20, 89.00, 8.00, 3.00, 8.50, 25.60, 0.2663, 44.50, 'D', 'Emergency Line Closure', TRUE, 'CRITICAL_CORROSION_HAZARD: Wall loss 28.20% exceeds cap 25.00% & CP Voltage 0.840V < 0.85V threshold');

-- Phase 4 Repair Authorizations Generated
INSERT INTO repair_orders
    (order_code, segment_code, audit_id, integrity_grade, severity_band, recommended_action, pressure_reduction_pct, target_operating_pressure, order_status, authorized_by)
VALUES
    ('REPAIR-ORD-2026-001', 'PIPE-SUB-10002', 2, 'D', 'CRITICAL_EMERGENCY', 'Emergency Line Closure - Immediate Subsea Isolation Valve Actuation', 50.00, 1825.00, 'AUTHORIZED', 'SAFETY_CONTROLLER_VM1'),
    ('REPAIR-ORD-2026-002', 'PIPE-SUB-20002', 4, 'C', 'MAINTENANCE_REQUIRED', 'Maintenance Sleeve Ordered - Type B Pressure Containment Sleeve', 20.00, 1000.00, 'AUTHORIZED', 'SAFETY_CONTROLLER_VM1'),
    ('REPAIR-ORD-2026-003', 'PIPE-SUB-30002', 6, 'D', 'CRITICAL_EMERGENCY', 'Emergency Line Closure - Flange Isolation & Diver Hyperbaric Welding Repair', 50.00, 1275.00, 'AUTHORIZED', 'SAFETY_CONTROLLER_VM1');

-- Commit Transaction
COMMIT;
