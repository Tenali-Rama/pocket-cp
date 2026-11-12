package app.pocketcp.core.problemview

import app.pocketcp.core.model.ProblemId
import app.pocketcp.core.model.StatementLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class StatementTitleTest {
    @Test
    fun recordedTitle_losesItsIndexPrefix() {
        assertEquals("The Projections of Paco", statementTitle(Paco, recordedStatement()))
        assertEquals("Проекции Пако", statementTitle(Paco, recordedStatement(StatementLanguage.Ru)))
    }

    @Test
    fun titleOfAnotherShape_isKept() {
        val statement = shortStatement().copy(title = "B. Projections")

        assertEquals("B. Projections", statementTitle(ProblemId(2274, "A"), statement))
    }
}
