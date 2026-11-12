# core/problemview API

The problem view features embed as a page or detail pane: a cached statement shown at once and refreshed in the background, with the EN/RU toggle, text size and reading width (spec §7.4).

Package `app.pocketcp.core.problemview`. Add with `implementation(project(":core:problemview"))`; it exposes
`core/model` and lifecycle's `viewModel { }` (lifecycle-viewmodel-compose) as `api`.

```kotlin
class FooEntry @Inject constructor(private val problems: ProblemViewModelFactory) : FeatureEntry {
    @Composable private fun Problem(id: ProblemId, onBack: (() -> Unit)?) {
        val viewModel = viewModel(key = id.display) { problems.create(id) }
        ProblemPane(viewModel, onBack)
    }
}
```

| Symbol | What it is |
|--------|-----------|
| `ProblemViewModelFactory` (`@Inject`) | `create(problem: ProblemId): ProblemViewModel`. Features never construct the view model themselves (ADR 0012 addendum). |
| `ProblemViewModel` | `state: StateFlow<ProblemUiState>` from `StatementRepository.statement(problem, settings.statementLanguage)` and the reading settings, read from construction on. Implements `ProblemActions`. |
| `ProblemActions` | `setLanguage(StatementLanguage)`, `setTextSize(TextSizePreference)`, `setWidth(StatementWidthPreference)` write the settings; `retry()` forces a refresh of the shown language; `openLink(url)` opens the system browser (047 refines it). |
| `ProblemUiState(problem, status, statement, error, language, textSize, width)` | `error` is the last refresh's failure; over a statement it is a banner. |
| `ProblemStatus` | `Loading` (no data, refreshing), `Loaded` (statement set), `Error` (no data: offline, rate limited, unavailable or rejected, with Retry), `Unparsable` (no data and `Malformed`: "Can't show this statement natively", with Retry; 047 swaps in the WebView). |
| `ProblemPane(viewModel, onBack: (() -> Unit)?, modifier)` | The route. `onBack` shows a back arrow (page on compact width); `null` in a detail pane. |
| `ProblemScreen(state, actions, onBack, modifier, listState)` | Stateless screen: top bar `<id> · <title>`, EN/RU toggle, reading menu, then `StatementContent`. PageDown/PageUp scroll `listState` by one viewport. |

Test tags: `problem_view` (content under the top bar, `PROBLEM_VIEW_TAG`), `problem_language_en` / `problem_language_ru`,
`problem_reading` (`READING_MENU_TAG`), its items `problem_text_<small|default|large|largest>` and
`problem_width_<narrow|comfortable|full>`; samples keep `core/statement`'s `sample_<n>_<input|output>_copy`.
Every control is reachable with Tab and activated with Enter (`ProblemScreenKeyboardTest`).
Screenshots: `problemView_*`, `problemView_ru_phone_dark`, `problemView_narrow_tablet_dark`,
`problemView_textLargest_phone_dark`, `problemView_font150_phone_dark`, `problemView_<loading|error|staleError|unparsable|readingMenu>_phone_dark`.
