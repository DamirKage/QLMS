package kz.qlms.app.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.CarCrash
import androidx.compose.material.icons.filled.Diversity3
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.PropaneTank
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import kz.qlms.app.R
import kz.qlms.app.core.Constants

/**
 * Incident categories a reporter can pick. Each maps to the real Kazakhstan
 * emergency service that would historically be dialed for it (101/102/103/104),
 * even though the app itself always routes SOS through the unified 112 line.
 */
enum class IncidentType(
    val labelRes: Int,
    val icon: ImageVector,
    val referenceNumber: String,
) {
    CRIME(R.string.incident_type_crime, Icons.Filled.Report, Constants.EMERGENCY_NUMBER_POLICE),
    FIRE(R.string.incident_type_fire, Icons.Filled.LocalFireDepartment, Constants.EMERGENCY_NUMBER_FIRE),
    MEDICAL(R.string.incident_type_medical, Icons.Filled.Bloodtype, Constants.EMERGENCY_NUMBER_AMBULANCE),
    ROAD_ACCIDENT(R.string.incident_type_road_accident, Icons.Filled.CarCrash, Constants.EMERGENCY_NUMBER_POLICE),
    DOMESTIC_VIOLENCE(R.string.incident_type_domestic_violence, Icons.Filled.Diversity3, Constants.EMERGENCY_NUMBER_POLICE),
    MISSING_PERSON(R.string.incident_type_missing_person, Icons.Filled.PersonSearch, Constants.EMERGENCY_NUMBER_POLICE),
    NATURAL_DISASTER(R.string.incident_type_natural_disaster, Icons.Filled.Warning, Constants.EMERGENCY_NUMBER_UNIFIED),
    GAS_LEAK(R.string.incident_type_gas_leak, Icons.Filled.PropaneTank, Constants.EMERGENCY_NUMBER_GAS),
    OTHER(R.string.incident_type_other, Icons.Filled.Gesture, Constants.EMERGENCY_NUMBER_UNIFIED),
}
