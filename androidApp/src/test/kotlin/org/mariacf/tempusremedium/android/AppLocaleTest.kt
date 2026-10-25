package org.mariacf.tempusremedium.android

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.jetbrains.compose.resources.stringResource
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mariacf.tempusremedium.core.model.AppLanguage
import org.mariacf.tempusremedium.ui.AppLocale
import org.mariacf.tempusremedium.ui.resources.Res
import org.mariacf.tempusremedium.ui.resources.nav_settings
import org.robolectric.annotation.Config
import java.util.Locale

/** The language setting overrides the device language (pt-PT here). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "pt-rPT")
class AppLocaleTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val device = Locale.getDefault()

    @After
    fun restore() = Locale.setDefault(device)

    private fun showSettingsLabel(language: AppLanguage) {
        composeRule.setContent { AppLocale(language.tag) { Text(stringResource(Res.string.nav_settings)) } }
    }

    @Test
    fun germanOverridesTheDevice() {
        showSettingsLabel(AppLanguage.DE)
        composeRule.onNodeWithText("Einstellungen").assertExists()
    }

    @Test
    fun frenchOverridesTheDevice() {
        showSettingsLabel(AppLanguage.FR)
        composeRule.onNodeWithText("Réglages").assertExists()
    }

    @Test
    fun spanishOverridesTheDevice() {
        showSettingsLabel(AppLanguage.ES)
        composeRule.onNodeWithText("Ajustes").assertExists()
    }

    @Test
    fun systemKeepsTheDeviceLanguage() {
        showSettingsLabel(AppLanguage.SYSTEM)
        composeRule.onNodeWithText("Definições").assertExists()
    }
}
