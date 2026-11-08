-- =====================================================================
-- SUBSEA PIPELINE INTEGRITY MONITORING SYSTEM
-- DATABASE HOST SETUP SCRIPT (STUDENT 2 DELIVERABLE)
-- Database Engine: MySQL 8.0+ / MariaDB 10.5+
-- Database Name: subsea_pipeline
-- Target Host: OCI VM 2 (Database Host) on Port 3306
-- =====================================================================

-- 1. DATABASE CREATION
CREATE DATABASE IF NOT EXISTS subsea_pipeline
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE subsea_pipeline;

-- 2. TABLE 1: pipe_segments (Phase 1: Segment Registration)
CREATE TABLE IF NOT EXISTS pipe_segments (
    segment_id INT AUTO_INCREMENT PRIMARY KEY,
    segment_code VARCHAR(30) UNIQUE NOT NULL,
    category VARCHAR(50) NOT NULL,
    subsea_depth_m DECIMAL(10,2) NOT NULL,
    wall_thickness_mm DECIMAL(10,2) NOT NULL,
    fluid_type VARCHAR(50) NOT NULL,
    maop_psi DECIMAL(10,2) NOT NULL,
    max_wall_loss_cap_pct DECIMAL(5,2) DEFAULT 30.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 3. TABLE 2: pig_inspections (Phase 2: Smart PIG Inspection Telemetry)
CREATE TABLE IF NOT EXISTS pig_inspections (
    inspection_id INT AUTO_INCREMENT PRIMARY KEY,
    segment_id INT NOT NULL,
    pig_run_tag VARCHAR(50) DEFAULT 'PIG-TAG-DEFAULT',
    wall_loss_percent DECIMAL(5,2) NOT NULL,
    cathodic_voltage_v DECIMAL(5,3) NOT NULL,
    internal_pressure_psi DECIMAL(10,2) NOT NULL,
    inspection_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pig_segment FOREIGN KEY (segment_id)
        REFERENCES pipe_segments(segment_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 4. TABLE 3: corrosion_audits (Phase 3: Flow Audit & Diagnostics)
CREATE TABLE IF NOT EXISTS corrosion_audits (
    audit_id INT AUTO_INCREMENT PRIMARY KEY,
    segment_id INT NOT NULL,
    upstream_flow_bpd DECIMAL(12,2) NOT NULL,
    downstream_flow_bpd DECIMAL(12,2) NOT NULL,
    flow_pressure_drop_pct DECIMAL(5,2) DEFAULT 0.00,
    acoustic_leak_hz DECIMAL(10,2) NOT NULL,
    hydrophone_noise_score DECIMAL(5,2) NOT NULL,
    environmental_corrosion_pct DECIMAL(5,2) DEFAULT 0.00,
    anchor_drag_risk_score DECIMAL(5,2) DEFAULT 0.00,
    temp_variance_celsius DECIMAL(5,2) DEFAULT 0.00,
    audit_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_segment FOREIGN KEY (segment_id)
        REFERENCES pipe_segments(segment_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 5. TABLE 4: repair_orders (Phase 4: Subsea Repair Authorizations)
CREATE TABLE IF NOT EXISTS repair_orders (
    repair_id INT AUTO_INCREMENT PRIMARY KEY,
    segment_id INT NOT NULL,
    hazard_code VARCHAR(100) NOT NULL,
    integrity_score DECIMAL(5,2) NOT NULL,
    integrity_grade VARCHAR(10) NOT NULL,
    rupture_risk_factor DECIMAL(10,4) NOT NULL,
    action_required VARCHAR(255) NOT NULL,
    pressure_reduction_pct DECIMAL(5,2) DEFAULT 0.00,
    target_operating_pressure DECIMAL(10,2) DEFAULT 0.00,
    repair_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_repair_segment FOREIGN KEY (segment_id)
        REFERENCES pipe_segments(segment_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 6. APPLICATION USERS & PRIVILEGES (STUDENT 2 RESPONSIBILITY)
-- Primary user for Student 1 Application Host
CREATE USER IF NOT EXISTS 'subsea_app'@'%' IDENTIFIED BY 'SubseaApp@2026#ChangeMe';
GRANT ALL PRIVILEGES ON subsea_pipeline.* TO 'subsea_app'@'%';

-- Secondary user for Set 30 compatibility
CREATE USER IF NOT EXISTS 'subsea_user'@'%' IDENTIFIED BY 'SubseaSecurePass2026!';
GRANT ALL PRIVILEGES ON subsea_pipeline.* TO 'subsea_user'@'%';

FLUSH PRIVILEGES;

-- 7. BASELINE SAMPLE DATA FOR TESTING (ALL 3 ASSET CATEGORIES)
-- Phase 1: Segments
INSERT IGNORE INTO pipe_segments
    (segment_id, segment_code, category, subsea_depth_m, wall_thickness_mm, fluid_type, maop_psi, max_wall_loss_cap_pct)
VALUES
    (1, 'PIPE-SUB-00001', 'HIGH_PRESSURE_DEEPWATER', 2500.00, 35.50, 'CRUDE_OIL', 5000.00, 30.00),
    (2, 'PIPE-SUB-00002', 'SHALLOW_COASTAL_TRUNK',   150.00,  28.00, 'NATURAL_GAS', 3000.00, 35.00),
    (3, 'PIPE-SUB-00003', 'LIQUEFIED_GAS_FEEDER',    800.00,  32.00, 'LIQUEFIED_GAS', 4000.00, 25.00),
    (4, 'PIPE-SUB-00004', 'HIGH_PRESSURE_DEEPWATER', 2150.00, 38.00, 'CRUDE_OIL', 4800.00, 30.00),
    (5, 'PIPE-SUB-00005', 'SHALLOW_COASTAL_TRUNK',   120.00,  25.00, 'MULTIPHASE', 2500.00, 35.00),
    (6, 'PIPE-SUB-00006', 'LIQUEFIED_GAS_FEEDER',    650.00,  27.00, 'LIQUEFIED_GAS', 3500.00, 25.00);

-- Phase 2: PIG Inspections (Includes Critical Hazards: Wall Loss > Cap or CP < 0.85V)
INSERT INTO pig_inspections (segment_id, pig_run_tag, wall_loss_percent, cathodic_voltage_v, internal_pressure_psi)
SELECT p.segment_id, 'PIG-TAG-DW-101', 10.00, 1.050, 3500.00
FROM pipe_segments p WHERE p.segment_code = 'PIPE-SUB-00001'
AND NOT EXISTS (SELECT 1 FROM pig_inspections i WHERE i.segment_id = p.segment_id)
UNION ALL
-- Segment 2: CRITICAL HAZARD: CP Voltage 0.82V < 0.85V
SELECT p.segment_id, 'PIG-TAG-CS-201', 20.00, 0.820, 2200.00
FROM pipe_segments p WHERE p.segment_code = 'PIPE-SUB-00002'
AND NOT EXISTS (SELECT 1 FROM pig_inspections i WHERE i.segment_id = p.segment_id)
UNION ALL
-- Segment 3: CRITICAL HAZARD: Wall loss 28.5% > Gas Feeder Cap 25.0%
SELECT p.segment_id, 'PIG-TAG-GF-301', 28.50, 0.840, 2800.00
FROM pipe_segments p WHERE p.segment_code = 'PIPE-SUB-00003'
AND NOT EXISTS (SELECT 1 FROM pig_inspections i WHERE i.segment_id = p.segment_id)
UNION ALL
SELECT p.segment_id, 'PIG-TAG-DW-102', 12.00, 1.100, 3900.00
FROM pipe_segments p WHERE p.segment_code = 'PIPE-SUB-00004'
AND NOT EXISTS (SELECT 1 FROM pig_inspections i WHERE i.segment_id = p.segment_id)
UNION ALL
SELECT p.segment_id, 'PIG-TAG-CS-202', 18.50, 0.950, 1800.00
FROM pipe_segments p WHERE p.segment_code = 'PIPE-SUB-00005'
AND NOT EXISTS (SELECT 1 FROM pig_inspections i WHERE i.segment_id = p.segment_id)
UNION ALL
SELECT p.segment_id, 'PIG-TAG-GF-302',  9.00, 1.150, 2400.00
FROM pipe_segments p WHERE p.segment_code = 'PIPE-SUB-00006'
AND NOT EXISTS (SELECT 1 FROM pig_inspections i WHERE i.segment_id = p.segment_id);

-- Phase 3: Corrosion Audits
INSERT INTO corrosion_audits 
    (segment_id, upstream_flow_bpd, downstream_flow_bpd, flow_pressure_drop_pct, acoustic_leak_hz, hydrophone_noise_score, environmental_corrosion_pct, anchor_drag_risk_score, temp_variance_celsius)
SELECT p.segment_id, 10000.00, 9850.00, 1.50, 120.00, 20.00, 2.00, 1.00, 1.00
FROM pipe_segments p WHERE p.segment_code = 'PIPE-SUB-00001'
AND NOT EXISTS (SELECT 1 FROM corrosion_audits c WHERE c.segment_id = p.segment_id)
UNION ALL
SELECT p.segment_id, 8000.00, 7200.00, 10.00, 450.00, 75.00, 25.00, 15.00, 2.00
FROM pipe_segments p WHERE p.segment_code = 'PIPE-SUB-00002'
AND NOT EXISTS (SELECT 1 FROM corrosion_audits c WHERE c.segment_id = p.segment_id)
UNION ALL
SELECT p.segment_id, 9000.00, 8100.00, 10.00, 520.00, 85.00, 8.00, 3.00, 8.00
FROM pipe_segments p WHERE p.segment_code = 'PIPE-SUB-00003'
AND NOT EXISTS (SELECT 1 FROM corrosion_audits c WHERE c.segment_id = p.segment_id)
UNION ALL
SELECT p.segment_id, 11000.00, 10900.00, 0.90, 85.00, 15.00, 1.50, 0.50, 1.20
FROM pipe_segments p WHERE p.segment_code = 'PIPE-SUB-00004'
AND NOT EXISTS (SELECT 1 FROM corrosion_audits c WHERE c.segment_id = p.segment_id)
UNION ALL
SELECT p.segment_id, 6500.00, 6200.00, 4.60, 190.00, 35.00, 14.00, 8.00, 1.00
FROM pipe_segments p WHERE p.segment_code = 'PIPE-SUB-00005'
AND NOT EXISTS (SELECT 1 FROM corrosion_audits c WHERE c.segment_id = p.segment_id)
UNION ALL
SELECT p.segment_id, 7500.00, 7450.00, 0.60, 95.00, 12.00, 2.00, 1.00, 1.50
FROM pipe_segments p WHERE p.segment_code = 'PIPE-SUB-00006'
AND NOT EXISTS (SELECT 1 FROM corrosion_audits c WHERE c.segment_id = p.segment_id);

-- Phase 4: Generated Repair Orders
INSERT INTO repair_orders
    (segment_id, hazard_code, integrity_score, integrity_grade, rupture_risk_factor, action_required, pressure_reduction_pct, target_operating_pressure)
SELECT p.segment_id, 'CRITICAL_CORROSION_HAZARD', 42.00, 'D', 0.1467, 'Emergency Line Closure - Subsea Isolation Valve Actuation', 50.00, 1100.00
FROM pipe_segments p WHERE p.segment_code = 'PIPE-SUB-00002'
AND NOT EXISTS (SELECT 1 FROM repair_orders r WHERE r.segment_id = p.segment_id)
UNION ALL
SELECT p.segment_id, 'CRITICAL_CORROSION_HAZARD', 38.50, 'D', 0.1995, 'Emergency Line Closure - Diver Hyperbaric Welding Repair', 50.00, 1400.00
FROM pipe_segments p WHERE p.segment_code = 'PIPE-SUB-00003'
AND NOT EXISTS (SELECT 1 FROM repair_orders r WHERE r.segment_id = p.segment_id);

COMMIT;
