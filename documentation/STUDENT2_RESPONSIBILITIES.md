# Student 2 - Database Host Responsibilities

**Role**: Database Administrator (DBA), Data Engineer, and OCI Database Cloud Host Specialist.

---

## 1. Overview of Student 2's Scope

Student 2 is responsible for provisioning, configuring, maintaining, and securing the MySQL Database Host that supports Student 1's Java Application Host.

In local development, Student 2 hosts the database on the local machine's MySQL Server 8.0 instance. For cloud deployment, Student 2 provisions and manages **OCI VM 2** (Database Host) on an isolated private cloud subnet.

---

## 2. Detailed Responsibility Checklist

### 1. MySQL Database Creation
* **Status**: Complete on Local Host (Ready for OCI VM 2).
* Create database `subsea_pipeline` with character set `utf8mb4` and collation `utf8mb4_unicode_ci`.
* Command:
  ```sql
  CREATE DATABASE IF NOT EXISTS subsea_pipeline
      CHARACTER SET utf8mb4
      COLLATE utf8mb4_unicode_ci;
  ```

### 2. Database Table Creation & Schema Enforcement
* **Status**: Complete on Local Host.
* Ensure all four tables exist with exact column names, datatypes, and storage engine `InnoDB`:
  1. `pipe_segments` (Structural specifications, depth, wall thickness, fluid, MAOP)
  2. `pig_inspections` (In-line PIG telemetry, wall loss %, CP voltage, internal pressure)
  3. `corrosion_audits` (Volumetric flow balance, acoustic frequency, hydrophone noise)
  4. `repair_orders` (Actionable repair directives, PCI score, grade, rupture risk, hazard code)

### 3. Primary and Foreign Key Constraints
* **Status**: Complete on Local Host.
* Primary Keys: `segment_id`, `inspection_id`, `audit_id`, `repair_id` (all `AUTO_INCREMENT`).
* Foreign Keys:
  * `pig_inspections(segment_id)` -> `pipe_segments(segment_id)`
  * `corrosion_audits(segment_id)` -> `pipe_segments(segment_id)`
  * `repair_orders(segment_id)` -> `pipe_segments(segment_id)`
  * Constraints configured with `ON DELETE CASCADE ON UPDATE CASCADE`.

### 4. Database User Creation & Permission Configuration
* **Status**: Complete on Local Host.
* Avoid exposing the MySQL `root` account to application tier.
* Create application user `subsea_app` with restricted privileges:
  ```sql
  CREATE USER IF NOT EXISTS 'subsea_app'@'%' IDENTIFIED BY 'SubseaApp@2026#ChangeMe';
  GRANT SELECT, INSERT, UPDATE, DELETE ON subsea_pipeline.* TO 'subsea_app'@'%';
  FLUSH PRIVILEGES;
  ```
* For OCI deployment, replace the wildcard host `'%'` with the private subnet CIDR (e.g. `'10.0.0.%'`) for heightened security.

### 5. Telemetry Baseline Data Population
* **Status**: Complete on Local Host.
* Seed realistic baseline test data across segments representing varied operating conditions:
  * Segment 1 (`PIPE-SUB-00001`): High-Pressure Deepwater (Crude Oil) - Healthy operational baseline.
  * Segment 2 (`PIPE-SUB-00002`): Shallow Coastal Trunk (Natural Gas) - Degraded wall loss with active acoustic leak.
  * Segment 3 (`PIPE-SUB-00003`): Liquefied Gas Feeder (Cryogenic Gas) - Moderate corrosion wear.

### 6. OCI VM 2 Cloud Database Hosting
* **Status**: Documented & Architecture Ready.
* Tasks for Student 2 during cloud rollout:
  1. Provision OCI VM 2 on an isolated **Private Subnet** (no public IP).
  2. Install MySQL Server 8.0 on Ubuntu (`sudo apt install mysql-server -y`).
  3. Edit `/etc/mysql/mysql.conf.d/mysqld.cnf` to set `bind-address = 0.0.0.0`.
  4. Execute `database/subsea_pipeline.sql` to instantiate the schema and seed data.
  5. Configure OCI Security List to allow TCP port `3306` incoming from VM 1's private subnet only (`10.0.0.0/24`).
  6. Verify port `3306` is not open to public CIDR `0.0.0.0/0`.

---

## 3. Collaboration Workflow (Student 1 <-> Student 2)

```
[Student 2 - Database Host]                     [Student 1 - Java App Host]
Provides:                                       Consumes:
* Running MySQL instance                        * Queries pipe_segments
* Database credentials in subsea_pipeline.sql   * Queries pig_inspections
* Host IP (localhost or OCI VM 2 10.0.1.20)     * Queries corrosion_audits
                                                * Concurrently computes PCI & Risk
                                                * Persists into repair_orders
                                                * Exports CSV reports
```
All schema definitions and seed data are archived in `database/subsea_pipeline.sql` for Student 2's direct execution.
