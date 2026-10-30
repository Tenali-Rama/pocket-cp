package app.pocketcp.feature.settings

import app.cash.turbine.test
import app.pocketcp.core.datastore.DensityPreference
import app.pocketcp.core.datastore.TextSizePreference
import app.pocketcp.core.datastore.ThemePreference
import app.pocketcp.core.datastore.UserSettings
import app.pocketcp.core.model.OptionalModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val stored =
        UserSettings(
            theme = ThemePreference.Amoled,
            appTextSize = TextSizePreference.Large,
            codeTextSize = TextSizePreference.Small,
            codeLigatures = false,
            density = DensityPreference.Compact,
            disabledModules = setOf(OptionalModule.Stats),
        )

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun state_reflectsStoredSettings_andInstalledModulesInDeclarationOrder() =
        runTest {
            val installed = setOf(OptionalModule.Stats, OptionalModule.Ai)
            val viewModel = SettingsViewModel(FakeSettingsRepository(stored), installed)

            viewModel.state.filterNotNull().test {
                val state = awaitItem()
                assertEquals(stored, state.settings)
                assertEquals(listOf(OptionalModule.Ai, OptionalModule.Stats), state.installedModules)
                assertEquals(listOf(SettingsCategory.Appearance, SettingsCategory.Features), state.categories)
            }
        }

    @Test
    fun features_hidden_whenNoModuleIsInstalled() =
        runTest {
            val viewModel = SettingsViewModel(FakeSettingsRepository(), emptySet())

            viewModel.state.filterNotNull().test {
                assertEquals(listOf(SettingsCategory.Appearance), awaitItem().categories)
            }
        }

    @Test
    fun everySetter_writesThroughTheRepository_andTheStateFollows() =
        runTest {
            val repository = FakeSettingsRepository()
            val viewModel = SettingsViewModel(repository, setOf(OptionalModule.Stats))

            viewModel.state.filterNotNull().test {
                assertEquals(UserSettings(), awaitItem().settings)

                viewModel.setTheme(ThemePreference.Light)
                viewModel.setAppTextSize(TextSizePreference.Largest)
                viewModel.setCodeTextSize(TextSizePreference.Small)
                viewModel.setCodeLigatures(false)
                viewModel.setDensity(DensityPreference.Compact)
                viewModel.setModuleEnabled(OptionalModule.Stats, false)

                val expected =
                    UserSettings(
                        theme = ThemePreference.Light,
                        appTextSize = TextSizePreference.Largest,
                        codeTextSize = TextSizePreference.Small,
                        codeLigatures = false,
                        density = DensityPreference.Compact,
                        disabledModules = setOf(OptionalModule.Stats),
                    )
                assertEquals(expected, repository.stored.value)
                assertEquals(expected, expectMostRecentItem().settings)
            }
        }

    @Test
    fun moduleOn_removesItFromDisabledModules_leavingOthersAlone() =
        runTest {
            val repository =
                FakeSettingsRepository(UserSettings(disabledModules = setOf(OptionalModule.Ai, OptionalModule.Stats)))
            val viewModel = SettingsViewModel(repository, setOf(OptionalModule.Ai, OptionalModule.Stats))

            viewModel.setModuleEnabled(OptionalModule.Ai, true)

            assertEquals(setOf(OptionalModule.Stats), repository.stored.value.disabledModules)
        }
}
