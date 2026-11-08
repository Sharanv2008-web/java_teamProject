# Project Assignment Set 30: Deep-Sea Oil & Gas Subsea Pipeline Integrity Diagnostics System
## Student 2 (Database Host) Complete Deliverable Package

### Overview
This workspace contains the complete project implementation and documentation tailored specifically for **Student 2 (Database Host)** on Oracle Cloud Infrastructure (OCI) VM 2, fully integrated with **Student 1 (Application Host)** on OCI VM 1.

---

### Directory Structure
```
c:\java team project\
├── database/
│   ├── subsea_pipeline_schema.sql          # Primary MySQL DDL schema & seed data (Deliverable 1)
│   ├── subsea_pipeline_schema_postgres.sql  # PostgreSQL DDL variant for port 5432
│   ├── setup_vm2_database.sh               # Automated provisioning shell script for OCI VM 2
│   └── verify_remote_access.sh             # Verification script for port 3306 & privileges
├── src/com/subsea/pipeline/
│   ├── DBConnection.java                   # Remote JDBC connection manager (OCI VM 2:3306/5432)
│   ├── PipelineDAO.java                    # Data Access Object with full CRUD & resilience
│   ├── IntegrityWorker.java                # Multi-threaded telemetry worker & formula engine
│   ├── RepairOrderWriter.java              # File I/O CSV export engine
│   ├── MainApp.java                        # Mission control & Phased incremental workflow
│   └── Student2_DBVerificationTest.java    # Dedicated verification test suite for Student 2
├── data/
│   └── pig_telemetry.txt                   # High-resolution Smart PIG telemetry log stream
├── reports/
│   ├── subsea_repair_authorizations.csv    # Exported subsea repair authorizations
│   └── pipeline_integrity_report.csv       # Exported pipeline diagnostic compliance report
├── docs/
│   ├── OCI_Student2_Architecture_Verification.md # OCI Security List rules proof & remote DB access
│   ├── Student2_Execution_Proofs.md             # Execution proofs, regex check, worker logs, CSVs
│   └── Student2_Viva_and_Submission_Guide.md     # Viva questions, defense strategy, & formulas
├── compile.bat                             # One-click build script
├── run.bat                                 # Execution script (use `run.bat test` for DB suite)
└── README.md                               # Project documentation overview
```

---

### Quick Start Instructions

#### 1. Compile the Source Code
```cmd
compile.bat
```

#### 2. Run Student 2 Database Verification Test Suite
```cmd
run.bat test
```

#### 3. Run the Full Multi-Threaded Diagnostics Mission System
```cmd
run.bat
```

---

### Core Deliverables Summary for Student 2

1. **Database Schema (`database/subsea_pipeline_schema.sql`):**
   - Tables: `pipe_segments`, `pig_inspections`, `corrosion_audits`, `repair_orders`.
   - Views: `v_pipeline_hazards`, `v_segment_integrity_overview`.
   - Rich seed data covering High-Pressure Deepwater, Shallow Coastal Trunk, and Liquefied Gas Feeder assets.

2. **Java Source Files (`src/com/subsea/pipeline/`):**
   - `DBConnection.java`: Remote JDBC configuration with fallback support.
   - `PipelineDAO.java`: Full relational CRUD and batch synchronization.
   - `IntegrityWorker.java`: Parallel worker threads, mathematical formulas (PCI, Rupture Risk), and Critical Corrosion Filtering.
   - `RepairOrderWriter.java`: CSV report generation.
   - `MainApp.java`: Orchestrates the Phased incremental telemetry pipeline.
   - `Student2_DBVerificationTest.java`: Verification suite for Student 2.

3. **Architecture Verification Document (`docs/OCI_Student2_Architecture_Verification.md`):**
   - OCI VCN Ingress Security List rules proof for port 3306/5432.
   - OS firewall (`iptables` / `firewalld`) and MySQL `bind-address = 0.0.0.0` configuration.
   - Three-way TCP handshake and remote connection proofs.

4. **Execution Proofs Document (`docs/Student2_Execution_Proofs.md`):**
   - PIG log ingestion from `data/pig_telemetry.txt`.
   - Segment regex check (`PIPE-SUB-\d{5}`) filtering invalid codes.
   - Substring tag extraction (`.substring()`) and depth cleaning (`.trim()`).
   - Multi-threaded corrosion analysis logs and exported CSV files.
