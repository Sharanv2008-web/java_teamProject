package com.subsea.pipeline;

import java.util.concurrent.Callable;
import java.util.regex.Pattern;

/**
 * IntegrityWorker - Multithreaded Subsea Pipeline Telemetry Processor
 * 
 * Implements parallel inspection analysis across hundreds of kilometers of subsea pipelines.
 * Enforces asset-specific categories, mathematical PCI & Rupture Risk calculations,
 * Critical Corrosion Filtering, string parsing, and hazard reporting.
 */
public class IntegrityWorker implements Callable<IntegrityWorker.WorkerResult> {

    // Regex requirement: Validate Segment Code regex (PIPE-SUB-\d{5})
    private static final Pattern SEGMENT_CODE_REGEX = Pattern.compile("^PIPE-SUB-\\d{5}$");

    private final String rawTelemetryLine;
    private final PipelineDAO pipelineDAO;
    private final int workerThreadId;

    public static class WorkerResult {
        public boolean success;
        public String segmentCode;
        public String pigRunTag;
        public PipelineDAO.PigInspection inspection;
        public PipelineDAO.CorrosionAudit audit;
        public PipelineDAO.RepairOrder repairOrder;
        public String logMessage;
        public String hazardNotification;
        public String validationError;
    }

    public IntegrityWorker(String rawTelemetryLine, PipelineDAO pipelineDAO, int workerThreadId) {
        this.rawTelemetryLine = rawTelemetryLine;
        this.pipelineDAO = pipelineDAO;
        this.workerThreadId = workerThreadId;
    }

    @Override
    public WorkerResult call() {
        WorkerResult result = new WorkerResult();
        long startTime = System.currentTimeMillis();

        try {
            // =================================================================
            // 1. STRING PROCESSING: SUBSTRING & TAG EXTRACTION
            // =================================================================
            if (rawTelemetryLine == null || rawTelemetryLine.trim().isEmpty() || rawTelemetryLine.startsWith("#")) {
                result.success = false;
                result.validationError = "Empty or comment line skipped.";
                return result;
            }

            // Extract PIG tag using .substring()
            String pigRunTag = "UNKNOWN";
            int tagIndex = rawTelemetryLine.indexOf("TAG:");
            if (tagIndex != -1) {
                int nextPipe = rawTelemetryLine.indexOf("|", tagIndex);
                if (nextPipe != -1) {
                    pigRunTag = rawTelemetryLine.substring(tagIndex + 4, nextPipe);
                } else {
                    pigRunTag = rawTelemetryLine.substring(tagIndex + 4);
                }
            }
            result.pigRunTag = pigRunTag;

            // Parse key-value tokens from telemetry line
            String[] tokens = rawTelemetryLine.split("\\|");
            String segmentCode = null;
            String rawDepthStr = null;
            double wallLossPct = 0.0;
            double cpVoltage = 0.0;
            double internalPressurePsi = 0.0;
            double upstreamBpd = 0.0;
            double downstreamBpd = 0.0;
            double acousticFreqKhz = 0.0;
            double hydrophoneNoise = 0.0;
            double envCorrosion = 0.0;
            double anchorRisk = 0.0;
            double tempVariance = 0.0;

            for (String token : tokens) {
                int colonIdx = token.indexOf(":");
                if (colonIdx == -1) continue;
                String key = token.substring(0, colonIdx).trim();
                String val = token.substring(colonIdx + 1).trim();

                switch (key) {
                    case "SEG":
                        segmentCode = val;
                        break;
                    case "DEPTH":
                        rawDepthStr = val; // Depth note to be cleaned via .trim()
                        break;
                    case "WALL_LOSS":
                        wallLossPct = Double.parseDouble(val.replace("%", ""));
                        break;
                    case "CP_VOLT":
                        cpVoltage = Math.abs(Double.parseDouble(val.replace("V", "")));
                        break;
                    case "PRESSURE":
                        internalPressurePsi = Double.parseDouble(val.replace("PSI", ""));
                        break;
                    case "UPSTREAM":
                        upstreamBpd = Double.parseDouble(val.replace("BPD", ""));
                        break;
                    case "DOWNSTREAM":
                        downstreamBpd = Double.parseDouble(val.replace("BPD", ""));
                        break;
                    case "FREQ":
                        acousticFreqKhz = Double.parseDouble(val.replace("kHz", ""));
                        break;
                    case "NOISE":
                        hydrophoneNoise = Double.parseDouble(val);
                        break;
                    case "ENV_CORR":
                        envCorrosion = Double.parseDouble(val.replace("%", ""));
                        break;
                    case "ANCHOR_RISK":
                        anchorRisk = Double.parseDouble(val);
                        break;
                    case "TEMP_VAR":
                        tempVariance = Double.parseDouble(val.replace("C", ""));
                        break;
                }
            }

            result.segmentCode = segmentCode;

            // =================================================================
            // 2. STRING PROCESSING: REGEX VALIDATION & DEPTH CLEANING
            // =================================================================
            if (segmentCode == null || !SEGMENT_CODE_REGEX.matcher(segmentCode).matches()) {
                result.success = false;
                result.validationError = String.format("REGEX VALIDATION FAILED for code '%s'. Expected format: PIPE-SUB-\\d{5}", segmentCode);
                return result;
            }

            // Clean sea-depth notes via .trim()
            double depthMeters = 0.0;
            if (rawDepthStr != null) {
                String cleanedDepth = rawDepthStr.trim().replaceAll("[^0-9.]", "");
                depthMeters = cleanedDepth.isEmpty() ? 0.0 : Double.parseDouble(cleanedDepth);
            }

            // Retrieve registered segment metadata
            PipelineDAO.PipeSegment segment = pipelineDAO.getSegment(segmentCode);
            if (segment == null) {
                // Auto-register segment based on depth heuristics if not already in catalog
                String category;
                double cap;
                double maop;
                String fluid;
                if (depthMeters >= 1000.0) {
                    category = "HIGH-PRESSURE DEEPWATER PIPE";
                    cap = 30.0;
                    maop = 4000.0;
                    fluid = "Crude Oil";
                } else if (rawTelemetryLine.contains("TEMP_VAR") && tempVariance > 3.0) {
                    category = "LIQUEFIED GAS FEEDER";
                    cap = 25.0;
                    maop = 2800.0;
                    fluid = "Liquefied Gas";
                } else {
                    category = "SHALLOW COASTAL TRUNK LINE";
                    cap = 35.0;
                    maop = 1500.0;
                    fluid = "Multiphase";
                }
                segment = new PipelineDAO.PipeSegment(segmentCode, category, depthMeters, 32.0, fluid, maop, cap, "Offshore Autonomous Sector");
                pipelineDAO.registerPipeSegment(segment);
            }

            // =================================================================
            // 3. MATHEMATICAL FORMULAS
            // =================================================================
            // Formula 1: Pipeline Corrosion Index (PCI)
            // PCI = 100 - (Wall Loss % * 2.0 + (1.2 - Cathodic Voltage) * 50)
            double pci = 100.0 - ((wallLossPct * 2.0) + ((1.2 - cpVoltage) * 50.0));

            // Formula 2: Rupture Risk Factor
            // Rupture Risk Factor = (Operating Pressure / MAOP) * (Wall Loss % / 100)
            double maop = (segment.maopPsi > 0) ? segment.maopPsi : 4000.0;
            double ruptureRiskFactor = (internalPressurePsi / maop) * (wallLossPct / 100.0);

            // Flow Pressure Drop %
            double flowPressureDropPct = 0.0;
            if (upstreamBpd > 0) {
                flowPressureDropPct = Math.max(0.0, ((upstreamBpd - downstreamBpd) / upstreamBpd) * 100.0);
            }

            // =================================================================
            // 4. SUBSEA ASSET CATEGORIES & COMPOSITE SCORING
            // =================================================================
            double compositeHealthScore = 0.0;
            double maxCap = segment.maxWallLossCapPct;

            switch (segment.assetCategory) {
                case "HIGH-PRESSURE DEEPWATER PIPE":
                    // Ultrasonic Wall Loss = 50% | Cathodic Protection Voltage = 30% | Flow Pressure Drop = 20% | Cap = 30%
                    double scoreWallLoss = Math.max(0.0, 100.0 - (wallLossPct / 30.0 * 100.0));
                    double scoreCp = Math.min(100.0, Math.max(0.0, (cpVoltage / 1.1) * 100.0));
                    double scoreDrop = Math.max(0.0, 100.0 - (flowPressureDropPct * 5.0));
                    compositeHealthScore = (scoreWallLoss * 0.50) + (scoreCp * 0.30) + (scoreDrop * 0.20);
                    break;

                case "SHALLOW COASTAL TRUNK LINE":
                    // Wall Loss = 40% | Environmental Corrosion = 40% | Anchor Drag Risk = 20% | Cap = 35%
                    double scoreWallCS = Math.max(0.0, 100.0 - (wallLossPct / 35.0 * 100.0));
                    double scoreEnv = Math.max(0.0, 100.0 - (envCorrosion * 2.5));
                    double scoreAnchor = Math.max(0.0, 100.0 - anchorRisk);
                    compositeHealthScore = (scoreWallCS * 0.40) + (scoreEnv * 0.40) + (scoreAnchor * 0.20);
                    break;

                case "LIQUEFIED GAS FEEDER":
                    // Wall Loss = 50% | Temperature Variance = 30% | Pressure Drop = 20% | Cap = 25%
                    double scoreWallGF = Math.max(0.0, 100.0 - (wallLossPct / 25.0 * 100.0));
                    double scoreTemp = Math.max(0.0, 100.0 - (tempVariance * 10.0));
                    double scoreDropGF = Math.max(0.0, 100.0 - (flowPressureDropPct * 5.0));
                    compositeHealthScore = (scoreWallGF * 0.50) + (scoreTemp * 0.30) + (scoreDropGF * 0.20);
                    break;

                default:
                    compositeHealthScore = Math.max(0.0, 100.0 - wallLossPct * 2.0);
                    break;
            }

            // Bound health score between 0.0 and 100.0
            compositeHealthScore = Math.max(0.0, Math.min(100.0, compositeHealthScore));

            // =================================================================
            // 5. CRITICAL CORROSION FILTER & INTEGRITY BANDS
            // =================================================================
            // Critical Corrosion Filter: Wall loss exceeding category cap OR Cathodic Voltage < 0.85V
            boolean isHazard = (wallLossPct > maxCap) || (cpVoltage < 0.850);
            char integrityGrade;
            String operationalStatus;
            String hazardDescription = "NONE";

            if (isHazard) {
                integrityGrade = 'D';
                operationalStatus = "Emergency Line Closure";
                hazardDescription = String.format("CRITICAL_CORROSION_HAZARD: Wall loss %.2f%% (Cap: %.2f%%) | CP Volt: %.3fV (<0.85V threshold)",
                        wallLossPct, maxCap, cpVoltage);
            } else if (compositeHealthScore >= 90.0) {
                integrityGrade = 'A';
                operationalStatus = "Unrestricted Operation";
            } else if (compositeHealthScore >= 75.0) {
                integrityGrade = 'B';
                operationalStatus = "Normal Service Flow";
            } else if (compositeHealthScore >= 60.0) {
                integrityGrade = 'C';
                operationalStatus = "Maintenance Sleeve Ordered";
            } else {
                integrityGrade = 'D';
                operationalStatus = "Emergency Line Closure";
                hazardDescription = String.format("INTEGRITY_FAILURE: Composite Score %.2f is below critical 60.0 threshold", compositeHealthScore);
            }

            // Format hazard notification report with String.format()
            if (isHazard || integrityGrade == 'D') {
                result.hazardNotification = String.format(
                        "*** CRITICAL HAZARD NOTIFICATION ***%n" +
                        "  [Thread-%02d] Segment: %s | Asset Category: %s%n" +
                        "  Depth: %.2fm | Measured Wall Loss: %.2f%% (Category Cap: %.2f%%)%n" +
                        "  Cathodic Protection: %.3fV | Operating Pressure: %.2f PSI (MAOP: %.2f PSI)%n" +
                        "  Calculated PCI: %.2f | Rupture Risk Factor: %.4f | Score: %.2f (Grade %c)%n" +
                        "  Mandatory Directive: %s%n" +
                        "  Mitigation Order: Reduce line pressure by 50%% and isolate subsea segment.",
                        workerThreadId, segmentCode, segment.assetCategory, depthMeters,
                        wallLossPct, maxCap, cpVoltage, internalPressurePsi, maop,
                        pci, ruptureRiskFactor, compositeHealthScore, integrityGrade, operationalStatus
                );
            }

            // =================================================================
            // 6. POPULATE INSPECTION & AUDIT RECORDS
            // =================================================================
            PipelineDAO.PigInspection inspection = new PipelineDAO.PigInspection(
                    segmentCode, pigRunTag, wallLossPct, cpVoltage, internalPressurePsi, depthMeters / 10.0, rawTelemetryLine
            );
            int inspId = pipelineDAO.insertPigInspection(inspection);

            PipelineDAO.CorrosionAudit audit = new PipelineDAO.CorrosionAudit();
            audit.segmentCode = segmentCode;
            audit.inspectionId = inspId;
            audit.upstreamFlowBpd = upstreamBpd;
            audit.downstreamFlowBpd = downstreamBpd;
            audit.flowPressureDropPct = flowPressureDropPct;
            audit.acousticLeakFreqKhz = acousticFreqKhz;
            audit.hydrophoneNoiseScore = hydrophoneNoise;
            audit.environmentalCorrosionPct = envCorrosion;
            audit.anchorDragRiskScore = anchorRisk;
            audit.tempVarianceCelsius = tempVariance;
            audit.pipelineCorrosionIndex = pci;
            audit.ruptureRiskFactor = ruptureRiskFactor;
            audit.compositeHealthScore = compositeHealthScore;
            audit.integrityGrade = integrityGrade;
            audit.operationalStatus = operationalStatus;
            audit.hazardFlag = isHazard;
            audit.hazardDescription = hazardDescription;
            int auditId = pipelineDAO.insertCorrosionAudit(audit);

            // =================================================================
            // 7. GENERATE REPAIR AUTHORIZATION IF REQUIRED
            // =================================================================
            PipelineDAO.RepairOrder order = null;
            if (integrityGrade == 'C' || integrityGrade == 'D' || isHazard) {
                order = new PipelineDAO.RepairOrder();
                order.orderCode = String.format("REPAIR-ORD-%d-%s", System.currentTimeMillis() % 100000, segmentCode.replace("PIPE-SUB-", ""));
                order.segmentCode = segmentCode;
                order.auditId = auditId;
                order.integrityGrade = integrityGrade;

                if (isHazard || integrityGrade == 'D') {
                    order.severityBand = "CRITICAL_HAZARD";
                    order.recommendedAction = "Emergency Line Closure - Subsea ROV Actuation & Isolation";
                    order.pressureReductionPct = 50.00;
                    order.targetOperatingPressure = internalPressurePsi * 0.50;
                } else {
                    order.severityBand = "MAINTENANCE_REQUIRED";
                    order.recommendedAction = "Maintenance Sleeve Ordered - Full-Encirclement Steel Sleeve";
                    order.pressureReductionPct = 20.00;
                    order.targetOperatingPressure = internalPressurePsi * 0.80;
                }
                order.orderStatus = "AUTHORIZED";
                order.authorizedBy = "SUBSEA_INTEGRITY_CONTROLLER_VM1";
                pipelineDAO.createRepairOrder(order);
            }

            long elapsed = System.currentTimeMillis() - startTime;
            result.success = true;
            result.inspection = inspection;
            result.audit = audit;
            result.repairOrder = order;
            result.logMessage = String.format(
                    "[WorkerThread-%02d] Processed %s (Tag: %s) in %dms | PCI: %.2f | RuptureRisk: %.4f | Score: %.2f | Grade: %c [%s]",
                    workerThreadId, segmentCode, pigRunTag, elapsed, pci, ruptureRiskFactor, compositeHealthScore, integrityGrade, operationalStatus
            );

        } catch (Exception e) {
            result.success = false;
            result.validationError = "Exception in worker thread: " + e.getMessage();
        }

        return result;
    }
}
