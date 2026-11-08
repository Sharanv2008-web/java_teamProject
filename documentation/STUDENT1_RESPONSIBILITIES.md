# Student 1 - Java Application Host Responsibilities

**Role**: Senior Java Developer, Concurrency Engineer, Software Architect, and OCI Application Deployment Specialist.

---

## 1. Summary of Completed Deliverables

Student 1 has fully developed, tested, and packaged the entire Java Application Host system as an independent, Eclipse-compatible Maven project physically located at:
`C:\Users\<Username>\Documents\SubseaPipelineIntegrityMonitoringSystem`

---

## 2. Detailed Responsibility Checklist

### 1. Java Application Development
* **Status**: Complete & Verified.
* Built modular, standard Java architecture conforming to enterprise separation of concerns:
  * `com.subsea.config`: Centralized database configuration and connection handling.
  * `com.subsea.model`: Strongly typed POJOs for pipeline entities.
  * `com.subsea.dao`: Data Access Objects utilizing `PreparedStatement` and connection pooling.
  * `com.subsea.worker`: Multi-threaded workers for concurrent telemetry fetching and processing.
  * `com.subsea.calculation`: Scientific and empirical condition and rupture risk calculators.
  * `com.subsea.service`: Primary orchestration service.
  * `com.subsea.report`: Professional CSV generator.
  * `com.subsea.MainApp`: User-friendly CLI application entry point.

### 2. Maven Project Configuration (`pom.xml`)
* **Status**: Complete & Verified.
* Configured Maven project with compiler version Java 21 LTS (`maven-compiler-plugin`).
* Added `mysql-connector-j` (version 8.3.0) and JUnit 5 dependencies (`junit-jupiter`).
* Configured `maven-assembly-plugin` to generate executable fat JARs (`target/subsea-pipeline-monitoring-1.0.0-all.jar`) with all dependencies embedded.

### 3. Eclipse IDE Compatibility
* **Status**: Complete & Verified.
* Created native Eclipse configuration files:
  * `.project` (Java and Maven natures)
  * `.classpath` (JavaSE-21 JRE container + Maven Classpath Container)
  * `.settings/org.eclipse.jdt.core.prefs` (Java compliance level 21)
  * `.settings/org.eclipse.m2e.core.prefs` (m2e preferences)
* Project imports directly into Eclipse Project Explorer without manual build path adjustments.

### 4. JDBC Database Connectivity (`DBConnection.java`)
* **Status**: Complete & Verified.
* Connects to MySQL on port `3306` with database name `subsea_pipeline`.
* Reads credentials via multi-tier fallback: Environment variables (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`), external `config/application.properties`, classpath `db.properties`, and safe defaults.
* Automatically disables SSL requirement for local development while supporting secure cloud connections.

### 5. DAO Implementation
* **Status**: Complete & Verified.
* `PipeSegmentDAO`: Full retrieval and insertion functions for pipeline segment metadata.
* `PigInspectionDAO`: Querying in-line inspection telemetry.
* `CorrosionAuditDAO`: Querying acoustic and flow meter telemetry.
* `RepairOrderDAO`: Inserting and retrieving repair orders with automated duplicate detection (`existsRecentOrder`).

### 6. Model Classes
* **Status**: Complete & Verified.
* Created `PipeSegment`, `PigInspection`, `CorrosionAudit`, `RepairOrder`, and `IntegrityResult`.
* All fields map 1:1 to database schema types with proper data types (`BigDecimal`, `int`, `String`, `Timestamp`).

### 7. Multithreading & Concurrency
* **Status**: Complete & Verified.
* Developed independent concurrent worker tasks using Java `Callable<T>`, `Future<T>`, and `ExecutorService`:
  * `PigInspectionWorker`: Concurrently retrieves and normalizes PIG inspection telemetry.
  * `CorrosionAuditWorker`: Concurrently retrieves and audits acoustic and flow telemetry.
  * `PipelineIntegrityWorker`: Concurrently executes condition calculations.
* Real-time console logs output active thread names (`SubseaWorker-X`) and timestamps demonstrating true parallel execution.

### 8. Pipeline Integrity Calculation & PCI Scoring (`PCICalculator.java`)
* **Status**: Complete & Verified.
* Multi-factor scoring model mapping conditions to a 0.00 to 100.00 scale based on ASME B31G, API 579, and NACE SP0169.
* Automatically assigns qualitative letter grades: **A** (85-100), **B** (70-84.9), **C** (55-69.9), **D** (40-54.9), **F** (0-39.9).

### 9. Rupture Risk Assessment & Hazard Classification (`RuptureRiskCalculator.java`)
* **Status**: Complete & Verified.
* Calculates normalized Rupture Risk Factor ($0.0001 \le RRF \le 0.9999$) factoring hoop stress, fluid volatility, depth, and wall thinning.
* Categorizes dominant hazard codes: `HAZ-CRIT-RUPTURE`, `HAZ-LEAK-ACTIVE`, `HAZ-CORR-SEVERE`, `HAZ-CP-DEFICIENT`, `HAZ-PRESSURE-SURGE`, `HAZ-CORR-MODERATE`, and `HAZ-NOMINAL`.
* Generates clear, actionable engineering repair directives.

### 10. Repair Order Generation & Persistence
* **Status**: Complete & Verified.
* Evaluates calculated risk conditions and inserts generated repair orders into the MySQL `repair_orders` table.
* Prevents duplicate active orders from cluttering the table during repeated runs.

### 11. CSV Report Generation (`CsvReportGenerator.java`)
* **Status**: Complete & Verified.
* Generates timestamped and latest CSV reports saved to `reports/`.
* Contains all 21 structural and operational telemetry attributes specified in the project requirements.

### 12. Application Testing
* **Status**: Complete & Verified.
* 7 automated JUnit 5 tests covering connectivity, calculation algorithms, edge cases, and end-to-end service execution:
  * `DBConnectionTest` (3 tests)
  * `CalculationTest` (3 tests)
  * `PipelineServiceTest` (1 test)
* Result: 100% pass rate.

### 13. OCI VM 1 Deployment Preparation
* **Status**: Complete & Verified.
* Executable Fat JAR prepared with Maven Assembly plugin.
* Created deployment guide, systemd service configuration, environment variable specifications, and firewall security list configurations.
