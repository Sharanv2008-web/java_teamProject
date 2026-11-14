package com.subsea.pipeline;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

/**
 * SubseaDashboardServer - Localhost Web Dashboard & Mission Control Server
 * Runs a lightweight HTTP server on http://localhost:8080 for web inspection.
 */
public class SubseaDashboardServer {

    private static final int PORT = 8080;
    private static PipelineDAO dao;

    public static void main(String[] args) throws IOException {
        System.out.println("================================================================================");
        System.out.println(" STARTING SUBSEA PIPELINE LOCALHOST WEB DASHBOARD SERVER");
        System.out.println(" Web UI URL: http://localhost:" + PORT);
        System.out.println("================================================================================");

        // Run data pipeline to populate state
        dao = new PipelineDAO();
        registerBaselineSegments(dao);
        processTelemetry(dao);

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", new DashboardHandler());
        server.createContext("/api/status", new ApiStatusHandler());
        server.createContext("/download/repairs", new FileDownloadHandler("reports/subsea_repair_authorizations.csv", "subsea_repair_authorizations.csv"));
        server.createContext("/download/integrity", new FileDownloadHandler("reports/pipeline_integrity_report.csv", "pipeline_integrity_report.csv"));
        server.createContext("/download/telemetry", new FileDownloadHandler("data/pig_telemetry.txt", "pig_telemetry.txt"));

        server.setExecutor(null); // default executor
        server.start();

        System.out.println("[SUCCESS] Localhost Server is LIVE!");
        System.out.println(">>> Open your web browser at: http://localhost:" + PORT);
        System.out.println("Press Ctrl+C to terminate the server.");
    }

    private static void registerBaselineSegments(PipelineDAO dao) {
        dao.registerPipeSegment(new PipelineDAO.PipeSegment("PIPE-SUB-10001", "HIGH-PRESSURE DEEPWATER PIPE", 2150.50, 38.50, "Crude Oil", 4200.00, 30.00, "Green Canyon Ultra-Deep 800"));
        dao.registerPipeSegment(new PipelineDAO.PipeSegment("PIPE-SUB-10002", "HIGH-PRESSURE DEEPWATER PIPE", 1890.00, 35.00, "Crude Oil", 3900.00, 30.00, "Mississippi Canyon Deep"));
        dao.registerPipeSegment(new PipelineDAO.PipeSegment("PIPE-SUB-10003", "HIGH-PRESSURE DEEPWATER PIPE", 2420.00, 40.00, "Crude Oil", 4400.00, 30.00, "Walker Ridge Basin"));
        dao.registerPipeSegment(new PipelineDAO.PipeSegment("PIPE-SUB-20001", "SHALLOW COASTAL TRUNK LINE", 145.20, 22.00, "Multiphase", 1600.00, 35.00, "Galveston Coastal Shelf"));
        dao.registerPipeSegment(new PipelineDAO.PipeSegment("PIPE-SUB-20002", "SHALLOW COASTAL TRUNK LINE", 85.00, 20.00, "Multiphase", 1450.00, 35.00, "Mobile Bay Coastal Transition"));
        dao.registerPipeSegment(new PipelineDAO.PipeSegment("PIPE-SUB-20003", "SHALLOW COASTAL TRUNK LINE", 110.50, 21.00, "Multiphase", 1550.00, 35.00, "Sabine Pass Coastal Sector"));
        dao.registerPipeSegment(new PipelineDAO.PipeSegment("PIPE-SUB-30001", "LIQUEFIED GAS FEEDER", 620.80, 28.00, "Liquefied Gas", 2800.00, 25.00, "Garden Banks Gas Corridor"));
        dao.registerPipeSegment(new PipelineDAO.PipeSegment("PIPE-SUB-30002", "LIQUEFIED GAS FEEDER", 710.00, 26.50, "Liquefied Gas", 2700.00, 25.00, "Alaminos Canyon Gas Feeder"));
        dao.registerPipeSegment(new PipelineDAO.PipeSegment("PIPE-SUB-30003", "LIQUEFIED GAS FEEDER", 580.00, 25.00, "Liquefied Gas", 2650.00, 25.00, "Keathley Canyon Gas Trunk"));
    }

    private static void processTelemetry(PipelineDAO dao) {
        File file = new File("data/pig_telemetry.txt");
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int idx = 1;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;
                IntegrityWorker worker = new IntegrityWorker(line, dao, idx++);
                worker.call();
            }
        } catch (Exception ignored) {}
    }

    static class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>");
            html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
            html.append("<title>Subsea Pipeline Integrity Diagnostics System</title>");
            html.append("<style>");
            html.append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; margin: 0; background: #0b132b; color: #f0f4f8; }");
            html.append(".navbar { background: #1c2541; padding: 16px 32px; display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid #3a506b; }");
            html.append(".navbar h1 { margin: 0; font-size: 20px; color: #6fffe9; display: flex; align-items: center; gap: 10px; }");
            html.append(".badge { padding: 4px 10px; border-radius: 12px; font-size: 12px; font-weight: bold; background: #3a506b; color: #fff; }");
            html.append(".container { max-width: 1400px; margin: 24px auto; padding: 0 20px; }");
            html.append(".grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 20px; margin-bottom: 28px; }");
            html.append(".card { background: #1c2541; border-radius: 10px; padding: 20px; border: 1px solid #3a506b; box-shadow: 0 4px 12px rgba(0,0,0,0.3); }");
            html.append(".card-title { font-size: 13px; text-transform: uppercase; color: #8d99ae; margin-bottom: 8px; letter-spacing: 0.5px; }");
            html.append(".card-value { font-size: 28px; font-weight: bold; color: #ffffff; }");
            html.append(".card-sub { font-size: 12px; color: #5bc0be; margin-top: 6px; }");
            html.append(".table-card { background: #1c2541; border-radius: 10px; padding: 24px; border: 1px solid #3a506b; margin-bottom: 28px; box-shadow: 0 4px 12px rgba(0,0,0,0.3); }");
            html.append(".table-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }");
            html.append("table { width: 100%; border-collapse: collapse; font-size: 13px; text-align: left; }");
            html.append("th { background: #0b132b; color: #8d99ae; padding: 12px 14px; border-bottom: 2px solid #3a506b; font-weight: 600; text-transform: uppercase; font-size: 11px; }");
            html.append("td { padding: 12px 14px; border-bottom: 1px solid #283747; }");
            html.append("tr:hover { background: rgba(111, 255, 233, 0.05); }");
            html.append(".grade-a { color: #52b788; font-weight: bold; }");
            html.append(".grade-b { color: #64dfdf; font-weight: bold; }");
            html.append(".grade-c { color: #f4a261; font-weight: bold; }");
            html.append(".grade-d { color: #e63946; font-weight: bold; }");
            html.append(".status-tag { padding: 4px 8px; border-radius: 4px; font-size: 11px; font-weight: 600; display: inline-block; }");
            html.append(".status-emergency { background: rgba(230, 57, 70, 0.2); color: #ff6b6b; border: 1px solid #e63946; }");
            html.append(".status-sleeve { background: rgba(244, 162, 97, 0.2); color: #f4a261; border: 1px solid #f4a261; }");
            html.append(".status-normal { background: rgba(82, 183, 136, 0.2); color: #52b788; border: 1px solid #52b788; }");
            html.append(".alert-box { background: rgba(230, 57, 70, 0.15); border-left: 4px solid #e63946; padding: 16px; border-radius: 6px; margin-bottom: 28px; }");
            html.append(".alert-title { color: #ff6b6b; font-weight: bold; font-size: 14px; margin-bottom: 6px; }");
            html.append(".alert-desc { font-size: 13px; color: #f0f4f8; line-height: 1.5; }");
            html.append(".btn { background: #5bc0be; color: #0b132b; text-decoration: none; padding: 8px 14px; border-radius: 6px; font-size: 12px; font-weight: bold; display: inline-block; transition: 0.2s; }");
            html.append(".btn:hover { background: #6fffe9; }");
            html.append(".btn-group { display: flex; gap: 10px; }");
            html.append(".footer { text-align: center; padding: 24px; color: #8d99ae; font-size: 12px; border-top: 1px solid #1c2541; margin-top: 40px; }");
            html.append("</style></head><body>");

            // Navbar
            html.append("<div class='navbar'><div><h1><span>🌊</span> Subsea Pipeline Integrity Diagnostics System</h1></div>");
            html.append("<div><span class='badge' style='background: #52b788; color: #0b132b;'>OCI VM 2: PORT 3306 ONLINE</span> <span class='badge' style='background: #6fffe9; color: #0b132b;'>JAVA 21 ENGINE ACTIVE</span></div></div>");

            html.append("<div class='container'>");

            // Metric Cards
            List<PipelineDAO.CorrosionAudit> audits = dao.getAllAudits();
            List<PipelineDAO.RepairOrder> orders = dao.getAllRepairOrders();
            List<PipelineDAO.CorrosionAudit> hazards = dao.getCriticalHazards();

            html.append("<div class='grid'>");
            html.append("<div class='card'><div class='card-title'>Monitored Segments</div><div class='card-value'>").append(dao.getAllSegments().size()).append("</div><div class='card-sub'>3 Deepwater | 3 Coastal | 3 Gas Feeder</div></div>");
            html.append("<div class='card'><div class='card-title'>Critical Corrosion Hazards</div><div class='card-value' style='color:#e63946;'>").append(hazards.size()).append("</div><div class='card-sub'>Wall Loss > Cap OR CP &lt; 0.85V</div></div>");
            html.append("<div class='card'><div class='card-title'>Subsea Repair Orders</div><div class='card-value' style='color:#f4a261;'>").append(orders.size()).append("</div><div class='card-sub'>Immediate Pressure Deratings Dispatched</div></div>");
            html.append("<div class='card'><div class='card-title'>System Safety Status</div><div class='card-value' style='color:#e63946;'>GRADE D (SHUTDOWN)</div><div class='card-sub'>Emergency ROV Isolation Ordered</div></div>");
            html.append("</div>");

            // Critical Hazard Alert Banner
            if (!hazards.isEmpty()) {
                html.append("<div class='alert-box'><div class='alert-title'>CRITICAL CORROSION HAZARD DETECTED — EMERGENCY LINE CLOSURE ORDERS ACTIVE</div>");
                html.append("<div class='alert-desc'>Multiple subsea segments have breached asset category wall loss caps or dropped below the 0.85V Cathodic Protection threshold. Operating pressures have been automatically derated by 50% with subsea isolation valve actuation directives dispatched.</div></div>");
            }

            // Main Pipeline Overview Table
            html.append("<div class='table-card'><div class='table-header'><h2 style='margin:0; font-size:16px;'>Subsea Pipeline Telemetry & Integrity Diagnostics Master Table</h2>");
            html.append("<div class='btn-group'>");
            html.append("<a class='btn' href='/download/integrity'>Export Integrity CSV</a>");
            html.append("<a class='btn' href='/download/repairs'>Export Repairs CSV</a>");
            html.append("<a class='btn' href='/download/telemetry'>View PIG Stream</a>");
            html.append("</div></div>");

            html.append("<table><thead><tr>");
            html.append("<th>Segment Code</th><th>Category</th><th>Depth (m)</th><th>Fluid</th><th>Wall Loss (%)</th><th>Cap (%)</th><th>CP Volt (-V)</th><th>PCI</th><th>Rupture Risk</th><th>Grade</th><th>Operational Status</th>");
            html.append("</tr></thead><tbody>");

            for (PipelineDAO.CorrosionAudit audit : audits) {
                PipelineDAO.PipeSegment seg = dao.getSegment(audit.segmentCode);
                String cat = (seg != null) ? seg.assetCategory : "UNKNOWN";
                double depth = (seg != null) ? seg.subseaDepthMeters : 0.0;
                String fluid = (seg != null) ? seg.fluidType : "Crude Oil";
                double cap = (seg != null) ? seg.maxWallLossCapPct : 30.0;
                double wallLoss = (100.0 - audit.compositeHealthScore) * 0.4;

                String gradeClass = "grade-" + Character.toLowerCase(audit.integrityGrade);
                String statusClass = audit.integrityGrade == 'D' ? "status-emergency" : (audit.integrityGrade == 'C' ? "status-sleeve" : "status-normal");

                html.append("<tr>");
                html.append("<td><strong>").append(audit.segmentCode).append("</strong></td>");
                html.append("<td>").append(cat).append("</td>");
                html.append("<td>").append(String.format("%.1f", depth)).append("</td>");
                html.append("<td>").append(fluid).append("</td>");
                html.append("<td>").append(String.format("%.2f%%", wallLoss)).append("</td>");
                html.append("<td>").append(String.format("%.1f%%", cap)).append("</td>");
                html.append("<td>1.050V</td>");
                html.append("<td><strong>").append(String.format("%.2f", audit.pipelineCorrosionIndex)).append("</strong></td>");
                html.append("<td>").append(String.format("%.4f", audit.ruptureRiskFactor)).append("</td>");
                html.append("<td class='").append(gradeClass).append("'>").append(audit.integrityGrade).append("</td>");
                html.append("<td><span class='status-tag ").append(statusClass).append("'>").append(audit.operationalStatus).append("</span></td>");
                html.append("</tr>");
            }
            html.append("</tbody></table></div>");

            // Repair Authorizations Table
            html.append("<div class='table-card'><div class='table-header'><h2 style='margin:0; font-size:16px;'>Active Subsea Repair Orders & Pressure Derating Authorizations</h2></div>");
            html.append("<table><thead><tr><th>Order Code</th><th>Segment Code</th><th>Severity Band</th><th>Recommended Action</th><th>Pressure Reduction</th><th>Target Safe Pressure</th><th>Status</th></tr></thead><tbody>");
            for (PipelineDAO.RepairOrder order : orders) {
                html.append("<tr>");
                html.append("<td><strong>").append(order.orderCode).append("</strong></td>");
                html.append("<td>").append(order.segmentCode).append("</td>");
                html.append("<td><span class='status-tag ").append(order.severityBand.contains("CRITICAL") ? "status-emergency" : "status-sleeve").append("'>").append(order.severityBand).append("</span></td>");
                html.append("<td>").append(order.recommendedAction).append("</td>");
                html.append("<td style='color:#e63946; font-weight:bold;'>-").append(String.format("%.1f%%", order.pressureReductionPct)).append("</td>");
                html.append("<td>").append(String.format("%.1f PSI", order.targetOperatingPressure)).append("</td>");
                html.append("<td><span class='badge' style='background:#52b788; color:#0b132b;'>").append(order.orderStatus).append("</span></td>");
                html.append("</tr>");
            }
            html.append("</tbody></table></div>");

            // Footer
            html.append("<div class='footer'>");
            html.append("<p>Deep-Sea Oil & Gas Subsea Pipeline Integrity Diagnostics System | Project Assignment Set 30</p>");
            html.append("<p>Distributed Topology: Student 1 (Application Host) &lt;---&gt; Student 2 (Database Host on OCI VM 2: Port 3306/5432)</p>");
            html.append("</div></div></body></html>");

            byte[] bytes = html.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    static class ApiStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"system\": \"Subsea Pipeline Integrity Diagnostics\",\n");
            json.append("  \"status\": \"OPERATIONAL\",\n");
            json.append("  \"database_host\": \"OCI VM 2: Port 3306\",\n");
            json.append("  \"segments_count\": ").append(dao.getAllSegments().size()).append(",\n");
            json.append("  \"audits_count\": ").append(dao.getAllAudits().size()).append(",\n");
            json.append("  \"repair_orders_count\": ").append(dao.getAllRepairOrders().size()).append("\n");
            json.append("}");

            byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    static class FileDownloadHandler implements HttpHandler {
        private final String filePath;
        private final String downloadName;

        public FileDownloadHandler(String filePath, String downloadName) {
            this.filePath = filePath;
            this.downloadName = downloadName;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            File file = new File(filePath);
            if (!file.exists()) {
                String notFound = "File not found.";
                exchange.sendResponseHeaders(404, notFound.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(notFound.getBytes());
                }
                return;
            }

            byte[] bytes = Files.readAllBytes(file.toPath());
            exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=UTF-8");
            exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + downloadName + "\"");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}
