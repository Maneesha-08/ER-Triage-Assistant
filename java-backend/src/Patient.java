/**
 * =============================================================================
 * ER Triage Assistant - Patient Model
 * =============================================================================
 * Academic/Demo Project: Hospital Emergency Room Triage Management System.
 * DISCLAIMER: Academic Demo Only - Not for clinical use or real medical diagnosis.
 *
 * Course Alignment:
 * - OOPJ (Object-Oriented Programming in Java): Encapsulation, State Management,
 *   Constructors, Accessors, Mutators, and String Representations.
 * =============================================================================
 */
public class Patient {

    // Unique Identifier & Demographic Attributes
    private String patientId;
    private String name;
    private int age;

    // Physiological Vital Signs
    private double temperature;       // in Fahrenheit (e.g., 98.6)
    private int heartRate;            // beats per minute (BPM)
    private String bloodPressure;     // format "Systolic/Diastolic" (e.g., "120/80")
    private int oxygenLevel;          // blood oxygen saturation % (SpO2)

    // Critical Symptom Flags (Binary Inputs for Propositional Logic)
    private boolean chestPain;
    private boolean breathingDifficulty;
    private boolean bleeding;
    private boolean unconscious;
    private String symptoms;          // narrative symptom description

    // Triage Scores and Classifications
    private double ruleScore;         // Discrete Mathematics / Propositional Logic score (0-100)
    private double pythonScore;       // Continuous weighted scoring model output (0-100)
    private double severityScore;     // Final blended severity score used by Max-Heap PriorityQueue
    private String priorityLevel;     // "CRITICAL", "HIGH", "MEDIUM", "LOW"
    private String status;            // "Waiting for Triage", "Triaged", "In Treatment", "Discharged"
    private long arrivalTimeMillis;   // Arrival timestamp for FIFO tie-breaking in heap

    /**
     * Default Constructor
     */
    public Patient() {
        this.arrivalTimeMillis = System.currentTimeMillis();
        this.status = "Waiting for Triage";
        this.priorityLevel = "LOW";
        this.bloodPressure = "120/80";
        this.oxygenLevel = 98;
        this.temperature = 98.6;
        this.heartRate = 75;
    }

    /**
     * Parameterized Constructor with Core Clinical Inputs
     */
    public Patient(String patientId, String name, int age, double temperature, int heartRate,
                   String bloodPressure, int oxygenLevel, boolean chestPain,
                   boolean breathingDifficulty, boolean bleeding, boolean unconscious,
                   String symptoms) {
        this.patientId = patientId;
        this.name = name;
        this.age = age;
        this.temperature = temperature;
        this.heartRate = heartRate;
        this.bloodPressure = (bloodPressure != null && !bloodPressure.trim().isEmpty()) ? bloodPressure : "120/80";
        this.oxygenLevel = oxygenLevel;
        this.chestPain = chestPain;
        this.breathingDifficulty = breathingDifficulty;
        this.bleeding = bleeding;
        this.unconscious = unconscious;
        this.symptoms = (symptoms != null) ? symptoms : "General assessment required";
        this.ruleScore = 0.0;
        this.pythonScore = 0.0;
        this.severityScore = 0.0;
        this.priorityLevel = "LOW";
        this.status = "Waiting for Triage";
        this.arrivalTimeMillis = System.currentTimeMillis();
    }

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(int heartRate) {
        this.heartRate = heartRate;
    }

    public String getBloodPressure() {
        return bloodPressure;
    }

    public void setBloodPressure(String bloodPressure) {
        this.bloodPressure = bloodPressure;
    }

    public int getOxygenLevel() {
        return oxygenLevel;
    }

    public void setOxygenLevel(int oxygenLevel) {
        this.oxygenLevel = oxygenLevel;
    }

    public boolean isChestPain() {
        return chestPain;
    }

    public void setChestPain(boolean chestPain) {
        this.chestPain = chestPain;
    }

    public boolean isBreathingDifficulty() {
        return breathingDifficulty;
    }

    public void setBreathingDifficulty(boolean breathingDifficulty) {
        this.breathingDifficulty = breathingDifficulty;
    }

    public boolean isBleeding() {
        return bleeding;
    }

    public void setBleeding(boolean bleeding) {
        this.bleeding = bleeding;
    }

    public boolean isUnconscious() {
        return unconscious;
    }

    public void setUnconscious(boolean unconscious) {
        this.unconscious = unconscious;
    }

    public String getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(String symptoms) {
        this.symptoms = symptoms;
    }

    public double getRuleScore() {
        return ruleScore;
    }

    public void setRuleScore(double ruleScore) {
        this.ruleScore = Math.round(ruleScore * 10.0) / 10.0;
    }

    public double getPythonScore() {
        return pythonScore;
    }

    public void setPythonScore(double pythonScore) {
        this.pythonScore = Math.round(pythonScore * 10.0) / 10.0;
    }

    public double getSeverityScore() {
        return severityScore;
    }

    public void setSeverityScore(double severityScore) {
        this.severityScore = Math.round(severityScore * 10.0) / 10.0;
    }

    public String getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(String priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getArrivalTimeMillis() {
        return arrivalTimeMillis;
    }

    public void setArrivalTimeMillis(long arrivalTimeMillis) {
        this.arrivalTimeMillis = arrivalTimeMillis;
    }

    // -------------------------------------------------------------------------
    // Utility and Display Methods
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return String.format("[%s] %-18s | Age: %2d | Score: %5.1f | Priority: %-8s | Symptoms: %s",
                patientId, name, age, severityScore, priorityLevel,
                (symptoms != null && symptoms.length() > 25 ? symptoms.substring(0, 22) + "..." : symptoms));
    }

    /**
     * Serializes this Patient object into a clean JSON string representation.
     * Zero-dependency implementation for maximum portability across any JDK.
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("  {\n");
        sb.append("    \"patientId\": \"").append(escapeJson(patientId)).append("\",\n");
        sb.append("    \"name\": \"").append(escapeJson(name)).append("\",\n");
        sb.append("    \"age\": ").append(age).append(",\n");
        sb.append("    \"temperature\": ").append(temperature).append(",\n");
        sb.append("    \"heartRate\": ").append(heartRate).append(",\n");
        sb.append("    \"bloodPressure\": \"").append(escapeJson(bloodPressure)).append("\",\n");
        sb.append("    \"oxygenLevel\": ").append(oxygenLevel).append(",\n");
        sb.append("    \"chestPain\": ").append(chestPain).append(",\n");
        sb.append("    \"breathingDifficulty\": ").append(breathingDifficulty).append(",\n");
        sb.append("    \"bleeding\": ").append(bleeding).append(",\n");
        sb.append("    \"unconscious\": ").append(unconscious).append(",\n");
        sb.append("    \"symptoms\": \"").append(escapeJson(symptoms)).append("\",\n");
        sb.append("    \"ruleScore\": ").append(ruleScore).append(",\n");
        sb.append("    \"pythonScore\": ").append(pythonScore).append(",\n");
        sb.append("    \"severityScore\": ").append(severityScore).append(",\n");
        sb.append("    \"priorityLevel\": \"").append(escapeJson(priorityLevel)).append("\",\n");
        sb.append("    \"status\": \"").append(escapeJson(status)).append("\",\n");
        sb.append("    \"arrivalTimeMillis\": ").append(arrivalTimeMillis).append("\n");
        sb.append("  }");
        return sb.toString();
    }

    /**
     * Escapes special characters for valid JSON output.
     */
    private static String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\b", "\\b")
                    .replace("\f", "\\f")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
}
