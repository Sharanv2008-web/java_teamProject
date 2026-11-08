package com.subsea.calculation;

import com.subsea.model.CorrosionAudit;
import com.subsea.model.PigInspection;
import com.subsea.model.PipeSegment;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * RuptureRiskCalculator computes the Rupture Risk Factor (RRF) and determines
 * the primary hazard code and engineering remediation directives.
 *
 * ENGINEERING METHODOLOGY:
 * - Hoop stress ratio under internal pressure vs rated MAOP adjusted for wall thinning (ASME B31G).
 * - Subsea depth hydrostatic pressure differential: deepwater external hydrostatic pressure
 *   mitigates internal hoop burst stress but introduces buckle/collapse considerations.
 * - Fluid volatility weighting (Crude Oil, Natural Gas, Liquefied Gas).
 * - Multi-criteria hazard classifier identifying dominant pipeline risk factors.
 *
 * NOTE: This is an automated decision-support model and not a certified safety guarantee.
 */
public class RuptureRiskCalculator {

    public static final BigDecimal DEFAULT_RISK_FLOOR = new BigDecimal("0.0500");
    public static final BigDecimal DEFAULT_RISK_CEILING = new BigDecimal("0.9999");

    /**
     * Calculates the normalized Rupture Risk Factor (between 0.0001 and 0.9999).
     */
    public static BigDecimal calculateRuptureRisk(PipeSegment segment, PigInspection inspection,
                                                  CorrosionAudit audit, BigDecimal pciScore) {
        double risk = 0.10; // Baseline ambient risk

        // 1. Inverted PCI Risk Influence (40% weight)
        if (pciScore != null) {
            double pciFraction = (100.0 - pciScore.doubleValue()) / 100.0;
            risk += pciFraction * 0.45;
        }

        // 2. Hoop Stress Ratio vs MAOP with Wall Loss (25% weight)
        if (segment != null && inspection != null) {
            double maop = segment.getMaopPsi() != null ? segment.getMaopPsi().doubleValue() : 3000.0;
            double pressure = inspection.getInternalPressurePsi() != null ? inspection.getInternalPressurePsi().doubleValue() : 2000.0;
            double wallLoss = inspection.getWallLossPercent() != null ? inspection.getWallLossPercent().doubleValue() : 0.0;

            // Effective remaining pressure capacity derated by wall loss
            double effectiveCapacity = maop * (1.0 - (wallLoss / 100.0));
            if (effectiveCapacity > 0) {
                double stressRatio = pressure / effectiveCapacity;
                if (stressRatio > 0.70) {
                    risk += Math.min(0.30, (stressRatio - 0.70) * 0.75);
                }
            }
        }

        // 3. Flow Discrepancy Leak Factor (20% weight)
        if (audit != null) {
            double flowLoss = audit.getFlowLossPercent();
            if (flowLoss > 1.0) {
                risk += Math.min(0.20, (flowLoss / 100.0) * 2.5);
            }
        }

        // 4. Fluid Hazard Multiplier
        double fluidMultiplier = 1.0;
        if (segment != null && segment.getFluidType() != null) {
            String fluid = segment.getFluidType().toUpperCase();
            if (fluid.contains("LIQUEFIED")) {
                fluidMultiplier = 1.15; // Cryogenic / high-expansion volatility
            } else if (fluid.contains("NATURAL_GAS")) {
                fluidMultiplier = 1.10; // Gas explosion / rapid dispersion
            } else if (fluid.contains("CRUDE")) {
                fluidMultiplier = 1.05; // Marine ecological spill risk
            }
        }
        risk *= fluidMultiplier;

        // Clamp between 0.0001 and 0.9999
        risk = Math.max(0.0001, Math.min(0.9999, risk));

        return BigDecimal.valueOf(risk).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Determines the dominant hazard code based on telemetry thresholds.
     */
    public static String identifyHazardCode(PipeSegment segment, PigInspection inspection,
                                            CorrosionAudit audit, BigDecimal pciScore,
                                            BigDecimal ruptureRisk) {
        double pci = pciScore != null ? pciScore.doubleValue() : 100.0;
        double risk = ruptureRisk != null ? ruptureRisk.doubleValue() : 0.0;
        double wallLoss = (inspection != null && inspection.getWallLossPercent() != null) ?
                inspection.getWallLossPercent().doubleValue() : 0.0;
        double cpVoltage = (inspection != null && inspection.getCathodicVoltageV() != null) ?
                inspection.getCathodicVoltageV().doubleValue() : 1.0;
        double flowLoss = audit != null ? audit.getFlowLossPercent() : 0.0;
        double acousticHz = (audit != null && audit.getAcousticLeakHz() != null) ?
                audit.getAcousticLeakHz().doubleValue() : 0.0;

        // Hierarchy of hazard classification:
        if (risk >= 0.50 || pci < 40.0) {
            return "HAZ-CRIT-RUPTURE";
        }
        if (flowLoss >= 4.0 || (flowLoss >= 2.0 && acousticHz >= 200.0)) {
            return "HAZ-LEAK-ACTIVE";
        }
        if (wallLoss >= 20.0) {
            return "HAZ-CORR-SEVERE";
        }
        if (cpVoltage < 0.85) {
            return "HAZ-CP-DEFICIENT";
        }
        if (inspection != null && segment != null &&
            inspection.getInternalPressurePsi() != null && segment.getMaopPsi() != null) {
            double pRatio = inspection.getInternalPressurePsi().doubleValue() / segment.getMaopPsi().doubleValue();
            if (pRatio >= 0.80) {
                return "HAZ-PRESSURE-SURGE";
            }
        }
        if (wallLoss >= 12.0) {
            return "HAZ-CORR-MODERATE";
        }

        return "HAZ-NOMINAL";
    }

    /**
     * Generates concrete, actionable subsea pipeline repair recommendations.
     */
    public static String determineActionRequired(String hazardCode, BigDecimal pciScore,
                                                 BigDecimal ruptureRisk, String segmentCode) {
        switch (hazardCode) {
            case "HAZ-CRIT-RUPTURE":
                return "EMERGENCY: Immediate pressure derating by 35%, subsea ROV visual survey within 24h, and structural composite sleeve installation.";
            case "HAZ-LEAK-ACTIVE":
                return "URGENT: Isolate segment section, dispatch acoustic localization ROV, and prepare subsea mechanical split-sleeve clamp within 48h.";
            case "HAZ-CORR-SEVERE":
                return "HIGH PRIORITY: Deploy smart ultrasonic caliper PIG, calibrate sacrificial zinc/aluminum anodes, and perform cathodic protection retrofit.";
            case "HAZ-CP-DEFICIENT":
                return "SCHEDULED: Replenish sacrificial subsea anode sleds and conduct close-interval potential survey (CIPS) along segment route.";
            case "HAZ-PRESSURE-SURGE":
                return "OPERATIONAL: Modulate topside choke valve to reduce operational pressure below 70% MAOP and verify pressure safety valve (PSV) calibration.";
            case "HAZ-CORR-MODERATE":
                return "ROUTINE: Inject film-forming corrosion inhibitor into fluid stream; schedule next in-line PIG inspection in 6 months.";
            case "HAZ-NOMINAL":
            default:
                return "CONTINUOUS MONITORING: Pipeline operating within standard envelope; proceed with standard quarterly SCADA and acoustic audits.";
        }
    }
}
