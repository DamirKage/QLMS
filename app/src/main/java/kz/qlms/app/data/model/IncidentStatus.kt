package kz.qlms.app.data.model

import kz.qlms.app.R

/**
 * Lifecycle of an incident as it moves through the dispatcher (web panel) workflow.
 * The original thesis only ever wrote a report and never modeled a status at all —
 * without this, a citizen who reported something had no way to know it was seen.
 */
enum class IncidentStatus(val labelRes: Int) {
    NEW(R.string.status_new),
    ACKNOWLEDGED(R.string.status_acknowledged),
    DISPATCHED(R.string.status_dispatched),
    RESOLVED(R.string.status_resolved),
    FALSE_ALARM(R.string.status_false_alarm),
    CANCELLED(R.string.status_cancelled);

    companion object {
        fun fromFirestoreValue(value: String?): IncidentStatus =
            entries.find { it.name == value } ?: NEW
    }
}
