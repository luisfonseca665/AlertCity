package com.example.alertcity.ui.screens

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.alertcity.R
import com.example.alertcity.data.location.LocationHelper
import com.example.alertcity.data.media.ImageCompressor
import com.example.alertcity.ui.components.ImagenPreviewDialog
import com.example.alertcity.ui.components.MapaPreviewDialog
import com.example.alertcity.ui.components.OsmMapCard
import com.example.alertcity.ui.components.PinMapaUi
import com.example.alertcity.ui.components.colorHexPin
import com.example.alertcity.ui.components.esPantallaGrande
import com.example.alertcity.ui.viewmodel.CrearReporteUiState
import com.example.alertcity.ui.viewmodel.CrearReporteViewModel
import java.io.File
import kotlinx.coroutines.launch

private fun crearUriTemporal(context: Context): Uri {
    val directorio = File(context.cacheDir, "images").apply { if (!exists()) mkdirs() }
    val archivo = File(directorio, "evidencia_${System.currentTimeMillis()}.jpg").apply { createNewFile() }

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        archivo
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearReportesScreen(
    viewModel: CrearReporteViewModel,
    onBack: () -> Unit = {},
    onReporteGuardado: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pantallaGrande = esPantallaGrande()

    LaunchedEffect(Unit) {
        viewModel.sincronizarHoraActualSiNoEsPersonalizada()
        if (LocationHelper.tienePermisoUbicacion(context) && uiState.latitudSeleccionada == null) {
            val (lat, lng) = LocationHelper.obtenerCoordenadas(context)
            viewModel.onUbicacionGpsObtenida(lat, lng)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val uriInterna = ImageCompressor.compressAndSaveToInternalStorage(context, uri)
                viewModel.onFotoCapturada((uriInterna ?: uri).toString())
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        val tempUriActual = viewModel.uiState.value.tempUri
        if (exito && tempUriActual != null) {
            coroutineScope.launch {
                val sourceUri = Uri.parse(tempUriActual)
                val uriInterna = ImageCompressor.compressAndSaveToInternalStorage(context, sourceUri)
                viewModel.onFotoCapturada((uriInterna ?: sourceUri).toString())
            }
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) {
            coroutineScope.launch {
                val coordenadas = LocationHelper.obtenerCoordenadas(context)
                viewModel.onUbicacionGpsObtenida(coordenadas.first, coordenadas.second)
            }
        }
    }

    val onSolicitarUbicacionGps = {
        if (LocationHelper.tienePermisoUbicacion(context)) {
            coroutineScope.launch {
                val coordenadas = LocationHelper.obtenerCoordenadas(context)
                viewModel.onUbicacionGpsObtenida(coordenadas.first, coordenadas.second)
            }
        } else {
            locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val pinesSeleccion = remember(uiState.latitudSeleccionada, uiState.longitudSeleccionada, uiState.urgente) {
        if (uiState.latitudSeleccionada != null && uiState.longitudSeleccionada != null) {
            listOf(
                PinMapaUi(
                    id = "seleccion_actual",
                    latitud = uiState.latitudSeleccionada!!,
                    longitud = uiState.longitudSeleccionada!!,
                    colorHex = colorHexPin(esUrgente = uiState.urgente),
                    titulo = "Ubicación del reporte"
                )
            )
        } else {
            emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.action_back)
                )
            }
            Text(
                text = stringResource(id = R.string.create_report_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (pantallaGrande) {
            // Diseño adaptativo de 2 paneles lado a lado para pantallas grandes / tablets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    FormularioDatosReporte(
                        uiState = uiState,
                        viewModel = viewModel,
                        onSolicitarUbicacionGps = { onSolicitarUbicacionGps() }
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    SeccionEvidenciaYMapaReporte(
                        uiState = uiState,
                        pinesSeleccion = pinesSeleccion,
                        viewModel = viewModel,
                        onGuardar = { viewModel.guardarReporte(onReporteGuardado) }
                    )
                }
            }
        } else {
            // Diseño compacto vertical para teléfonos
            FormularioDatosReporte(
                uiState = uiState,
                viewModel = viewModel,
                onSolicitarUbicacionGps = { onSolicitarUbicacionGps() }
            )
            SeccionEvidenciaYMapaReporte(
                uiState = uiState,
                pinesSeleccion = pinesSeleccion,
                viewModel = viewModel,
                onGuardar = { viewModel.guardarReporte(onReporteGuardado) }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    if (uiState.mostrarPreviewImagen && uiState.fotoUri != null) {
        ImagenPreviewDialog(
            imagenUri = uiState.fotoUri!!,
            titulo = uiState.titulo.ifBlank { stringResource(id = R.string.create_photo_section) },
            onDismiss = { viewModel.onMostrarPreviewImagenChange(false) }
        )
    }

    if (uiState.mostrarPreviewMapa) {
        MapaPreviewDialog(
            titulo = stringResource(id = R.string.create_select_location_title),
            subtitulo = uiState.ubicacion ?: stringResource(id = R.string.create_select_location_subtitle),
            latitudInicial = uiState.latitudSeleccionada ?: uiState.latitudCamara,
            longitudInicial = uiState.longitudSeleccionada ?: uiState.longitudCamara,
            pines = pinesSeleccion,
            zoomInicial = 16.5,
            etiquetaInferior = uiState.ubicacion,
            onMapClick = viewModel::onMapaClick,
            onDismiss = { viewModel.onMostrarPreviewMapaChange(false) }
        )
    }

    if (uiState.mostrarTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = uiState.hora,
            initialMinute = uiState.minuto,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { viewModel.onMostrarTimePickerChange(false) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onConfirmarHora(timeState.hour, timeState.minute) }
                ) {
                    Text(stringResource(id = R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.onMostrarTimePickerChange(false) }
                ) {
                    Text(stringResource(id = R.string.action_cancel))
                }
            },
            text = { TimePicker(state = timeState) }
        )
    }

    if (uiState.mostrarOpcionesFoto) {
        AlertDialog(
            onDismissRequest = { viewModel.onMostrarOpcionesFotoChange(false) },
            title = { Text(stringResource(id = R.string.create_attach_dialog_title)) },
            text = {
                Column {
                    TextButton(onClick = {
                        viewModel.onMostrarOpcionesFotoChange(false)
                        try {
                            val nuevoTempUri = crearUriTemporal(context)
                            viewModel.onTempUriGenerada(nuevoTempUri.toString())
                            cameraLauncher.launch(nuevoTempUri)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }) { Text(stringResource(id = R.string.create_take_photo)) }

                    TextButton(onClick = {
                        viewModel.onMostrarOpcionesFotoChange(false)
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text(stringResource(id = R.string.create_pick_gallery)) }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onMostrarOpcionesFotoChange(false) }) {
                    Text(stringResource(id = R.string.action_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormularioDatosReporte(
    uiState: CrearReporteUiState,
    viewModel: CrearReporteViewModel,
    onSolicitarUbicacionGps: () -> Unit
) {
    OutlinedTextField(
        value = uiState.titulo,
        onValueChange = { viewModel.onTituloChange(it) },
        label = { Text(stringResource(id = R.string.create_issue_label)) },
        placeholder = { Text(stringResource(id = R.string.create_issue_placeholder)) },
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(10.dp))

    OutlinedTextField(
        value = uiState.descripcion,
        onValueChange = { viewModel.onDescripcionChange(it) },
        label = { Text(stringResource(id = R.string.create_desc_label)) },
        placeholder = { Text(stringResource(id = R.string.create_desc_placeholder)) },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        minLines = 3
    )

    Spacer(modifier = Modifier.height(12.dp))

    ExposedDropdownMenuBox(
        expanded = uiState.categoriaExpandida,
        onExpandedChange = { viewModel.onCategoriaExpandidaChange(it) },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = uiState.categoria,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(id = R.string.create_category_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.categoriaExpandida) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = uiState.categoriaExpandida,
            onDismissRequest = { viewModel.onCategoriaExpandidaChange(false) }
        ) {
            uiState.categorias.forEach { cat ->
                DropdownMenuItem(
                    text = { Text(cat) },
                    onClick = { viewModel.onCategoriaChange(cat) }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedButton(
        onClick = { viewModel.onMostrarTimePickerChange(true) },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(stringResource(id = R.string.create_time_button, uiState.horaFormateada))
    }

    Spacer(modifier = Modifier.height(8.dp))

    Surface(
        onClick = { viewModel.onUrgenteChange(!uiState.urgente) },
        shape = RoundedCornerShape(12.dp),
        color = if (uiState.urgente) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.urgente,
                onCheckedChange = { viewModel.onUrgenteChange(it) }
            )
            Text(
                text = stringResource(id = R.string.create_mark_urgent),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (uiState.urgente) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = { viewModel.onMostrarOpcionesFotoChange(true) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
            Text(
                if (uiState.fotoUri != null) {
                    stringResource(id = R.string.create_change_photo)
                } else {
                    stringResource(id = R.string.create_add_photo)
                }
            )
        }

        OutlinedButton(
            onClick = onSolicitarUbicacionGps,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
            Text(
                if (uiState.ubicacion != null) {
                    stringResource(id = R.string.create_change_location)
                } else {
                    stringResource(id = R.string.create_add_location)
                }
            )
        }
    }
}

@Composable
private fun SeccionEvidenciaYMapaReporte(
    uiState: CrearReporteUiState,
    pinesSeleccion: List<PinMapaUi>,
    viewModel: CrearReporteViewModel,
    onGuardar: () -> Unit
) {
    uiState.fotoUri?.let { fotoActual ->
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(id = R.string.create_photo_section),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        ElevatedCard(
            onClick = { viewModel.onMostrarPreviewImagenChange(true) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(185.dp)
            ) {
                AsyncImage(
                    model = fotoActual,
                    contentDescription = stringResource(id = R.string.create_photo_section),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.5f)
                                )
                            )
                        )
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp),
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(id = R.string.create_preview_badge),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(id = R.string.create_location_section),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        TextButton(
            onClick = { viewModel.onMostrarPreviewMapaChange(true) },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        ) {
            Icon(
                imageVector = Icons.Default.OpenInFull,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(id = R.string.action_expand_map),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
    Text(
        text = uiState.ubicacion?.let { stringResource(id = R.string.create_location_selected, it) }
            ?: stringResource(id = R.string.create_location_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(8.dp))

    OsmMapCard(
        latitudCamara = uiState.latitudCamara,
        longitudCamara = uiState.longitudCamara,
        pines = pinesSeleccion,
        zoomInicial = 15.5,
        zoomCamara = 16.5,
        animarTrigger = uiState.animarMapaTrigger,
        etiquetaInferior = uiState.ubicacion,
        onAmpliarClick = { viewModel.onMostrarPreviewMapaChange(true) },
        onMapClick = viewModel::onMapaClick
    )

    Spacer(modifier = Modifier.height(22.dp))

    Button(
        onClick = onGuardar,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        enabled = uiState.formularioValido
    ) {
        Text(
            text = if (uiState.guardando) {
                stringResource(id = R.string.create_saving)
            } else {
                stringResource(id = R.string.create_save_button)
            },
            style = MaterialTheme.typography.titleMedium
        )
    }
}
