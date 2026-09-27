#!/usr/bin/env python3
"""
ER Triage Assistant - Automated End-to-End System Test Suite
Verifies:
1. Java compilation & Max-Heap PriorityQueue
2. DMGT Propositional Logic evaluation
3. Python weighted continuous scoring
4. Inter-process communication via data/patients.json
5. REST API endpoints & Frontend static asset delivery
"""

import subprocess
import time
import urllib.request
import json
import os
import sys

PORT = 8080
BASE_URL = f"http://localhost:{PORT}"
PROJECT_ROOT = os.path.dirname(os.path.abspath(__file__))

def run_test():
    print("=" * 70)
    print("  ER TRIAGE ASSISTANT - RUNNING SYSTEM VERIFICATION TESTS")
    print("=" * 70)

    # Step 1: Verify data/patients.json exists
    json_path = os.path.join(PROJECT_ROOT, "data", "patients.json")
    assert os.path.exists(json_path), f"File missing: {json_path}"
    print("[PASS] data/patients.json exists.")

    # Step 2: Verify Python Scoring script execution
    py_script = os.path.join(PROJECT_ROOT, "python-scoring", "severity_model.py")
    res = subprocess.run([sys.executable, py_script, json_path], capture_output=True, text=True)
    assert res.returncode == 0, f"Python scoring failed: {res.stderr}"
    print("[PASS] Python scoring executed successfully.")

    # Step 3: Launch Java Server in background
    bin_dir = os.path.join(PROJECT_ROOT, "java-backend", "bin")
    java_proc = subprocess.Popen(
        ["java", "-cp", bin_dir, "Main"],
        cwd=PROJECT_ROOT,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
        text=True
    )

    try:
        # Wait for server to bind
        time.sleep(2.5)

        # Step 4: Reset to clean baseline demo patients
        reset_req = urllib.request.Request(f"{BASE_URL}/api/patients/reset", method="POST")
        urllib.request.urlopen(reset_req, timeout=5)

        # Step 5: Test GET /api/status
        req = urllib.request.urlopen(f"{BASE_URL}/api/status", timeout=5)
        status_data = json.loads(req.read().decode("utf-8"))
        print(f"[PASS] GET /api/status -> {status_data}")
        assert status_data.get("status") == "ONLINE"
        assert status_data.get("pythonAvailable") is True

        # Step 6: Test GET /api/patients (Verify Max-Heap descending order)
        req = urllib.request.urlopen(f"{BASE_URL}/api/patients", timeout=5)
        patients_resp = json.loads(req.read().decode("utf-8"))
        patients = patients_resp.get("patients", [])
        print(f"[PASS] GET /api/patients returned {len(patients)} patients.")

        # Verify descending severity order (Max-Heap property)
        scores = [p["severityScore"] for p in patients]
        assert scores == sorted(scores, reverse=True), f"Heap ordering violated: {scores}"
        print(f"[PASS] Max-Heap invariant confirmed: scores strictly descending -> {scores}")

        # Top patient must be Swamy (Patient E - Trauma Unconscious)
        assert patients[0]["patientId"] == "P105", f"Expected P105 at root, got {patients[0]['patientId']}"
        print(f"[PASS] Top urgency patient at Heap Root: {patients[0]['name']} (Score: {patients[0]['severityScore']})")

        # Step 6: Test GET /api/rules/explain?id=P101 (DMGT Logic)
        req = urllib.request.urlopen(f"{BASE_URL}/api/rules/explain?id=P101", timeout=5)
        dmgt_resp = json.loads(req.read().decode("utf-8"))
        print(f"[PASS] GET /api/rules/explain?id=P101 returned explanations:")
        for exp in dmgt_resp.get("explanations", []):
            print(f"       -> {exp}")
        assert len(dmgt_resp.get("explanations", [])) > 0

        # Step 7: Test POST /api/patients (Add new critical patient)
        new_patient = {
            "patientId": "P999",
            "name": "Sarah Connor (Emergency Test)",
            "age": 35,
            "temperature": 103.5,
            "heartRate": 140,
            "bloodPressure": "80/50",
            "oxygenLevel": 82,
            "chestPain": True,
            "breathingDifficulty": True,
            "bleeding": True,
            "unconscious": True,
            "symptoms": "Severe polytrauma, cyanosis, shock"
        }
        post_data = json.dumps(new_patient).encode("utf-8")
        post_req = urllib.request.Request(
            f"{BASE_URL}/api/patients",
            data=post_data,
            headers={"Content-Type": "application/json"},
            method="POST"
        )
        post_resp = urllib.request.urlopen(post_req, timeout=5)
        updated_data = json.loads(post_resp.read().decode("utf-8"))
        updated_patients = updated_data.get("patients", [])
        assert any(p["patientId"] == "P999" for p in updated_patients), "New patient was not added"
        print(f"[PASS] POST /api/patients dynamically inserted P999 into queue. New count: {len(updated_patients)}")

        # Step 8: Test POST /api/triage/analyze (Full pipeline execution)
        analyze_req = urllib.request.Request(f"{BASE_URL}/api/triage/analyze", data=b"{}", method="POST")
        analyze_resp = urllib.request.urlopen(analyze_req, timeout=10)
        analyze_data = json.loads(analyze_resp.read().decode("utf-8"))
        assert analyze_data.get("success") is True
        print("[PASS] POST /api/triage/analyze executed Java -> JSON -> Python -> JSON -> Java pipeline!")

        # Step 9: Test Static Frontend Files
        req_html = urllib.request.urlopen(f"{BASE_URL}/", timeout=5)
        html_content = req_html.read().decode("utf-8")
        assert "<title>ER Triage Assistant" in html_content
        print("[PASS] GET / served frontend/index.html")

        req_css = urllib.request.urlopen(f"{BASE_URL}/style.css", timeout=5)
        assert req_css.status == 200
        print("[PASS] GET /style.css served successfully.")

        req_js = urllib.request.urlopen(f"{BASE_URL}/script.js", timeout=5)
        assert req_js.status == 200
        print("[PASS] GET /script.js served successfully.")

        # Step 10: Clean up test patient P999
        del_req = urllib.request.Request(f"{BASE_URL}/api/patients?id=P999", method="DELETE")
        urllib.request.urlopen(del_req, timeout=5)
        print("[PASS] Cleaned up test patient P999.")

        print("\n" + "=" * 70)
        print(">>> ALL 10 SYSTEM VERIFICATION TESTS PASSED SUCCESSFULLY! <<<")
        print("=" * 70)

    finally:
        java_proc.terminate()
        java_proc.wait(timeout=3)
        print("[INFO] Java test server terminated cleanly.")

if __name__ == "__main__":
    run_test()
