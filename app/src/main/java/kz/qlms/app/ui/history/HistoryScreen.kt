package kz.qlms.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kz.qlms.app.R
import kz.qlms.app.core.rememberAppContainer
import kz.qlms.app.ui.components.EmptyState
import kz.qlms.app.ui.components.IncidentCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onIncidentClick: (String) -> Unit) {
    val container = rememberAppContainer()
    val viewModel: HistoryViewModel = viewModel(
        factory = viewModelFactory {
            initializer { HistoryViewModel(container.incidentRepository, container.authRepository) }
        },
    )
    val incidents by viewModel.myIncidents.collectAsState()

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text(stringResource(R.string.nav_history)) }) }) { padding ->
        if (incidents.isEmpty()) {
            EmptyState(text = stringResource(R.string.history_empty), modifier = Modifier.padding(padding).fillMaxSize())
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(incidents, key = { it.id }) { incident ->
                    IncidentCard(incident = incident, onClick = { onIncidentClick(incident.id) })
                }
            }
        }
    }
}
