package com.miti99.loto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.miti99.loto.ui.LotoAppRoot

/**
 * Single Activity — hosts the root Compose tree via [LotoAppRoot].
 * Replaced the Phase 01 placeholder as specified in Phase 10.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LotoAppRoot()
        }
    }
}
