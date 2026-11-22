package com.example.alertcity.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.alertcity.data.remote.ApiClient
import com.example.alertcity.data.remote.NetworkConstants
import com.example.alertcity.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onIrARegistro: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var mostrarPassword by remember { mutableStateOf(false) }
    var mostrarDialogoServidor by remember { mutableStateOf(false) }
    var servidorActual by remember { mutableStateOf(NetworkConstants.BASE_URL.trimEnd('/')) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.LocationCity,
            contentDescription = "AlertCity",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "AlertCity",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Inicia sesión para reportar y seguir incidencias",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = uiState.usernameOrEmail,
            onValueChange = { viewModel.onUsernameOrEmailChange(it) },
            label = { Text("Usuario o correo electrónico") },
            placeholder = { Text("Ej. laura_ciudadana") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.password,
            onValueChange = { viewModel.onPasswordChange(it) },
            label = { Text("Contraseña") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { mostrarPassword = !mostrarPassword }) {
                    Icon(
                        imageVector = if (mostrarPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (mostrarPassword) "Ocultar contraseña" else "Mostrar contraseña"
                    )
                }
            },
            visualTransformation = if (mostrarPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        uiState.mensajeError?.let { error ->
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.iniciarSesion() },
            enabled = uiState.loginValido,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (uiState.cargando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Iniciar Sesión", style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        TextButton(onClick = {
            viewModel.limpiarError()
            onIrARegistro()
        }) {
            Text("¿No tienes cuenta? Regístrate como ciudadano")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { mostrarDialogoServidor = true },
            shape = RoundedCornerShape(20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Configurar servidor",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Servidor: $servidorActual",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }

    if (mostrarDialogoServidor) {
        ServidorLocalDialog(
            onDismiss = { mostrarDialogoServidor = false },
            onGuardado = {
                servidorActual = NetworkConstants.BASE_URL.trimEnd('/')
                mostrarDialogoServidor = false
            }
        )
    }
}

@Composable
private fun ServidorLocalDialog(
    onDismiss: () -> Unit,
    onGuardado: () -> Unit
) {
    var ipInput by remember { mutableStateOf(NetworkConstants.hostIp) }
    var puertoInput by remember { mutableStateOf(NetworkConstants.port.toString()) }
    var probando by remember { mutableStateOf(false) }
    var resultadoPrueba by remember { mutableStateOf<String?>(null) }
    var esErrorPrueba by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Servidor en Red Local") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Ingresa la IP local de tu PC en la misma red Wi-Fi (puerto 8080 por defecto).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = ipInput,
                    onValueChange = {
                        ipInput = it
                        resultadoPrueba = null
                    },
                    label = { Text("IP del servidor") },
                    placeholder = { Text("Ej. 192.168.1.5") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = puertoInput,
                    onValueChange = {
                        puertoInput = it.filter { ch -> ch.isDigit() }
                        resultadoPrueba = null
                    },
                    label = { Text("Puerto") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = {
                            ipInput = NetworkConstants.DEFAULT_HOST_IP
                            puertoInput = NetworkConstants.DEFAULT_PORT.toString()
                            resultadoPrueba = null
                        },
                        label = { Text("LAN (${NetworkConstants.DEFAULT_HOST_IP})") }
                    )
                    AssistChip(
                        onClick = {
                            ipInput = "10.0.2.2"
                            puertoInput = "8080"
                            resultadoPrueba = null
                        },
                        label = { Text("Emulador") }
                    )
                }

                OutlinedButton(
                    onClick = {
                        val puerto = puertoInput.toIntOrNull() ?: NetworkConstants.DEFAULT_PORT
                        probando = true
                        resultadoPrueba = null
                        scope.launch {
                            val res = ApiClient.checkHealth(ipInput, puerto)
                            probando = false
                            res.fold(
                                onSuccess = { msg ->
                                    esErrorPrueba = false
                                    resultadoPrueba = msg
                                },
                                onFailure = { err ->
                                    esErrorPrueba = true
                                    resultadoPrueba = err.localizedMessage
                                }
                            )
                        }
                    },
                    enabled = !probando && ipInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (probando) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Probando...")
                    } else {
                        Text("Probar conexión (/actuator/health)")
                    }
                }

                resultadoPrueba?.let { msg ->
                    Surface(
                        color = if (esErrorPrueba) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            color = if (esErrorPrueba) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val puerto = puertoInput.toIntOrNull() ?: NetworkConstants.DEFAULT_PORT
                    NetworkConstants.updateServer(ipInput, puerto)
                    onGuardado()
                },
                enabled = ipInput.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
