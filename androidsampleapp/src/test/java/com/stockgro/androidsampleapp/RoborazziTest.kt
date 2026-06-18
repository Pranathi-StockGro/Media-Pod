package com.stockgro.androidsampleapp

import android.content.Context
import android.widget.ImageView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.stockgro.mediapod.ImageLoaderProvider
import com.stockgro.mediapod.coil.CoilImageLoaderImpl
import com.stockgro.mediapod.ui.MPImage
import com.stockgro.mediapod.view.load
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class RoborazziTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    @Before
    fun setup() {
        ImageLoaderProvider.reset()
        val loader = CoilImageLoaderImpl(context)
        ImageLoaderProvider.setDefault(loader)
    }

    @Test
    fun testViewCircleCrop() {
        composeTestRule.setContent {
            AndroidView(
                factory = { ctx ->
                    ImageView(ctx).apply {
                        layoutParams = android.view.ViewGroup.LayoutParams(200,

                            200)

                        load(android.R.drawable.ic_menu_gallery) {
                            circleCrop()
                        }
                    }
                }
            )
        }
        composeTestRule.onRoot().captureRoboImage("view_circle_crop.png")
    }
    @Test
    fun testViewRoundedCorners() {
        composeTestRule.setContent {
            AndroidView(
                factory = { ctx ->
                    ImageView(ctx).apply {
                        layoutParams = android.view.ViewGroup.LayoutParams(200,

                            200)

                        load(android.R.drawable.ic_menu_gallery) {
                            roundedCorners(20f)
                        }
                    }
                }
            )
        }
        composeTestRule.onRoot().captureRoboImage("view_rounded_corners.png")
    }
    @Test
    fun testComposeCircleCrop() {
        composeTestRule.setContent {
            MPImage(
                data = android.R.drawable.ic_menu_gallery,
                contentDescription = null,
                modifier = Modifier.size(200.dp),
                requestBuilder = {
                    circleCrop()
                }
            )

        }
        composeTestRule.onRoot().captureRoboImage("compose_circle_crop.png")
    }
    @Test
    fun testComposePlaceholdersAndFallback() {
// Test fallback when data is null
        composeTestRule.setContent {
            Box(modifier = Modifier.size(200.dp)) {
                MPImage(
                    data = null,
                    contentDescription = null,
                    modifier = Modifier.size(200.dp),
                    placeholder = { Text("Loading...") },
                    fallback = { Text("Fallback Content") }
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("compose_fallback.png")
    }
    @Test
    fun testComposeError() {
// Test error when data is invalid
        composeTestRule.setContent {
            Box(modifier = Modifier.size(200.dp)) {
                MPImage(
                    data = "invalid_url",
                    contentDescription = null,
                    modifier = Modifier.size(200.dp),
                    error = { Text("Error Loading") }
                )
            }
        }
// Need to wait for error to trigger
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("compose_error.png")
    }
    @Test
    fun testViewPlaceholdersAndError() {
        composeTestRule.setContent {
            AndroidView(
                factory = { ctx ->
                    ImageView(ctx).apply {
                        layoutParams = android.view.ViewGroup.LayoutParams(200,

                            200)

                        load("invalid_url") {

                            placeholder(android.R.drawable.ic_menu_gallery)
                            error(android.R.drawable.ic_delete)
                        }
                    }
                }
            )
        }
        composeTestRule.onRoot().captureRoboImage("view_placeholder_error.png")
    }
}