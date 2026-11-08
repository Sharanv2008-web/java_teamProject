# Subsea Pipeline Integrity Monitoring System

An enterprise-grade, multithreaded Java monitoring and risk analysis system designed to evaluate offshore subsea oil and gas pipeline integrity, process in-line PIG inspection telemetry, assess acoustic and volumetric flow corrosion indicators, calculate the Pipeline Condition Index (PCI), evaluate catastrophic rupture risks, and generate actionable subsea repair orders stored in MySQL and exported as CSV audit reports.

---

## 1. Project Overview & Architecture

The system coordinates between two primary hosts:
* **Student 1 (Java Application Host - This Repository)**: Core Java engine, Maven configuration, multithreaded telemetry workers, mathematical calculation engines (PCI and Rupture Risk), CSV report generation, and OCI VM 1 deployment.
* **Student 2 (Database Host)**: MySQL 8.0 server host, database schema definitions, baseline telemetry data, application user privileges, and OCI VM 2 private database hosting.

```
+-------------------------------------------------------------------------+
|                  Subsea Pipeline Physical Infrastructure                |
+-------------------------------------------------------------------------+
                                    |
                                    v
+-------------------------------------------------------------------------+
|                 Student 2: MySQL Database Host (Port 3306)              |
|  * pipe_segments     * pig_inspections     * corrosion_audits           |
|  * repair_orders     * subsea_app user                                  |
+-------------------------------------------------------------------------+
                                    |
                            JDBC (PreparedStatement)
                                    v
+-------------------------------------------------------------------------+
|                 Student 1: Java Application Engine (Host)               |
|                                                                         |
|  [MainApp]                                                              |
|     │                                                                   |
|     ├── [DBConnection] (Environment / Config Fallbacks)                |
|     │                                                                   |
|     ├── [PipelineMonitoringService] (Multithreaded Executor)            |
|     │       ├── PigInspectionWorker (Async Telemetry Fetch)            |
|     │       ├── CorrosionAuditWorker (Async Telemetry Fetch)           |
|     │       └── PipelineIntegrityWorker (Async Computation)             |
|     │                                                                   |
|     ├── [PCICalculator] (Pipeline Condition Index 0 - 100 & Grade A-F)  |
|     ├── [RuptureRiskCalculator] (RRF 0.0001 - 0.9999 & Hazard Codes)    |
|     │                                                                   |
|     ├── [RepairOrderDAO] (Persistence & Duplicate Prevention)          |
|     └── [CsvReportGenerator] (Timestamped Reports in reports/)         |
+-------------------------------------------------------------------------+
                                    │
                                    ├───> reports/pipeline_integrity_report_*.csv
                                    └───> MySQL `repair_orders` Table
```

---

## 2. Directory Structure

```
SubseaPipelineIntegrityMonitoringSystem/
├── .project                                  # Eclipse IDE project definition
├── .classpath                                # Eclipse build path definition
├── .settings/                                # Eclipse compiler & m2e preferences
│   ├── org.eclipse.jdt.core.prefs
│   └── org.eclipse.m2e.core.prefs
│
├── src/
│   ├── main/
│   │   ├── java/com/subsea/
│   │   │   ├── config/
│   │   │   │   └── DBConnection.java         # JDBC manager with env/properties fallback
│   │   │   ├── dao/
│   │   │   │   ├── PipeSegmentDAO.java       # Pipeline segment CRUD
│   │   │   │   ├── PigInspectionDAO.java     # PIG telemetry retrieval
│   │   │   │   ├── CorrosionAuditDAO.java    # Flow & acoustic audit retrieval
│   │   │   │   └── RepairOrderDAO.java       # Repair order persistence & deduplication
│   │   │   ├── model/
│   │   │   │   ├── PipeSegment.java          # Segment entity
│   │   │   │   ├── PigInspection.java        # PIG inspection entity
│   │   │   │   ├── CorrosionAudit.java       # Corrosion audit entity
│   │   │   │   ├── RepairOrder.java          # Repair order entity
│   │   │   │   └── IntegrityResult.java      # Combined assessment model
│   │   │   ├── calculation/
│   │   │   │   ├── PCICalculator.java        # Multi-factor PCI scoring model
│   │   │   │   └── RuptureRiskCalculator.java# Rupture risk & hazard classification
│   │   │   ├── worker/
│   │   │   │   ├── PigInspectionWorker.java  # Concurrent PIG worker
│   │   │   │   ├── CorrosionAuditWorker.java # Concurrent audit worker
│   │   │   │   └── PipelineIntegrityWorker.java # Concurrent calculation worker
│   │   │   ├── service/
│   │   │   │   └── PipelineMonitoringService.java # Concurrency orchestrator
│   │   │   ├── report/
│   │   │   │   └── CsvReportGenerator.java   # CSV export engine
│   │   │   └── MainApp.java                  # Application entry point
│   │   └── resources/
│   │       └── db.properties                 # Classpath default database configuration
│   └── test/
│       └── java/com/subsea/
│           ├── DBConnectionTest.java         # JDBC connectivity tests
│           ├── CalculationTest.java          # PCI & Rupture Risk unit tests
│           └── PipelineServiceTest.java      # Full end-to-end integration test
│
├── database/
│   └── subsea_pipeline.sql                   # Student 2 database schema & sample seed script
├── reports/                                  # Generated CSV reports
├── config/
│   └── application.properties                # External runtime configuration
├── pom.xml                                   # Maven dependencies & build configuration
├── README.md                                 # Primary project documentation
└── documentation/                            # Detailed architectural & operational guides
    ├── PROJECT_OVERVIEW.md
    ├── INSTALLATION_GUIDE.md
    ├── DATABASE_GUIDE.md
    ├── TESTING_REPORT.md
    ├── OCI_DEPLOYMENT_GUIDE.md
    ├── STUDENT1_RESPONSIBILITIES.md
    └── STUDENT2_RESPONSIBILITIES.md
```

---

## 3. Quick Start (Windows Developer Environment)

### Prerequisites
* Java JDK 21+ (`java -version` and `javac -version`)
* Apache Maven 3.9+ (`mvn -v`)
* MySQL Server 8.0+ running on port 3306
* Eclipse IDE for Java Developers (2023-12 or later)

### Step 1: Open and Import in Eclipse IDE
1. Launch **Eclipse IDE**.
2. Select your default workspace (e.g., `C:\Users\<Username>\eclipse-workspace`).
3. Click **File -> Import...**
4. Expand **Maven** and choose **Existing Maven Projects**, then click **Next**.
5. In the **Root Directory** field, browse to:
   `C:\Users\<Username>\Documents\SubseaPipelineIntegrityMonitoringSystem`
6. Ensure `pom.xml` is checked, and click **Finish**.
7. In the Eclipse Project Explorer, right-click the project -> **Maven -> Update Project...** (check *Force Update of Snapshots/Releases* and click OK).

### Step 2: Run the Application from Eclipse
1. In Eclipse Project Explorer, navigate to:
   `src/main/java` -> `com.subsea` -> `MainApp.java`.
2. Right-click `MainApp.java` -> **Run As -> Java Application**.
3. Observe the concurrent worker execution, the assessment table, and repair order generation in the Eclipse Console.

### Step 3: Run the Application from Windows Terminal / PowerShell
```powershell
cd C:\Users\<Username>\Documents\SubseaPipelineIntegrityMonitoringSystem
mvn clean package
java -jar target\subsea-pipeline-monitoring-1.0.0-all.jar
```

---

## 4. Engineering Scoring & Risk Models

### Pipeline Condition Index (PCI)
The system calculates a composite score from 0.00 to 100.00 based on standard subsea pipeline integrity engineering criteria (ASME B31G, API 579, NACE SP0169):
* **Wall Loss Factor ($D_{WL}$)**: Progressive deduction based on ultrasonic caliper wall loss %.
* **Cathodic Protection Deviation ($D_{CP}$)**: Penalizes voltages deviating from the standard NACE window (-0.85V to -1.15V equivalent).
* **Operating Pressure Stress ($D_{PR}$)**: Penalizes operations exceeding 70% of Maximum Allowable Operating Pressure (MAOP).
* **Volumetric Flow Deficit ($D_{FL}$)**: Evaluates discrepancies between upstream and downstream mass-flow meters.
* **Acoustic Leak Frequency ($D_{AC}$)**: Analyzes hydrophone noise score and high-frequency orifice turbulence ($\ge 200\text{ Hz}$).

| PCI Range | Grade | Description | Maintenance Directive |
| :--- | :--- | :--- | :--- |
| **85.0 - 100.0** | **Grade A** | Optimal Integrity | Nominal quarterly SCADA monitoring |
| **70.0 - 84.9** | **Grade B** | Stable / Minor Wear | Routine corrosion inhibitor injection |
| **55.0 - 69.9** | **Grade C** | Degraded / Corrosion | Ultrasonic caliper inspection & anode review |
| **40.0 - 54.9** | **Grade D** | High Risk / Flow Leak | Operating pressure derating by 20% |
| **0.0 - 39.9** | **Grade F** | Critical Rupture Hazard | Emergency shutdown & ROV clamp installation |

---

## 5. Generated Artifacts
* **Live Database Records**: Stored in MySQL `repair_orders` table with foreign keys referencing `pipe_segments`.
* **CSV Audit Reports**: Stored in `reports/pipeline_integrity_report_<TIMESTAMP>.csv` and `reports/latest_pipeline_integrity_report.csv`.
* **Standalone Executable Fat JAR**: Generated at `target/subsea-pipeline-monitoring-1.0.0-all.jar`.
