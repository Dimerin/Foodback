package unipi.msss.foodback.auth.signup.ui

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import unipi.msss.foodback.R
import unipi.msss.foodback.commons.ViewEvent
import unipi.msss.foodback.ui.theme.FoodbackPreview
import unipi.msss.foodback.ui.theme.FoodbackTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SignUpView(
    viewModel: SignUpViewModel = hiltViewModel(),
    onSignUpSuccess: () -> Unit = {},
    onBackToLogin: () -> Unit = {},
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    SignUp(
        state.value,
        onEvent = { viewModel.onEvent(it) },
        onBackToLogin = onBackToLogin,
        context = LocalContext.current
    )

    ViewEvent(viewModel.eventsFlow) { event ->
        when (event) {
            SignUpNavigationEvents.SignedUp -> onSignUpSuccess()
        }
    }
}


@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignUp(
    state: SignUpState,
    onEvent: (SignUpEvent) -> Unit,
    onBackToLogin: () -> Unit,
    context: Context,
) {
    val openDialog = remember { mutableStateOf(false) }
    val selectedDate = remember { mutableStateOf<Date?>(null) }
    val formattedDate = remember(selectedDate.value) {
        selectedDate.value?.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it) } ?: "None"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Create your account",
                        fontSize = 22.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackToLogin) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LottieAnimation(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    composition = rememberLottieComposition(
                        spec = LottieCompositionSpec.RawRes(
                            R.raw.signup_anim
                        )
                    ).value,
                    iterations = LottieConstants.IterateForever
                )
                ElevatedCard( elevation = CardDefaults.cardElevation(
                    defaultElevation = 6.dp
                ),
                    modifier = Modifier.size(width = 500.dp, height = 750.dp)
                        .padding(16.dp).
                        scale(0.9f),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Signup",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )

                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = state.name,
                            onValueChange = { onEvent(SignUpEvent.NameChanged(it)) },
                            label = { Text("Name") },
                            placeholder = { Text("Enter your name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = state.nameError != null,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        AnimatedVisibility(visible = state.nameError != null) {
                            Text(
                                text = state.nameError.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = state.surname,
                            onValueChange = { onEvent(SignUpEvent.SurnameChanged(it)) },
                            label = { Text("Surname") },
                            placeholder = { Text("Enter your surname") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = state.surnameError != null,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        AnimatedVisibility(visible = state.surnameError != null) {
                            Text(
                                text = state.surnameError.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = state.email,
                            onValueChange = { onEvent(SignUpEvent.EmailChanged(it)) },
                            label = { Text("Email") },
                            placeholder = { Text("Enter your email") },
                            modifier = Modifier
                                .fillMaxWidth(),
                            singleLine = true,
                            isError = state.emailError != null,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        AnimatedVisibility(visible = state.emailError != null) {
                            Text(
                                text = state.emailError.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.gender == "Male",
                                onClick = { onEvent(SignUpEvent.GenderChanged("Male")) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Text(
                                text = "Male",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.gender == "Female",
                                onClick = { onEvent(SignUpEvent.GenderChanged("Female")) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Text(
                                text = "Female",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )

                        }
                    }
                    AnimatedVisibility(visible = state.genderError != null) {
                        Text(
                            text = state.genderError.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = formattedDate,
                            onValueChange = {},
                            label = { Text("Date of Birth") },
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { openDialog.value = !openDialog.value }) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Select date"
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            isError = state.dateOfBirthError != null,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        AnimatedVisibility(visible = state.dateOfBirthError != null) {
                            Text(
                                text = state.dateOfBirthError.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    if (openDialog.value) {
                        val datePickerState = rememberDatePickerState()
                        val confirmEnabled =
                            derivedStateOf { datePickerState.selectedDateMillis != null }
                        DatePickerDialog(
                            onDismissRequest = { openDialog.value = false },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        openDialog.value = false
                                        val date =
                                            datePickerState.selectedDateMillis?.let { Date(it) }
                                        if (date != null) {
                                            selectedDate.value = date
                                            onEvent(SignUpEvent.DateOfBirthChanged(date))
                                        }
                                    },
                                    enabled = confirmEnabled.value
                                ) {
                                    Text("OK")
                                }
                            }
                        ) {
                            DatePicker(state = datePickerState)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = state.password,
                            onValueChange = { onEvent(SignUpEvent.PasswordChanged(it)) },
                            label = { Text("Password") },
                            placeholder = { Text("Enter your password") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { onEvent(SignUpEvent.TogglePasswordVisibility) }) {
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = state.confirmPassword,
                            onValueChange = { onEvent(SignUpEvent.ConfirmPasswordChanged(it)) },
                            label = { Text("Confirm Password") },
                            placeholder = { Text("Re-enter your password") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            visualTransformation = if (state.isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { onEvent(SignUpEvent.ToggleConfirmPasswordVisibility) }) {
                                    Icon(
                                        modifier = Modifier.padding(end = 4.dp),
                                        painter = painterResource(if (state.isConfirmPasswordVisible) R.drawable.ic_eye_open else R.drawable.ic_eye_closed),
                                        contentDescription = "Password Visibility Toggle",
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = state.confirmPasswordError != null,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        AnimatedVisibility(visible = state.confirmPasswordError != null) {
                            Text(
                                text = state.confirmPasswordError.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    AnimatedVisibility(visible = state.signupError != null) {
                        Text(
                            text = state.signupError.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {
                            onEvent(SignUpEvent.SignUpClicked(context = context, successIconResId = R.drawable.success))
                        },
                        modifier = Modifier.fillMaxWidth()
                            .scale(0.8f),
                    ) {
                        Text("Sign Up", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
@FoodbackPreview
private fun SignupPreview() {
    FoodbackTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            SignUp(
                state = SignUpState(),
                onEvent = {},
                onBackToLogin = {},
                context = LocalContext.current
            )
        }
    }
}