package com.example.myapplication7.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
// ЛР №13 Завдання 4: Імпорти для доступності (Accessibility)
// Role       — семантична роль елемента (Button, Image, Checkbox...)
//              TalkBack використовує роль для оголошення: "двічі торкніться для активації"
// contentDescription — текст, який озвучує TalkBack при фокусуванні на елементі
// semantics  — дозволяє вручну задавати семантичні властивості будь-якого Composable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication7.data.Concert
import com.example.myapplication7.ui.theme.AppTheme
import com.example.myapplication7.ui.viewmodels.BeatStreamViewModel
import com.example.myapplication7.ui.viewmodels.ConcertListUiState
import com.example.myapplication7.ui.viewmodels.UiEvent
import kotlinx.coroutines.launch

// ============================================================
// ЛР №11 Завдання 4: Pull-to-refresh через ViewModel → Repository → API
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConcertListTab(
    viewModel: BeatStreamViewModel,
    onConcertClick: (Int) -> Unit = {},
    onAddClick: () -> Unit = {}
) {
    val uiState by viewModel.listUiState.collectAsStateWithLifecycle()
    val isOperationInProgress by viewModel.isOperationInProgress.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // ЛР №9 Завдання 5: Підписка на одноразові події (Snackbar)
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> scope.launch {
                    snackbarHostState.showSnackbar(event.message)
                }
                else -> Unit
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BeatStream", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    // ЛР №13 Завдання 4: Кнопка «+» у TopAppBar — аудит доступності
                    // -------------------------------------------------------------------
                    // ПРОБЛЕМА (до виправлення): contentDescription містив гарbage-символи
                    // через проблеми кодування файлу. TalkBack озвучував нечитабельний текст.
                    //
                    // РІШЕННЯ:
                    // 1. contentDescription = "Додати концерт" — зрозумілий текст для TalkBack
                    //    Також використовується у UI-тесті (Завдання 2):
                    //    composeTestRule.onNodeWithContentDescription("Додати концерт")
                    //
                    // 2. Modifier.semantics { role = Role.Button } — явно вказуємо роль.
                    //    IconButton вже має семантику кнопки за замовчуванням, але
                    //    явне зазначення ролі гарантує коректне оголошення TalkBack:
                    //    "Додати концерт, Кнопка" (замість просто «Додати концерт»)
                    IconButton(
                        onClick = onAddClick,
                        enabled = !isOperationInProgress,
                        modifier = Modifier.semantics { role = Role.Button }
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Додати концерт"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (!isOperationInProgress) onAddClick() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                // ЛР №13 Завдання 4: FAB — contentDescription для TalkBack
                // -------------------------------------------------------------------
                // FloatingActionButton не має вбудованого contentDescription.
                // Без нього TalkBack просто озвучує «Кнопка» — незрозуміло для користувача.
                //
                // Окремий опис від кнопки «+» у TopAppBar: "Додати новий концерт"
                // (не "Додати концерт") — щоб TalkBack розрізняв два елементи.
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Додати новий концерт"
                )
            }
        }
    ) { padding ->
        // ЛР №11 Завдання 4: PullToRefreshBox
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshConcerts() },
            modifier = Modifier.padding(padding),
            state = rememberPullToRefreshState()
        ) {
            when (val state = uiState) {
                is ConcertListUiState.Loading -> LoadingScreen()
                is ConcertListUiState.Error -> ErrorScreen(
                    message = state.message,
                    onRetry = { viewModel.loadConcerts() }
                )
                is ConcertListUiState.Empty -> EmptyScreen(onAddClick = onAddClick)
                is ConcertListUiState.Success -> ConcertListScreen(
                    concerts = state.concerts,
                    isOffline = state.isOffline,
                    isOperationInProgress = isOperationInProgress,
                    favoriteIds = favoriteIds,
                    onConcertClick = onConcertClick,
                    onDeleteConcert = { id -> viewModel.deleteConcert(id) },
                    onToggleFavorite = { id -> viewModel.toggleFavorite(id) }
                )
            }
        }
    }
}

// ЛР №9 Завдання 5: Екран помилки з кнопкою «Повторити»
@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "Помилка завантаження",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Повторити")
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
            Spacer(Modifier.height(16.dp))
            Text("Завантаження...", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun EmptyScreen(onAddClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline)
            Text("Список порожній", style = MaterialTheme.typography.titleMedium)
            Button(onClick = onAddClick) { Text("Додати перший концерт") }
        }
    }
}

// ============================================================
// ЛР №11 Завдання 1+3: Список з анімованою появою/зникненням та swipe-to-dismiss
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConcertListScreen(
    concerts: List<Concert>,
    isOffline: Boolean = false,
    isOperationInProgress: Boolean = false,
    favoriteIds: Set<Int> = emptySet(),
    onConcertClick: (Int) -> Unit = {},
    onDeleteConcert: (Int) -> Unit = {},
    onToggleFavorite: (Int) -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize()) {

        // ЛР №9 Завдання 5: Банер офлайн-режиму
        AnimatedVisibility(visible = isOffline) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    "Офлайн-режим. Відображаються збережені дані.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ЛР №11 Завдання 1: Анімована поява елементів
            items(concerts, key = { it.id }) { concert ->
                // ЛР №11 Завдання 1: AnimatedVisibility для кожного елемента списку
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(400)) +
                            slideInVertically(animationSpec = tween(400)) { it / 2 },
                    exit = fadeOut(animationSpec = tween(300)) +
                            shrinkVertically(animationSpec = tween(300))
                ) {
                    // ЛР №11 Завдання 3: Повноцінний swipe-to-dismiss з візуальним зворотним зв'язком
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            if (dismissValue == SwipeToDismissBoxValue.EndToStart && !isOperationInProgress) {
                                onDeleteConcert(concert.id)
                                true
                            } else false
                        }
                    )

                    // Анімуємо колір фону залежно від targetValue свайпу
                    val backgroundColor by animateColorAsState(
                        targetValue = when (dismissState.targetValue) {
                            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.errorContainer
                        },
                        animationSpec = tween(300),
                        label = "swipeBackground"
                    )
                    val iconScale by animateFloatAsState(
                        targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) 1.2f else 1f,
                        animationSpec = tween(300),
                        label = "iconScale"
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(backgroundColor)
                                    .padding(end = 24.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.scale(iconScale)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        // ЛР №13 Завдання 4: contentDescription для іконки у фоні свайпу
                                        // Ця іконка є частиною SwipeToDismissBox backgroundContent — вона
                                        // відображається при свайпі вліво. TalkBack теж читає фон.
                                        contentDescription = "Видалити",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Text(
                                        "Видалити",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    ) {
                        // ЛР №11 Завдання 5: Довге натискання з контекстним меню
                        ConcertCard(
                            concert = concert,
                            isOperationInProgress = isOperationInProgress,
                            isFavorite = concert.id in favoriteIds,
                            onClick = { onConcertClick(concert.id) },
                            onDelete = { onDeleteConcert(concert.id) },
                            onToggleFavorite = { onToggleFavorite(concert.id) }
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
// ЛР №11 Завдання 5: ConcertCard з довгим натисканням і контекстним меню
// ============================================================
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ConcertCard(
    concert: Concert,
    isOperationInProgress: Boolean = false,
    isFavorite: Boolean = false,
    onClick: () -> Unit = {},
    onDelete: () -> Unit = {},
    onToggleFavorite: () -> Unit = {}
) {
    // Стан відображення контекстного меню (довге натискання)
    var showContextMenu by remember { mutableStateOf(false) }

    // Анімація кольору карточки при виборі "обраного"
    val cardColor by animateColorAsState(
        targetValue = if (isFavorite)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else
            MaterialTheme.colorScheme.surface,
        animationSpec = tween(400),
        label = "cardColor"
    )

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            // ЛР №13 Завдання 4: semantics(mergeDescendants=true) — головне виправлення доступності
            // -------------------------------------------------------------------
            // ПРОБЛЕМА (до виправлення):
            // TalkBack проходив по кожному елементу картки окремо:
            //   [1] Іконка PlayArrow → [2] Текст "Тхе Weeкнд" → [3] Іконка Delete →
            //   [4] Текст "Афter Hours Live" → [5] Дата... — 5+ фокусів на одну картку!
            //
            // РІШЕННЯ: semantics(mergeDescendants = true)
            //   Об'єднує всіх нащадків картки в ОДИН семантичний вузол.
            //   contentDescription на рівні картки перевизначає автоматичне.
            //
            //   ДО: TalkBack читає "Тхe Weeknd", зотім "After Hours Live", зотім дату...
            //   ПІСЛЯ: TalkBack читає одне речення:
            //   "Концерт The Weeknd: After Hours Live, 2024-05-12"
            .semantics(mergeDescendants = true) {
                contentDescription = "Концерт ${concert.artist}: ${concert.title}, ${concert.date}"
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showContextMenu = true }
            ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    // ЛР №13 Завдання 4: contentDescription = null — декоративна іконка
                    // Іконка PlayArrow несе лише візуальне значення ("це концерт"), а не передає
                    // унікальної інформації. null = TalkBack ігнорує цю іконку.
                    // Правило: декоративні елементи, що не несуть інформації — null.
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = concert.artist,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                // ЛР №13 Завдання 4: іконка «Обране» — смислова, озвучується TalkBack
                if (isFavorite) {
                    Icon(
                        Icons.Default.Favorite,
                        // ЛР №13 Завдання 4: смислова іконка — contentDescription
                        // Іконка Favorite передає інформацію: концерт додано до улюблених.
                        // Без contentDescription TalkBack би ігнорував її (null) або
                        // озвучував назву ресурсу доступу незрозуміло.
                        contentDescription = "Концерт в обраному",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                }
                // ЛР №13 Завдання 4: кнопка видалення — детальний contentDescription
                // -------------------------------------------------------------------
                // contentDescription = "Видалити концерт ${concert.artist}"
                // Містить ім'я виконавця — TalkBack озвучує:
                // "Видалити концерт The Weeknd, Кнопка"
                // це уникає неоднозначності: користувач знає для якого концерту видаляє.
                IconButton(
                    onClick = onDelete,
                    enabled = !isOperationInProgress,
                    modifier = Modifier.semantics { role = Role.Button }
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Видалити концерт ${concert.artist}",
                        tint = if (isOperationInProgress) MaterialTheme.colorScheme.outline
                        else MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = concert.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            concert.venue?.let { venue ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "📍 $venue",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SuggestionChip(
                    onClick = { },
                    label = { Text(concert.genre) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null,
                        modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = concert.date,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }

    // ЛР №11 Завдання 5: Контекстне меню після довгого натискання
    if (showContextMenu) {
        AlertDialog(
            onDismissRequest = { showContextMenu = false },
            title = {
                Text(
                    concert.artist,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Оберіть дію:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider()
                    // Дія 1: Додати до обраного / Видалити з обраного
                    TextButton(
                        onClick = {
                            onToggleFavorite()
                            showContextMenu = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            if (isFavorite) Icons.Default.FavoriteBorder else Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isFavorite) "Видалити з обраного" else "Додати до обраного",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Дія 2: Видалити концерт
                    TextButton(
                        onClick = {
                            onDelete()
                            showContextMenu = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Видалити концерт", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showContextMenu = false }) {
                    Text("Скасувати")
                }
            }
        )
    }
}

// --- Previews для всіх станів (вимога ЛР №9 та №11) ---
val mockConcerts = listOf(
    Concert(1, "The Weeknd", "After Hours Live", "2024-05-12", "R&B", "Kyiv Arena"),
    Concert(2, "Dua Lipa", "Future Nostalgia Tour", "2024-06-20", "Pop"),
    Concert(3, "Imagine Dragons", "Mercury World Tour", "2024-08-05", "Rock", "Lviv Palace")
)

@Preview(showBackground = true, name = "Success State")
@Composable
fun ConcertListSuccessPreview() {
    AppTheme(darkTheme = false) {
        ConcertListScreen(concerts = mockConcerts, isOffline = false)
    }
}

@Preview(showBackground = true, name = "Offline Banner")
@Composable
fun ConcertListOfflinePreview() {
    AppTheme(darkTheme = false) {
        ConcertListScreen(concerts = mockConcerts, isOffline = true)
    }
}

@Preview(showBackground = true, name = "Error State")
@Composable
fun ErrorScreenPreview() {
    AppTheme(darkTheme = false) {
        ErrorScreen(message = "Немає підключення до мережі", onRetry = {})
    }
}

@Preview(showBackground = true, name = "Loading State")
@Composable
fun LoadingScreenPreview() {
    AppTheme(darkTheme = false) {
        LoadingScreen()
    }
}

@Preview(showBackground = true, name = "Empty State")
@Composable
fun EmptyScreenPreview() {
    AppTheme(darkTheme = false) {
        EmptyScreen(onAddClick = {})
    }
}

@Preview(showBackground = true, name = "Dark Mode")
@Composable
fun ConcertListDarkPreview() {
    AppTheme(darkTheme = true) {
        ConcertListScreen(concerts = mockConcerts, isOffline = false, favoriteIds = setOf(1))
    }
}

// ЛР №11: Preview контекстного меню
@Preview(showBackground = true, name = "Concert Card with Favorite")
@Composable
fun ConcertCardFavoritePreview() {
    AppTheme(darkTheme = false) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ConcertCard(concert = mockConcerts[0], isFavorite = true)
            ConcertCard(concert = mockConcerts[1], isFavorite = false)
        }
    }
}