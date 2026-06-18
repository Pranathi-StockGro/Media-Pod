package com.stockgro.mediapod.coil

import androidx.test.core.app.ApplicationProvider
import coil3.request.CachePolicy
import coil3.request.transformations
import com.stockgro.mediapod.ImageRequest
import com.stockgro.mediapod.coil.mappers.toCoilRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.stockgro.mediapod.enums.CachePolicy as MediapodCachePolicy
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CoilConversionTest {
    @Test
    fun `ImageRequest maps to Coil Request correctly`() {
        val context =
            ApplicationProvider.getApplicationContext<android.content.Context>()
        val request = ImageRequest.Builder("https://example.com")
            .circleCrop()
            .memoryCachePolicy(MediapodCachePolicy.DISABLED)
            .build()
        val coilRequest = request.toCoilRequest(context)
        assertEquals("https://example.com", coilRequest.data)
        assertEquals(CachePolicy.DISABLED, coilRequest.memoryCachePolicy)
        assertTrue(coilRequest.transformations.any { it is
                coil3.transform.CircleCropTransformation })
    }
}