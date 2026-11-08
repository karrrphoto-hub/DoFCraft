package com.example.dofcalculator.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.cameraPresetsDataStore by preferencesDataStore(
    name = "camera_presets",
)
