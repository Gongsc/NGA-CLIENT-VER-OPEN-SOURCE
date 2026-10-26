package gov.anzong.androidnga.util
import org.junit.Assert.*
import org.junit.Test
class AppUpdateTest {
    @Test fun parsesCommonTagFormats() {
        assertEquals(listOf(4,0,0),parseVersion("v4.0.0"))
        assertEquals(listOf(4,1),parseVersion(" 4.1 "))
        assertEquals(listOf(4,1,2),parseVersion("4.1.2-beta+7"))
        assertNull(parseVersion("release"))
        assertNull(parseVersion(""))
        assertNull(parseVersion("4..1"))
    }

    @Test fun comparesVersionsNumerically() {
        assertTrue(isNewerVersion("v4.0.1","4.0.0"))
        assertTrue(isNewerVersion("4.10.0","4.9.9"))
        assertFalse(isNewerVersion("4.0","4.0.0"))
        assertFalse(isNewerVersion("3.7.6","4.0.0"))
        assertFalse(isNewerVersion("nightly","4.0.0"))
    }

    @Test fun interpretsGithubResponses() {
        assertEquals(UpdateResult.UpToDate(null),interpretLatestRelease(404,null,null,null,"4.0.0"))
        assertTrue(interpretLatestRelease(403,null,null,null,"4.0.0") is UpdateResult.Failed)
        assertTrue(interpretLatestRelease(500,null,null,null,"4.0.0") is UpdateResult.Failed)
        assertTrue(interpretLatestRelease(200,"latest",null,null,"4.0.0") is UpdateResult.Failed)
        assertEquals(UpdateResult.UpToDate("4.0.0"),interpretLatestRelease(200,"v4.0.0","$RELEASES_URL/tag/v4.0.0","","4.0.0"))
        val available=interpretLatestRelease(200,"v4.1.0","$RELEASES_URL/tag/v4.1.0"," 修复问题 ","4.0.0") as UpdateResult.Available
        assertEquals(ReleaseInfo("4.1.0","$RELEASES_URL/tag/v4.1.0","修复问题"),available.release)
    }

    @Test fun releaseLinksOutsideProjectFallBackToReleasePage() {
        val spoofed=interpretLatestRelease(200,"v5.0.0","https://example.com/app.apk","","4.0.0") as UpdateResult.Available
        assertEquals(RELEASES_URL,spoofed.release.url)
        val prefix=interpretLatestRelease(200,"v5.0.0","${RELEASES_URL}evil/tag/v5","","4.0.0") as UpdateResult.Available
        assertEquals(RELEASES_URL,prefix.release.url)
    }
}
