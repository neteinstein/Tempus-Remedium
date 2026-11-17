package org.mariacf.tempusremedium.android

import android.graphics.BitmapFactory
import android.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Photos are stored small and upright, whatever the camera gave. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class PhotoShrinkTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun ByteArray.size(): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(this, 0, this.size, options)
        return options.outWidth to options.outHeight
    }

    @Test
    fun aLargePhotoFitsTheLongestSide() {
        assertEquals(1600 to 1200, shrinkPhoto(samplePhoto(4000, 3000))!!.size())
    }

    @Test
    fun aSmallPhotoKeepsItsSize() {
        assertEquals(800 to 600, shrinkPhoto(samplePhoto(800, 600))!!.size())
    }

    @Test
    fun aRotatedPhotoStandsUpright() {
        val file = folder.newFile("shot.jpg").apply { writeBytes(samplePhoto(2000, 1500)) }
        ExifInterface(file.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }
        assertEquals(1200 to 1600, shrinkPhoto(file.readBytes())!!.size())
    }

    @Test
    fun somethingElseIsNotAPhoto() {
        assertNull(shrinkPhoto("not an image".encodeToByteArray()))
    }
}
