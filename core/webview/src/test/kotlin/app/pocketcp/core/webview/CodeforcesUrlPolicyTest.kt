package app.pocketcp.core.webview

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeforcesUrlPolicyTest {
    @Test
    fun codeforcesAndItsSubdomains_overHttps_areAllowed() {
        listOf(
            "https://codeforces.com/contest/2274/problem/A",
            "https://codeforces.com",
            "https://m1.codeforces.com/blog/entry/1",
            "https://espresso.codeforces.com/a1b2c3.png",
            "HTTPS://CodeForces.com/blog/entry/1",
        ).forEach { url -> assertTrue(url, CodeforcesUrlPolicy.isAllowed(url)) }
    }

    @Test
    fun otherSchemes_lookalikeHosts_andNonUrls_areRejected() {
        listOf(
            "http://codeforces.com",
            "http://codeforces.com/contest/2274/problem/A",
            "https://codeforces.com.evil.io/contest/1",
            "https://evilcodeforces.com",
            "https://evil.io/?next=https://codeforces.com",
            "https://codeforces.com@evil.io/",
            "javascript:alert(document.cookie)",
            "file:///data/data/app.pocketcp/databases/cache.db",
            "content://app.pocketcp.files/notes",
            "intent://codeforces.com#Intent;scheme=https;end",
            "codeforces.com/contest/1",
            "",
            "https://codeforces.com\\@evil.io",
        ).forEach { url -> assertFalse(url, CodeforcesUrlPolicy.isAllowed(url)) }
    }
}
