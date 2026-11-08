package com.subsea.report;

import com.subsea.model.CorrosionAudit;
import com.subsea.model.IntegrityResult;
import com.subsea.model.PigInspection;
import com.subsea.model.PipeSegment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CsvReportGenerator exports calculated subsea pipeline integrity assessments,
 * telemetry attributes, PCI scores, rupture risks, and repair recommendations
 * into CSV reports stored in the 'reports/' directory.
 */
public class CsvReportGenerator {

    private static final Logger LOGGER = Logger.getLogger(CsvReportGenerator.class.getName());
    private static final DateTimeFormatter FILE_TS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DISPLAY_TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String outputDirectory;

    public CsvReportGenerator() {
        this("reports");
    }

    public CsvReportGenerator(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    /**
     * Generates both a timestamped CSV report and a latest symlink/file copy.
     *
     * @param results List of calculated pipeline integrity results
     * @return File object pointing to the created timestamped report
     * @throws IOException if file creation fails
     */
    public File generateReport(List<IntegrityResult> results) throws IOException {
        File dir = new File(outputDirectory);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created && !dir.exists()) {
                throw new IOException("Failed to create reports directory: " + dir.getAbsolutePath());
            }
        }

        String timestamp = LocalDateTime.now().format(FILE_TS_FORMAT);
        String filename = String.format("pipeline_integrity_report_%s.csv", timestamp);
        File reportFile = new File(dir, filename);
        File latestFile = new File(dir, "latest_pipeline_integrity_report.csv");

        writeCsvContent(reportFile, results);
        writeCsvContent(latestFile, results);

        System.out.printf("%n[REPORT GENERATED] Master CSV Report: %s%n", reportFile.getAbsolutePath());
        System.out.printf("[REPORT GENERATED] Latest CSV Copy:   %s%n", latestFile.getAbsolutePath());

        return reportFile;
    }

    private void writeCsvContent(File file, List<IntegrityResult> results) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            // Write CSV Header
            writer.println(
                "Segment ID," +
                "Segment Code," +
                "Pipeline Category," +
                "Subsea Depth (m)," +
                "Wall Thickness (mm)," +
                "Fluid Type," +
                "MAOP (psi)," +
                "PIG Wall Loss (%)," +
                "PIG Cathodic Voltage (V)," +
                "PIG Internal Pressure (psi)," +
                "Upstream Flow (BPD)," +
                "Downstream Flow (BPD)," +
                "Flow Discrepancy (%)," +
                "Acoustic Leak (Hz)," +
                "Hydrophone Noise Score," +
                "Pipeline Condition Index (PCI)," +
                "Integrity Grade," +
                "Rupture Risk Factor," +
                "Hazard Code," +
                "Recommended Action," +
                "Report Timestamp"
            );

            // Write Data Rows
            String now = LocalDateTime.now().format(DISPLAY_TS_FORMAT);

            for (IntegrityResult r : results) {
                PipeSegment seg = r.getSegment();
                PigInspection pig = r.getInspection();
                CorrosionAudit audit = r.getAudit();

                writer.printf("%d,\"%s\",\"%s\",%.2f,%.2f,\"%s\",%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,\"%s\",%.4f,\"%s\",\"%s\",\"%s\"%n",
                        seg != null ? seg.getSegmentId() : 0,
                        escape(seg != null ? seg.getSegmentCode() : "N/A"),
                        escape(seg != null ? seg.getCategory() : "N/A"),
                        seg != null && seg.getSubseaDepthM() != null ? seg.getSubseaDepthM().doubleValue() : 0.0,
                        seg != null && seg.getWallThicknessMm() != null ? seg.getWallThicknessMm().doubleValue() : 0.0,
                        escape(seg != null ? seg.getFluidType() : "N/A"),
                        seg != null && seg.getMaopPsi() != null ? seg.getMaopPsi().doubleValue() : 0.0,
                        pig != null && pig.getWallLossPercent() != null ? pig.getWallLossPercent().doubleValue() : 0.0,
                        pig != null && pig.getCathodicVoltageV() != null ? pig.getCathodicVoltageV().doubleValue() : 0.0,
                        pig != null && pig.getInternalPressurePsi() != null ? pig.getInternalPressurePsi().doubleValue() : 0.0,
                        audit != null && audit.getUpstreamFlowBpd() != null ? audit.getUpstreamFlowBpd().doubleValue() : 0.0,
                        audit != null && audit.getDownstreamFlowBpd() != null ? audit.getDownstreamFlowBpd().doubleValue() : 0.0,
                        audit != null ? audit.getFlowLossPercent() : 0.0,
                        audit != null && audit.getAcousticLeakHz() != null ? audit.getAcousticLeakHz().doubleValue() : 0.0,
                        audit != null && audit.getHydrophoneNoiseScore() != null ? audit.getHydrophoneNoiseScore().doubleValue() : 0.0,
                        r.getPciScore() != null ? r.getPciScore().doubleValue() : 0.0,
                        escape(r.getIntegrityGrade() != null ? r.getIntegrityGrade() : "N/A"),
                        r.getRuptureRiskFactor() != null ? r.getRuptureRiskFactor().doubleValue() : 0.0,
                        escape(r.getHazardCode() != null ? r.getHazardCode() : "N/A"),
                        escape(r.getRecommendedAction() != null ? r.getRecommendedAction() : "N/A"),
                        now
                );
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to write CSV report: " + file.getAbsolutePath(), e);
            throw e;
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\"\"");
    }
}
