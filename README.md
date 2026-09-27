# ER Triage Assistant

> **Hospital Emergency Room Priority Management System**  
> *An Integrated Academic Software Engineering Demonstration*  
> **DISCLAIMER: Academic Demo Only — Not for Clinical Use, Medical Diagnosis, or Autonomous Triage.**

---

## 1. Problem Statement

Emergency Department (ED/ER) medical staff face high cognitive loads when sorting incoming patients during surges. In conventional triage (such as the Emergency Severity Index - ESI), nurses rapidly evaluate physiological vital signs and chief complaints to prioritize critical patients before stable ones.

Without structured priority management, delays in identifying occult deterioration (e.g., subtle hypoxia, acute coronary syndromes, or internal hemorrhage) can lead to adverse clinical outcomes.

---

## 2. Solution Overview

**ER Triage Assistant** is a full-stack academic demonstration system that demonstrates how four fundamental computer science subjects synthesize into a life-critical decision support tool:

1. **Discrete Mathematics & Graph Theory (DMGT):** Uses formal Propositional Logic ($P, Q, R, S, T$) to evaluate categorical triage rules and baseline clinical severity.
2. **Advanced Data Structures & Algorithms (ADSA):** Implements a **Binary Max-Heap** backed by Java's `PriorityQueue` to maintain an $O(1)$ peek and $O(\log N)$ extraction of the highest-severity patient.
3. **Object-Oriented Programming with Java (OOPJ):** Encapsulates patient physiological state, ensures thread-safety, and coordinates an embedded HTTP microservice with zero third-party dependencies.
4. **Artificial Intelligence & Data Science (AI / Python):** Uses an explainable, continuous multi-factor regression-style scoring engine to refine patient severity based on vital sign deviations.
5. **Software Engineering / IPC:** Bridges Java and Python asynchronously through a shared JSON schema (`data/patients.json`) invoked via Java `ProcessBuilder`.

```
+-----------------------------------------------------------------------------------+
|                                  WORKFLOW PIPELINE                                |
+-----------------------------------------------------------------------------------+
| [Patient Intake Form / API]                                                       |
|        |                                                                          |
|        v                                                                          |
| [Java Patient Entity (OOPJ)]                                                      |
|        |                                                                          |
|        v                                                                          |
| [DMGT Propositional Logic Evaluation (SeverityRules.java)]                         |
|        |                                                                          |
|        v                                                                          |
| [ADSA Max-Heap PriorityQueue (PatientComparator.java)]                            |
|        |                                                                          |
|        v                                                                          |
| [Shared JSON Serialization: data/patients.json]                                   |
|        |                                                                          |
|        v (ProcessBuilder Subprocess)                                              |
| [Python Scoring Engine (python-scoring/severity_model.py)]                        |
|        |                                                                          |
|        v (Writes refined continuous scores)                                       |
| [data/patients.json Updated]                                                      |
|        |                                                                          |
|        v                                                                          |
| [Java Re-imports JSON & Rebuilds Max-Heap O(N)]                                   |
|        |                                                                          |
|        v                                                                          |
| [Real-Time Web Dashboard Updated via REST API]                                    |
+-----------------------------------------------------------------------------------+
```

---

## 3. Technologies Used

| Layer | Technologies | Role |
| :--- | :--- | :--- |
| **Frontend** | Vanilla HTML5, Modern CSS3, JavaScript (ES6+) | Hospital triage dashboard, stats cards, details modal, search/filter |
| **Backend** | Java SE (JDK 17 / 21 / 26) | OOP state, `PriorityQueue`, `com.sun.net.httpserver.HttpServer` |
| **Scoring Engine** | Python 3 | Multi-factor weighted regression formula, JSON processing |
| **Data Exchange** | JSON (`data/patients.json`) | Inter-process communication medium between Java and Python |
| **Build & Run** | Standard JDK tools (`javac`, `java`) | Zero external frameworks or package managers required |

---

## 4. Project Structure

```
ER-Triage-Assistant/
├── frontend/
│   ├── index.html            # Responsive hospital-style dashboard UI
│   ├── style.css             # Clinical design system with severity badges & modals
│   └── script.js             # Controller for API calls, heap rendering, & preset cases
│
├── java-backend/
│   ├── bin/                  # Compiled Java .class bytecode
│   └── src/
│       ├── Patient.java           # OOP entity with getters/setters & JSON serializer
│       ├── SeverityRules.java     # DMGT Propositional Logic engine
│       ├── PatientComparator.java # ADSA Max-Heap comparator (descending score + FIFO)
│       ├── TriageManager.java     # PriorityQueue heap manager & ProcessBuilder bridge
│       └── Main.java              # CLI demo & embedded HTTP server (REST API + static)
│
├── python-scoring/
│   └── severity_model.py     # Weighted continuous regression scoring script
│
├── data/
│   └── patients.json         # Shared JSON persistence file for Java <-> Python exchange
│
├── run.bat                   # 1-click Windows launcher
├── run.sh                    # 1-click Linux / macOS launcher
└── README.md                 # Complete academic documentation
```

---

## 5. College Subject Connections

### A. DMGT — Discrete Mathematics & Graph Theory
* **Topic:** Propositional Logic, Truth Tables, Compound Logical Connectives ($\land, \lor, \neg, \implies$).
* **Implementation (`SeverityRules.java`):**
  * Let atomic propositions be:
    * $P$: Acute chest pain
    * $Q$: Breathing difficulty
    * $R$: Unconscious / neurological unresponsiveness
    * $S$: Severe active bleeding / hemorrhage
    * $T$: Hypoxemia ($\text{SpO}_2 < 90\%$)
    * $U$: Heart rate arrhythmia ($\text{HR} > 120 \lor \text{HR} < 50\text{ bpm}$)
    * $V$: Hyperpyrexia ($\text{Temp} \ge 102.5^\circ\text{F}$)
  * **Inference Rules:**
    $$\text{Rule 1 (Resuscitation)}: R \lor S \implies \text{CRITICAL}\ (\text{Score: } 95-100)$$
    $$\text{Rule 2 (Cardiopulmonary)}: P \land Q \implies \text{HIGH}\ (\text{Score: } 75-85)$$
    $$\text{Rule 3 (Hypoxemic Failure)}: T \land Q \implies \text{HIGH}\ (\text{Score: } 75)$$
    $$\text{Rule 4 (Tachy-Dyspnea)}: (P \lor Q) \land U \implies \text{HIGH}\ (\text{Score: } 68)$$
    $$\text{Rule 5 (Febrile Non-Distress)}: V \land \neg P \land \neg Q \implies \text{MEDIUM}\ (\text{Score: } 40)$$
    $$\text{Rule 6 (Ambulatory Baseline)}: \neg P \land \neg Q \land \neg R \land \neg S \land \neg T \implies \text{LOW}\ (\text{Score: } 15-25)$$

### B. ADSA — Advanced Data Structures & Algorithms
* **Topic:** Binary Heaps, Priority Queues, Comparator Relations, Algorithmic Complexities.
* **Implementation (`PatientComparator.java`, `TriageManager.java`):**
  * Java's `java.util.PriorityQueue` is a **Min-Heap** by default.
  * To maintain the top-priority emergency patient at the root, `PatientComparator` reverses the order:
    $$\text{compare}(p_1, p_2) \implies \text{Double.compare}(p_2.\text{score}, p_1.\text{score})$$
  * **Tie-Breaking (Fairness):** If two patients share the same score, FIFO (First-In, First-Out) arrival timestamp order is preserved:
    $$\text{Long.compare}(p_1.\text{arrivalTimeMillis}, p_2.\text{arrivalTimeMillis})$$
  * **Time Complexity Analysis:**
    * `peekNextPatient()`: $\mathcal{O}(1)$ — constant-time inspection of root.
    * `addPatient()`: $\mathcal{O}(\log N)$ — binary heap sift-up operation.
    * `pollNextPatient()`: $\mathcal{O}(\log N)$ — binary heap sift-down operation.
    * `rebuildHeap()`: $\mathcal{O}(N)$ — linear-time heapify after batch updates.

### C. OOPJ — Object-Oriented Programming in Java
* **Topic:** Encapsulation, State Management, Robust Exception Handling, HTTP Networking.
* **Implementation (`Patient.java`, `Main.java`):**
  * Private fields with validated accessors and mutators.
  * Dependency-free JSON serialization and parsing routines.
  * Thread-safe synchronized methods in `TriageManager`.
  * Embedded HTTP server (`com.sun.net.httpserver.HttpServer`) exposing REST API endpoints (`/api/patients`, `/api/triage/analyze`, `/api/rules/explain`).

### D. AI & Data Science — Weighted Feature Scoring
* **Topic:** Multi-Criteria Decision Analysis (MCDA), Linear Scoring Functions.
* **Implementation (`python-scoring/severity_model.py`):**
  * Computes a continuous physiological risk score:
    $$\text{Python Score} = \min\left(100.0, \sum W_i \cdot X_i\right)$$
  * Weights calibrated for clinical risk:
    * Unconscious ($W_u = 38.0$)
    * Severe Bleeding ($W_b = 28.0$)
    * Chest Pain ($W_c = 22.0$)
    * Breathing Difficulty ($W_d = 22.0$)
    * Cardiopulmonary Interaction Synergy ($W_{cd} = 12.0$)
    * Oxygen Saturation Deficit: $1.8 \times (95 - \text{SpO}_2)$ if $<95\%$
    * Heart Rate Deviation: $0.25 \times |\text{HR} - 75|$
    * Blood Pressure (Systolic) Shock/Hypertension Penalty
    * Temperature Deviation: $3.0 \times (\text{Temp} - 100.4)$
  * **Blended Triage Fusion:**
    $$\text{Final Severity Score} = 0.40 \times \text{RuleScore} + 0.60 \times \text{PythonScore}$$

---

## 6. Java $\leftrightarrow$ Python Communication Flow

1. When the user clicks **"Run Triage Analysis"** or triggers the REST API:
2. Java calls `TriageManager.exportToJson("data/patients.json")`, serializing all patients.
3. Java invokes the Python script using `java.lang.ProcessBuilder`:
   ```java
   List<String> command = Arrays.asList(pythonCmd, scriptFile, jsonFile);
   Process process = new ProcessBuilder(command).start();
   ```
4. Python reads `data/patients.json`, computes the continuous weighted scores, combines them with the DMGT discrete score, updates each patient object, and writes back to `data/patients.json`.
5. Java captures stdout/stderr, waits for exit code 0, and re-imports the updated records with `importFromJson()`.
6. Java triggers `rebuildHeap()`, restoring the Max-Heap invariant in $\mathcal{O}(N)$ time.
7. The web dashboard refreshes dynamically with the updated rankings.

---

## 7. How to Run the Project

### Prerequisites
* **Java JDK:** JDK 17 or higher (`javac` and `java` available on PATH).
* **Python:** Python 3.8+ (`python` or `python3` available on PATH).
* **Web Browser:** Any modern browser (Chrome, Edge, Firefox, Safari).

### Quick Start (Windows)
Double-click `run.bat` or run in PowerShell:
```cmd
cd ER-Triage-Assistant
run.bat
```

### Quick Start (Linux / macOS)
```bash
cd ER-Triage-Assistant
chmod +x run.sh
./run.sh
```

### Manual Compilation & Execution
From the `ER-Triage-Assistant` directory:
```bash
# 1. Compile Java classes
javac -d java-backend/bin java-backend/src/*.java

# 2. Run Main
java -cp java-backend/bin Main
```

Open your browser and navigate to:
```
http://localhost:8080
```

---

## 8. Sample Output & Test Cases

### Pre-loaded Clinical Demo Cases
* **Patient E (Swamy, Age 74):** High-impact trauma fall, unresponsive ($R$), severe arterial laceration ($S$).  
  $\implies$ **Rank #1 | Score: 98.0 | Priority: CRITICAL (Max-Heap Root)**
* **Patient A (Rajesh, Age 60):** Crushing retrosternal chest pain ($P$), acute dyspnea ($Q$), SpO2 91%.  
  $\implies$ **Rank #2 | Score: 73.6 | Priority: HIGH**
* **Patient C (Mahesh, Age 48):** Severe wheezing, acute dyspnea ($Q$), severe hypoxia SpO2 86% ($T$).  
  $\implies$ **Rank #3 | Score: 63.7 | Priority: HIGH**
* **Patient B (Swetha, Age 22):** Mild fever (100.4°F), sore throat, ambulatory.  
  $\implies$ **Rank #4 | Score: 10.5 | Priority: LOW**
* **Patient D (Mounika, Age 30):** Minor superficial right ankle sprain, ambulatory.  
  $\implies$ **Rank #5 | Score: 6.5 | Priority: LOW**

### Console Output
```text
==============================================================================
                   ER TRIAGE ASSISTANT - CORE BACKEND                         
       Hospital Emergency Room Priority Management System (Academic Demo)     
==============================================================================
[CONFIG] Project Root:    C:\Users\...\ER-Triage-Assistant
[CONFIG] Data JSON File:  C:\Users\...\ER-Triage-Assistant\data\patients.json
[CONFIG] Python Script:   C:\Users\...\ER-Triage-Assistant\python-scoring\severity_model.py

>>> INITIAL MAX-HEAP QUEUE (DMGT Propositional Rules Only) <<<
Rank  | ID     | Patient Name             | Age  | RuleScore | PyScore  | FinalScore
------------------------------------------------------------------------------
#1    | P105   | Swamy (Patient E)        | 74   | 95.0      | 0.0      | 95.0     (CRITICAL)
#2    | P101   | Rajesh (Patient A)       | 60   | 75.0      | 0.0      | 75.0     (HIGH)
#3    | P103   | Mahesh (Patient C)       | 48   | 75.0      | 0.0      | 75.0     (HIGH)
#4    | P102   | Swetha (Patient B)       | 22   | 25.0      | 0.0      | 25.0     (LOW)
#5    | P104   | Mounika (Patient D)      | 30   | 15.0      | 0.0      | 15.0     (LOW)
------------------------------------------------------------------------------

==============================================================================
>>> ER TRIAGE ASSISTANT WEB DASHBOARD ACTIVE <<<
  Local URL:  http://localhost:8080
  REST API:   http://localhost:8080/api/patients
==============================================================================
[INFO] Python runtime detected: 'python'. Ready for triage analysis.
```

---

## 9. Limitations & Clinical Disclaimer

1. **Academic Demonstration Only:** The weights, propositional logic thresholds, and mathematical formulas are strictly illustrative for teaching Computer Science data structures and systems integration.
2. **Not Clinically Validated:** This project has not undergone clinical validation, medical trials, or regulatory approval (FDA, CE, or CDSCO).
3. **No Autonomous Decision-Making:** In any real-world healthcare environment, triage decisions must only be rendered by licensed healthcare professionals.
