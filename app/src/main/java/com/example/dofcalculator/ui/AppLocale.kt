package com.example.dofcalculator.ui

import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import com.example.dofcalculator.settings.AppLanguage
import java.util.Locale

/** Applies the selected language to the activity and all nested Compose windows. */
@Composable
internal fun AppLocale(language: AppLanguage, content: @Composable () -> Unit) {
    val baseContext = LocalContext.current
    val systemConfiguration = LocalConfiguration.current
    val localizedContext = remember(baseContext, language, systemConfiguration) {
        val configuration = Configuration(systemConfiguration)
        val tag = when (language) {
            AppLanguage.System -> null
            AppLanguage.English -> "en"
            AppLanguage.Russian -> "ru"
        }
        tag?.let { configuration.setLocales(LocaleList(Locale.forLanguageTag(it))) }
        baseContext.createConfigurationContext(configuration)
    }
    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration,
        // Dialogs/popups install their own LocalContext/LocalConfiguration. Pin the
        // app resources explicitly so stringResource keeps the chosen app language.
        LocalResources provides localizedContext.resources,
        content = content,
    )
}
