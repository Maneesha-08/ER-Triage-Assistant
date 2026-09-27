import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * =============================================================================
 * ER Triage Assistant - Main Entrypoint & Server Orchestrator
 * =============================================================================
 * Academic/Demo Project: Hospital Emergency Room Triage Management System.
 * DISCLAIMER: Academic Demo Only - Not for clinical use or real medical diagnosis.
 *
 * Course Alignment:
 * - OOPJ: Modular systems architecture, robust exception handling, HTTP microservice.
 * - ADSA: PriorityQueue Max-Heap state presentation.
 * - DMGT: Propositional logic evaluation pipeline.
 * =============================================================================
 */
public class Main {

    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_JSON_PATH = "data/patients.json";
    private static final String DEFAULT_PYTHON_SCRIPT = "python-scoring/severity_model.py";
    private static final String FRONTEND_DIR = "frontend";

    private static TriageManager triageManager;
    private static String projectRootPath;
    private static String jsonFilePath;
    private static String pythonScriptPath;

    public static void main(String[] args) {
        printAcademicBanner();

        // Resolve absolute project roots
        resolvePaths();

        triageManager = new TriageManager();

        // 1. Initial State Loading
        File jsonFile = new File(jsonFilePath);
        if (jsonFile.exists() && jsonFile.length() > 10) {
            try {
                System.out.println("[INFO] Loading initial patient records from: " + jsonFilePath);
                triageManager.importFromJson(jsonFilePath);
            } catch (Exception e) {
                System.err.println("[WARN] Could not load JSON, populating demo patients: " + e.getMessage());
                triageManager.loadDemoPatients();
            }
        } else {
            System.out.println("[INFO] Initializing sample academic demo patients (A, B, C, D, E)...");
            triageManager.loadDemoPatients();
            try {
                triageManager.exportToJson(jsonFilePath);
            } catch (Exception e) {
                System.err.println("[WARN] Could not export demo patients to JSON: " + e.getMessage());
            }
        }

        // Print initial Max-Heap state to terminal
        printCurrentQueue("INITIAL MAX-HEAP QUEUE (DMGT Propositional Rules Only)");

        // 2. Start Embedded Web Server
        int port = DEFAULT_PORT;
        try {
            HttpServer server = startHttpServer(port);
            System.out.println("\n" + "=".repeat(78));
            System.out.println(">>> ER TRIAGE ASSISTANT WEB DASHBOARD ACTIVE <<<");
            System.out.println("  Local URL:  http://localhost:" + port);
            System.out.println("  REST API:   http://localhost:" + port + "/api/patients");
            System.out.println("=".repeat(78) + "\n");
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to start HTTP server on port " + port + ": " + e.getMessage());
            System.err.println("Attempting fallback to port 8081...");
            try {
                port = 8081;
                HttpServer server = startHttpServer(port);
                System.out.println("\n[SUCCESS] Web Dashboard active on http://localhost:" + port + "\n");
            } catch (Exception ex) {
                System.err.println("[FATAL] Could not start server: " + ex.getMessage());
            }
        }

        // Check Python Availability
        String pyCmd = TriageManager.detectPythonCommand();
        if (pyCmd != null) {
            System.out.println("[INFO] Python runtime detected: '" + pyCmd + "'. Ready for triage analysis.");
        } else {
            System.err.println("[WARN] Python 3 not detected on system PATH. Automated scoring requires Python.");
        }
    }

    /**
     * Starts the lightweight embedded HTTP server with static file serving & REST API endpoints.
     */
    private static HttpServer startHttpServer(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // API Endpoints
        server.createContext("/api/patients", new PatientsApiHandler());
        server.createContext("/api/triage/analyze", new TriageAnalyzeHandler());
        server.createContext("/api/patients/reset", new ResetHandler());
        server.createContext("/api/rules/explain", new RuleExplainHandler());
        server.createContext("/api/status", new StatusHandler());

        // Static Frontend Files
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
        return server;
    }

    // -------------------------------------------------------------------------
    // HTTP Handlers (REST API + Static Assets)
    // -------------------------------------------------------------------------

    /**
     * Handles GET /api/patients and POST /api/patients and DELETE /api/patients?id=...
     */
    static class PatientsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String method = exchange.getRequestMethod().toUpperCase();

            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                if ("GET".equals(method)) {
                    List<Patient> patients = triageManager.getAllPatientsSorted();
                    String responseJson = buildPatientsListJsonResponse(patients);
                    sendResponse(exchange, 200, "application/json", responseJson);
                } else if ("POST".equals(method)) {
                    String body = readRequestBody(exchange);
                    List<Patient> parsed = TriageManager.parseJsonPatients("[" + body + "]");
                    if (parsed.isEmpty()) {
                        sendResponse(exchange, 400, "application/json", "{\"error\": \"Invalid patient JSON data\"}");
                        return;
                    }
                    Patient newPatient = parsed.get(0);
                    triageManager.addPatient(newPatient);
                    triageManager.exportToJson(jsonFilePath);

                    System.out.println("[LOG] New Patient Added: " + newPatient.getName() + " (Score: " + newPatient.getSeverityScore() + ")");

                    List<Patient> updated = triageManager.getAllPatientsSorted();
                    String responseJson = buildPatientsListJsonResponse(updated);
                    sendResponse(exchange, 201, "application/json", responseJson);
                } else if ("DELETE".equals(method)) {
                    String query = exchange.getRequestURI().getQuery();
                    String id = extractQueryParam(query, "id");
                    if (id != null && triageManager.removePatient(id)) {
                        triageManager.exportToJson(jsonFilePath);
                        sendResponse(exchange, 200, "application/json", "{\"message\": \"Patient " + id + " discharged.\"}");
                    } else {
                        sendResponse(exchange, 404, "application/json", "{\"error\": \"Patient not found\"}");
                    }
                } else {
                    sendResponse(exchange, 405, "application/json", "{\"error\": \"Method Not Allowed\"}");
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "application/json", "{\"error\": \"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    /**
     * Handles POST /api/triage/analyze
     * Runs Java -> JSON -> Python -> JSON -> Java reheapify pipeline!
     */
    static class TriageAnalyzeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String method = exchange.getRequestMethod().toUpperCase();

            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equals(method)) {
                sendResponse(exchange, 405, "application/json", "{\"error\": \"Method Not Allowed\"}");
                return;
            }

            try {
                System.out.println("\n[TRIGGER] Running Full Triage Analysis Pipeline...");
                String pythonLogs = triageManager.runFullTriageAnalysis(jsonFilePath, pythonScriptPath);
                System.out.println("[LOG] Python Scoring Output:\n" + pythonLogs);

                printCurrentQueue("RE-RANKED MAX-HEAP QUEUE (Post-Python Refined Scoring)");

                List<Patient> updated = triageManager.getAllPatientsSorted();
                StringBuilder sb = new StringBuilder();
                sb.append("{\n");
                sb.append("  \"success\": true,\n");
                sb.append("  \"message\": \"Triage ranking updated successfully.\",\n");
                sb.append("  \"pythonLogs\": \"").append(escapeJson(pythonLogs)).append("\",\n");
                sb.append("  \"patients\": ").append(buildPatientsArrayJson(updated)).append("\n");
                sb.append("}");

                sendResponse(exchange, 200, "application/json", sb.toString());
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "application/json",
                        "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    /**
     * Handles POST /api/patients/reset (Restores standard academic demo patients)
     */
    static class ResetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            try {
                triageManager.loadDemoPatients();
                triageManager.exportToJson(jsonFilePath);
                List<Patient> patients = triageManager.getAllPatientsSorted();
                sendResponse(exchange, 200, "application/json", buildPatientsListJsonResponse(patients));
            } catch (Exception e) {
                sendResponse(exchange, 500, "application/json", "{\"error\": \"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    /**
     * Handles GET /api/rules/explain?id=P101 (Returns DMGT Propositional Logic breakdown)
     */
    static class RuleExplainHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String query = exchange.getRequestURI().getQuery();
            String id = extractQueryParam(query, "id");
            if (id == null) {
                sendResponse(exchange, 400, "application/json", "{\"error\": \"Missing id parameter\"}");
                return;
            }
            Patient p = triageManager.getPatientById(id);
            if (p == null) {
                sendResponse(exchange, 404, "application/json", "{\"error\": \"Patient not found\"}");
                return;
            }

            List<String> explanations = SeverityRules.explainLogic(p);
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("  \"patientId\": \"").append(escapeJson(p.getPatientId())).append("\",\n");
            sb.append("  \"name\": \"").append(escapeJson(p.getName())).append("\",\n");
            sb.append("  \"ruleScore\": ").append(p.getRuleScore()).append(",\n");
            sb.append("  \"pythonScore\": ").append(p.getPythonScore()).append(",\n");
            sb.append("  \"severityScore\": ").append(p.getSeverityScore()).append(",\n");
            sb.append("  \"priorityLevel\": \"").append(escapeJson(p.getPriorityLevel())).append("\",\n");
            sb.append("  \"explanations\": [\n");
            for (int i = 0; i < explanations.size(); i++) {
                sb.append("    \"").append(escapeJson(explanations.get(i))).append("\"");
                if (i < explanations.size() - 1) sb.append(",\n");
                else sb.append("\n");
            }
            sb.append("  ]\n");
            sb.append("}");

            sendResponse(exchange, 200, "application/json", sb.toString());
        }
    }

    /**
     * Handles GET /api/status (Health check & statistics)
     */
    static class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String py = TriageManager.detectPythonCommand();
            String json = String.format("{\"status\": \"ONLINE\", \"heapSize\": %d, \"pythonAvailable\": %b, \"pythonCommand\": \"%s\"}",
                    triageManager.getQueueSize(), py != null, py != null ? py : "none");
            sendResponse(exchange, 200, "application/json", json);
        }
    }

    /**
     * Serves static frontend files (HTML, CSS, JS) from the frontend/ folder.
     */
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            // Prevent path traversal
            if (path.contains("..")) {
                sendResponse(exchange, 403, "text/plain", "Forbidden");
                return;
            }

            File file = new File(projectRootPath, FRONTEND_DIR + path);
            if (!file.exists() || file.isDirectory()) {
                // Try direct relative path
                file = new File(FRONTEND_DIR + path);
            }

            if (!file.exists() || file.isDirectory()) {
                sendResponse(exchange, 404, "text/plain", "File Not Found: " + path);
                return;
            }

            String mimeType = "text/plain";
            if (path.endsWith(".html")) mimeType = "text/html; charset=UTF-8";
            else if (path.endsWith(".css")) mimeType = "text/css; charset=UTF-8";
            else if (path.endsWith(".js")) mimeType = "application/javascript; charset=UTF-8";
            else if (path.endsWith(".json")) mimeType = "application/json; charset=UTF-8";
            else if (path.endsWith(".svg")) mimeType = "image/svg+xml";

            byte[] bytes = Files.readAllBytes(file.toPath());
            addCorsHeaders(exchange);
            exchange.getResponseHeaders().set("Content-Type", mimeType);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helper Methods & Path Resolution
    // -------------------------------------------------------------------------

    private static void resolvePaths() {
        // Look for project root relative to execution working directory
        File current = new File(".").getAbsoluteFile();
        if (new File(current, "data/patients.json").exists() || new File(current, "frontend/index.html").exists()) {
            projectRootPath = current.getAbsolutePath();
        } else if (new File(current, "ER-Triage-Assistant").exists()) {
            projectRootPath = new File(current, "ER-Triage-Assistant").getAbsolutePath();
        } else if (new File(current.getParentFile(), "data/patients.json").exists()) {
            projectRootPath = current.getParentFile().getAbsolutePath();
        } else {
            projectRootPath = current.getAbsolutePath();
        }

        jsonFilePath = new File(projectRootPath, DEFAULT_JSON_PATH).getAbsolutePath();
        pythonScriptPath = new File(projectRootPath, DEFAULT_PYTHON_SCRIPT).getAbsolutePath();

        System.out.println("[CONFIG] Project Root:    " + projectRootPath);
        System.out.println("[CONFIG] Data JSON File:  " + jsonFilePath);
        System.out.println("[CONFIG] Python Script:   " + pythonScriptPath);
    }

    private static String buildPatientsListJsonResponse(List<Patient> patients) {
        int critical = 0, high = 0, medium = 0, low = 0;
        for (Patient p : patients) {
            String prio = p.getPriorityLevel();
            if ("CRITICAL".equalsIgnoreCase(prio)) critical++;
            else if ("HIGH".equalsIgnoreCase(prio)) high++;
            else if ("MEDIUM".equalsIgnoreCase(prio)) medium++;
            else low++;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"stats\": {\n");
        sb.append("    \"total\": ").append(patients.size()).append(",\n");
        sb.append("    \"critical\": ").append(critical).append(",\n");
        sb.append("    \"high\": ").append(high).append(",\n");
        sb.append("    \"medium\": ").append(medium).append(",\n");
        sb.append("    \"low\": ").append(low).append("\n");
        sb.append("  },\n");
        sb.append("  \"patients\": ").append(buildPatientsArrayJson(patients)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    private static String buildPatientsArrayJson(List<Patient> patients) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < patients.size(); i++) {
            sb.append(patients.get(i).toJson());
            if (i < patients.size() - 1) sb.append(",\n");
            else sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    private static void printAcademicBanner() {
        System.out.println("==============================================================================");
        System.out.println("                   ER TRIAGE ASSISTANT - CORE BACKEND                         ");
        System.out.println("       Hospital Emergency Room Priority Management System (Academic Demo)     ");
        System.out.println("==============================================================================");
        System.out.println("Academic Subjects Integrated:");
        System.out.println("  * DMGT  : Discrete Math Propositional Logic Severity Rules (P, Q, R, S)");
        System.out.println("  * ADSA  : Max-Heap PriorityQueue Ordering with O(log N) Priority Invariants");
        System.out.println("  * OOPJ  : Encapsulated Patient State, Thread-Safe Queues & Microservice");
        System.out.println("  * AI/Py : Transparent Weighted Severity Scoring Model & JSON Bridge");
        System.out.println("------------------------------------------------------------------------------");
        System.out.println("DISCLAIMER: FOR ACADEMIC EVALUATION ONLY. NOT FOR CLINICAL DIAGNOSIS.");
        System.out.println("==============================================================================");
    }

    private static void printCurrentQueue(String title) {
        System.out.println("\n>>> " + title + " <<<");
        System.out.printf("%-5s | %-6s | %-24s | %-4s | %-9s | %-8s | %-10s%n",
                "Rank", "ID", "Patient Name", "Age", "RuleScore", "PyScore", "FinalScore");
        System.out.println("-".repeat(78));

        List<Patient> list = triageManager.getAllPatientsSorted();
        int rank = 1;
        for (Patient p : list) {
            System.out.printf("#%-4d | %-6s | %-24s | %-4d | %-9.1f | %-8.1f | %-10.1f (%s)%n",
                    rank++, p.getPatientId(),
                    (p.getName().length() > 22 ? p.getName().substring(0, 20) + ".." : p.getName()),
                    p.getAge(), p.getRuleScore(), p.getPythonScore(), p.getSeverityScore(), p.getPriorityLevel());
        }
        System.out.println("-".repeat(78));
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String contentType, String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static String extractQueryParam(String query, String paramName) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=");
            if (parts.length >= 2 && parts[0].equals(paramName)) {
                try {
                    return URLDecoder.decode(parts[1], StandardCharsets.UTF_8.name());
                } catch (Exception e) {
                    return parts[1];
                }
            }
        }
        return null;
    }

    private static String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\r", "\\r")
                    .replace("\n", "\\n")
                    .replace("\t", "\\t");
    }
}
