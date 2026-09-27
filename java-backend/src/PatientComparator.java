import java.util.Comparator;

/**
 * =============================================================================
 * ER Triage Assistant - PatientComparator (ADSA Max-Heap Comparator)
 * =============================================================================
 * Academic/Demo Project: Hospital Emergency Room Triage Management System.
 * DISCLAIMER: Academic Demo Only - Not for clinical use or real medical diagnosis.
 *
 * Course Alignment:
 * - ADSA (Advanced Data Structures & Algorithms): Binary Heap, Priority Queue,
 *   Total Ordering Relations, Comparator Interface.
 *
 * Algorithmic Mechanics:
 * - In standard Java, java.util.PriorityQueue implements a Min-Heap by default.
 * - To transform this into a MAX-HEAP where the highest severity patient sits
 *   at the root (index 0) and is dequeued first, the comparator inverts the
 *   comparison logic:
 *       compare(p1, p2) returns positive if p2 has higher score than p1.
 * - Tie-Breaking Logic (FIFO Fairness):
 *   If two patients possess identical severity scores, the patient who arrived
 *   earlier (lower arrivalTimeMillis) receives higher priority.
 *
 * Complexity Characteristics:
 * - Peek Max:           O(1)
 * - Enqueue (Insert):   O(log N) via siftUp()
 * - Dequeue (Poll Max): O(log N) via siftDown()
 * - Heap Construction: O(N)
 * =============================================================================
 */
public class PatientComparator implements Comparator<Patient> {

    @Override
    public int compare(Patient p1, Patient p2) {
        if (p1 == null && p2 == null) return 0;
        if (p1 == null) return 1;
        if (p2 == null) return -1;

        // Primary Sorting Criterion: Blended Severity Score (Max-Heap order: Descending)
        int scoreComparison = Double.compare(p2.getSeverityScore(), p1.getSeverityScore());
        if (scoreComparison != 0) {
            return scoreComparison;
        }

        // Secondary Criterion (Tie-Breaker): First-Come First-Served (FIFO)
        // Earlier arrival timestamp (smaller long value) gets served first
        int timeComparison = Long.compare(p1.getArrivalTimeMillis(), p2.getArrivalTimeMillis());
        if (timeComparison != 0) {
            return timeComparison;
        }

        // Tertiary Criterion: Lexicographical Patient ID stability
        if (p1.getPatientId() != null && p2.getPatientId() != null) {
            return p1.getPatientId().compareTo(p2.getPatientId());
        }

        return 0;
    }
}
