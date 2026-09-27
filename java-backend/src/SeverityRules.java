import java.util.ArrayList;
import java.util.List;

/**
 * =============================================================================
 * ER Triage Assistant - SeverityRules Engine (DMGT Module)
 * =============================================================================
 * Academic/Demo Project: Hospital Emergency Room Triage Management System.
 * DISCLAIMER: Academic Demo Only - Not for clinical use or real medical diagnosis.
 *
 * Course Alignment:
 * - DMGT (Discrete Mathematics & Graph Theory): Propositional Logic, Truth Values,
 *   Compound Propositions, Logical Connectives (AND ^, OR v, NOT ~, IMPLIES ->).
 *
 * Formal Logical Foundations:
 * Let the atomic propositional variables be:
 *   P = Patient has acute chest pain
 *   Q = Patient has breathing difficulty
 *   R = Patient is unconscious (unresponsive)
 *   S = Patient has severe external/internal bleeding
 *   T = Patient is hypoxic (oxygen saturation SpO2 < 90%)
 *   U = Severe heart rate anomaly (HR > 120 or HR < 50 bpm)
 *   V = High fever (temperature >= 102.5°F)
 *   W = Vulnerable demographic age factor (Age >= 65 or Age <= 5)
 *
 * Logical Inference Rules:
 *   Rule 1 (Resuscitation / Immediate):   R v S           -> Severity: CRITICAL  (Base Score: 95)
 *   Rule 2 (Cardiopulmonary Distress):    P ^ Q           -> Severity: HIGH      (Base Score: 75)
 *   Rule 3 (Hypoxemic Respiratory Fail):  T ^ Q           -> Severity: HIGH      (Base Score: 75)
 *   Rule 4 (Tachy-Dyspneic/Cardio Event): (P v Q) ^ U     -> Severity: HIGH      (Base Score: 68)
 *   Rule 5 (Febrile Non-Distress):        V ^ ~P ^ ~Q     -> Severity: MEDIUM    (Base Score: 40)
 *   Rule 6 (Mild Ambulation Baseline):    ~P ^ ~Q ^ ~R ^ ~S ^ ~T -> Severity: LOW (Base Score: 15-25)
 * =============================================================================
 */
public class SeverityRules {

    public static final String CRITICAL = "CRITICAL";
    public static final String HIGH = "HIGH";
    public static final String MEDIUM = "MEDIUM";
    public static final String LOW = "LOW";

    /**
     * Evaluates propositional logic rules on the patient, updates ruleScore,
     * severityScore, and priorityLevel.
     *
     * @param patient The patient to evaluate
     * @return Calculated rule-based score (0 - 100)
     */
    public static double evaluate(Patient patient) {
        if (patient == null) return 0.0;

        // 1. Atomic Proposition Evaluation (Truth assignment in Boolean domain)
        boolean P = patient.isChestPain();
        boolean Q = patient.isBreathingDifficulty();
        boolean R = patient.isUnconscious();
        boolean S = patient.isBleeding();
        boolean T = patient.getOxygenLevel() < 90;
        boolean U = (patient.getHeartRate() > 120 || patient.getHeartRate() < 50);
        boolean V = patient.getTemperature() >= 102.0;
        boolean W = (patient.getAge() >= 65 || patient.getAge() <= 5);

        double score = 15.0; // Baseline low severity
        String priority = LOW;

        // Rule 1: R v S (Unconscious OR Severe Bleeding) -> CRITICAL
        if (R || S) {
            score = 95.0;
            priority = CRITICAL;
            if (R && S) {
                score = 100.0; // Both emergency conditions active
            }
        }
        // Rule 2: P ^ Q (Chest Pain AND Breathing Difficulty) -> HIGH
        else if (P && Q) {
            score = 75.0;
            priority = HIGH;
            if (T) score += 10.0; // Compounded by hypoxia
        }
        // Rule 3: T ^ Q (Hypoxia AND Breathing Difficulty) -> HIGH
        else if (T && Q) {
            score = 75.0;
            priority = HIGH;
        }
        // Rule 4: (P v Q) ^ U (Cardiopulmonary symptom with severe tachycardia/bradycardia) -> HIGH
        else if ((P || Q) && U) {
            score = 68.0;
            priority = HIGH;
        }
        // Isolated Single Serious Symptom: P or Q or T
        else if (P || Q || T) {
            score = 55.0;
            priority = MEDIUM;
        }
        // Rule 5: V ^ ~P ^ ~Q (High Fever without Cardiopulmonary Distress) -> MEDIUM
        else if (V && !P && !Q) {
            score = 40.0;
            priority = MEDIUM;
        }
        // Rule 6: (~P ^ ~Q ^ ~R ^ ~S ^ ~T) (Mild/Non-urgent symptoms) -> LOW
        else {
            if (patient.getTemperature() > 99.5) {
                score = 25.0;
                priority = LOW;
            } else {
                score = 15.0;
                priority = LOW;
            }
        }

        // Demographic vulnerability adjustment (Age predicate W)
        if (W && score < 90.0) {
            score += 5.0;
        }

        // Clamp to valid range [0.0, 100.0]
        score = Math.min(100.0, Math.max(0.0, score));

        // Re-classify priority based on final score boundaries
        if (score >= 85.0) {
            priority = CRITICAL;
        } else if (score >= 65.0) {
            priority = HIGH;
        } else if (score >= 35.0) {
            priority = MEDIUM;
        } else {
            priority = LOW;
        }

        patient.setRuleScore(score);
        // Initially before Python runs, severityScore = ruleScore
        if (patient.getSeverityScore() == 0.0) {
            patient.setSeverityScore(score);
        }
        patient.setPriorityLevel(priority);

        return score;
    }

    /**
     * Generates a step-by-step DMGT Propositional Logic breakdown for academic presentation.
     */
    public static List<String> explainLogic(Patient patient) {
        List<String> explanations = new ArrayList<>();
        if (patient == null) return explanations;

        boolean P = patient.isChestPain();
        boolean Q = patient.isBreathingDifficulty();
        boolean R = patient.isUnconscious();
        boolean S = patient.isBleeding();
        boolean T = patient.getOxygenLevel() < 90;
        boolean U = (patient.getHeartRate() > 120 || patient.getHeartRate() < 50);
        boolean V = patient.getTemperature() >= 102.0;

        explanations.add(String.format("Atomic Propositions: P(ChestPain)=%b, Q(BreathingDiff)=%b, R(Unconscious)=%b, S(Bleeding)=%b, T(SpO2<90)=%b",
                P, Q, R, S, T));

        if (R || S) {
            explanations.add(String.format("DMGT Rule 1 Fired: (R v S) <=> (%b v %b) <=> TRUE  ==> Priority = CRITICAL (Score: 95+)", R, S));
        } else if (P && Q) {
            explanations.add(String.format("DMGT Rule 2 Fired: (P ^ Q) <=> (%b ^ %b) <=> TRUE  ==> Priority = HIGH (Score: 75+)", P, Q));
        } else if (T && Q) {
            explanations.add(String.format("DMGT Rule 3 Fired: (T ^ Q) <=> (%b ^ %b) <=> TRUE  ==> Priority = HIGH (Score: 75+)", T, Q));
        } else if ((P || Q) && U) {
            explanations.add(String.format("DMGT Rule 4 Fired: ((P v Q) ^ U) <=> TRUE  ==> Priority = HIGH (Score: 68+)"));
        } else if (P || Q || T) {
            explanations.add("DMGT Sub-Rule Fired: Isolated single major symptom (P v Q v T) <=> TRUE  ==> Priority = MEDIUM (Score: 55)");
        } else if (V && !P && !Q) {
            explanations.add("DMGT Rule 5 Fired: (V ^ ~P ^ ~Q) <=> TRUE (High fever without cardiopulmonary signs) ==> Priority = MEDIUM (Score: 40)");
        } else {
            explanations.add("DMGT Rule 6 Fired: (~P ^ ~Q ^ ~R ^ ~S ^ ~T) <=> TRUE (Mild/Minor condition) ==> Priority = LOW (Score: 15-25)");
        }

        return explanations;
    }
}
