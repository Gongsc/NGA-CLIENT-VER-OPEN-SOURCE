package gov.anzong.androidnga.mvvm.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileAvatarTest {
    @Test fun readsAvatarFromProfileResponse() {
        val js = "window.script_muti_get_var_store={\"data\":{\"0\":{\"uid\":1,\"avatar\":\"https://img.example.test/a.jpg\"}}}/*\$js\$*/"
        assertEquals("https://img.example.test/a.jpg", parseProfileAvatar(js))
        assertEquals("https://img.example.test/b.jpg", parseProfileAvatar("{\"data\":{\"0\":{\"avatar\":\"//img.example.test/b.jpg\"}}}"))
    }

    @Test fun missingOrBrokenAvatarFallsBackToEmpty() {
        assertEquals("", parseProfileAvatar("{\"data\":{\"0\":{\"uid\":1}}}"))
        assertEquals("", parseProfileAvatar("{\"error\":{\"0\":\"需要登录\"}}"))
        assertEquals("", parseProfileAvatar("not json"))
    }
}
