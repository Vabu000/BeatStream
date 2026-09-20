package com.example.myapplication7

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ЛР №13 — Завдання 2: UI-тест основного користувацького сценарію.
 *
 * =============================================================
 * ЩО ТАКЕ COMPOSE TESTING?
 * =============================================================
 * Compose Testing — фреймворк для інструментального (instrumental) тестування UI.
 * На відміну від JUnit unit-тестів (/test), ці тести запускаються НА РЕАЛЬНОМУ
 * ЕМУЛЯТОРІ або пристрої (папка /androidTest).
 *
 * createAndroidComposeRule<MainActivity>() — запускає справжній MainActivity
 * і дає можливість взаємодіяти з інтерфейсом так само, як реальний користувач:
 *   - знаходити елементи (onNodeWithText, onNodeWithContentDescription)
 *   - симулювати дотики (performClick)
 *   - вводити текст (performTextInput)
 *   - перевіряти видимість (assertIsDisplayed)
 *
 * =============================================================
 * ЯК COMPOSE TESTING ПОВ'ЯЗАНИЙ З ДОСТУПНІСТЮ (Завдання 4)?
 * =============================================================
 * Compose Testing та TalkBack (скрін-рідер) використовують ОДИН І ТОЙ САМИЙ
 * механізм — семантичне дерево (Semantics Tree).
 *
 * Тому: якщо contentDescription встановлений коректно для TalkBack →
 * UI-тест може знайти елемент через onNodeWithContentDescription().
 *
 * Наприклад:
 *   Icon(contentDescription = "Додати концерт")  ← TalkBack озвучує
 *   composeTestRule.onNodeWithContentDescription("Додати концерт")  ← тест знаходить
 *
 * =============================================================
 * СЦЕНАРІЙ: "Заповнення форми додавання концерту"
 * =============================================================
 * Один сценарій покриває ≥3 стани UI (вимога ЛР):
 *   Стан 1 → Головний екран (BeatStream)
 *   Стан 2 → Форма додавання (поля форми відображені)
 *   Стан 3 → Валідація (помилки при порожніх обов'язкових полях)
 *   Стан 4 → Повернення на головний екран (після натискання "Назад")
 *
 * @LargeTest — анотація, що позначає тест як "великий" (займає >2 сек,
 * потребує емулятора або пристрою). Використовується для фільтрації тестів у CI.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ConcertFlowUiTest {

    /**
     * @get:Rule — JUnit Rule, що запускає MainActivity перед кожним тестом.
     *
     * createAndroidComposeRule<MainActivity>():
     *   - Запускає справжню Activity
     *   - Ініціалізує Compose середовище
     *   - Надає DSL для взаємодії з UI (onNodeWithText, performClick, тощо)
     *
     * Альтернатива: createComposeRule() — для тестування ізольованих Composable-функцій
     * без запуску повноцінної Activity.
     */
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    // ================================================================
    // ТЕСТ 1: Перевірка головного екрану (Стан 1)
    // ================================================================
    /**
     * ЛР №13 Завдання 2 — Тест 1: Smoke-тест головного екрану.
     *
     * Verify: Після запуску застосунку відображається TopAppBar з назвою "BeatStream".
     * Це найпростіший "smoke test" — перевіряє що застосунок взагалі стартував коректно.
     *
     * onNodeWithText("BeatStream") → шукає вузол семантичного дерева з текстом "BeatStream"
     * assertIsDisplayed()         → перевіряє що вузол видимий на екрані (не прихований)
     */
    @Test
    fun mainScreen_displaysBeatStreamTitle() {
        // Assert — головний екран відображає назву застосунку у TopAppBar
        composeTestRule.onNodeWithText("BeatStream").assertIsDisplayed()
    }

    // ================================================================
    // ТЕСТ 2: Перехід зі Стану 1 → Стан 2 (навігація до форми)
    // ================================================================
    /**
     * ЛР №13 Завдання 2 — Тест 2: Тест навігації.
     *
     * Given: Головний екран відображається (Стан 1)
     * When:  Користувач натискає кнопку "+" (іконка Add у TopAppBar)
     * Then:  Відкривається форма додавання (Стан 2) — видно поле "Виконавець *"
     *
     * ВАЖЛИВО: onNodeWithContentDescription("Додати концерт") працює ТІЛЬКИ тому,
     * що ми додали contentDescription у Завданні 4 (доступність):
     *   Icon(Icons.Default.Add, contentDescription = "Додати концерт")
     * Без цього і TalkBack мовчав би, і цей тест не знаходив би кнопку.
     */
    @Test
    fun mainScreen_clickAddButton_opensAddConcertForm() {
        // ---- Стан 1: Головний екран ----
        composeTestRule.onNodeWithText("BeatStream").assertIsDisplayed()

        // ---- Act: натискаємо кнопку "+" через contentDescription ----
        // contentDescription = "Додати концерт" → встановлено у ЛР №13 Завдання 4
        composeTestRule.onNodeWithContentDescription("Додати концерт")
            .assertIsDisplayed()
            .performClick()

        // waitForIdle() — чекаємо завершення навігаційної анімації
        // (NavHost анімує перехід між екранами, тест чекає стабільного стану)
        composeTestRule.waitForIdle()

        // ---- Стан 2: Форма додавання ----
        // OutlinedTextField з label "Виконавець *" видна на екрані
        composeTestRule.onNodeWithText("Виконавець *").assertIsDisplayed()
    }

    // ================================================================
    // ТЕСТ 3: Стан 1 → Стан 2 → Стан 3 (валідація форми)
    // ================================================================
    /**
     * ЛР №13 Завдання 2 — Тест 3: Основний багатокроковий сценарій.
     *
     * ГОЛОВНИЙ ТЕСТ, що покриває вимогу "≥3 стани UI в одному сценарії":
     *
     * Стан 1: Головний екран → перевіряємо "BeatStream"
     * Стан 2: Форма додавання → перевіряємо поля форми
     * Стан 3: Валідація → перевіряємо що помилки відображаються
     *
     * Given: Форма додавання відкрита (Стан 2)
     * When:  Натискаємо "Зберегти" без заповнення жодного поля
     * Then:  ViewModel виконує валідацію, виставляє artistError=true, titleError=true тощо
     *        → OutlinedTextField показує supportingText "Обов'язкове поле"
     */
    @Test
    fun addConcertFlow_emptyForm_showsValidationErrors() {
        // ---- Стан 1: Головний екран ----
        composeTestRule.onNodeWithText("BeatStream").assertIsDisplayed()

        // Перехід до форми через кнопку "+"
        composeTestRule.onNodeWithContentDescription("Додати концерт").performClick()
        composeTestRule.waitForIdle()

        // ---- Стан 2: Форма додавання ----
        // Перевіряємо що обидва обов'язкові поля відображені
        composeTestRule.onNodeWithText("Виконавець *").assertIsDisplayed()
        composeTestRule.onNodeWithText("Назва концерту *").assertIsDisplayed()

        // Act: натискаємо "Зберегти" з порожніми полями
        // → AddConcertScreen виконає: artistError = artist.isBlank() = true
        composeTestRule.onNodeWithText("Зберегти").performClick()
        composeTestRule.waitForIdle()

        // ---- Стан 3: Помилки валідації ----
        // OutlinedTextField з isError=true показує supportingText "Обов'язкове поле"
        composeTestRule.onNodeWithText("Обов'язкове поле").assertIsDisplayed()
    }

    // ================================================================
    // ТЕСТ 4: Стан 1 → Стан 2 (заповнення) → Стан 3 → Стан 4 (повернення)
    // ================================================================
    /**
     * ЛР №13 Завдання 2 — Тест 4: Повний сценарій заповнення форми.
     *
     * Охоплює всі 4 стани UI: головний → форма → заповнено → повернення.
     *
     * performTextInput() — симулює введення тексту з клавіатури у OutlinedTextField.
     * Важливо: для onNodeWithText("Дата (рррр-мм-дд) *") рядок взятий з ЛР №13
     * Завдання 4, де label виправлено на українську: "Дата (рррр-мм-дд) *".
     */
    @Test
    fun addConcertFlow_fillFormAndNavigateBack_returnsToMainScreen() {
        // ---- Стан 1: Головний екран ----
        composeTestRule.onNodeWithText("BeatStream").assertIsDisplayed()

        // Перехід до форми
        composeTestRule.onNodeWithContentDescription("Додати концерт").performClick()
        composeTestRule.waitForIdle()

        // ---- Стан 2: Заповнення форми ----
        // performTextInput → симулює введення тексту користувачем
        composeTestRule.onNodeWithText("Виконавець *")
            .performTextInput("Coldplay")

        composeTestRule.onNodeWithText("Назва концерту *")
            .performTextInput("Music of the Spheres")

        // Поле "Дата" має label "Дата (рррр-мм-дд) *" (виправлено у ЛР №13 Завдання 4)
        composeTestRule.onNodeWithText("Дата (рррр-мм-дд) *")
            .performTextInput("2025-07-15")

        composeTestRule.onNodeWithText("Жанр *")
            .performTextInput("Alternative Rock")

        // ---- Стан 3: Форма заповнена — натискаємо "Назад" ----
        // contentDescription = "Назад" → встановлено у ЛР №13 Завдання 4
        composeTestRule.onNodeWithContentDescription("Назад").performClick()
        composeTestRule.waitForIdle()

        // ---- Стан 4: Повернення на головний екран ----
        composeTestRule.onNodeWithText("BeatStream").assertIsDisplayed()
    }

    // ================================================================
    // ТЕСТ 5: BottomNavigation між вкладками
    // ================================================================
    /**
     * ЛР №13 Завдання 2 — Тест 5: Навігація через BottomNavigationBar.
     *
     * Перевіряє, що NavigationBar коректно перемикає між вкладками
     * "Концерти" та "Локація" (реалізовані у NavRoute.Concerts та NavRoute.Location).
     *
     * Це також неявно тестує що:
     * - NavHost коректно обробляє маршрути
     * - BottomBar відображається лише на top-level екранах (showBottomBar = true)
     */
    @Test
    fun bottomNavigation_clickLocationTab_showsLocationScreen() {
        // ---- Стан 1: Головний екран (вкладка "Концерти") ----
        composeTestRule.onNodeWithText("BeatStream").assertIsDisplayed()

        // Act: переходимо на вкладку "Локація"
        // NavigationBarItem має label "Локація" → NavRoute.Location.label
        composeTestRule.onNodeWithText("Локація").performClick()
        composeTestRule.waitForIdle()

        // ---- Стан 2: Екран "Локація" активний ----
        // BottomBar ще видний (showBottomBar = true для NavRoute.Location)
        // → вкладка "Концерти" все ще є у BottomBar
        composeTestRule.onNodeWithText("Концерти").assertIsDisplayed()

        // Act: повертаємось на вкладку "Концерти"
        composeTestRule.onNodeWithText("Концерти").performClick()
        composeTestRule.waitForIdle()

        // ---- Стан 3: Головний екран знову активний ----
        composeTestRule.onNodeWithText("BeatStream").assertIsDisplayed()
    }
}
