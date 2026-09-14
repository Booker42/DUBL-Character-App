package com.dubl.character.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dubl.character.android.ui.DublApp
import com.dubl.character.android.ui.theme.DublTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DublTheme {
                DublApp()
            }
        }
    }
}
