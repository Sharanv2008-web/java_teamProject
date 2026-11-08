# Project Overview - Subsea Pipeline Integrity Monitoring System

## 1. Executive Summary

Offshore oil and gas pipelines represent mission-critical energy transportation arteries operating under harsh subsea environments characterized by high hydrostatic pressure, extreme salinity, mechanical currents, and dynamic internal fluid pressures. Structural failure or catastrophic rupture of a subsea pipeline results in catastrophic environmental degradation, multi-million dollar economic losses, and prolonged shutdown of offshore production facilities.

The **Subsea Pipeline Integrity Monitoring System** is an enterprise-grade Java and MySQL decision-support and condition-monitoring platform developed to automate the aggregation, concurrent processing, and engineering analysis of real-time subsea telemetry.

---

## 2. Real-World Engineering Context

Subsea pipelines are subject to multiple degradation mechanisms:
1. **Internal & External Corrosion**: Electrochemical reaction between metallic pipe walls and acidic produced fluids ($H_2S$, $CO_2$, water cut) internally, or seawater externally when coating fails.
2. **Cathodic Protection (CP) Inadequacy**: Subsea sacrificial zinc/aluminum anodes deplete over time. When potential rises above -0.85V (CSE), corrosion accelerates exponentially.
3. **Hoop Stress Under MAOP**: Internal pressure generates circumferential hoop stress ($\sigma_h = \frac{P \cdot D}{2t}$). As corrosion reduces wall thickness $t$, the remaining pipe steel experiences intensified stress, drastically increasing burst probability.
4. **Subsea Micro-Leaks**: Orifice leaks produce high-frequency acoustic hiss (100–300 Hz) and create mass-flow deficits between topside platform meters and subsea manifolds.

---

## 3. High-Level Technical Architecture

```
+-------------------------------------------------------------------------------+
|                           Physical Subsea Pipeline                            |
|             (Sensors: PIG Calipers, Hydrophones, Ultrasonic Meters)           |
+-------------------------------------------------------------------------------+
                                        │
                                        ▼
+-------------------------------------------------------------------------------+
|              Student 2: MySQL Database Host ('subsea_pipeline')              |
|  * pipe_segments: Structural parameters, Depth, Wall Thickness, MAOP          |
|  * pig_inspections: Wall loss %, Cathodic voltage, Internal pressure          |
|  * corrosion_audits: Upstream/downstream flow, Acoustic Hz, Hydrophone noise  |
|  * repair_orders: Actionable engineering repair directives                    |
+-------------------------------------------------------------------------------+
                                        │
                         JDBC Connection (Port 3306)
                                        ▼
+-------------------------------------------------------------------------------+
|               Student 1: Java Application Engine Architecture                 |
|                                                                               |
|  1. Configuration Layer:                                                      |
|     - DBConnection: Connection pooling, environment variables, SSL parameters |
|                                                                               |
|  2. Data Access Layer (DAO):                                                  |
|     - PipeSegmentDAO, PigInspectionDAO, CorrosionAuditDAO, RepairOrderDAO     |
|                                                                               |
|  3. Concurrency Layer (Workers & ExecutorService):                            |
|     - PigInspectionWorker (Async PIG telemetry retrieval)                    |
|     - CorrosionAuditWorker (Async acoustic and flow telemetry retrieval)      |
|     - PipelineIntegrityWorker (Async multi-factor risk calculation)           |
|                                                                               |
|  4. Calculation Engines:                                                      |
|     - PCICalculator (0 - 100 condition scoring & Grade A-F mapping)           |
|     - RuptureRiskCalculator (RRF calculation & hazard code detection)         |
|                                                                               |
|  5. Reporting & Export Layer:                                                 |
|     - CsvReportGenerator (Generates audit-ready CSV reports)                  |
|     - Database Transaction Manager (Persists repair orders with deduplication)|
+-------------------------------------------------------------------------------+
```

---

## 4. Key Capabilities & Deliverables

1. **Independent Windows Project**: Can be imported and run directly in Eclipse IDE, built using standard Maven CLI, and managed through MySQL Workbench.
2. **Zero Antigravity Dependency**: The deliverable is stored in standard Windows folders and does not rely on any proprietary AI runtime.
3. **Enterprise Multithreading**: Utilizes Java `ExecutorService`, `Callable`, and `Future` to parallelize data retrieval and computational analysis.
4. **Duplicate Prevention**: Intelligently inspects active repair orders before insertion to prevent table pollution during high-frequency cycles.
5. **Dual Report Generation**: Emits both a timestamped audit record and a standardized `latest_pipeline_integrity_report.csv` file.
6. **OCI Cloud Readiness**: Fully architected for decoupled cloud deployment across two separate Oracle Cloud Infrastructure virtual machines.
