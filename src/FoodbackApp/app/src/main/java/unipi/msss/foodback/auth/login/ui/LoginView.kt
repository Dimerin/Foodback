package unipi.msss.foodback.auth.login.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import unipi.msss.foodback.R
import unipi.msss.foodback.commons.ViewEvent
import unipi.msss.foodback.ui.theme.FoodbackPreview
import unipi.msss.foodback.ui.theme.FoodbackTheme

@Composable
fun LoginView(
    viewModel: LoginViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit = {},
    onSignUpClicked: () -> Unit = {},
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    Login(state.value) { event -> viewModel.onEvent(event) }

    ViewEvent(viewModel.eventsFlow) { event ->
        when (event) {
            LoginNavigationEvents.Authenticated -> onLoginSuccess()
            LoginNavigationEvents.SignUp -> onSignUpClicked()
        }
    }
}

@Composable
private fun Login(
    state: LoginState = LoginState(),
    onEvent: (LoginEvent) -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Image(
                modifier = Modifier.size(200.dp),
                painter = painterResource(R.drawable.logo),
                contentDescription = null,
            )

            Text(
                text = "Welcome in Foodback!",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )

            Text(
                text = "Login to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Column(modifier = Modifier.fillMaxWidth().scale(0.8f)) {
                OutlinedTextField(
                    value = state.email,
                    onValueChange = { onEvent(LoginEvent.EmailChanged(it)) },
                    label = { Text("Email") },
                    placeholder = { Text("Enter your email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = state.emailError != null,
                )
                Spacer(modifier = Modifier.height(4.dp)) // Space for error message
                AnimatedVisibility(visible = state.emailError != null) {
                    Text(
                        text = state.emailError.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth().scale(0.8f)) {
                OutlinedTextField(
                    value = state.password,
                    onValueChange = { onEvent(LoginEvent.PasswordChanged(it)) },
                    label = { Text("Password") },
                    placeholder = { Text("Enter your password") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { onEvent(LoginEvent.TogglePasswordVisibility) }) {
                            Icon(
                                modifier = Modifier.padding(end = 4.dp),
                                painter = painterResource(if (state.isPasswordVisible) R.drawable.ic_eye_open else R.drawable.ic_eye_closed),
                                contentDescription = "Password Visibility Toggle",
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = state.passwordError != null,
                )
                Spacer(modifier = Modifier.height(4.dp))
                AnimatedVisibility(visible = state.passwordError != null) {
                    Text(
                        text = state.passwordError.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            AnimatedVisibility(visible = state.loginError != null) {
                Text(
                    text = state.loginError.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Button(
                onClick = { onEvent(LoginEvent.LoginClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .scale(0.8f),
                enabled = !state.isLoading,
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Login", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }


                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center

                ){
                Text(
                    text = "Don't have an account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                TextButton(onClick = { onEvent(LoginEvent.SignUpClicked) }) {
                    Text("Sign Up", fontSize = 16.sp, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)
                }
            }

        }
    }
}

@Composable
@FoodbackPreview
private fun LoginPreview() {
    FoodbackTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Login()
        }
    }
}
