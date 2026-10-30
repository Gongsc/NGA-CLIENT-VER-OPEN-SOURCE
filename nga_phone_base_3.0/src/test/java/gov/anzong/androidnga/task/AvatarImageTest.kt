package gov.anzong.androidnga.task

import org.junit.Assert.assertEquals
import org.junit.Test

class AvatarImageTest {
    @Test fun imagesShrinkToFitWithoutUpscaling() {
        assertEquals(180 to 255, AvatarImage.fit(360, 510))
        assertEquals(180 to 90, AvatarImage.fit(1000, 500))
        assertEquals(128 to 255, AvatarImage.fit(500, 1000))
        assertEquals(100 to 100, AvatarImage.fit(100, 100))
        assertEquals(180 to 1, AvatarImage.fit(4000, 10))
    }
}
