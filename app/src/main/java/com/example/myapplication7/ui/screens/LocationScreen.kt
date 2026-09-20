package com.example.myapplication7.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.myapplication7.ui.theme.AppTheme
import com.example.myapplication7.ui.viewmodels.LocationUiState
import com.example.myapplication7.ui.viewmodels.LocationViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================================
// ЛР №12: Головний екран — Геолокація та Фото
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen(viewModel: LocationViewModel) {
    val context = LocalContext.current
    val locationState by viewModel.locationState.collectAsStateWithLifecycle()
    val selectedPhotoPath by viewModel.selectedPhotoPath.collectAsStateWithLifecycle()

    // ---- ЛР №12 Завдання 2: Стан дозволів ----
    // Геолокація
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var locationPermissionDeniedPermanently by remember { mutableStateOf(false) }

    // Медіа (для галереї)
    val mediaPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.READ_MEDIA_IMAGES
    else
        Manifest.permission.READ_EXTERNAL_STORAGE

    var hasMediaPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, mediaPermission) == PackageManager.PERMISSION_GRANTED
        )
    }
    var mediaPermissionDeniedPermanently by remember { mutableStateOf(false) }

    val activity = context as? androidx.activity.ComponentActivity

    // ---- Launcher: запит дозволу геолокації ----
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!hasLocationPermission) {
            locationPermissionDeniedPermanently = activity?.let {
                !ActivityCompat.shouldShowRequestPermissionRationale(
                    it, Manifest.permission.ACCESS_FINE_LOCATION
                )
            } ?: false
        }
        if (hasLocationPermission) viewModel.fetchLocation()
    }

    // ---- Launcher: запит дозволу медіа ----
    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMediaPermission = granted
        if (!granted) {
            mediaPermissionDeniedPermanently = activity?.let {
                !ActivityCompat.shouldShowRequestPermissionRationale(it, mediaPermission)
            } ?: false
        }
    }

    // ---- Launcher: Photo Picker (Варіант Б — Галерея) ----
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.savePhotoFromUri(context, it) }
    }

    // Функція відкриття системних налаштувань (Завдання 2)
    val openSettings = {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Локація та фото", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ============================================================
            // ЛР №12 Завдання 3: Секція вибору фото з галереї
            // ============================================================
            SectionCard(title = "Фото концерту", icon = Icons.Default.Person) {
                PhotoSection(
                    selectedPhotoPath = selectedPhotoPath,
                    hasMediaPermission = hasMediaPermission,
                    mediaPermissionDeniedPermanently = mediaPermissionDeniedPermanently,
                    onRequestPermission = { mediaPermissionLauncher.launch(mediaPermission) },
                    onPickPhoto = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onOpenSettings = openSettings,
                    onClearPhoto = { viewModel.clearPhoto() }
                )
            }

            // ============================================================
            // ЛР №12 Завдання 4: Секція геолокації
            // ============================================================
            SectionCard(title = "Геолокація", icon = Icons.Default.LocationOn) {
                LocationSection(
                    locationState = locationState,
                    hasLocationPermission = hasLocationPermission,
                    locationPermissionDeniedPermanently = locationPermissionDeniedPermanently,
                    onRequestPermission = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    onFetchLocation = { viewModel.fetchLocation() },
                    onOpenSettings = openSettings
                )
            }
        }
    }
}

// ============================================================
// ЛР №12 Завдання 3: UI вибору фото з галереї
// ============================================================
@Composable
fun PhotoSection(
    selectedPhotoPath: String?,
    hasMediaPermission: Boolean,
    mediaPermissionDeniedPermanently: Boolean,
    onRequestPermission: () -> Unit,
    onPickPhoto: () -> Unit,
    onOpenSettings: () -> Unit,
    onClearPhoto: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // ЛР №12 Завдання 2: Три стани дозволу
        // Photo Picker API не потребує дозволу на Android 13+, але показуємо статус
        val showPermissionBanner = !hasMediaPermission && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU

        if (showPermissionBanner) {
            PermissionBanner(
                message = "Для доступу до галереї потрібен дозвіл на читання медіафайлів.",
                isDeniedPermanently = mediaPermissionDeniedPermanently,
                onRequest = onRequestPermission,
                onOpenSettings = onOpenSettings
            )
        }

        if (selectedPhotoPath != null) {
            // Відображення вибраного фото
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = File(selectedPhotoPath),
                    contentDescription = "Фото концерту",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Кнопка видалення фото
                IconButton(
                    onClick = onClearPhoto,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                            RoundedCornerShape(50)
                        )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Видалити фото",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            Text(
                "Фото збережено: ${File(selectedPhotoPath).name}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        } else {
            // Placeholder для вибору фото
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Face,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Фото не вибрано",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        Button(
            onClick = onPickPhoto,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (selectedPhotoPath != null) "Замінити фото" else "Вибрати з галереї")
        }
    }
}

// ============================================================
// ЛР №12 Завдання 4: UI відображення геолокації
// ============================================================
@Composable
fun LocationSection(
    locationState: LocationUiState,
    hasLocationPermission: Boolean,
    locationPermissionDeniedPermanently: Boolean,
    onRequestPermission: () -> Unit,
    onFetchLocation: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // ЛР №12 Завдання 2: Три стани дозволу на геолокацію
        when {
            !hasLocationPermission && !locationPermissionDeniedPermanently -> {
                PermissionBanner(
                    message = "Для визначення поточного місцезнаходження потрібен дозвіл на геолокацію.",
                    isDeniedPermanently = false,
                    onRequest = onRequestPermission,
                    onOpenSettings = onOpenSettings
                )
            }
            !hasLocationPermission && locationPermissionDeniedPermanently -> {
                PermissionBanner(
                    message = "Дозвіл на геолокацію заблоковано. Відкрийте налаштування застосунку.",
                    isDeniedPermanently = true,
                    onRequest = onRequestPermission,
                    onOpenSettings = onOpenSettings
                )
            }
            else -> {
                // Дозвіл надано — відображаємо дані
                AnimatedVisibility(
                    visible = locationState is LocationUiState.Success,
                    enter = fadeIn(tween(500)),
                    exit = fadeOut(tween(300))
                ) {
                    if (locationState is LocationUiState.Success) {
                        LocationDataCard(state = locationState)
                    }
                }

                when (locationState) {
                    is LocationUiState.Loading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Text("Визначення місцезнаходження...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    is LocationUiState.Error -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    locationState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                    is LocationUiState.Idle -> {
                        Text(
                            "Натисніть кнопку для визначення місцезнаходження",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    else -> Unit
                }

                Button(
                    onClick = onFetchLocation,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = locationState !is LocationUiState.Loading
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Оновити локацію")
                }
            }
        }
    }
}

// ЛР №12 Завдання 4: Картка з даними локації
@Composable
fun LocationDataCard(state: LocationUiState.Success) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
    val timeString = dateFormat.format(Date(state.timestamp))
    val distanceKm = state.distanceToTarget / 1000f

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Поточне місцезнаходження",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            LocationInfoRow(
                icon = Icons.Default.LocationOn,
                label = "Широта",
                value = "%.6f°".format(state.latitude)
            )
            LocationInfoRow(
                icon = Icons.Default.LocationOn,
                label = "Довгота",
                value = "%.6f°".format(state.longitude)
            )
            LocationInfoRow(
                icon = Icons.Default.Info,
                label = "Точність",
                value = "%.1f м".format(state.accuracy)
            )
            LocationInfoRow(
                icon = Icons.Default.DateRange,
                label = "Час оновлення",
                value = timeString
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // ЛР №12 Завдання 4: Відстань до фіксованої точки
            LocationInfoRow(
                icon = Icons.Default.Place,
                label = "До ${LocationViewModel.TARGET_NAME}",
                value = if (distanceKm >= 1f) "%.2f км".format(distanceKm)
                        else "%.0f м".format(state.distanceToTarget)
            )
        }
    }
}

@Composable
fun LocationInfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon, contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline)
            Text(value, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Medium)
        }
    }
}

// ============================================================
// ЛР №12 Завдання 2: Банер дозволів (3 стани)
// ============================================================
@Composable
fun PermissionBanner(
    message: String,
    isDeniedPermanently: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isDeniedPermanently)
            MaterialTheme.colorScheme.errorContainer
        else
            MaterialTheme.colorScheme.tertiaryContainer,
        animationSpec = tween(400),
        label = "permBgColor"
    )
    val contentColor = if (isDeniedPermanently)
        MaterialTheme.colorScheme.onErrorContainer
    else
        MaterialTheme.colorScheme.onTertiaryContainer

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (isDeniedPermanently) Icons.Default.Lock else Icons.Default.Warning,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(message, style = MaterialTheme.typography.bodySmall, color = contentColor)
            }

            if (isDeniedPermanently) {
                // Стан 3: Постійно відхилено — кнопка «Перейти в налаштування»
                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Перейти в налаштування")
                }
            } else {
                // Стан 2: Відхилено — кнопка «Надати дозвіл»
                Button(
                    onClick = onRequest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Надати дозвіл")
                }
            }
        }
    }
}

// Обгортка-картка для секцій екрану
@Composable
fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider()
            content()
        }
    }
}

// ---- Previews ----
@Preview(showBackground = true, name = "Location Screen — Permission Denied")
@Composable
fun PermissionBannerPreview() {
    AppTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PermissionBanner(
                message = "Для визначення місцезнаходження потрібен дозвіл на геолокацію.",
                isDeniedPermanently = false,
                onRequest = {}, onOpenSettings = {}
            )
            PermissionBanner(
                message = "Дозвіл заблоковано. Відкрийте налаштування застосунку.",
                isDeniedPermanently = true,
                onRequest = {}, onOpenSettings = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Location Data Card")
@Composable
fun LocationDataCardPreview() {
    AppTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            LocationDataCard(
                state = LocationUiState.Success(
                    latitude = 50.4501,
                    longitude = 30.5234,
                    accuracy = 12.5f,
                    timestamp = System.currentTimeMillis(),
                    distanceToTarget = 2340f
                )
            )
        }
    }
}
