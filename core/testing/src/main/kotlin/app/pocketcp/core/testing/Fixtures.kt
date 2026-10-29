package app.pocketcp.core.testing

import java.io.File

/** Recorded responses in the repo-root `testdata/` directory (see `testdata/README.md`). */
object Fixtures {
    private const val DIR = "testdata"

    /** The `testdata/` directory, found by walking up from the working directory (tests run in the module dir). */
    val root: File by lazy {
        generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
            .map { File(it, DIR) }
            .firstOrNull { it.isDirectory }
            ?: error("No $DIR/ directory above ${System.getProperty("user.dir")}")
    }

    /** The UTF-8 text of `testdata/<path>`, e.g. `text("codeforces/api/user.info.json")`. */
    fun text(path: String): String {
        val file = File(root, path)
        check(file.isFile) { "Missing fixture: $DIR/$path" }
        return file.readText()
    }
}
