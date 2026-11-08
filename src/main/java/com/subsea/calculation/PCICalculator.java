package com.subsea.calculation;

import com.subsea.model.CorrosionAudit;
import com.subsea.model.PigInspection;
import com.subsea.model.PipeSegment;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * PCICalculator calculates the Pipeline Condition Index (PCI) on a normalized 0 to 100 scale.
 *
 * ENGINEERING ASSUMPTIONS AND LIMITATIONS:
 * 1. This formula is a project-specific composite scoring index designed for automated telemetry
 *    assessment based on ASME B31G, API 579 Fitness-For-Service principles, and NACE SP0169.
 * 2. It is not an official statutory certified calculation and should be supplemented by formal
 *    finite-element engineering analysis before performing physical subsea intervention.
 * 3. Base Score is 100. Deductions are calculated from:
 *    - Wall loss percentage and remaining wall thickness (Max 40 points)
 *    - Cathodic protection voltage deviation from NACE standard (Max 25 points)
 *    - Operating pressure vs. Maximum Allowable Operating Pressure (MAOP) (Max 20 points)
 *    - Volumetric flow balance deficit between upstream and downstream meters (Max 15 points)
 *    - Acoustic noise and hydrophone score indicative of orifice leakage turbulence (Max 15 points)
 */
public class PCICalculator {

    public static final double BASE_SCORE = 100.0;

    /**
     * Calculates the composite PCI score from segment and telemetry data.
     *
     * @param segment Pipe segment structural parameters
     * @param inspection PIG inspection telemetry
     * @param audit Corrosion and acoustic audit telemetry
     * @return BigDecimal PCI score between 0.00 and 100.00
     */
    public static BigDecimal calculatePCI(PipeSegment segment, PigInspection inspection, CorrosionAudit audit) {
        double score = BASE_SCORE;

        // 1. Wall Loss Deduction (Max 40 points)
        if (inspection != null && inspection.getWallLossPercent() != null) {
            double wallLoss = inspection.getWallLossPercent().doubleValue();
            // Up to 10% wall loss is normal operational wear. Above 10%, progressive deduction.
            double wallLossDeduction = Math.min(40.0, wallLoss * 0.90);
            score -= wallLossDeduction;
        }

        // 2. Cathodic Protection Potential Deduction (NACE SP0169 standard: 0.85V to 1.15V)
        if (inspection != null && inspection.getCathodicVoltageV() != null) {
            double cpVoltage = inspection.getCathodicVoltageV().doubleValue();
            if (cpVoltage < 0.85) {
                // Under-protection: severe galvanic corrosion hazard
                double cpDeduction = Math.min(25.0, (0.85 - cpVoltage) * 80.0);
                score -= cpDeduction;
            } else if (cpVoltage > 1.20) {
                // Over-protection: risk of cathodic disbondment and hydrogen embrittlement
                double cpDeduction = Math.min(15.0, (cpVoltage - 1.20) * 40.0);
                score -= cpDeduction;
            }
        }

        // 3. Operating Pressure vs MAOP Ratio Deduction (Max 20 points)
        if (segment != null && segment.getMaopPsi() != null &&
            inspection != null && inspection.getInternalPressurePsi() != null) {
            double maop = segment.getMaopPsi().doubleValue();
            double pressure = inspection.getInternalPressurePsi().doubleValue();
            if (maop > 0) {
                double pressureRatio = pressure / maop;
                // Standard design safety factor threshold is 0.72 MAOP
                if (pressureRatio > 0.70) {
                    double pressureDeduction = Math.min(20.0, (pressureRatio - 0.70) * 60.0);
                    score -= pressureDeduction;
                }
            }
        }

        // 4. Volumetric Flow Discrepancy (Upstream vs Downstream) Deduction (Max 15 points)
        if (audit != null) {
            double flowLoss = audit.getFlowLossPercent();
            if (flowLoss > 0.5) { // Tolerating 0.5% metering inaccuracy
                double flowDeduction = Math.min(15.0, flowLoss * 2.0);
                score -= flowDeduction;
            }
        }

        // 5. Acoustic & Hydrophone Sensor Noise Deduction (Max 15 points)
        if (audit != null) {
            double noiseScore = audit.getHydrophoneNoiseScore() != null ?
                    audit.getHydrophoneNoiseScore().doubleValue() : 0.0;
            double acousticHz = audit.getAcousticLeakHz() != null ?
                    audit.getAcousticLeakHz().doubleValue() : 0.0;

            double acousticDeduction = Math.min(10.0, (noiseScore / 100.0) * 15.0);
            if (acousticHz >= 200.0) {
                // High frequency acoustic hiss typical of pressurized gas/fluid orifice leak
                acousticDeduction += 5.0;
            }
            score -= Math.min(15.0, acousticDeduction);
        }

        // Clamp between 0.00 and 100.00
        score = Math.max(0.0, Math.min(100.0, score));

        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Determines the qualitative integrity grade from the numeric PCI score.
     *
     * @param pciScore numeric PCI
     * @return Grade letter (A, B, C, D, or F)
     */
    public static String determineGrade(BigDecimal pciScore) {
        if (pciScore == null) return "N/A";
        double s = pciScore.doubleValue();
        if (s >= 85.0) return "A";
        if (s >= 70.0) return "B";
        if (s >= 55.0) return "C";
        if (s >= 40.0) return "D";
        return "F";
    }
}
