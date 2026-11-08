# Deep-Sea Subsea Pipeline Integrity Diagnostics System
## Project Assignment Set 30 — Student 2 Viva & Project Defense Guide
**Role:** Student 2 (Database Host & Cloud Infrastructure Specialist)  
**Host Environment:** OCI VM 2 (Database Host) on Port 3306 (MySQL) / Port 5432 (PostgreSQL)  

---

## 1. Quick Summary of Student 2 Scope & Responsibilities

In Project Assignment Set 30, the workload is distributed across two cloud nodes:
- **Student 1 (Application Host on VM 1):** Ingests Smart PIG logs, runs multi-threaded string parsing, calculates diagnostics, and issues hazard notifications.
- **Student 2 (Database Host on VM 2):** 
  1. Architects, normalizes, and deploys the relational subsea database schema (`subsea_pipeline_schema.sql`).
  2. Provisions Oracle Cloud Infrastructure (OCI) VM 2, configuring Virtual Cloud Network (VCN) Ingress Security Rules for remote JDBC traffic on port 3306/5432.
  3. Configures OS firewall (`iptables`/`firewalld`/`ufw`) and RDBMS network bindings (`bind-address = 0.0.0.0`).
  4. Provides DAO integration (`PipelineDAO.java`, `DBConnection.java`) ensuring transactional persistence and verification test utilities (`Student2_DBVerificationTest.java`).

---

## 2. 5-Minute Live Demo Walkthrough for Evaluators

When presenting your project to your professor or examiner:

1. **Step 1: Present OCI Cloud Architecture & Security Lists**
   - Open `docs/OCI_Student2_Architecture_Verification.md`.
   - Explain how Ingress Rule `IR-01` allows TCP port 3306 from Student 1's VM 1 subnet (`10.0.1.0/24`) into VM 2 (`10.0.2.0/24`).
   - Mention that OCI default Linux images have active `iptables` that must be explicitly opened alongside the cloud security list.

2. **Step 2: Show Database Schema & Normalized Tables**
   - Open `database/subsea_pipeline_schema.sql`.
   - Walk through the 4 core tables:
     - `pipe_segments`: Phase 1 Segment registration, category, depth, thickness, fluid, MAOP, and max wall loss cap.
     - `pig_inspections`: Phase 2 Smart PIG inspection telemetry (Wall loss %, CP voltage, internal pressure).
     - `corrosion_audits`: Phase 3 Flow audit, calculated PCI, Rupture Risk, category weighted score, grade, and hazard flags.
     - `repair_orders`: Phase 4 Subsea repair authorizations, pressure reduction orders, target safe operating pressure.
   - Point out the analytical views `v_pipeline_hazards` and `v_segment_integrity_overview`.

3. **Step 3: Run Database Verification Test**
   - Execute `Student2_DBVerificationTest`:
     ```cmd
     run.bat test
     ```
   - Show the terminal output confirming table accessibility, record counts, and query against `v_pipeline_hazards`.

4. **Step 4: Execute Full System Integration**
   - Execute `MainApp`:
     ```cmd
     run.bat
     ```
   - Highlight:
     - Regex validation filtering `INVALID-SEG-99`.
     - Multi-threaded worker pool analyzing 9 telemetry records in parallel.
     - Critical Corrosion Filter identifying wall loss > category cap (e.g., `PIPE-SUB-10002` at 32.8% vs cap 30%, and CP 0.820V < 0.85V).
     - CSV reports written to `reports/subsea_repair_authorizations.csv` and `reports/pipeline_integrity_report.csv`.

---

## 3. High-Frequency Viva Questions & Model Answers

### Q1: Why did you have to configure both OCI Security Lists AND OS-level firewall rules?
> **Answer:** In Oracle Cloud Infrastructure, security is implemented with defense-in-depth:
> 1. **OCI VCN Security Lists** operate at the virtual hypervisor/cloud network layer outside the VM. They control whether packets can enter the virtual network interface (VNIC).
> 2. **OS-Level Firewalls (`firewalld`/`iptables`)** operate inside the Linux kernel on the VM instance itself. Oracle Linux default cloud images enforce active `iptables` rules with an explicit `REJECT` on non-standard ports. Opening only the OCI console rule will still leave packets blocked by the guest OS kernel unless `iptables -I INPUT ... -p tcp --dport 3306 -j ACCEPT` is executed.

### Q2: What is the purpose of setting `bind-address = 0.0.0.0` in `my.cnf`?
> **Answer:** By default, MySQL binds only to `127.0.0.1` (loopback adapter), which strictly restricts connections to local processes inside VM 2. Setting `bind-address = 0.0.0.0` instructs MySQL to listen across all network interfaces (eth0, private IP, public IP), permitting incoming remote JDBC connections from Student 1's VM 1.

### Q3: Explain how the mathematical formula for Pipeline Corrosion Index (PCI) works.
> **Answer:**
> $$\text{PCI} = 100 - \left( \text{Wall Loss \%} \times 2.0 + (1.2 - \text{Cathodic Voltage}) \times 50 \right)$$
> A perfect pipeline with 0% wall loss and full cathodic protection ($1.2\text{V}$) yields $\text{PCI} = 100 - (0 + 0) = 100$.
> Every 1% of wall loss reduces PCI by 2 points. Cathodic protection voltage should ideally be between $1.0\text{V}$ and $1.2\text{V}$. If voltage drops below $0.85\text{V}$, the term $(1.2 - 0.85) \times 50 = 17.5$ points are deducted, signaling severe electrochemical vulnerability.

### Q4: Explain the Rupture Risk Factor formula and its real-world physical significance.
> **Answer:**
> $$\text{Rupture Risk Factor} = \frac{\text{Operating Pressure (PSI)}}{\text{MAOP}} \times \left( \frac{\text{Wall Loss \%}}{100} \right)$$
> It represents the product of pipeline stress ratio and geometric wall thinning. If operating pressure approaches the Maximum Allowable Operating Pressure (MAOP) while wall loss is high (e.g. $32.8\%$), the rupture risk spikes (e.g. $0.3070$). When combined with deepwater hydro-static pressure, this demands immediate line isolation and pressure reduction.

### Q5: What is the Critical Corrosion Filter and what specific conditions trigger it?
> **Answer:** Under Section 3 of the project specification:
> 1. If **Wall Loss % exceeds the subsea asset category cap**:
>    - High-Pressure Deepwater Pipe cap = **30%**
>    - Shallow Coastal Trunk Line cap = **35%**
>    - Liquefied Gas Feeder cap = **25%**
> 2. OR if **Cathodic Protection Voltage < 0.85V**:
> The segment is immediately flagged as `CRITICAL_CORROSION_HAZARD`. The system automatically forces the integrity grade to Grade D (Emergency Line Closure) and generates an urgent repair authorization mandating a 50% line pressure reduction.

### Q6: How does Student 1's application communicate with Student 2's database over JDBC?
> **Answer:**
> Student 1's JVM initializes a `DriverManager.getConnection()` call using the remote connection string:
> `jdbc:mysql://<VM2_IP>:3306/subsea_db?connectTimeout=5000&useSSL=false&allowPublicKeyRetrieval=true`
> The connection uses credentials `subsea_user` / `SubseaSecurePass2026!`, which were granted remote privileges (`GRANT ALL PRIVILEGES ON subsea_db.* TO 'subsea_user'@'%'`). The DAO uses parameterized `PreparedStatement` queries to prevent SQL injection and support batching.

### Q7: What are the differences between the three subsea asset categories in your evaluation?
> **Answer:**
> 1. **High-Pressure Deepwater Pipe:** Subject to extreme subsea hydrostatic pressure (>1000m). Weighted: Wall Loss (50%), CP Voltage (30%), Flow Pressure Drop (20%), Wall Loss Cap = 30%.
> 2. **Shallow Coastal Trunk Line:** Vulnerable to shipping traffic and marine environmental factors. Weighted: Wall Loss (40%), Environmental Corrosion (40%), Anchor Drag Risk (20%), Wall Loss Cap = 35%.
> 3. **Liquefied Gas Feeder:** Susceptible to cryogenic Joule-Thomson temperature variance and phase changes. Weighted: Wall Loss (50%), Temperature Variance (30%), Pressure Drop (20%), Wall Loss Cap = 25%.

### Q8: How did you implement String processing requirements?
> **Answer:**
> 1. **Regex Validation:** Evaluated using `Pattern.compile("^PIPE-SUB-\\d{5}$")` to guarantee standardized asset identification. Invalid codes (e.g., `INVALID-SEG-99`) are rejected before DB insertion.
> 2. **Substring Extraction:** Extracted PIG inspection run tags (e.g. `PIG-TAG-DW-901`) using `.substring()`.
> 3. **Sea-Depth Cleaning:** Trimmed whitespace and noise tokens from subsea depth notes using `.trim()`.
> 4. **Hazard Report Formatting:** Built using `String.format()` with standardized column alignments and emergency directives.

### Q9: How does the multithreading worker pool work?
> **Answer:**
> In `MainApp.java`, an `ExecutorService` with a fixed thread pool is created. Each line of the telemetry stream is wrapped in an `IntegrityWorker` implementing `Callable<WorkerResult>`. These run concurrently, simulating real-time telemetry processing across hundreds of kilometers of subsea pipeline. Future objects collect thread results and handle exceptions gracefully.

### Q10: How does your database ensure data integrity?
> **Answer:**
> 1. **Primary & Unique Keys:** `segment_code` in `pipe_segments` is unique; synthetic autoincrement PKs are used for fast indexing.
> 2. **Foreign Keys:** `pig_inspections`, `corrosion_audits`, and `repair_orders` reference `pipe_segments(segment_code)` with `ON UPDATE CASCADE ON DELETE RESTRICT`.
> 3. **Check Constraints:** Enforce positive depth (`CHECK (subsea_depth_meters >= 0)`), valid percentage ranges (`CHECK (ultrasonic_wall_loss_pct BETWEEN 0 AND 100)`), and valid grade bands (`CHECK (integrity_grade IN ('A', 'B', 'C', 'D'))`).
> 4. **B-Tree Indexes:** Provisioned on `segment_code`, `hazard_flag`, `integrity_grade`, and timestamps for fast real-time query retrieval.
