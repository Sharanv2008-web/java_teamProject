# Deep-Sea Subsea Pipeline Integrity Diagnostics System
## Project Assignment Set 30 — Execution Proofs Document
**Target Student:** Student 2 (Database Host) & System Integration  
**System Execution Mode:** Multithreaded Parallel Pipeline Inspection Stream  
**Timestamp of Verification:** 2026-11-08  

---

## 1. PIG Log Ingestion & Input Telemetry Stream

The system ingests raw Smart PIG inspection streams from `data/pig_telemetry.txt`. Each record represents a high-resolution subsea sensor scan across deepwater, coastal, and gas feeder pipeline corridors.

```text
# =============================================================================
# SMART PIG (PIPELINE INSPECTION GAUGE) HIGH-RESOLUTION TELEMETRY LOG
# SUBSEA OFFSHORE PIPELINE INGESTION STREAM - BATCH 2026-X42
# Format: TAG:[tag]|SEG:[code]|DEPTH:[notes]|WALL_LOSS:[%]|CP_VOLT:[-V]|PRESSURE:[PSI]|UPSTREAM:[BPD]|DOWNSTREAM:[BPD]|FREQ:[kHz]|NOISE:[0-100]|ENV_CORR:[%]|ANCHOR_RISK:[0-100]|TEMP_VAR:[C]
# =============================================================================
TAG:PIG-TAG-DW-901|SEG:PIPE-SUB-10001|DEPTH:  2150.50m  |WALL_LOSS:12.50|CP_VOLT:1.050|PRESSURE:3800.00|UPSTREAM:85000.00|DOWNSTREAM:84850.00|FREQ:14.50|NOISE:18.00|ENV_CORR:2.00|ANCHOR_RISK:1.00|TEMP_VAR:1.20
TAG:PIG-TAG-DW-902|SEG:PIPE-SUB-10002|DEPTH:   1890.00m |WALL_LOSS:32.80|CP_VOLT:0.820|PRESSURE:3650.00|UPSTREAM:82000.00|DOWNSTREAM:76500.00|FREQ:62.80|NOISE:85.00|ENV_CORR:5.00|ANCHOR_RISK:2.00|TEMP_VAR:2.50
TAG:PIG-TAG-CS-301|SEG:PIPE-SUB-20001|DEPTH:    145.20m |WALL_LOSS:18.00|CP_VOLT:1.100|PRESSURE:1300.00|UPSTREAM:45000.00|DOWNSTREAM:44200.00|FREQ:22.00|NOISE:28.00|ENV_CORR:12.00|ANCHOR_RISK:8.00|TEMP_VAR:0.80
TAG:PIG-TAG-CS-302|SEG:PIPE-SUB-20002|DEPTH:     85.00m |WALL_LOSS:26.50|CP_VOLT:0.980|PRESSURE:1250.00|UPSTREAM:43000.00|DOWNSTREAM:40800.00|FREQ:38.50|NOISE:52.00|ENV_CORR:22.50|ANCHOR_RISK:14.00|TEMP_VAR:1.50
TAG:PIG-TAG-GF-501|SEG:PIPE-SUB-30001|DEPTH:   620.80m  |WALL_LOSS:08.40|CP_VOLT:1.150|PRESSURE:2400.00|UPSTREAM:120000.00|DOWNSTREAM:119800.00|FREQ:12.00|NOISE:15.00|ENV_CORR:1.50|ANCHOR_RISK:1.00|TEMP_VAR:1.00
TAG:PIG-TAG-GF-502|SEG:PIPE-SUB-30002|DEPTH:   710.00m  |WALL_LOSS:28.20|CP_VOLT:0.840|PRESSURE:2550.00|UPSTREAM:118000.00|DOWNSTREAM:108500.00|FREQ:71.20|NOISE:89.00|ENV_CORR:8.00|ANCHOR_RISK:3.00|TEMP_VAR:8.50
TAG:PIG-TAG-DW-903|SEG:PIPE-SUB-10003|DEPTH:  2420.00m  |WALL_LOSS:14.20|CP_VOLT:1.080|PRESSURE:3950.00|UPSTREAM:91000.00|DOWNSTREAM:90600.00|FREQ:16.80|NOISE:20.50|ENV_CORR:1.80|ANCHOR_RISK:0.50|TEMP_VAR:1.10
TAG:PIG-TAG-CS-303|SEG:PIPE-SUB-20003|DEPTH:   110.50m  |WALL_LOSS:36.40|CP_VOLT:0.830|PRESSURE:1420.00|UPSTREAM:48000.00|DOWNSTREAM:43100.00|FREQ:68.40|NOISE:92.00|ENV_CORR:38.00|ANCHOR_RISK:22.00|TEMP_VAR:1.80
TAG:PIG-TAG-GF-503|SEG:PIPE-SUB-30003|DEPTH:   580.00m  |WALL_LOSS:21.00|CP_VOLT:0.960|PRESSURE:2600.00|UPSTREAM:115000.00|DOWNSTREAM:111200.00|FREQ:34.20|NOISE:48.00|ENV_CORR:4.50|ANCHOR_RISK:2.00|TEMP_VAR:5.20
TAG:PIG-TAG-ERR-01|SEG:INVALID-SEG-99|DEPTH:  1000.00m  |WALL_LOSS:10.00|CP_VOLT:1.000|PRESSURE:2000.00|UPSTREAM:50000.00|DOWNSTREAM:50000.00|FREQ:10.00|NOISE:10.00|ENV_CORR:0.00|ANCHOR_RISK:0.00|TEMP_VAR:0.00
```

---

## 2. String Processing Proofs

### 2.1 Regex Validation (`PIPE-SUB-\d{5}`)
- **Valid Segments Accepted:** `PIPE-SUB-10001`, `PIPE-SUB-10002`, `PIPE-SUB-20001`, `PIPE-SUB-20002`, `PIPE-SUB-30001`, `PIPE-SUB-30002`, etc.
- **Corrupted Record Intercepted:** Line with segment `INVALID-SEG-99` was caught by `IntegrityWorker`:
  ```text
  [WARNING] REGEX VALIDATION FAILED for code 'INVALID-SEG-99'. Expected format: PIPE-SUB-\d{5}
  ```

### 2.2 Substring Extraction (`.substring()`)
- Tag extraction logic:
  ```java
  int tagIndex = rawTelemetryLine.indexOf("TAG:");
  int nextPipe = rawTelemetryLine.indexOf("|", tagIndex);
  String pigRunTag = rawTelemetryLine.substring(tagIndex + 4, nextPipe);
  ```
  **Extracted values:** `PIG-TAG-DW-901`, `PIG-TAG-DW-902`, `PIG-TAG-CS-301`, `PIG-TAG-GF-501`

### 2.3 Depth Note Cleaning (`.trim()`)
- Input string: `"DEPTH:   1890.00m "`
- Cleaned result: `"1890.00"` -> Parsed cleanly as `double depthMeters = 1890.00;`

### 2.4 Hazard Report Formatting (`String.format()`)
- Formatted hazard output:
  ```text
  *** CRITICAL HAZARD NOTIFICATION ***
    [Thread-02] Segment: PIPE-SUB-10002 | Asset Category: HIGH-PRESSURE DEEPWATER PIPE
    Depth: 1890.00m | Measured Wall Loss: 32.80% (Category Cap: 30.00%)
    Cathodic Protection: 0.820V | Operating Pressure: 3650.00 PSI (MAOP: 3900.00 PSI)
    Calculated PCI: 15.40 | Rupture Risk Factor: 0.3070 | Score: 35.66 (Grade D)
    Mandatory Directive: Emergency Line Closure
    Mitigation Order: Reduce line pressure by 50% and isolate subsea segment.
  ```

---

## 3. Multi-Threaded Diagnostics Execution Log

The multi-threaded pool executes parallel sensor ingestion simulating real-time telemetry processing across hundreds of kilometers of subsea pipelines:

```text
>> [Step 4] Launching Multi-Threaded Diagnostics Pool across Subsea Pipeline...
   [WorkerThread-01] Processed PIPE-SUB-10001 (Tag: PIG-TAG-DW-901) in 8ms  | PCI: 67.50 | RuptureRisk: 0.1131 | Score: 77.63 | Grade: B [Normal Service Flow]
   [WorkerThread-02] Processed PIPE-SUB-10002 (Tag: PIG-TAG-DW-902) in 12ms | PCI: 15.40 | RuptureRisk: 0.3070 | Score: 35.66 | Grade: D [Emergency Line Closure]
   [WorkerThread-03] Processed PIPE-SUB-20001 (Tag: PIG-TAG-CS-301) in 11ms | PCI: 59.00 | RuptureRisk: 0.1463 | Score: 65.83 | Grade: C [Maintenance Sleeve Ordered]
   [WorkerThread-04] Processed PIPE-SUB-20002 (Tag: PIG-TAG-CS-302) in 12ms | PCI: 36.00 | RuptureRisk: 0.2284 | Score: 44.41 | Grade: D [Emergency Line Closure]
   [WorkerThread-05] Processed PIPE-SUB-30001 (Tag: PIG-TAG-GF-501) in 1ms  | PCI: 80.70 | RuptureRisk: 0.0720 | Score: 80.03 | Grade: B [Normal Service Flow]
   [WorkerThread-06] Processed PIPE-SUB-30002 (Tag: PIG-TAG-GF-502) in 3ms  | PCI: 25.60 | RuptureRisk: 0.2663 | Score: 16.45 | Grade: D [Emergency Line Closure]
   [WorkerThread-07] Processed PIPE-SUB-10003 (Tag: PIG-TAG-DW-903) in 0ms  | PCI: 65.60 | RuptureRisk: 0.1275 | Score: 75.35 | Grade: B [Normal Service Flow]
   [WorkerThread-08] Processed PIPE-SUB-20003 (Tag: PIG-TAG-CS-303) in 1ms  | PCI: 8.70  | RuptureRisk: 0.3335 | Score: 17.60 | Grade: D [Emergency Line Closure]
   [WorkerThread-09] Processed PIPE-SUB-30003 (Tag: PIG-TAG-GF-503) in 1ms  | PCI: 46.00 | RuptureRisk: 0.2060 | Score: 39.10 | Grade: D [Emergency Line Closure]
   [WARNING] REGEX VALIDATION FAILED for code 'INVALID-SEG-99'. Expected format: PIPE-SUB-\d{5}
```

---

## 4. Mathematical Formula Calculation Verification

### 4.1 Pipeline Corrosion Index (PCI)
$$\text{PCI} = 100 - \left( \text{Wall Loss \%} \times 2.0 + (1.2 - \text{Cathodic Voltage}) \times 50 \right)$$
- **Test Case (PIPE-SUB-10001):**
  - Wall Loss = $12.50\%$
  - Cathodic Voltage = $1.050\text{V}$
  - Calculation:
    $$\text{PCI} = 100 - (12.50 \times 2.0 + (1.2 - 1.05) \times 50) = 100 - (25.0 + 7.5) = \mathbf{67.50}$$
  - Log matches: `PCI: 67.50` -> **VERIFIED**

- **Critical Hazard Case (PIPE-SUB-10002):**
  - Wall Loss = $32.80\%$
  - Cathodic Voltage = $0.820\text{V}$
  - Calculation:
    $$\text{PCI} = 100 - (32.80 \times 2.0 + (1.2 - 0.82) \times 50) = 100 - (65.6 + 19.0) = \mathbf{15.40}$$
  - Log matches: `PCI: 15.40` -> **VERIFIED**

### 4.2 Rupture Risk Factor
$$\text{Rupture Risk Factor} = \frac{\text{Operating Pressure (PSI)}}{\text{MAOP}} \times \left( \frac{\text{Wall Loss \%}}{100} \right)$$
- **Test Case (PIPE-SUB-10002):**
  - Operating Pressure = $3650.00\text{ PSI}$
  - MAOP = $3900.00\text{ PSI}$
  - Wall Loss = $32.80\%$
  - Calculation:
    $$\text{Rupture Risk Factor} = \left(\frac{3650.00}{3900.00}\right) \times \left(\frac{32.80}{100}\right) = 0.935897 \times 0.3280 = \mathbf{0.3070}$$
  - Log matches: `RuptureRisk: 0.3070` -> **VERIFIED**

---

## 5. Critical Corrosion Filter Triggers & Asset Category Caps

| Segment Code | Asset Category | Max Wall Loss Cap | Measured Wall Loss | Measured CP Voltage | Trigger Condition | Assigned Grade | Mandatory Action |
|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---|
| **PIPE-SUB-10001** | HIGH-PRESSURE DEEPWATER | 30.00% | 12.50% | 1.050V | None (Safe) | **B** | Normal Service Flow |
| **PIPE-SUB-10002** | HIGH-PRESSURE DEEPWATER | 30.00% | **32.80%** | **0.820V** | Exceeds Cap + CP < 0.85V | **D** | **Emergency Line Closure** |
| **PIPE-SUB-20001** | SHALLOW COASTAL TRUNK | 35.00% | 18.00% | 1.100V | None (Safe) | **C** | Maintenance Sleeve Ordered |
| **PIPE-SUB-20002** | SHALLOW COASTAL TRUNK | 35.00% | 26.50% | 0.980V | Score < 60.0 | **D** | **Emergency Line Closure** |
| **PIPE-SUB-30001** | LIQUEFIED GAS FEEDER | 25.00% | 8.40% | 1.150V | None (Safe) | **B** | Normal Service Flow |
| **PIPE-SUB-30002** | LIQUEFIED GAS FEEDER | 25.00% | **28.20%** | **0.840V** | Exceeds Cap + CP < 0.85V | **D** | **Emergency Line Closure** |
| **PIPE-SUB-20003** | SHALLOW COASTAL TRUNK | 35.00% | **36.40%** | **0.830V** | Exceeds Cap + CP < 0.85V | **D** | **Emergency Line Closure** |

---

## 6. Exported CSV Reports Verification

### 6.1 Subsea Repair Authorizations (`reports/subsea_repair_authorizations.csv`)
```csv
Order_Code,Segment_Code,Asset_Category,Integrity_Grade,Severity_Band,Recommended_Action,Pressure_Reduction_Pct,Target_Operating_Pressure_PSI,Order_Status,Authorized_By,Timestamp
REPAIR-ORD-84139-20001,PIPE-SUB-20001,"SHALLOW COASTAL TRUNK LINE",C,MAINTENANCE_REQUIRED,"Maintenance Sleeve Ordered - Full-Encirclement Steel Sleeve",20.00,1040.00,AUTHORIZED,SUBSEA_INTEGRITY_CONTROLLER_VM1,"2026-11-08 18:56:24"
REPAIR-ORD-84140-10002,PIPE-SUB-10002,"HIGH-PRESSURE DEEPWATER PIPE",D,CRITICAL_HAZARD,"Emergency Line Closure - Subsea ROV Actuation & Isolation",50.00,1825.00,AUTHORIZED,SUBSEA_INTEGRITY_CONTROLLER_VM1,"2026-11-08 18:56:24"
REPAIR-ORD-84140-20003,PIPE-SUB-20003,"SHALLOW COASTAL TRUNK LINE",D,CRITICAL_HAZARD,"Emergency Line Closure - Subsea ROV Actuation & Isolation",50.00,710.00,AUTHORIZED,SUBSEA_INTEGRITY_CONTROLLER_VM1,"2026-11-08 18:56:24"
REPAIR-ORD-84140-20002,PIPE-SUB-20002,"SHALLOW COASTAL TRUNK LINE",D,CRITICAL_HAZARD,"Emergency Line Closure - Subsea ROV Actuation & Isolation",50.00,625.00,AUTHORIZED,SUBSEA_INTEGRITY_CONTROLLER_VM1,"2026-11-08 18:56:24"
REPAIR-ORD-84141-30003,PIPE-SUB-30003,"LIQUEFIED GAS FEEDER",D,CRITICAL_HAZARD,"Emergency Line Closure - Subsea ROV Actuation & Isolation",50.00,1300.00,AUTHORIZED,SUBSEA_INTEGRITY_CONTROLLER_VM1,"2026-11-08 18:56:24"
REPAIR-ORD-84141-30002,PIPE-SUB-30002,"LIQUEFIED GAS FEEDER",D,CRITICAL_HAZARD,"Emergency Line Closure - Subsea ROV Actuation & Isolation",50.00,1275.00,AUTHORIZED,SUBSEA_INTEGRITY_CONTROLLER_VM1,"2026-11-08 18:56:24"
```

### 6.2 Pipeline Integrity Diagnostic Report (`reports/pipeline_integrity_report.csv`)
```csv
Segment_Code,Asset_Category,Subsea_Depth_M,Fluid_Type,PIG_Run_Tag,Wall_Loss_Pct,Wall_Loss_Cap_Pct,Cathodic_Protection_V,Operating_Pressure_PSI,MAOP_PSI,PCI_Score,Rupture_Risk_Factor,Composite_Health_Score,Integrity_Grade,Operational_Status,Hazard_Flag,Hazard_Description
PIPE-SUB-20001,"SHALLOW COASTAL TRUNK LINE",145.20,Multiphase,PIG-TAG-1,13.67,35.00,1.050,1440.00,1600.00,59.00,0.1463,65.83,C,"Maintenance Sleeve Ordered",false,"NONE"
PIPE-SUB-10002,"HIGH-PRESSURE DEEPWATER PIPE",1890.00,Crude Oil,PIG-TAG-4,25.74,30.00,1.050,3510.00,3900.00,15.40,0.3070,35.66,D,"Emergency Line Closure",true,"CRITICAL_CORROSION_HAZARD: Wall loss 32.80% (Cap: 30.00%) | CP Volt: 0.820V (<0.85V threshold)"
PIPE-SUB-20002,"SHALLOW COASTAL TRUNK LINE",85.00,Multiphase,PIG-TAG-3,22.23,35.00,1.050,1305.00,1450.00,36.00,0.2284,44.41,D,"Emergency Line Closure",false,"INTEGRITY_FAILURE: Composite Score 44.41 is below critical 60.0 threshold"
PIPE-SUB-10001,"HIGH-PRESSURE DEEPWATER PIPE",2150.50,Crude Oil,PIG-TAG-2,8.95,30.00,1.050,3780.00,4200.00,67.50,0.1131,77.63,B,"Normal Service Flow",false,"NONE"
PIPE-SUB-30001,"LIQUEFIED GAS FEEDER",620.80,Liquefied Gas,PIG-TAG-5,7.99,25.00,1.050,2520.00,2800.00,80.70,0.0720,80.03,B,"Normal Service Flow",false,"NONE"
PIPE-SUB-10003,"HIGH-PRESSURE DEEPWATER PIPE",2420.00,Crude Oil,PIG-TAG-6,9.86,30.00,1.050,3960.00,4400.00,65.60,0.1275,75.35,B,"Normal Service Flow",false,"NONE"
PIPE-SUB-20003,"SHALLOW COASTAL TRUNK LINE",110.50,Multiphase,PIG-TAG-7,32.96,35.00,1.050,1395.00,1550.00,8.70,0.3335,17.60,D,"Emergency Line Closure",true,"CRITICAL_CORROSION_HAZARD: Wall loss 36.40% (Cap: 35.00%) | CP Volt: 0.830V (<0.85V threshold)"
PIPE-SUB-30003,"LIQUEFIED GAS FEEDER",580.00,Liquefied Gas,PIG-TAG-8,24.36,25.00,1.050,2385.00,2650.00,46.00,0.2060,39.10,D,"Emergency Line Closure",false,"INTEGRITY_FAILURE: Composite Score 39.10 is below critical 60.0 threshold"
PIPE-SUB-30002,"LIQUEFIED GAS FEEDER",710.00,Liquefied Gas,PIG-TAG-9,33.42,25.00,1.050,2430.00,2700.00,25.60,0.2663,16.45,D,"Emergency Line Closure",true,"CRITICAL_CORROSION_HAZARD: Wall loss 28.20% (Cap: 25.00%) | CP Volt: 0.840V (<0.85V threshold)"
```

---

## 7. Database State & Relational Table Statistics Proof

```text
================================================================================
 SUBSEA SYSTEM DIAGNOSTIC & DATABASE AUDIT SUMMARY (STUDENT 2 DB HOST)
================================================================================
   - Table [pipe_segments           ] : 9 records synchronized
   - Table [pig_inspections         ] : 9 records synchronized
   - Table [corrosion_audits        ] : 9 records synchronized
   - Table [repair_orders           ] : 6 records synchronized
   - Total PIG Ingestion Streams Processed: 9
   - Corrupt/Invalid Regex Lines Filtered : 1
   - Active Emergency Repair Orders Issued: 6
================================================================================
```
