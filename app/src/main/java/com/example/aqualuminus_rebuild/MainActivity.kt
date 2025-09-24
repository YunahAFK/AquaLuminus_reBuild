@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.aqualuminus_rebuild

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import com.example.aqualuminus_rebuild.data.manager.AuthStateManager
import com.example.aqualuminus_rebuild.ui.navigation.NavGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                val backgroundColor = MaterialTheme.colorScheme.background.toArgb()
                val iconColor = MaterialTheme.colorScheme.onBackground.toArgb()

                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.light(backgroundColor, iconColor),
                    navigationBarStyle = SystemBarStyle.light(backgroundColor, iconColor)
                )

                val authStateManager = remember { AuthStateManager() }
                val authState by authStateManager.authState.collectAsState()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                ) {
                    NavGraph(
                        authState = authState
                    )
                }
            }
        }
    }
}