# app API

The application module: `PocketCpApplication` (`@HiltAndroidApp`) and the single `MainActivity` (`@AndroidEntryPoint`).

- Package / namespace / applicationId: `app.pocketcp`.
- `MainActivity` currently shows only the app name (`R.string.app_name`); the app shell replaces this in task 006.
- Nothing depends on `app`; it depends on `core/*` and `feature/*` modules.
