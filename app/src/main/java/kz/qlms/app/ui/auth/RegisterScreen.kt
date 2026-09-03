package kz.qlms.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kz.qlms.app.R
import kz.qlms.app.core.rememberAppContainer
import kz.qlms.app.ui.components.QlmsPasswordField
import kz.qlms.app.ui.components.QlmsPrimaryButton
import kz.qlms.app.ui.components.QlmsTextField

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    val container = rememberAppContainer()
    val viewModel: AuthViewModel = viewModel(
        factory = viewModelFactory {
            initializer { AuthViewModel(container.authRepository, container.userRepository) }
        },
    )

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) onRegisterSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.register_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.register_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp, top = 4.dp),
        )

        QlmsTextField(value = fullName, onValueChange = { fullName = it }, label = stringResource(R.string.field_full_name))
        Spacer(Modifier.height(12.dp))
        QlmsTextField(value = email, onValueChange = { email = it }, label = stringResource(R.string.field_email), keyboardType = KeyboardType.Email)
        Spacer(Modifier.height(12.dp))
        QlmsPasswordField(value = password, onValueChange = { password = it }, label = stringResource(R.string.field_password))
        Spacer(Modifier.height(12.dp))
        QlmsPasswordField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = stringResource(R.string.field_confirm_password))

        Text(
            text = stringResource(R.string.register_consent_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 16.dp),
        )

        if (uiState is AuthUiState.Error) {
            Text(
                text = authErrorMessage((uiState as AuthUiState.Error).message),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        QlmsPrimaryButton(
            text = stringResource(R.string.register_action),
            onClick = { viewModel.register(fullName, email, password, confirmPassword) },
            loading = uiState is AuthUiState.Loading,
        )

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onNavigateToLogin, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(stringResource(R.string.register_go_to_login))
        }
    }
}
