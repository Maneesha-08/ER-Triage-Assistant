/**
 * =============================================================================
 * ER Triage Assistant - Frontend Controller (script.js)
 * =============================================================================
 * Academic/Demo Project: Hospital Emergency Room Triage Management System.
 * DISCLAIMER: Academic Demo Only - Not for clinical use or real medical diagnosis.
 *
 * Coordinates:
 * - REST API interactions with Java embedded backend.
 * - Dynamic UI rendering of the Max-Heap PriorityQueue.
 * - Propositional Logic (DMGT) breakdown inspections.
 * - End-to-end Python scoring pipeline triggers.
 * =============================================================================
 */

// Determine API Base URL:
// When served via Java HttpServer, window.location.origin is 'http://localhost:8080'.
// When opened directly via file://, fallback to 'http://localhost:8080'.
const API_BASE = 'https://er-triage-assistant.onrender.com';

// Global State
let patientsData = [];
let activeFilter = 'ALL';
let searchQuery = '';

// DOM Elements
const triageTableBody = document.getElementById('triageTableBody');
const searchInput = document.getElementById('searchInput');
const filterPriority = document.getElementById('filterPriority');
const btnRunAnalysis = document.getElementById('btnRunAnalysis');
const btnOpenAddModal = document.getElementById('btnOpenAddModal');
const btnResetDemo = document.getElementById('btnResetDemo');
const btnRefresh = document.getElementById('btnRefresh');

const addPatientModal = document.getElementById('addPatientModal');
const btnCloseModal = document.getElementById('btnCloseModal');
const btnCancelAdd = document.getElementById('btnCancelAdd');
const addPatientForm = document.getElementById('addPatientForm');

const detailsModal = document.getElementById('detailsModal');
const btnCloseDetails = document.getElementById('btnCloseDetails');
const btnCloseDetailsBottom = document.getElementById('btnCloseDetailsBottom');
const detailsBody = document.getElementById('detailsBody');

const statusNotification = document.getElementById('statusNotification');
const statusMessage = document.getElementById('statusMessage');
const statusSpinner = document.getElementById('statusSpinner');
const systemStatusPill = document.getElementById('systemStatusPill');
const systemStatusText = document.getElementById('systemStatusText');

// KPI Counter Elements
const statTotal = document.getElementById('statTotalPatients');
const statCritical = document.getElementById('statCriticalPatients');
const statHigh = document.getElementById('statHighPatients');
const statMedium = document.getElementById('statMediumPatients');
const statLow = document.getElementById('statLowPatients');

// Preset Sample Patient Definitions (Academic Clinical Cases)
const PRESETS = {
  A: {
    name: 'Rajesh (Patient A)',
    age: 60,
    temperature: 98.8,
    heartRate: 110,
    bloodPressure: '145/95',
    oxygenLevel: 91,
    chestPain: true,
    breathingDifficulty: true,
    bleeding: false,
    unconscious: false,
    symptoms: 'Crushing retrosternal chest pain radiating to left arm, acute dyspnea'
  },
  B: {
    name: 'Swetha (Patient B)',
    age: 22,
    temperature: 100.4,
    heartRate: 78,
    bloodPressure: '118/75',
    oxygenLevel: 98,
    chestPain: false,
    breathingDifficulty: false,
    bleeding: false,
    unconscious: false,
    symptoms: 'Mild fever, sore throat, mild body aches, ambulatory'
  },
  C: {
    name: 'Mahesh (Patient C)',
    age: 48,
    temperature: 101.2,
    heartRate: 105,
    bloodPressure: '130/85',
    oxygenLevel: 86,
    chestPain: false,
    breathingDifficulty: true,
    bleeding: false,
    unconscious: false,
    symptoms: 'Severe wheezing, respiratory distress, acute oxygen desaturation (SpO2 86%)'
  },
  D: {
    name: 'Mounika (Patient D)',
    age: 30,
    temperature: 98.6,
    heartRate: 72,
    bloodPressure: '120/80',
    oxygenLevel: 99,
    chestPain: false,
    breathingDifficulty: false,
    bleeding: false,
    unconscious: false,
    symptoms: 'Minor superficial right ankle sprain, ambulatory without assistance'
  },
  E: {
    name: 'Swamy (Patient E)',
    age: 74,
    temperature: 97.4,
    heartRate: 138,
    bloodPressure: '85/52',
    oxygenLevel: 88,
    chestPain: false,
    breathingDifficulty: false,
    bleeding: true,
    unconscious: true,
    symptoms: 'High-impact trauma fall, unresponsive, acute arterial laceration with severe hemorrhage'
  }
};

// -----------------------------------------------------------------------------
// Initialization
// -----------------------------------------------------------------------------
document.addEventListener('DOMContentLoaded', () => {
  setupEventListeners();
  checkSystemStatus();
  fetchPatientsQueue();
});

function setupEventListeners() {
  // Toolbar Actions
  btnRunAnalysis.addEventListener('click', handleRunTriageAnalysis);
  btnResetDemo.addEventListener('click', handleResetDemoData);
  btnRefresh.addEventListener('click', () => {
    showNotification('Refreshing queue from Java backend...', false);
    fetchPatientsQueue();
  });

  // Search & Filter
  searchInput.addEventListener('input', (e) => {
    searchQuery = e.target.value.toLowerCase().trim();
    renderQueueTable();
  });

  filterPriority.addEventListener('change', (e) => {
    activeFilter = e.target.value;
    renderQueueTable();
  });

  // Add Patient Modal Handlers
  btnOpenAddModal.addEventListener('click', () => {
    // Generate next sequential ID
    const nextId = 'P' + (100 + patientsData.length + 1);
    document.getElementById('formPatientId').value = nextId;
    addPatientModal.classList.remove('hidden');
  });

  btnCloseModal.addEventListener('click', () => addPatientModal.classList.add('hidden'));
  btnCancelAdd.addEventListener('click', () => addPatientModal.classList.add('hidden'));

  // Preset Buttons
  document.querySelectorAll('.btn-preset').forEach(btn => {
    btn.addEventListener('click', (e) => {
      const presetKey = e.target.getAttribute('data-preset');
      loadPresetIntoForm(presetKey);
    });
  });

  // Form Submit
  addPatientForm.addEventListener('submit', handleAddPatientSubmit);

  // Details Modal Handlers
  btnCloseDetails.addEventListener('click', () => detailsModal.classList.add('hidden'));
  btnCloseDetailsBottom.addEventListener('click', () => detailsModal.classList.add('hidden'));

  // Close modals when clicking backdrop
  window.addEventListener('click', (e) => {
    if (e.target === addPatientModal) addPatientModal.classList.add('hidden');
    if (e.target === detailsModal) detailsModal.classList.add('hidden');
  });
}

// -----------------------------------------------------------------------------
// API Communications
// -----------------------------------------------------------------------------

/**
 * Checks Java Backend & Python engine status
 */
async function checkSystemStatus() {
  try {
    const res = await fetch(`${API_BASE}/api/status`);
    if (res.ok) {
      const data = await res.json();
      systemStatusPill.innerHTML = `
        <span class="status-dot online"></span>
        <span>Java Max-Heap Online &bull; Python: ${data.pythonAvailable ? 'Ready' : 'Not Detected'}</span>
      `;
    }
  } catch (err) {
    systemStatusPill.innerHTML = `
      <span class="status-dot offline"></span>
      <span>Backend Offline (Check port 8080)</span>
    `;
  }
}

/**
 * Fetches the ranked patient queue from the Java backend
 */
async function fetchPatientsQueue() {
  try {
    const res = await fetch(`${API_BASE}/api/patients`);
    if (!res.ok) throw new Error(`HTTP Error: ${res.status}`);
    const data = await res.json();

    patientsData = data.patients || [];
    updateStatistics(data.stats);
    renderQueueTable();
  } catch (error) {
    console.error('Failed to fetch patient queue:', error);
    triageTableBody.innerHTML = `
      <tr>
        <td colspan="9" style="text-align: center; color: #dc2626; padding: 2rem;">
          <strong>Error connecting to Java backend.</strong><br>
          Please make sure the Java server is running (e.g. <code>java -cp bin Main</code>).
        </td>
      </tr>
    `;
  }
}

/**
 * Executes the complete pipeline: Java -> JSON -> Python -> JSON -> Java reheapify
 */
async function handleRunTriageAnalysis() {
  btnRunAnalysis.disabled = true;
  showNotification('Analyzing patient data: invoking Python regression model...', true);

  try {
    const res = await fetch(`${API_BASE}/api/triage/analyze`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    });

    const data = await res.json();
    if (data.success) {
      patientsData = data.patients || [];
      updateStatistics();
      renderQueueTable();

      showNotification('Triage ranking updated successfully! Max-Heap reordered.', false, 'success');
      setTimeout(hideNotification, 3500);
    } else {
      showNotification(`Analysis Failed: ${data.error}`, false, 'error');
    }
  } catch (error) {
    showNotification(`Network Error: ${error.message}`, false, 'error');
  } finally {
    btnRunAnalysis.disabled = false;
  }
}

/**
 * Submits a new patient into the Java PriorityQueue
 */
async function handleAddPatientSubmit(e) {
  e.preventDefault();

  const newPatient = {
    patientId: document.getElementById('formPatientId').value.trim(),
    name: document.getElementById('formName').value.trim(),
    age: parseInt(document.getElementById('formAge').value, 10),
    temperature: parseFloat(document.getElementById('formTemperature').value),
    heartRate: parseInt(document.getElementById('formHeartRate').value, 10),
    bloodPressure: document.getElementById('formBloodPressure').value.trim(),
    oxygenLevel: parseInt(document.getElementById('formOxygenLevel').value, 10),
    chestPain: document.getElementById('formChestPain').checked,
    breathingDifficulty: document.getElementById('formBreathingDifficulty').checked,
    bleeding: document.getElementById('formBleeding').checked,
    unconscious: document.getElementById('formUnconscious').checked,
    symptoms: document.getElementById('formSymptoms').value.trim(),
    status: 'Waiting for Triage'
  };

  try {
    const res = await fetch(`${API_BASE}/api/patients`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(newPatient)
    });

    if (!res.ok) throw new Error('Failed to add patient');
    const data = await res.json();

    patientsData = data.patients || [];
    updateStatistics(data.stats);
    renderQueueTable();

    addPatientModal.classList.add('hidden');
    addPatientForm.reset();

    showNotification(`Patient ${newPatient.name} added to Max-Heap!`, false, 'success');
    setTimeout(hideNotification, 3000);
  } catch (err) {
    alert('Error saving patient: ' + err.message);
  }
}

/**
 * Resets queue back to default demo patients
 */
async function handleResetDemoData() {
  if (!confirm('Reset triage queue to standard academic demo patients (A, B, C, D, E)?')) return;

  showNotification('Resetting to standard demo patients...', true);
  try {
    const res = await fetch(`${API_BASE}/api/patients/reset`, { method: 'POST' });
    const data = await res.json();
    patientsData = data.patients || [];
    updateStatistics(data.stats);
    renderQueueTable();
    showNotification('Sample demo patients loaded into Max-Heap.', false, 'success');
    setTimeout(hideNotification, 2500);
  } catch (err) {
    showNotification('Reset failed: ' + err.message, false, 'error');
  }
}

/**
 * Discharges a patient from the queue
 */
async function dischargePatient(id, name) {
  if (!confirm(`Discharge / Admit patient ${name} (${id}) from the triage queue?`)) return;

  try {
    const res = await fetch(`${API_BASE}/api/patients?id=${encodeURIComponent(id)}`, {
      method: 'DELETE'
    });
    if (res.ok) {
      showNotification(`Patient ${name} discharged from queue.`, false, 'success');
      fetchPatientsQueue();
      setTimeout(hideNotification, 2500);
    }
  } catch (err) {
    alert('Error discharging: ' + err.message);
  }
}

// -----------------------------------------------------------------------------
// UI Rendering Functions
// -----------------------------------------------------------------------------

function renderQueueTable() {
  // Apply Search & Priority Filters
  let filtered = patientsData.filter(p => {
    const matchesFilter = (activeFilter === 'ALL') || (p.priorityLevel === activeFilter);
    const matchesSearch = !searchQuery ||
      (p.name && p.name.toLowerCase().includes(searchQuery)) ||
      (p.patientId && p.patientId.toLowerCase().includes(searchQuery)) ||
      (p.symptoms && p.symptoms.toLowerCase().includes(searchQuery));
    return matchesFilter && matchesSearch;
  });

  if (filtered.length === 0) {
    triageTableBody.innerHTML = `
      <tr>
        <td colspan="9" style="text-align: center; color: var(--text-muted); padding: 2.5rem;">
          No patients match the current search or filter criteria.
        </td>
      </tr>
    `;
    return;
  }

  let html = '';
  filtered.forEach((patient, index) => {
    const isRoot = (index === 0 && activeFilter === 'ALL' && !searchQuery);
    const priorityClass = getPriorityBadgeClass(patient.priorityLevel);

    html += `
      <tr class="${isRoot ? 'heap-root-row' : ''}">
        <td>
          <span class="rank-badge ${isRoot ? 'rank-root' : ''}" title="${isRoot ? 'Max-Heap Root: Top Urgency' : 'Heap Rank'}">
            ${isRoot ? '★ 1' : '#' + (index + 1)}
          </span>
        </td>
        <td>
          <div class="patient-info">
            <span class="patient-name" onclick="viewPatientBreakdown('${patient.patientId}')" title="Click to view DMGT & Python breakdown">
              ${escapeHtml(patient.name)}
            </span>
            <span class="patient-id">${escapeHtml(patient.patientId)}</span>
          </div>
        </td>
        <td>${patient.age}</td>
        <td>
          <div class="symptoms-snippet" title="${escapeHtml(patient.symptoms)}">
            ${escapeHtml(patient.symptoms)}
          </div>
        </td>
        <td class="score-display">${(patient.ruleScore || 0).toFixed(1)}</td>
        <td class="score-display">${(patient.pythonScore || 0).toFixed(1)}</td>
        <td class="score-display score-final">
          <strong>${(patient.severityScore || 0).toFixed(1)}</strong>
        </td>
        <td>
          <span class="badge-priority ${priorityClass}">
            ${patient.priorityLevel === 'CRITICAL' ? '<span class="pulsing-dot"></span>' : ''}
            ${patient.priorityLevel}
          </span>
        </td>
        <td>
          <div class="action-btns">
            <button class="btn btn-outline btn-sm" onclick="viewPatientBreakdown('${patient.patientId}')" title="View DMGT Logic & Python Scoring Breakdown">
              Details
            </button>
            <button class="btn btn-outline btn-sm" style="color: #dc2626;" onclick="dischargePatient('${patient.patientId}', '${escapeHtml(patient.name)}')" title="Discharge from queue">
              &times;
            </button>
          </div>
        </td>
      </tr>
    `;
  });

  triageTableBody.innerHTML = html;
}

function updateStatistics(stats) {
  if (!stats) {
    // Compute from patientsData
    let critical = 0, high = 0, medium = 0, low = 0;
    patientsData.forEach(p => {
      if (p.priorityLevel === 'CRITICAL') critical++;
      else if (p.priorityLevel === 'HIGH') high++;
      else if (p.priorityLevel === 'MEDIUM') medium++;
      else low++;
    });
    stats = { total: patientsData.length, critical, high, medium, low };
  }

  statTotal.textContent = stats.total;
  statCritical.textContent = stats.critical;
  statHigh.textContent = stats.high;
  statMedium.textContent = stats.medium;
  statLow.textContent = stats.low;
}

function getPriorityBadgeClass(priority) {
  switch ((priority || '').toUpperCase()) {
    case 'CRITICAL': return 'priority-critical';
    case 'HIGH': return 'priority-high';
    case 'MEDIUM': return 'priority-medium';
    case 'LOW':
    default: return 'priority-low';
  }
}

/**
 * Opens detailed DMGT Propositional Logic and Python scoring breakdown modal
 */
async function viewPatientBreakdown(patientId) {
  const patient = patientsData.find(p => p.patientId === patientId);
  if (!patient) return;

  document.getElementById('detailsTitle').textContent = `${patient.name} (${patient.patientId})`;

  // Fetch logic explanation from Java backend
  let explanations = [];
  try {
    const res = await fetch(`${API_BASE}/api/rules/explain?id=${encodeURIComponent(patientId)}`);
    if (res.ok) {
      const data = await res.json();
      explanations = data.explanations || [];
    }
  } catch (e) {
    console.warn('Could not fetch explanation:', e);
  }

  const priorityClass = getPriorityBadgeClass(patient.priorityLevel);
  const queueRank = patientsData.findIndex(p => p.patientId === patientId) + 1;

  detailsBody.innerHTML = `
    <!-- Top Summary Banner -->
    <div style="display: flex; justify-content: space-between; align-items: center; background: #f8fafc; padding: 1rem; border-radius: 8px; border: 1px solid var(--border-color);">
      <div>
        <span style="font-size: 0.75rem; color: var(--text-muted); text-transform: uppercase; font-weight: 700;">Queue Heap Position</span>
        <div style="font-size: 1.4rem; font-weight: 800; font-family: var(--font-mono);">Rank #${queueRank} of ${patientsData.length}</div>
      </div>
      <div>
        <span class="badge-priority ${priorityClass}" style="font-size: 0.9rem; padding: 0.4rem 0.85rem;">
          ${patient.priorityLevel}
        </span>
      </div>
    </div>

    <!-- Vital Signs -->
    <div>
      <h3 style="font-size: 0.85rem; font-weight: 700; color: var(--text-muted); text-transform: uppercase; margin-bottom: 0.5rem;">
        Physiological Vital Signs
      </h3>
      <div class="vitals-grid">
        <div class="vital-box">
          <div class="vital-label">Heart Rate</div>
          <div class="vital-value">${patient.heartRate} <span style="font-size: 0.75rem;">BPM</span></div>
        </div>
        <div class="vital-box">
          <div class="vital-label">Blood Pressure</div>
          <div class="vital-value">${patient.bloodPressure}</div>
        </div>
        <div class="vital-box">
          <div class="vital-label">Oxygen SpO2</div>
          <div class="vital-value" style="color: ${patient.oxygenLevel < 90 ? '#dc2626' : 'inherit'};">
            ${patient.oxygenLevel}%
          </div>
        </div>
        <div class="vital-box">
          <div class="vital-label">Temperature</div>
          <div class="vital-value">${patient.temperature}°F</div>
        </div>
      </div>
    </div>

    <!-- Narrative Presentation -->
    <div>
      <h3 style="font-size: 0.85rem; font-weight: 700; color: var(--text-muted); text-transform: uppercase; margin-bottom: 0.4rem;">
        Clinical Presentation & Symptoms
      </h3>
      <p style="background: #f8fafc; padding: 0.75rem; border-radius: 6px; border: 1px solid var(--border-color); font-size: 0.875rem;">
        ${escapeHtml(patient.symptoms)}
      </p>
    </div>

    <!-- DMGT Propositional Logic Panel -->
    <div class="academic-panel panel-dmgt">
      <h4>
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
          <polyline points="14 2 14 8 20 8"></polyline>
          <line x1="16" y1="13" x2="8" y2="13"></line>
          <line x1="16" y1="17" x2="8" y2="17"></line>
        </svg>
        DMGT Module: Propositional Logic Rule Evaluation (Discrete Mathematics)
      </h4>
      <p style="font-size: 0.75rem; color: #166534; margin-bottom: 0.5rem;">
        Proposition Assignment: P = ${patient.chestPain}, Q = ${patient.breathingDifficulty}, R = ${patient.unconscious}, S = ${patient.bleeding}, T = ${patient.oxygenLevel < 90}
      </p>
      <ul class="rule-list">
        ${explanations.map(exp => `<li>${escapeHtml(exp)}</li>`).join('')}
      </ul>
      <div style="margin-top: 0.5rem; font-weight: 700; font-size: 0.85rem; color: #166534;">
        Calculated DMGT Rule Score: ${patient.ruleScore.toFixed(1)} / 100.0
      </div>
    </div>

    <!-- Python Regression Panel -->
    <div class="academic-panel panel-python">
      <h4>
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"></circle>
          <polyline points="12 6 12 12 14 14"></polyline>
        </svg>
        AI / Python Module: Continuous Weighted Severity Model
      </h4>
      <p style="font-size: 0.75rem; color: #075985; margin-bottom: 0.5rem;">
        Applies multi-factor regression weights for continuous SpO2 deficit, heart rate deviation, and systolic blood pressure.
      </p>
      <div style="background: rgba(255,255,255,0.7); padding: 0.6rem; border-radius: 6px; font-family: var(--font-mono); font-size: 0.8rem;">
        Python Continuous Score: <strong>${(patient.pythonScore || 0).toFixed(1)}</strong> / 100.0<br>
        Fusion Formula: <code>Final = (0.40 &times; RuleScore) + (0.60 &times; PythonScore)</code><br>
        Blended Heap Severity Score: <strong>${patient.severityScore.toFixed(1)}</strong>
      </div>
    </div>
  `;

  detailsModal.classList.remove('hidden');
}

// Preset Loader
function loadPresetIntoForm(key) {
  const p = PRESETS[key];
  if (!p) return;

  document.getElementById('formName').value = p.name;
  document.getElementById('formAge').value = p.age;
  document.getElementById('formTemperature').value = p.temperature;
  document.getElementById('formHeartRate').value = p.heartRate;
  document.getElementById('formBloodPressure').value = p.bloodPressure;
  document.getElementById('formOxygenLevel').value = p.oxygenLevel;
  document.getElementById('formChestPain').checked = p.chestPain;
  document.getElementById('formBreathingDifficulty').checked = p.breathingDifficulty;
  document.getElementById('formBleeding').checked = p.bleeding;
  document.getElementById('formUnconscious').checked = p.unconscious;
  document.getElementById('formSymptoms').value = p.symptoms;
}

// Notification Helper
function showNotification(message, isLoading, type = '') {
  statusMessage.textContent = message;
  statusNotification.className = 'status-banner';
  if (type) statusNotification.classList.add(type);

  if (isLoading) {
    statusSpinner.classList.remove('hidden');
  } else {
    statusSpinner.classList.add('hidden');
  }
  statusNotification.classList.remove('hidden');
}

function hideNotification() {
  statusNotification.classList.add('hidden');
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
}

// Expose helper functions globally for inline onclick handlers
window.viewPatientBreakdown = viewPatientBreakdown;
window.dischargePatient = dischargePatient;
