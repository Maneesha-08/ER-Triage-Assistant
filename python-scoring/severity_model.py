#!/usr/bin/env python3
"""
=============================================================================
ER Triage Assistant - Refined Python Severity Scoring Model
=============================================================================
ACADEMIC DEMO ONLY - NOT FOR CLINICAL USE OR MEDICAL DIAGNOSIS.
This model implements a weighted multi-factor regression-style scoring function
to refine Emergency Room patient severity scores calculated by the Java DMGT engine.

Course Alignment:
- AI & Data Science: Weighted Feature Scoring, Multi-Criteria Decision Analysis
- Software Engineering: Inter-process communication via serialized JSON

Mathematical Scoring Architecture:
  Base Feature Vector: X = [x_unconscious, x_bleeding, x_chest_pain, x_breathing,
                            x_spo2_deficit, x_hr_anomaly, x_bp_anomaly,
                            x_temp_anomaly, x_age_risk]
  Weights Vector:      W = [w_u, w_b, w_cp, w_bd, w_spo2, w_hr, w_bp, w_temp, w_age]

  Raw Python Score = min(100.0, sum(W_i * X_i))
  Final Severity   = alpha * DMGT_Rule_Score + (1 - alpha) * Raw_Python_Score
  where alpha = 0.35 (DMGT logical weight) and (1 - alpha) = 0.65 (continuous feature weight).
=============================================================================
"""

import sys
import os
import json
from typing import Dict, Any, List, Tuple

# ---------------------------------------------------------------------------
# Transparent Weight Configuration & Academic Rationale
# ---------------------------------------------------------------------------
WEIGHTS = {
    # 1. Unconsciousness (GCS impairment proxy): Immediate life threat
    # Neurological unresponsiveness carries top urgency in emergency triage.
    "unconscious": 38.0,

    # 2. Acute Active Bleeding: Severe risk of hemorrhagic shock
    "bleeding": 28.0,

    # 3. Chest Pain: Potential acute coronary syndrome / myocardial infarction
    "chest_pain": 22.0,

    # 4. Breathing Difficulty: Potential respiratory arrest / hypoxemic distress
    "breathing_difficulty": 22.0,

    # 5. Synergistic Cardiopulmonary Interaction (Chest Pain AND Breathing Difficulty)
    "cardiopulmonary_synergy": 12.0,

    # 6. Hypoxemia Penalty Rate: Baseline normal SpO2 is ~96-100%
    # For every 1% drop below 95%, add penalty points (steep slope below 90%)
    "spo2_below_95_factor": 1.8,
    "spo2_max_penalty": 30.0,

    # 7. Heart Rate Anomaly: Normal resting adult HR is ~60-100 bpm
    # Deviations (tachycardia > 100 or bradycardia < 60) add progressive penalty
    "hr_normal_median": 75.0,
    "hr_deviation_factor": 0.25,
    "hr_max_penalty": 15.0,

    # 8. Blood Pressure Anomaly (Systolic): Normal ~110-125 mmHg
    # Severe hypotension (<90 mmHg = shock) or hypertensive emergency (>170 mmHg)
    "bp_systolic_normal": 120.0,
    "bp_deviation_factor": 0.15,
    "bp_max_penalty": 14.0,

    # 9. Body Temperature Anomaly: Normal ~98.6°F (37°C)
    "temp_normal": 98.6,
    "temp_deviation_factor": 3.0,
    "temp_max_penalty": 12.0,

    # 10. Age Vulnerability: Geriatric (>=65) or pediatric (<=5)
    "age_vulnerable_bonus": 6.0
}

# Fusion Weight between DMGT Propositional Rules and Python Continuous Scoring
# 40% discrete propositional logic rules + 60% multi-factor continuous vitals
ALPHA_RULE_WEIGHT = 0.40
BETA_PYTHON_WEIGHT = 0.60


def parse_systolic_bp(bp_str: Any) -> float:
    """Safely parse systolic blood pressure from strings like '120/80' or numeric values."""
    try:
        if isinstance(bp_str, (int, float)):
            return float(bp_str)
        if isinstance(bp_str, str) and "/" in bp_str:
            return float(bp_str.split("/")[0].strip())
        return float(bp_str)
    except (ValueError, IndexError, AttributeError):
        return 120.0  # fallback to normal baseline systolic


def calculate_feature_score(patient: Dict[str, Any]) -> Tuple[float, Dict[str, float]]:
    """
    Computes explainable weighted severity score based on physiological parameters.
    Returns (raw_score, component_breakdown).
    """
    components: Dict[str, float] = {}

    # Feature 1: Unconscious
    if bool(patient.get("unconscious", False)):
        components["Unconscious Flag"] = WEIGHTS["unconscious"]
    else:
        components["Unconscious Flag"] = 0.0

    # Feature 2: Bleeding
    if bool(patient.get("bleeding", False)):
        components["Severe Bleeding"] = WEIGHTS["bleeding"]
    else:
        components["Severe Bleeding"] = 0.0

    # Feature 3: Chest Pain
    if bool(patient.get("chestPain", False)):
        components["Chest Pain"] = WEIGHTS["chest_pain"]
    else:
        components["Chest Pain"] = 0.0

    # Feature 4: Breathing Difficulty
    if bool(patient.get("breathingDifficulty", False)):
        components["Breathing Difficulty"] = WEIGHTS["breathing_difficulty"]
    else:
        components["Breathing Difficulty"] = 0.0

    # Interaction: Chest Pain AND Breathing Difficulty synergy
    if bool(patient.get("chestPain", False)) and bool(patient.get("breathingDifficulty", False)):
        components["Cardiopulmonary Synergy"] = WEIGHTS["cardiopulmonary_synergy"]
    else:
        components["Cardiopulmonary Synergy"] = 0.0

    # Feature 5: Oxygen Saturation (SpO2 %)
    try:
        spo2 = float(patient.get("oxygenLevel", 98.0))
    except (ValueError, TypeError):
        spo2 = 98.0
    
    if spo2 < 95.0:
        deficit = 95.0 - spo2
        spo2_penalty = min(deficit * WEIGHTS["spo2_below_95_factor"], WEIGHTS["spo2_max_penalty"])
        # Severe penalty booster if cyanotic / critical hypoxia (<88%)
        if spo2 < 88.0:
            spo2_penalty += 8.0
        components["Oxygen Deficit"] = round(min(spo2_penalty, 30.0), 2)
    else:
        components["Oxygen Deficit"] = 0.0

    # Feature 6: Heart Rate (BPM)
    try:
        hr = float(patient.get("heartRate", 75.0))
    except (ValueError, TypeError):
        hr = 75.0
    
    hr_delta = abs(hr - WEIGHTS["hr_normal_median"])
    hr_penalty = min(hr_delta * WEIGHTS["hr_deviation_factor"], WEIGHTS["hr_max_penalty"])
    components["Heart Rate Anomaly"] = round(hr_penalty, 2)

    # Feature 7: Blood Pressure (Systolic)
    systolic = parse_systolic_bp(patient.get("bloodPressure", "120/80"))
    if systolic < 90.0:  # Hypotension / cardiogenic or septic shock danger
        bp_penalty = min((90.0 - systolic) * 0.4 + 10.0, WEIGHTS["bp_max_penalty"])
    elif systolic > 140.0:  # Hypertension
        bp_penalty = min((systolic - 140.0) * WEIGHTS["bp_deviation_factor"], WEIGHTS["bp_max_penalty"])
    else:
        bp_penalty = 0.0
    components["Blood Pressure Anomaly"] = round(bp_penalty, 2)

    # Feature 8: Temperature
    try:
        temp = float(patient.get("temperature", 98.6))
    except (ValueError, TypeError):
        temp = 98.6
    
    # Normalize Celsius to Fahrenheit if entered as Celsius (e.g., 36-41)
    if temp < 45.0:
        temp = (temp * 9.0 / 5.0) + 32.0

    if temp > 100.4:
        temp_penalty = min((temp - 100.4) * WEIGHTS["temp_deviation_factor"], WEIGHTS["temp_max_penalty"])
    elif temp < 96.0:
        temp_penalty = min((96.0 - temp) * 2.5, WEIGHTS["temp_max_penalty"])
    else:
        temp_penalty = 0.0
    components["Temperature Anomaly"] = round(temp_penalty, 2)

    # Feature 9: Age Risk
    try:
        age = int(patient.get("age", 30))
    except (ValueError, TypeError):
        age = 30
    
    if age >= 65 or age <= 5:
        components["Age Vulnerability"] = WEIGHTS["age_vulnerable_bonus"]
    else:
        components["Age Vulnerability"] = 0.0

    raw_score = sum(components.values())
    raw_score = min(100.0, max(0.0, raw_score))
    return round(raw_score, 1), components


def classify_priority(score: float) -> str:
    """Classify numerical score into clinical priority categories (Emergency Severity Index inspired)."""
    if score >= 80.0:
        return "CRITICAL"
    elif score >= 60.0:
        return "HIGH"
    elif score >= 35.0:
        return "MEDIUM"
    else:
        return "LOW"


def process_patients_file(filepath: str) -> bool:
    """Reads JSON, recalculates refined severity scores, and updates the file."""
    if not os.path.exists(filepath):
        print(f"[ERROR] Patients data file not found at: {filepath}", file=sys.stderr)
        return False

    try:
        with open(filepath, "r", encoding="utf-8") as f:
            patients = json.load(f)
    except json.JSONDecodeError as e:
        print(f"[ERROR] Corrupted JSON file: {e}", file=sys.stderr)
        return False
    except Exception as e:
        print(f"[ERROR] Unable to read file: {e}", file=sys.stderr)
        return False

    if not isinstance(patients, list):
        print(f"[ERROR] Expected JSON list of patients, got {type(patients)}", file=sys.stderr)
        return False

    print("\n" + "=" * 78)
    print(" ER TRIAGE ASSISTANT - PYTHON REFINED SCORING ENGINE (ACADEMIC DEMO)")
    print("=" * 78)
    print(f" Processing {len(patients)} patients from: {filepath}\n")
    print(f"{'ID':<6} | {'Name':<22} | {'DMGT Rule':<10} | {'Py Score':<9} | {'Final Score':<11} | {'Priority'}")
    print("-" * 78)

    for patient in patients:
        dmgt_score = float(patient.get("ruleScore", 20.0))
        py_score, _ = calculate_feature_score(patient)

        # Mathematical Fusion of DMGT discrete logic + continuous weighted features
        # If unconscious or massive bleeding, ensure floor of 90+
        if bool(patient.get("unconscious", False)) or (bool(patient.get("bleeding", False)) and float(patient.get("oxygenLevel", 100)) < 90):
            final_score = max(92.0, (ALPHA_RULE_WEIGHT * dmgt_score) + (BETA_PYTHON_WEIGHT * py_score))
        else:
            final_score = (ALPHA_RULE_WEIGHT * dmgt_score) + (BETA_PYTHON_WEIGHT * py_score)
        
        final_score = round(min(100.0, max(5.0, final_score)), 1)
        priority = classify_priority(final_score)

        patient["pythonScore"] = py_score
        patient["severityScore"] = final_score
        patient["priorityLevel"] = priority

        name_display = (patient.get("name", "Unknown")[:20] + "..") if len(patient.get("name", "")) > 20 else patient.get("name", "Unknown")
        print(f"{patient.get('patientId', 'N/A'):<6} | {name_display:<22} | {dmgt_score:<10.1f} | {py_score:<9.1f} | {final_score:<11.1f} | {priority}")

    print("-" * 78)

    try:
        with open(filepath, "w", encoding="utf-8") as f:
            json.dump(patients, f, indent=2)
        print(f"\n[SUCCESS] Successfully written {len(patients)} refined patient scores to: {filepath}\n")
        return True
    except Exception as e:
        print(f"[ERROR] Failed to write updated patients to JSON: {e}", file=sys.stderr)
        return False


def main():
    # Allow overriding file path via command line argument
    if len(sys.argv) > 1:
        target_file = sys.argv[1]
    else:
        # Default relative location
        script_dir = os.path.dirname(os.path.abspath(__file__))
        target_file = os.path.join(script_dir, "..", "data", "patients.json")

    success = process_patients_file(target_file)
    sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()
