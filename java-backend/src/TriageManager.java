import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * =============================================================================
 * ER Triage Assistant - TriageManager (Core Queue & Orchestrator)
 * =============================================================================
 * Academic/Demo Project: Hospital Emergency Room Triage Management System.
 * DISCLAIMER: Academic Demo Only - Not for clinical use or real medical diagnosis.
 *
 * Course Alignment:
 * - ADSA: PriorityQueue (Binary Max-Heap) lifecycle management, heapify operations.
 * - OOPJ: Encapsulation of ER operational state, error handling, thread safety.
 * - Software Engineering: Inter-process pipeline execution (Java <-> Python via JSON).
 * =============================================================================
 */
public class TriageManager {

    // Internal Binary Max-Heap storage structure backed by java.util.PriorityQueue
    private final PriorityQueue<Patient> triageHeap;
    // Auxiliary lookup map for fast O(1) patient retrieval by ID
    private final Map<String, Patient> patientMap;

    public TriageManager() {
        this.triageHeap = new PriorityQueue<>(new PatientComparator());
        this.patientMap = new LinkedHashMap<>();
    }

    /**
     * Inserts a new patient into the triage system.
     * Evaluates DMGT propositional rules, sets initial scores, and enqueues into heap O(log N).
     */
    public synchronized void addPatient(Patient patient) {
        if (patient == null) return;

        // Ensure unique patient ID if none provided
        if (patient.getPatientId() == null || patient.getPatientId().trim().isEmpty()) {
            patient.setPatientId("P" + (100 + patientMap.size() + 1));
        }

        // Apply Discrete Math Propositional Logic Rules
        SeverityRules.evaluate(patient);

        // Remove previous instance if updating
        if (patientMap.containsKey(patient.getPatientId())) {
            removePatient(patient.getPatientId());
        }

        patientMap.put(patient.getPatientId(), patient);
        triageHeap.offer(patient);
    }

    /**
     * Removes a patient from the triage system by ID.
     */
    public synchronized boolean removePatient(String patientId) {
        Patient p = patientMap.remove(patientId);
        if (p != null) {
            triageHeap.remove(p);
            return true;
        }
        return false;
    }

    /**
     * Returns a snapshot of all patients currently in the triage queue,
     * ordered strictly by their Max-Heap priority (highest severity score first).
     * Does NOT alter or dequeue the internal heap.
     */
    public synchronized List<Patient> getAllPatientsSorted() {
        List<Patient> sortedList = new ArrayList<>(patientMap.values());
        sortedList.sort(new PatientComparator());
        return sortedList;
    }

    /**
     * Peeks at the highest urgency patient at the root of the Max-Heap in O(1) time.
     */
    public synchronized Patient peekNextPatient() {
        return triageHeap.peek();
    }

    /**
     * Dequeues (polls) the highest priority patient from the Max-Heap in O(log N) time.
     */
    public synchronized Patient pollNextPatient() {
        Patient highest = triageHeap.poll();
        if (highest != null) {
            patientMap.remove(highest.getPatientId());
            highest.setStatus("Admitted to Emergency Treatment");
        }
        return highest;
    }

    public synchronized Patient getPatientById(String patientId) {
        return patientMap.get(patientId);
    }

    public synchronized int getQueueSize() {
        return triageHeap.size();
    }

    public synchronized void clear() {
        triageHeap.clear();
        patientMap.clear();
    }

    /**
     * Rebuilds the entire heap in O(N) time after external Python score modifications.
     */
    public synchronized void rebuildHeap() {
        triageHeap.clear();
        for (Patient p : patientMap.values()) {
            triageHeap.offer(p);
        }
    }

    // -------------------------------------------------------------------------
    // Shared JSON File Export & Import (Java <-> Python Bridge)
    // -------------------------------------------------------------------------

    /**
     * Exports all current patient objects into the shared JSON file.
     */
    public synchronized void exportToJson(String filePath) throws IOException {
        List<Patient> patients = getAllPatientsSorted();
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < patients.size(); i++) {
            sb.append(patients.get(i).toJson());
            if (i < patients.size() - 1) {
                sb.append(",\n");
            } else {
                sb.append("\n");
            }
        }
        sb.append("]\n");

        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(sb.toString());
        }
    }

    /**
     * Imports patients from JSON file, updating scores and rebuilding the PriorityQueue.
     */
    public synchronized void importFromJson(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("Patient data file not found: " + filePath);
        }

        String jsonContent = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
        List<Patient> parsedPatients = parseJsonPatients(jsonContent);

        patientMap.clear();
        for (Patient p : parsedPatients) {
            patientMap.put(p.getPatientId(), p);
        }
        rebuildHeap();
    }

    /**
     * Lightweight built-in JSON parser for Patient objects.
     * Avoids third-party dependencies so students can run this without maven or gradle.
     */
    public static List<Patient> parseJsonPatients(String json) {
        List<Patient> list = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) return list;

        String content = json.trim();
        if (!content.startsWith("[") || !content.endsWith("]")) {
            return list;
        }

        // Find each object between { and }
        int depth = 0;
        int start = -1;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start != -1) {
                    String objJson = content.substring(start, i + 1);
                    Patient p = parseSinglePatient(objJson);
                    if (p != null) list.add(p);
                    start = -1;
                }
            }
        }
        return list;
    }

    private static Patient parseSinglePatient(String objJson) {
        Patient p = new Patient();
        Map<String, String> kv = parseSimpleJsonObject(objJson);

        if (kv.containsKey("patientId")) p.setPatientId(kv.get("patientId"));
        if (kv.containsKey("name")) p.setName(kv.get("name"));
        if (kv.containsKey("age")) p.setAge(parseInt(kv.get("age"), 30));
        if (kv.containsKey("temperature")) p.setTemperature(parseDouble(kv.get("temperature"), 98.6));
        if (kv.containsKey("heartRate")) p.setHeartRate(parseInt(kv.get("heartRate"), 75));
        if (kv.containsKey("bloodPressure")) p.setBloodPressure(kv.get("bloodPressure"));
        if (kv.containsKey("oxygenLevel")) p.setOxygenLevel(parseInt(kv.get("oxygenLevel"), 98));
        if (kv.containsKey("chestPain")) p.setChestPain(Boolean.parseBoolean(kv.get("chestPain")));
        if (kv.containsKey("breathingDifficulty")) p.setBreathingDifficulty(Boolean.parseBoolean(kv.get("breathingDifficulty")));
        if (kv.containsKey("bleeding")) p.setBleeding(Boolean.parseBoolean(kv.get("bleeding")));
        if (kv.containsKey("unconscious")) p.setUnconscious(Boolean.parseBoolean(kv.get("unconscious")));
        if (kv.containsKey("symptoms")) p.setSymptoms(kv.get("symptoms"));
        if (kv.containsKey("ruleScore")) p.setRuleScore(parseDouble(kv.get("ruleScore"), 0.0));
        if (kv.containsKey("pythonScore")) p.setPythonScore(parseDouble(kv.get("pythonScore"), 0.0));
        if (kv.containsKey("severityScore")) p.setSeverityScore(parseDouble(kv.get("severityScore"), 0.0));
        if (kv.containsKey("priorityLevel")) p.setPriorityLevel(kv.get("priorityLevel"));
        if (kv.containsKey("status")) p.setStatus(kv.get("status"));
        if (kv.containsKey("arrivalTimeMillis")) p.setArrivalTimeMillis(parseLong(kv.get("arrivalTimeMillis"), System.currentTimeMillis()));

        return p;
    }

    private static Map<String, String> parseSimpleJsonObject(String objJson) {
        Map<String, String> map = new HashMap<>();
        String inner = objJson.trim();
        if (inner.startsWith("{")) inner = inner.substring(1);
        if (inner.endsWith("}")) inner = inner.substring(0, inner.length() - 1);

        // Simple token scanner supporting quoted strings and numbers/booleans
        int i = 0;
        int len = inner.length();
        while (i < len) {
            // Find key start
            while (i < len && (Character.isWhitespace(inner.charAt(i)) || inner.charAt(i) == ',')) i++;
            if (i >= len) break;

            if (inner.charAt(i) != '"') {
                i++;
                continue;
            }
            i++; // skip quote
            int keyStart = i;
            while (i < len && inner.charAt(i) != '"') i++;
            String key = inner.substring(keyStart, i);
            i++; // skip closing quote

            // Find colon
            while (i < len && inner.charAt(i) != ':') i++;
            i++; // skip colon

            // Find value
            while (i < len && Character.isWhitespace(inner.charAt(i))) i++;
            if (i >= len) break;

            String value = "";
            if (inner.charAt(i) == '"') {
                i++; // skip opening quote
                int valStart = i;
                StringBuilder valSb = new StringBuilder();
                boolean escape = false;
                while (i < len) {
                    char c = inner.charAt(i);
                    if (escape) {
                        valSb.append(c);
                        escape = false;
                    } else if (c == '\\') {
                        escape = true;
                    } else if (c == '"') {
                        break;
                    } else {
                        valSb.append(c);
                    }
                    i++;
                }
                value = valSb.toString();
                i++; // skip closing quote
            } else {
                int valStart = i;
                while (i < len && inner.charAt(i) != ',' && inner.charAt(i) != '}') i++;
                value = inner.substring(valStart, i).trim();
            }

            map.put(key, value);
        }
        return map;
    }

    private static int parseInt(String val, int def) {
        try { return Integer.parseInt(val.trim()); } catch (Exception e) { return def; }
    }

    private static double parseDouble(String val, double def) {
        try { return Double.parseDouble(val.trim()); } catch (Exception e) { return def; }
    }

    private static long parseLong(String val, long def) {
        try { return Long.parseLong(val.trim()); } catch (Exception e) { return def; }
    }

    // -------------------------------------------------------------------------
    // Python Subprocess Orchestration (ProcessBuilder)
    // -------------------------------------------------------------------------

    /**
     * Executes the Python refined severity scoring script using Java's ProcessBuilder.
     * Automatically tests for python / python3 / py binaries.
     *
     * @param pythonScriptPath Absolute or relative path to severity_model.py
     * @param jsonFilePath Path to patients.json
     * @return Execution output log from Python stdout/stderr
     * @throws Exception with descriptive error if python is missing or failed
     */
    public String executePythonScoring(String pythonScriptPath, String jsonFilePath) throws Exception {
        File scriptFile = new File(pythonScriptPath);
        if (!scriptFile.exists()) {
            throw new FileNotFoundException("Python scoring script not found at: " + pythonScriptPath);
        }

        File jsonFile = new File(jsonFilePath);
        if (!jsonFile.exists()) {
            throw new FileNotFoundException("Target JSON file not found at: " + jsonFilePath);
        }

        // Detect available Python command
        String pythonCmd = detectPythonCommand();
        if (pythonCmd == null) {
            throw new IllegalStateException("Python runtime not found on system PATH. Please ensure Python 3 is installed.");
        }

        List<String> command = Arrays.asList(
                pythonCmd,
                scriptFile.getAbsolutePath(),
                jsonFile.getAbsolutePath()
        );

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Python scoring script exited with error code " + exitCode + ":\n" + output);
        }

        return output.toString();
    }

    private static String cachedPythonCmd = null;

    /**
     * Detects working python executable command across Windows / Linux / macOS.
     */
    public static String detectPythonCommand() {
        if (cachedPythonCmd != null) {
            return cachedPythonCmd;
        }
        String[] candidates = {"python", "python3", "py"};
        for (String cmd : candidates) {
            try {
                Process p = new ProcessBuilder(cmd, "--version").start();
                int exit = p.waitFor();
                if (exit == 0) {
                    cachedPythonCmd = cmd;
                    return cmd;
                }
            } catch (Exception ignored) {
                // Try next candidate
            }
        }
        return null;
    }

    /**
     * Executes the complete end-to-end triage analysis cycle:
     * 1. Export current Java state to JSON
     * 2. Execute Python weighted model via ProcessBuilder
     * 3. Read back refined scores from JSON
     * 4. Rebuild the PriorityQueue Max-Heap
     *
     * @return Output log of the analysis
     */
    public synchronized String runFullTriageAnalysis(String jsonFilePath, String pythonScriptPath) throws Exception {
        exportToJson(jsonFilePath);
        String pythonLogs = executePythonScoring(pythonScriptPath, jsonFilePath);
        importFromJson(jsonFilePath);
        rebuildHeap();
        return pythonLogs;
    }

    /**
     * Populates the queue with standard academic sample patients (A, B, C, D, E).
     */
    public synchronized void loadDemoPatients() {
        clear();
        addPatient(new Patient("P101", "Rajesh (Patient A)", 60, 98.8, 110, "145/95", 91,
                true, true, false, false,
                "Severe crushing chest pain radiating to left arm, shortness of breath"));

        addPatient(new Patient("P102", "Swetha (Patient B)", 22, 100.4, 78, "118/75", 98,
                false, false, false, false,
                "Mild fever, sore throat, mild body aches"));

        addPatient(new Patient("P103", "Mahesh (Patient C)", 48, 101.2, 105, "130/85", 86,
                false, true, false, false,
                "Severe wheezing, respiratory distress, oxygen desaturation (SpO2 86%)"));

        addPatient(new Patient("P104", "Mounika (Patient D)", 30, 98.6, 72, "120/80", 99,
                false, false, false, false,
                "Minor superficial right ankle sprain, ambulatory"));

        addPatient(new Patient("P105", "Swamy (Patient E)", 74, 97.4, 138, "85/52", 88,
                false, false, true, true,
                "Trauma fall, unresponsive, acute arterial laceration with severe hemorrhage"));
    }
}
