package app.pocketcp.core.problemview

import app.cash.turbine.test
import app.pocketcp.core.datastore.StatementWidthPreference
import app.pocketcp.core.datastore.TextSizePreference
import app.pocketcp.core.datastore.UserSettings
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.model.StatementLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProblemViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val statements = FakeStatementRepository()
    private val settings = FakeSettingsRepository()
    private val opened = mutableListOf<String>()
    private val english = shortStatement(StatementLanguage.En)
    private val russian = shortStatement(StatementLanguage.Ru)

    private fun viewModel() = ProblemViewModel(Paco, statements, settings, openUrl = { opened += it })

    private fun FakeStatementRepository.key(language: StatementLanguage) = FakeStatementRepository.Key(Paco, language)

    @Test
    fun cachedStatement_isTheFirstState_neverLoading() =
        runTest {
            statements.row(Paco, StatementLanguage.En).value = cached(english)

            viewModel().state.test {
                val first = awaitItem()
                assertEquals(ProblemStatus.Loaded, first.status)
                assertEquals(english, first.statement)
            }
            assertEquals(emptyList<FakeStatementRepository.Key>(), statements.fetches)
        }

    @Test
    fun noCache_isLoading_untilTheFetchLands() =
        runTest {
            val viewModel = viewModel()

            viewModel.state.test {
                assertEquals(ProblemStatus.Loading, awaitItem().status)
                statements.row(Paco, StatementLanguage.En).value = cached(english)
                assertEquals(english, awaitItem().statement)
            }
            assertEquals(listOf(statements.key(StatementLanguage.En)), statements.fetches)
        }

    @Test
    fun switchingToRussian_writesTheSetting_andShowsTheRussianStatement_thenBackToCachedEnglish() =
        runTest {
            statements.row(Paco, StatementLanguage.En).value = cached(english)
            val viewModel = viewModel()

            viewModel.state.test {
                assertEquals(english, awaitItem().statement)

                viewModel.setLanguage(StatementLanguage.Ru)
                assertEquals(StatementLanguage.Ru, settings.stored.value.statementLanguage)
                val fetching = awaitItem()
                assertEquals(StatementLanguage.Ru, fetching.language)
                assertEquals(ProblemStatus.Loading, fetching.status)
                statements.row(Paco, StatementLanguage.Ru).value = cached(russian)
                assertEquals(russian, awaitItem().statement)

                viewModel.setLanguage(StatementLanguage.En)
                val back = awaitItem()
                assertEquals(StatementLanguage.En, back.language)
                assertEquals(english, back.statement)
            }
            // Only the uncached Russian row was fetched; English came back from the cache.
            assertEquals(listOf(statements.key(StatementLanguage.Ru)), statements.fetches)
            assertEquals(emptyList<Any>(), statements.refreshes)
        }

    @Test
    fun textSizeAndWidth_persist_andShowInState() =
        runTest {
            statements.row(Paco, StatementLanguage.En).value = cached(english)
            val viewModel = viewModel()

            viewModel.state.test {
                assertEquals(TextSizePreference.Default, awaitItem().textSize)

                viewModel.setTextSize(TextSizePreference.Largest)
                assertEquals(TextSizePreference.Largest, awaitItem().textSize)
                viewModel.setWidth(StatementWidthPreference.Narrow)
                assertEquals(StatementWidthPreference.Narrow, awaitItem().width)
            }
            assertEquals(TextSizePreference.Largest, settings.stored.value.statementTextSize)
            assertEquals(StatementWidthPreference.Narrow, settings.stored.value.statementWidth)
        }

    @Test
    fun storedReadingSettings_areTheInitialOnes() =
        runTest {
            settings.stored.value =
                UserSettings(
                    statementLanguage = StatementLanguage.Ru,
                    statementTextSize = TextSizePreference.Small,
                    statementWidth = StatementWidthPreference.Full,
                )
            statements.row(Paco, StatementLanguage.Ru).value = cached(russian)

            val state = viewModel().state.value

            assertEquals(russian, state.statement)
            assertEquals(TextSizePreference.Small, state.textSize)
            assertEquals(StatementWidthPreference.Full, state.width)
        }

    @Test
    fun retry_forcesARefreshOfTheShownLanguage() =
        runTest {
            settings.stored.value = UserSettings(statementLanguage = StatementLanguage.Ru)
            statements.row(Paco, StatementLanguage.Ru).value = cached(null, error = JudgeError.Offline())
            val viewModel = viewModel()
            assertEquals(ProblemStatus.Loading, viewModel.state.value.status)

            viewModel.retry()

            assertEquals(listOf(statements.key(StatementLanguage.Ru) to true), statements.refreshes)
        }

    @Test
    fun failedRefreshWithoutData_isAnError() =
        runTest {
            val viewModel = viewModel()
            statements.row(Paco, StatementLanguage.En).value = cached(null, error = JudgeError.Offline())

            val state = viewModel.state.value
            assertEquals(ProblemStatus.Error, state.status)
            assertEquals(JudgeError.Offline::class, state.error!!::class)
        }

    @Test
    fun malformedWithoutData_isUnparsable() =
        runTest {
            val viewModel = viewModel()
            statements.row(Paco, StatementLanguage.En).value = cached(null, error = JudgeError.Malformed())

            assertEquals(ProblemStatus.Unparsable, viewModel.state.value.status)
        }

    @Test
    fun malformedOverData_isLoaded_withTheBanner() =
        runTest {
            statements.row(Paco, StatementLanguage.En).value = cached(english, error = JudgeError.Malformed())

            val state = viewModel().state.value

            assertEquals(ProblemStatus.Loaded, state.status)
            assertEquals(english, state.statement)
            assertEquals(JudgeError.Malformed::class, state.error!!::class)
        }

    @Test
    fun loadedWithoutError_hasNoBanner() =
        runTest {
            statements.row(Paco, StatementLanguage.En).value = cached(english)

            assertNull(viewModel().state.value.error)
        }

    @Test
    fun openLink_onCodeforces_opensTheWebPage_andCloseWebPageClosesIt() {
        val viewModel = viewModel()

        viewModel.openLink("https://codeforces.com/blog/entry/1")
        assertEquals("https://codeforces.com/blog/entry/1", viewModel.state.value.webPage)
        viewModel.openLink("https://m1.codeforces.com/contest/2274/problem/B")
        assertEquals("https://m1.codeforces.com/contest/2274/problem/B", viewModel.state.value.webPage)

        viewModel.closeWebPage()
        assertNull(viewModel.state.value.webPage)
        assertEquals(emptyList<String>(), opened)
    }

    @Test
    fun openLink_offCodeforces_goesToTheBrowser_withoutAWebPage() {
        val viewModel = viewModel()

        viewModel.openLink("https://en.wikipedia.org/wiki/Graph")
        viewModel.openLink("http://codeforces.com/blog/entry/1")

        assertEquals(listOf("https://en.wikipedia.org/wiki/Graph", "http://codeforces.com/blog/entry/1"), opened)
        assertNull(viewModel.state.value.webPage)
    }

    @Test
    fun openLink_toNonWebSchemes_isDropped() {
        val viewModel = viewModel()

        viewModel.openLink("javascript:alert(1)")
        viewModel.openLink("intent://codeforces.com#Intent;scheme=https;end")
        viewModel.openLink("file:///sdcard/a.html")

        assertEquals(emptyList<String>(), opened)
        assertNull(viewModel.state.value.webPage)
    }

    @Test
    fun unparsable_carriesThePageUrlOfTheShownLanguage() =
        runTest {
            settings.stored.value = UserSettings(statementLanguage = StatementLanguage.Ru)
            val viewModel = viewModel()
            statements.row(Paco, StatementLanguage.Ru).value = cached(null, error = JudgeError.Malformed())

            val state = viewModel.state.value
            assertEquals(ProblemStatus.Unparsable, state.status)
            assertEquals(statements.pageUrl(Paco, StatementLanguage.Ru), state.pageUrl)
        }
}
