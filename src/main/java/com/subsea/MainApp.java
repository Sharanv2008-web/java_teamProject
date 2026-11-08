package com.subsea;

import com.subsea.config.DBConnection;
import com.subsea.dao.RepairOrderDAO;
import com.subsea.model.IntegrityResult;
import com.subsea.model.RepairOrder;
import com.subsea.report.CsvReportGenerator;
import com.subsea.service.PipelineMonitoringService;

import java.io.File;
import java.util.List;

/**
 * MainApp - Subsea Pipeline Integrity Monitoring System
 *
 * Primary Execution Class for Student 1 (Java Application Host).
 * Integrates with MySQL Database Host (Student 2 co-deliverable).
 */
public class MainApp {

    public static void main(String[] args) {
        printBanner();

        System.out.println("\n[PHASE A] Diagnosing Database Host Connectivity...");
        System.out.printf("  Target Host:     %s:%d%n", DBConnection.getHost(), DBConnection.getPort());
        System.out.printf("  Target Database: %s%n", DBConnection.getDatabase());
        System.out.printf("  Auth User:       %s%n", DBConnection.getUsername());

        if (!DBConnection.testConnection()) {
            System.err.println("\n[CRITICAL ERROR] Unable to connect to MySQL database!");
            System.err.println("Please verify that MySQL Server is running and Student 2's database exists.");
            System.err.println("Database URL: " + DBConnection.getJdbcUrl());
            System.exit(1);
        }
        System.out.println("  [SUCCESS] Database connection established successfully.");

        try {
            // Initialize Core Service
            PipelineMonitoringService monitoringService = new PipelineMonitoringService();

            // Execute Multithreaded Monitoring Cycle
            List<IntegrityResult> results = monitoringService.executeMonitoringCycle();

            if (results.isEmpty()) {
                System.out.println("\n[INFO] Monitoring cycle completed with no results.");
                return;
            }

            // Print Summary Assessment Table to Console
            printAssessmentTable(results);

            // Generate CSV Reports
            System.out.println("\n[PHASE B] Generating CSV Audit Reports...");
            CsvReportGenerator reportGenerator = new CsvReportGenerator("reports");
            File reportFile = reportGenerator.generateReport(results);
            System.out.printf("  -> Verified Report Size: %d bytes%n", reportFile.length());

            // Verify Repair Orders in Database
            System.out.println("\n[PHASE C] Inspecting Updated MySQL 'repair_orders' Table...");
            RepairOrderDAO roDAO = new RepairOrderDAO();
            List<RepairOrder> allOrders = roDAO.getAllRepairOrders();
            System.out.printf("  Total Repair Orders currently in MySQL: %d%n", allOrders.size());
            for (RepairOrder order : allOrders) {
                System.out.printf("   * Repair ID #%-3d | Segment ID: %-2d | Hazard: %-18s | PCI: %5.2f | Grade: %-2s | Risk: %6.4f | Action: %s%n",
                        order.getRepairId(), order.getSegmentId(), order.getHazardCode(),
                        order.getIntegrityScore() != null ? order.getIntegrityScore().doubleValue() : 0.0,
                        order.getIntegrityGrade(),
                        order.getRuptureRiskFactor() != null ? order.getRuptureRiskFactor().doubleValue() : 0.0,
                        order.getActionRequired());
            }

            printFooter();

        } catch (Exception e) {
            System.err.println("\n[ERROR] An error occurred during pipeline monitoring execution:");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void printBanner() {
        System.out.println("=========================================================================================");
        System.out.println("       SUBSEA PIPELINE INTEGRITY MONITORING SYSTEM - JAVA HOST ENGINE                    ");
        System.out.println("       Enterprise Concurrency, PCI Assessment & Subsea Risk Analytics                    ");
        System.out.println("=========================================================================================");
        System.out.println("  Role Assignment:");
        System.out.println("    - Student 1 (Current Host): Java App / Multithreading / Analytics / CSV Reporting");
        System.out.println("    - Student 2 (Database Host): MySQL Server / Schema / Telemetry Records / OCI VM 2");
        System.out.println("=========================================================================================");
    }

    private static void printAssessmentTable(List<IntegrityResult> results) {
        System.out.println("\n=============================================================================================================");
        System.out.println("                               PIPELINE INTEGRITY & RUPTURE RISK ASSESSMENT                                  ");
        System.out.println("=============================================================================================================");
        System.out.printf("%-16s | %-12s | %-8s | %-6s | %-10s | %-18s | %s%n",
                "Segment Code", "Category", "PCI", "Grade", "RuptureRisk", "Hazard Code", "Action Required");
        System.out.println("-------------------------------------------------------------------------------------------------------------");

        for (IntegrityResult r : results) {
            System.out.printf("%-16s | %-12s | %6.2f   |   %-3s  | %10.4f | %-18s | %s%n",
                    r.getSegment().getSegmentCode(),
                    truncate(r.getSegment().getCategory(), 12),
                    r.getPciScore().doubleValue(),
                    r.getIntegrityGrade(),
                    r.getRuptureRiskFactor().doubleValue(),
                    r.getHazardCode(),
                    truncate(r.getRecommendedAction(), 40));
        }
        System.out.println("=============================================================================================================");
    }

    private static void printFooter() {
        System.out.println("\n=========================================================================================");
        System.out.println(">>> MONITORING CYCLE COMPLETED SUCCESSFULLY.");
        System.out.println("    * Telemetry Analyzed:  OK");
        System.out.println("    * Multithreading:      OK");
        System.out.println("    * PCI & Risk Assessed: OK");
        System.out.println("    * Database Persisted:  OK");
        System.out.println("    * CSV Reports Saved:   OK");
        System.out.println("=========================================================================================");
    }

    private static String truncate(String str, int maxLen) {
        if (str == null) return "";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen - 3) + "...";
    }
}
