package kz.qlms.app.ui.medical

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kz.qlms.app.R
import kz.qlms.app.core.rememberAppContainer
import kz.qlms.app.data.model.MedicalProfile
import kz.qlms.app.ui.components.QlmsPrimaryButton
import kz.qlms.app.ui.components.QlmsTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalInfoScreen(onNavigateBack: () -> Unit) {
    val container = rememberAppContainer()
    val viewModel: MedicalInfoViewModel = viewModel(
        factory = viewModelFactory {
            initializer { MedicalInfoViewModel(container.userRepository, container.authRepository) }
        },
    )
    val cached by viewModel.profile.collectAsState()
    val saved by viewModel.saved.collectAsState()

    var bloodType by remember { mutableStateOf(cached.bloodType) }
    var allergies by remember { mutableStateOf(cached.allergies.joinToString(", ")) }
    var chronicConditions by remember { mutableStateOf(cached.chronicConditions.joinToString(", ")) }
    var medications by remember { mutableStateOf(cached.medications.joinToString(", ")) }
    var isOrganDonor by remember { mutableStateOf(cached.isOrganDonor) }
    var notes by remember { mutableStateOf(cached.additionalNotes) }

    LaunchedEffect(saved) {
        if (saved) viewModel.consumeSaved()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.medical_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.medical_privacy_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            QlmsTextField(value = bloodType, onValueChange = { bloodType = it }, label = stringResource(R.string.medical_blood_type))
            Spacer(Modifier.height(12.dp))
            QlmsTextField(value = allergies, onValueChange = { allergies = it }, label = stringResource(R.string.medical_allergies), singleLine = false)
            Spacer(Modifier.height(12.dp))
            QlmsTextField(value = chronicConditions, onValueChange = { chronicConditions = it }, label = stringResource(R.string.medical_chronic_conditions), singleLine = false)
            Spacer(Modifier.height(12.dp))
            QlmsTextField(value = medications, onValueChange = { medications = it }, label = stringResource(R.string.medical_medications), singleLine = false)
            Spacer(Modifier.height(12.dp))
            QlmsTextField(value = notes, onValueChange = { notes = it }, label = stringResource(R.string.medical_notes), singleLine = false, minLines = 2)

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.medical_organ_donor))
                Switch(checked = isOrganDonor, onCheckedChange = { isOrganDonor = it })
            }

            Spacer(Modifier.height(24.dp))
            QlmsPrimaryButton(
                text = stringResource(R.string.action_save),
                onClick = {
                    viewModel.save(
                        MedicalProfile(
                            bloodType = bloodType.trim(),
                            allergies = allergies.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                            chronicConditions = chronicConditions.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                            medications = medications.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                            isOrganDonor = isOrganDonor,
                            additionalNotes = notes.trim(),
                        ),
                    )
                },
            )

            if (saved) {
                Spacer(Modifier.height(12.dp))
                Snackbar { Text(stringResource(R.string.medical_saved_confirmation)) }
            }
        }
    }
}
