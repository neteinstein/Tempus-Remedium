package org.mariacf.tempusremedium.android

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mariacf.tempusremedium.android.MainActivity.Companion.toLaunchRequest
import org.mariacf.tempusremedium.core.model.TimeSlot
import org.mariacf.tempusremedium.ui.screens.LaunchRequest
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class MainActivityIntentTest {
    @Test
    fun widgetAndReminderExtrasOpenTheRightPlace() {
        assertEquals(
            LaunchRequest.OpenSlot(TimeSlot.BEDTIME),
            Intent().putExtra(MainActivity.EXTRA_SLOT, "BEDTIME").toLaunchRequest(),
        )
        assertEquals(LaunchRequest.OpenRefill("omega-3"), Intent().putExtra(MainActivity.EXTRA_REFILL, "omega-3").toLaunchRequest())
        assertNull(Intent().toLaunchRequest())
        assertNull(Intent().putExtra(MainActivity.EXTRA_SLOT, "LUNCH").toLaunchRequest())
    }
}
