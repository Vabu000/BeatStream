package com.example.myapplication7.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
// ЛР №13 Завдання 4: Імпорти для доступності (Accessibility)
// Role      — семантична роль елемента. Визначає як TalkBack описує елемент:
//   Role.Button  → "двічі торкніться для активації"
//   Role.Image   → оголошується як зображення
// role      — властивість у Modifier.semantics { role = Role.Button }
// semantics — дозволяє додати або перевизначити семантику будь-якого Composable
//
// ЩО БУЛО ВИПРАВЛЕНО в рамках ЛР №13 Завдання 4:
//   1. Кнопка "Назад" → додано Role.Button + зрозумілий contentDescription
//   2. AsyncImage → додано contentDescription з ім'ям виконавця
//   3. Row (розгортання секції) → додано Role.Button (TalkBack знає що можна натиснути)
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.myapplication7.data.Concert
import com.example.myapplication7.ui.theme.AppTheme
import com.example.myapplication7.ui.viewmodels.BeatStreamViewModel
import com.example.myapplication7.ui.viewmodels.ConcertDetailUiState
import java.io.File

// ЛР №9 Завдання 3: Екран деталей — дані завантажуються з мережі за ID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    concertId: Int,
    viewModel: BeatStreamViewModel,
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.detailUiState.collectAsStateWithLifecycle()

    // Завантажуємо деталі при відкритті екрану
    LaunchedEffect(concertId) {
        viewModel.loadConcertDetail(concertId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Деталі концерту") },
                navigationIcon = {
                    // ЛР №13 Завдання 4: Кнопка "Назад" — повний аудит доступності
                    // -------------------------------------------------------------------
                    // ПРОБЛЕМА (до виправлення):
                    //   contentDescription містив нечитабельні символи через кодування файлу.
                    //   TalkBack озвучував щось на кшталт "Н з".
                    //
                    // РІШЕННЯ:
                    //   contentDescription = "Повернутись на попередній екран"
                    //     — описує ДІЮ (що відбудеться), а не вигляд іконки.
                    //     Добра практика: "Назад" → краще "Повернутись на попередній екран"
                    //
                    //   modifier = Modifier.semantics { role = Role.Button }
                    //     — TalkBack оголошує: "Повернутись на попередній екран, Кнопка"
                    //     — без Role.Button озвучувалось тільки: "Повернутись на попередній екран"
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { role = Role.Button }
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Повернутись на попередній екран"
                        )
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is ConcertDetailUiState.Loading -> LoadingScreen()
                is ConcertDetailUiState.Error -> ErrorScreen(
                    message = state.message,
                    onRetry = { viewModel.loadConcertDetail(concertId) }
                )
                is ConcertDetailUiState.Success -> ConcertDetailContent(concert = state.concert)
            }
        }
    }
}

@Composable
fun ConcertDetailContent(concert: Concert) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ЛР №12 Завдання 3: Відображення фото концерту (якщо є)
        concert.imagePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = file,
                        // ЛР №13 Завдання 4: contentDescription для AsyncImage
                        // -------------------------------------------------------------------
                        // AsyncImage відображає зображення — нетекстовий контент.
                        // За WCAG 1.1.1 (Текстова альтернатива): будь-який нетекстовий
                        // контент має мати текстовий еквівалент.
                        //
                        // contentDescription = "Фотографія концерту ${concert.artist}"
                        // Містить ім'я виконавця — TalkBack озвучує:
                        //   "Фотографія концерту The Weeknd"
                        // замість загального "Зображення" або мовчання.
                        contentDescription = "Фотографія концерту ${concert.artist}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Виконавець
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Person, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Text(text = concert.artist, style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface)
        }

        HorizontalDivider()

        // Назва
        DetailRow(icon = Icons.Default.Star, label = "Назва", value = concert.title)

        // Дата
        DetailRow(icon = Icons.Default.DateRange, label = "Дата", value = concert.date)

        // Жанр
        DetailRow(icon = Icons.Default.List, label = "Жанр", value = concert.genre)

        // Місце (nullable поле)
        concert.venue?.let { venue ->
            DetailRow(icon = Icons.Default.LocationOn, label = "Місце проведення", value = venue)
        }

        // Опис (nullable поле)
        concert.description?.let { desc ->
            Spacer(Modifier.height(4.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Опис", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.height(8.dp))
                    Text(desc, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }

        // ============================================================
        // ЛР №11 Завдання 2: Анімована розгортувана секція «Додаткова інформація»
        // ============================================================
        ExpandableInfoSection(concert = concert)
    }
}

// ЛР №11 Завдання 2: Розгортувана секція з анімацією кольору заголовка, повороту стрілки та видимості контенту
@Composable
fun ExpandableInfoSection(concert: Concert) {
    var isExpanded by remember { mutableStateOf(false) }

    // Анімація повороту іконки-стрілки (0° → 180°)
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "arrowRotation"
    )

    // Анімація кольору фону заголовка секції
    val headerBackground by animateColorAsState(
        targetValue = if (isExpanded)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(durationMillis = 400),
        label = "headerBackground"
    )
    val headerContentColor by animateColorAsState(
        targetValue = if (isExpanded)
            MaterialTheme.colorScheme.onPrimaryContainer
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 400),
        label = "headerContentColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // ЛР №13 Завдання 4: Розгортувана секція — Row з Role.Button
            // -------------------------------------------------------------------
            // ПРОБЛЕМА (до виправлення):
            // Row з .clickable { } — візуально кнопка, але TalkBack не знав:
            //   "це клікабельний елемент чи просто текстова група?"
            // TalkBack не оголошував "двічі торкніться для активації".
            //
            // РІШЕННЯ: .semantics { role = Role.Button }
            // TalkBack тепер оголошує:
            //   "Додаткова інформація, Кнопка — двічі торкніться для активації"
            // Користувач з вадами зору розуміє що може розгорнути секцію.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(if (isExpanded) RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                          else RoundedCornerShape(12.dp))
                    .background(headerBackground)
                    // Role.Button → TalkBack оголошує "Кнопка" і підказує про подвійний тап
                    .semantics { role = Role.Button }
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = headerContentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Додаткова інформація",
                    style = MaterialTheme.typography.titleSmall,
                    color = headerContentColor,
                    modifier = Modifier.weight(1f)
                )
                // Іконка-стрілка з анімацією повороту на 180°
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Згорнути" else "Розгорнути",
                    tint = headerContentColor,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(arrowRotation)
                )
            }

            // Контент секції з анімацією появи/зникнення
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(400)) +
                        fadeIn(animationSpec = tween(400)),
                exit = shrinkVertically(animationSpec = tween(300)) +
                        fadeOut(animationSpec = tween(300))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 4 додаткові поля відповідно до тематики (концерти)
                    ExpandableDetailRow(
                        icon = Icons.Default.Star,
                        label = "Тривалість шоу",
                        value = "~2 години 30 хвилин"
                    )
                    ExpandableDetailRow(
                        icon = Icons.Default.Person,
                        label = "Вікове обмеження",
                        value = "16+"
                    )
                    ExpandableDetailRow(
                        icon = Icons.Default.ShoppingCart,
                        label = "Вартість квитка",
                        value = "від 800 до 3500 грн"
                    )
                    ExpandableDetailRow(
                        icon = Icons.Default.Phone,
                        label = "Контакти організатора",
                        value = "+380 44 123 45 67"
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        "Придбати квитки можна на офіційному сайті або в касах арени.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ExpandableDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline)
            Text(value, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null,
            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline)
            Text(value, style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// ============================================================
// Preview для ЛР №11 Завдання 2: Розгортувана секція
// ============================================================
@Preview(showBackground = true, name = "Detail Success")
@Composable
fun DetailSuccessPreview() {
    AppTheme(darkTheme = false) {
        ConcertDetailContent(
            concert = Concert(
                id = 1, artist = "The Weeknd", title = "After Hours Live",
                date = "2024-05-12", genre = "R&B",
                venue = "Kyiv Arena, Kyiv",
                description = "Грандіозне шоу з новою програмою та спецефектами."
            )
        )
    }
}

@Preview(showBackground = true, name = "Expandable Section — Expanded")
@Composable
fun ExpandableSectionExpandedPreview() {
    AppTheme(darkTheme = false) {
        Column(modifier = Modifier.padding(16.dp)) {
            ExpandableInfoSection(
                concert = Concert(
                    id = 1, artist = "Dua Lipa", title = "Future Nostalgia Tour",
                    date = "2024-06-20", genre = "Pop"
                )
            )
        }
    }
}

@Preview(showBackground = true, name = "Detail Loading")
@Composable
fun DetailLoadingPreview() {
    AppTheme { LoadingScreen() }
}

@Preview(showBackground = true, name = "Detail Error")
@Composable
fun DetailErrorPreview() {
    AppTheme { ErrorScreen(message = "Концерт не знайдено", onRetry = {}) }
}