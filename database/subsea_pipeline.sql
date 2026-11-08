-- =====================================================================
-- SUBSEA PIPELINE INTEGRITY MONITORING SYSTEM
-- DATABASE HOST SETUP SCRIPT (STUDENT 2 CO-DELIVERABLE)
-- Database Engine: MySQL 8.0+
-- Database Name: subsea_pipeline
-- =====================================================================

-- 1. DATABASE CREATION
CREATE DATABASE IF NOT EXISTS subsea_pipeline
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE subsea_pipeline;

-- 2. TABLE 1: pipe_segments
CREATE TABLE IF NOT EXISTS pipe_segments (
    segment_id INT AUTO_INCREMENT PRIMARY KEY,
    segment_code VARCHAR(30) UNIQUE NOT NULL,
    category VARCHAR(50) NOT NULL,
    subsea_depth_m DECIMAL(10,2) NOT NULL,
    wall_thickness_mm DECIMAL(10,2) NOT NULL,
    fluid_type VARCHAR(50) NOT NULL,
    maop_psi DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 3. TABLE 2: pig_inspections
CREATE TABLE IF NOT EXISTS pig_inspections (
    inspection_id INT AUTO_INCREMENT PRIMARY KEY,
    segment_id INT NOT NULL,
    wall_loss_percent DECIMAL(5,2) NOT NULL,
    cathodic_voltage_v DECIMAL(5,2) NOT NULL,
    internal_pressure_psi DECIMAL(10,2) NOT NULL,
    inspection_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pig_segment FOREIGN KEY (segment_id)
        REFERENCES pipe_segments(segment_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 4. TABLE 3: corrosion_audits
CREATE TABLE IF NOT EXISTS corrosion_audits (
    audit_id INT AUTO_INCREMENT PRIMARY KEY,
    segment_id INT NOT NULL,
    upstream_flow_bpd DECIMAL(12,2) NOT NULL,
    downstream_flow_bpd DECIMAL(12,2) NOT NULL,
    acoustic_leak_hz DECIMAL(10,2) NOT NULL,
    hydrophone_noise_score DECIMAL(5,2) NOT NULL,
    audit_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_segment FOREIGN KEY (segment_id)
        REFERENCES pipe_segments(segment_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 5. TABLE 4: repair_orders
CREATE TABLE IF NOT EXISTS repair_orders (
    repair_id INT AUTO_INCREMENT PRIMARY KEY,
    segment_id INT NOT NULL,
    hazard_code VARCHAR(100) NOT NULL,
    integrity_score DECIMAL(5,2) NOT NULL,
    integrity_grade VARCHAR(10) NOT NULL,
    rupture_risk_factor DECIMAL(10,4) NOT NULL,
    action_required VARCHAR(255) NOT NULL,
    repair_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_repair_segment FOREIGN KEY (segment_id)
        REFERENCES pipe_segments(segment_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 6. APPLICATION USER CREATION & PRIVILEGES (STUDENT 2 RESPONSIBILITY)
CREATE USER IF NOT EXISTS 'subsea_app'@'%' IDENTIFIED BY 'SubseaApp@2026#ChangeMe';
GRANT SELECT, INSERT, UPDATE, DELETE ON subsea_pipeline.* TO 'subsea_app'@'%';
FLUSH PRIVILEGES;

-- 7. BASELINE SAMPLE DATA FOR TESTING
INSERT IGNORE INTO pipe_segments
    (segment_id, segment_code, category, subsea_depth_m, wall_thickness_mm, fluid_type, maop_psi)
VALUES
    (1, 'PIPE-SUB-00001', 'HIGH_PRESSURE_DEEPWATER', 2500.00, 35.50, 'CRUDE_OIL', 5000.00),
    (2, 'PIPE-SUB-00002', 'SHALLOW_COASTAL_TRUNK',   150.00,  28.00, 'NATURAL_GAS', 3000.00),
    (3, 'PIPE-SUB-00003', 'LIQUEFIED_GAS_FEEDER',   800.00,  32.00, 'LIQUEFIED_GAS', 4000.00);

INSERT INTO pig_inspections (segment_id, wall_loss_percent, cathodic_voltage_v, internal_pressure_psi)
SELECT p.segment_id, 10.00, 1.05, 3500.00
FROM pipe_segments p WHERE p.segment_code = 'PIPE-SUB-00001'
AND NOT EXISTS (SELECT 1 FROM pig_inspections i WHERE i.segment_id = p.segment_id)
UNION ALL
SELECT p.segment_id, 20.00, 0.90, 2200.00
FROM pipe_segments p WHERE p.segment_code = 'PIPE-SUB-00002'
AND NOT EXISTS (SELECT 1 FROM pig_inspections i WHERE i.segment_id = p.segment_id)
UNION ALL
SELECT p.segment_id, 15.00, 1.10, 2800.00
FROM pipe_segments p WHERE p.segment_code = 'PIPE-SUB-00003'
AND NOT EXISTS (SELECT 1 FROM pig_inspections i WHERE i.segment_id = p.segment_id);

INSERT INTO corrosion_audits (segment_id, upstream_flow_bpd, downstream_flow_bpd, acoustic_leak_hz, hydrophone_noise_score)
SELECT p.segment_id, 10000.00, 9800.00, 120.00, 20.00
FROM pipe_segments p WHERE p.segment_code = 'PIPE-SUB-00001'
AND NOT EXISTS (SELECT 1 FROM corrosion_audits c WHERE c.segment_id = p.segment_id)
UNION ALL
SELECT p.segment_id, 8000.00, 7500.00, 250.00, 40.00
FROM pipe_segments p WHERE p.segment_code = 'PIPE-SUB-00002'
AND NOT EXISTS (SELECT 1 FROM corrosion_audits c WHERE c.segment_id = p.segment_id)
UNION ALL
SELECT p.segment_id, 9000.00, 8900.00, 100.00, 15.00
FROM pipe_segments p WHERE p.segment_code = 'PIPE-SUB-00003'
AND NOT EXISTS (SELECT 1 FROM corrosion_audits c WHERE c.segment_id = p.segment_id);
