package gov.anzong.androidnga.ui.compose

import org.junit.Assert.*
import org.junit.Test

class LoginSessionTest {
    @Test fun parsesExactCookieNamesAndPreservesTokenEquals() {
        val session = parseLoginSession("ignored=x; ngaPassportUid=42; ngaPassportCid=abc=def; ngaPassportUrlencodedUname=Alice")
        assertEquals(LoginSession("42", "abc=def", "Alice"), session)
    }
    @Test fun rejectsPartialAndLookalikeCookieNames() {
        assertNull(parseLoginSession("ngaPassportUid=42; fake_ngaPassportCid=abc; ngaPassportUrlencodedUname=Alice"))
        assertNull(parseLoginSession("ngaPassportUid=42; ngaPassportCid=abc"))
        assertNull(parseLoginSession("ngaPassportUid=0; ngaPassportCid=abc; ngaPassportUrlencodedUname=Alice"))
    }
    @Test fun decodesSingleAndDoubleEncodedNamesWithoutLosingPlus() {
        assertEquals("Alice+Bob", parseLoginSession("ngaPassportUid=42; ngaPassportCid=x; ngaPassportUrlencodedUname=Alice%2BBob")?.userName)
        assertEquals("中", parseLoginSession("ngaPassportUid=42; ngaPassportCid=x; ngaPassportUrlencodedUname=%25D6%25D0")?.userName)
        assertEquals("Alice+ Bob", parseLoginSession("ngaPassportUid=42; ngaPassportCid=x; ngaPassportUrlencodedUname=Alice%2B%2520Bob")?.userName)
    }
    @Test fun rejectsMalformedEscapesWithoutThrowing() {
        assertNull(parseLoginSession("ngaPassportUid=42; ngaPassportCid=x; ngaPassportUrlencodedUname=%broken"))
    }
    @Test fun restrictsCookieCaptureToHttpsNgaDomains() {
        assertTrue(isNgaLoginUrl("https://ngabbs.com/nuke.php"))
        assertTrue(isNgaLoginUrl("https://bbs.nga.cn/nuke.php"))
        assertFalse(isNgaLoginUrl("http://ngabbs.com/nuke.php"))
        assertFalse(isNgaLoginUrl("https://ngabbs.com.evil.example/"))
        assertFalse(isNgaLoginUrl("https://ngabbs.com@evil.example/"))
        assertFalse(isNgaLoginUrl("javascript:alert(1)"))
    }
}
