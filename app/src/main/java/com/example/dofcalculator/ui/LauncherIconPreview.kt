package com.example.dofcalculator.ui

import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.dofcalculator.R

/** Renders the packaged Android icons with square and circular launcher masks. */
@Preview(name = "Launcher icons", widthDp = 240, heightDp = 120, showBackground = true)
@Composable
private fun LauncherIconPreview() {
    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        listOf(R.mipmap.ic_launcher, R.mipmap.ic_launcher_round).forEach { resource ->
            AndroidView(
                factory = { context ->
                    ImageView(context).apply {
                        setImageResource(resource)
                        if (resource == R.mipmap.ic_launcher_round) {
                            outlineProvider = object : ViewOutlineProvider() {
                                override fun getOutline(view: View, outline: Outline) {
                                    outline.setOval(0, 0, view.width, view.height)
                                }
                            }
                            clipToOutline = true
                        }
                    }
                },
                modifier = Modifier.size(96.dp),
            )
        }
    }
}
