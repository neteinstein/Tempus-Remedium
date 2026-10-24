package io.github.neteinstein.remedioemdia.android.screenshots

import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions

/** Tolerates sub-pixel anti-aliasing differences between machines; layout or colour changes still fail. */
@OptIn(ExperimentalRoborazziApi::class)
val screenshotOptions =
    RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.005f),
    )
