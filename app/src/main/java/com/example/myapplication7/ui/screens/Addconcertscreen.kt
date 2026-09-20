package com.example.myapplication7.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication7.ui.theme.AppTheme
import com.example.myapplication7.ui.viewmodels.BeatStreamViewModel
import com.example.myapplication7.ui.viewmodels.UiEvent
import kotlinx.coroutines.launch

// ЛР №13 Завдання 4: Імпорти для доступності (Accessibility)
// Role      — семантична роль елемента для TalkBack
// role      — властивість в Modifier.semantics { role = Role.Button }
// semantics — дозволяє задавати семантику будь-якому Composable
//
// ЩО БУЛО ВИПРАВЛЕНО в рамках ЛР №13 Завдання 4:
//   1. Кнопка "Назад" → Role.Button + зрозумілий contentDescription
//   2. Поле дати → label українською ("Дата (рррр-мм-дд) *")
//      це критично для UI-тесту: composeTestRule.onNodeWithText("Дата (рррр-мм-дд) *")
// ============================================================
// ЛР №9 Завдання 4: Екран додавання концерту (POST-запит)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddConcertScreen(
    viewModel: BeatStreamViewModel,
    onBack: () -> Unit = {}
) {
    val isOperationInProgress by viewModel.isOperationInProgress.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Поля форми
    var artist by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }
    var venue by remember { mutableStateOf("") }

    // Помилки валідації
    var artistError by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf(false) }
    var dateError by remember { mutableStateOf(false) }
    var genreError by remember { mutableStateOf(false) }

    // Підписка на NavigateBack
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvent.NavigateBack -> onBack()
                is UiEvent.ShowSnackbar -> scope.launch {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Додати концерт") },
                navigationIcon = {
                    // ЛР №13 Завдання 4: кнопка "Назад" — доступність
                    // -------------------------------------------------------------------
                    // Ця кнопка також знаходиться у UI-тесті (Завдання 2):
                    //   composeTestRule.onNodeWithContentDescription("Назад").performClick()
                    //
                    // Role.Button → TalkBack оголошує: "Назад, Кнопка"
                    // і підказує "двічі торкніться для активації"
                    IconButton(
                        onClick = onBack,
                        enabled = !isOperationInProgress,
                        modifier = Modifier.semantics { role = Role.Button }
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Новий концерт", style = MaterialTheme.typography.titleLarge)

            // Поле: Виконавець
            OutlinedTextField(
                value = artist,
                onValueChange = { artist = it; artistError = false },
                label = { Text("Виконавець *") },
                modifier = Modifier.fillMaxWidth(),
                isError = artistError,
                supportingText = if (artistError) {{ Text("Обов'язкове поле") }} else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                enabled = !isOperationInProgress
            )

            // Поле: Назва концерту
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; titleError = false },
                label = { Text("Назва концерту *") },
                modifier = Modifier.fillMaxWidth(),
                isError = titleError,
                supportingText = if (titleError) {{ Text("Обов'язкове поле") }} else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                enabled = !isOperationInProgress
            )

            // ЛР №13 Завдання 4: Поле "Дата" — локалізований label українською
            // -------------------------------------------------------------------
            // ПРОБЛЕМА (до виправлення):
            //   label = { Text("Дата *") } — гарbage-символи через проблеми кодування,
            //   TalkBack озвучував нечитабельний текст.
            //
            // РІШЕННЯ:
            //   label = { Text("Дата (рррр-мм-дд) *") }
            //   1. Зрозумілий текст для TalkBack
            //   2. Опис формату в лейблі — WCAG 3.3.2 (Labels or Instructions)
            //   3. Використовується в UI-тесті (Завдання 2):
            //      composeTestRule.onNodeWithText("Дата (рррр-мм-дд) *").performTextInput("2025-07-15")
            OutlinedTextField(
                value = date,
                onValueChange = { date = it; dateError = false },
                label = { Text("Дата (рррр-мм-дд) *") },
                modifier = Modifier.fillMaxWidth(),
                isError = dateError,
                supportingText = if (dateError) {{ Text("Обов'язкове поле") }} else null,
                placeholder = { Text("2024-12-31") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                enabled = !isOperationInProgress
            )

            // Поле: Жанр
            OutlinedTextField(
                value = genre,
                onValueChange = { genre = it; genreError = false },
                label = { Text("Жанр *") },
                modifier = Modifier.fillMaxWidth(),
                isError = genreError,
                supportingText = if (genreError) {{ Text("Обов'язкове поле") }} else null,
                placeholder = { Text("Rock, Pop, R&B...") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                enabled = !isOperationInProgress
            )

            // Поле: Місце проведення (необов'язкове)
            OutlinedTextField(
                value = venue,
                onValueChange = { venue = it },
                label = { Text("Місце проведення (необов'язково)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                enabled = !isOperationInProgress
            )

            Spacer(Modifier.height(8.dp))

            // ЛР №13 Завдання 2 (взаємозв'язок з UI-тестом) + Завдання 4:
            // Кнопка "Зберегти" — ключовий елемент обох завдань
            // -------------------------------------------------------------------
            // UI-тест (Завдання 2) використовує:
            //   composeTestRule.onNodeWithText("Зберегти").performClick()
            // → запускає валідацію полів і перевіряє відображення помилок
            //
            // Button (Матеріал 3) вже має Role.Button за замовчуванням —
            // не потребує додаткової анотації.
            //
            // ВАЛІДАЦІЯ: виконується перед POST-запитом:
            //   artistError = artist.isBlank()  → якщо порожнє — isError=true в полі
            //   titleError  = title.isBlank()   → OutlinedTextField показує підтримку,
            //   dateError   = date.isBlank()    → supportingText "Обов'язкове поле"
            //   genreError  = genre.isBlank()   → UI-тест (Завдання 2) перевіряє це
            Button(
                onClick = {
                    // Валідація — перевіряємо обов'язкові поля перед відправкою запиту
                    artistError = artist.isBlank()
                    titleError = title.isBlank()
                    dateError = date.isBlank()
                    genreError = genre.isBlank()

                    if (!artistError && !titleError && !dateError && !genreError) {
                        viewModel.addConcert(
                            artist = artist,
                            title = title,
                            date = date,
                            genre = genre,
                            venue = venue.ifBlank { null }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !isOperationInProgress
            ) {
                if (isOperationInProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Збереження...")
                } else {
                    Text("Зберегти")
                }
            }

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isOperationInProgress
            ) {
                Text("Скасувати")
            }
        }
    }
}

@Preview(showBackground = true, name = "Add Concert Form")
@Composable
fun AddConcertPreview() {
    AppTheme(darkTheme = false) {
        // Статичний preview форми
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Новий концерт", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(value = "The Weeknd", onValueChange = {}, label = { Text("Виконавець *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = "After Hours Live", onValueChange = {}, label = { Text("Назва концерту *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = "2024-05-12", onValueChange = {}, label = { Text("Дата *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = "R&B", onValueChange = {}, label = { Text("Жанр *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = "Kyiv Arena", onValueChange = {}, label = { Text("Місце (необов'язково)") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Зберегти") }
        }
    }
}
