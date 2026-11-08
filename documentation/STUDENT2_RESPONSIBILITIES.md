# Student 2 - Database Host Responsibilities

**Role**: Database Administrator (DBA), Data Engineer, and OCI Database Cloud Host Specialist.

---

## 1. Overview of Student 2's Scope

Student 2 is responsible for provisioning, configuring, maintaining, and securing the Relational Database Host that supports Student 1's Java Application Host.

In local development, Student 2 hosts the database on MySQL Server 8.0 on port 3306. For cloud deployment, Student 2 provisions and manages **OCI VM 2** (Database Host) on an isolated cloud subnet with verified Ingress Security List rules permitting remote JDBC traffic.

---

## 2. Detailed Responsibility Checklist

### 1. MySQL Database Creation
* **Status**: Complete on Local Host & Automated for OCI VM 2.
* Create database `subsea_pipeline` / `subsea_db` with character set `utf8mb4` and collation `utf8mb4_unicode_ci`.
* Command:
  ```sql
  CREATE DATABASE IF NOT EXISTS subsea_db
      CHARACTER SET utf8mb4
      COLLATE utf8mb4_unicode_ci;
  ```

### 2. Database Table Creation & Schema Enforcement (`subsea_pipeline_schema.sql`)
* **Status**: Complete & Verified.
* All four core tables exist with exact column names, datatypes, and storage engine `InnoDB`:
  1. `pipe_segments` (Structural specifications, depth, wall thickness, fluid, MAOP, category caps)
  2. `pig_inspections` (In-line PIG telemetry, wall loss %, CP voltage, internal pressure)
  3. `corrosion_audits` (Flow balance, acoustic frequency, hydrophone noise, PCI, Rupture Risk, integrity grades)
  4. `repair_orders` (Actionable repair directives, pressure reduction orders, target operating pressure)
* Analytical views: `v_pipeline_hazards`, `v_segment_integrity_overview`.

### 3. Primary and Foreign Key Constraints
* **Status**: Complete & Verified.
* Primary Keys: `segment_id`, `inspection_id`, `audit_id`, `order_id` (all `AUTO_INCREMENT`).
* Foreign Keys with referential integrity:
  * `pig_inspections(segment_code)` -> `pipe_segments(segment_code)`
  * `corrosion_audits(segment_code)` -> `pipe_segments(segment_code)`
  * `repair_orders(segment_code)` -> `pipe_segments(segment_code)`
  * Constraints configured with `ON UPDATE CASCADE ON DELETE RESTRICT`.

### 4. Database User Creation & Permission Configuration
* **Status**: Complete & Verified.
* Avoid exposing the MySQL `root` account to application tier.
* Application users configured with required grants:
  ```sql
  CREATE USER IF NOT EXISTS 'subsea_user'@'%' IDENTIFIED BY 'SubseaSecurePass2026!';
  GRANT ALL PRIVILEGES ON subsea_db.* TO 'subsea_user'@'%';
  FLUSH PRIVILEGES;
  ```

### 5. Telemetry Baseline Data Population
* **Status**: Complete & Verified.
* Seed realistic baseline test data across all three asset categories:
  * Category 1: `HIGH-PRESSURE DEEPWATER PIPE` (e.g. `PIPE-SUB-10001`, `PIPE-SUB-10002`)
  * Category 2: `SHALLOW COASTAL TRUNK LINE` (e.g. `PIPE-SUB-20001`, `PIPE-SUB-20002`)
  * Category 3: `LIQUEFIED GAS FEEDER` (e.g. `PIPE-SUB-30001`, `PIPE-SUB-30002`)

### 6. OCI VM 2 Cloud Database Hosting & Remote Access Proof
* **Status**: Complete, Verified & Documented.
* Architecture deliverables completed:
  1. Automated provisioning script: `database/setup_vm2_database.sh`
  2. Network verification script: `database/verify_remote_access.sh`
  3. OCI Ingress Security List proof (Port 3306/5432): `docs/OCI_Student2_Architecture_Verification.md`
  4. Execution proofs and multi-threaded worker logs: `docs/Student2_Execution_Proofs.md`
  5. Student 2 Viva defense guide: `docs/Student2_Viva_and_Submission_Guide.md`
  6. Standalone verification test: `src/com/subsea/pipeline/Student2_DBVerificationTest.java`

---

## 3. Collaboration Workflow (Student 1 <-> Student 2)

```
[Student 2 - Database Host (OCI VM 2)]          [Student 1 - Java App Host (OCI VM 1)]
Provides:                                       Consumes:
* Running MySQL instance on OCI VM 2            * Ingests Smart PIG Telemetry
* Database Schema (subsea_pipeline_schema.sql)  * Queries pipe_segments
* Ingress Security Rule on Port 3306/5432       * PreparedStatement Batch Queries
* Automated setup script (setup_vm2_database.sh)* Concurrently computes PCI & Risk
* Remote verification test suite                * Persists into repair_orders
                                                * Exports CSV compliance reports
```
