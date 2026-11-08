package com.subsea;

import com.subsea.calculation.PCICalculator;
import com.subsea.calculation.RuptureRiskCalculator;
import com.subsea.model.CorrosionAudit;
import com.subsea.model.PigInspection;
import com.subsea.model.PipeSegment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class CalculationTest {

    private PipeSegment baselineSegment;
    private PigInspection nominalPig;
    private CorrosionAudit nominalAudit;

    @BeforeEach
    void setUp() {
        baselineSegment = new PipeSegment(
                1, "PIPE-TEST-001", "HIGH_PRESSURE_DEEPWATER",
                new BigDecimal("2000.00"), new BigDecimal("35.00"),
                "CRUDE_OIL", new BigDecimal("5000.00")
        );

        nominalPig = new PigInspection(
                1, 1, new BigDecimal("5.00"),
                new BigDecimal("1.05"), new BigDecimal("3000.00"),
                new java.sql.Timestamp(System.currentTimeMillis())
        );

        nominalAudit = new CorrosionAudit(
                1, 1, new BigDecimal("10000.00"),
                new BigDecimal("9980.00"), new BigDecimal("80.00"),
                new BigDecimal("10.00"),
                new java.sql.Timestamp(System.currentTimeMillis())
        );
    }

    @Test
    @DisplayName("Nominal pipeline condition should achieve Grade A")
    void testNominalPCI() {
        BigDecimal pci = PCICalculator.calculatePCI(baselineSegment, nominalPig, nominalAudit);
        assertNotNull(pci);
        assertTrue(pci.doubleValue() >= 85.0, "Nominal condition should have PCI >= 85, got: " + pci);
        assertEquals("A", PCICalculator.determineGrade(pci));
    }

    @Test
    @DisplayName("Severe wall loss and flow leak should downgrade PCI")
    void testDegradedPCI() {
        PigInspection degradedPig = new PigInspection(
                2, 1, new BigDecimal("35.00"), // 35% wall loss
                new BigDecimal("0.75"),        // Deficient cathodic voltage
                new BigDecimal("4500.00"),     // 90% of MAOP
                new java.sql.Timestamp(System.currentTimeMillis())
        );

        CorrosionAudit leakAudit = new CorrosionAudit(
                2, 1, new BigDecimal("10000.00"),
                new BigDecimal("8500.00"),     // 15% flow loss
                new BigDecimal("280.00"),      // 280 Hz acoustic leak hiss
                new BigDecimal("75.00"),       // 75 hydrophone noise
                new java.sql.Timestamp(System.currentTimeMillis())
        );

        BigDecimal pci = PCICalculator.calculatePCI(baselineSegment, degradedPig, leakAudit);
        assertNotNull(pci);
        assertTrue(pci.doubleValue() < 60.0, "Degraded pipeline should score < 60, got: " + pci);
        String grade = PCICalculator.determineGrade(pci);
        assertTrue(grade.equals("C") || grade.equals("D") || grade.equals("F"));
    }

    @Test
    @DisplayName("Rupture risk factor calculation and hazard classification")
    void testRuptureRiskAndHazard() {
        BigDecimal pci = PCICalculator.calculatePCI(baselineSegment, nominalPig, nominalAudit);
        BigDecimal risk = RuptureRiskCalculator.calculateRuptureRisk(baselineSegment, nominalPig, nominalAudit, pci);

        assertNotNull(risk);
        assertTrue(risk.doubleValue() >= 0.0001 && risk.doubleValue() <= 1.0);

        String hazard = RuptureRiskCalculator.identifyHazardCode(baselineSegment, nominalPig, nominalAudit, pci, risk);
        assertNotNull(hazard);
        assertEquals("HAZ-NOMINAL", hazard);

        String action = RuptureRiskCalculator.determineActionRequired(hazard, pci, risk, baselineSegment.getSegmentCode());
        assertNotNull(action);
        assertTrue(action.contains("CONTINUOUS MONITORING"));
    }
}
