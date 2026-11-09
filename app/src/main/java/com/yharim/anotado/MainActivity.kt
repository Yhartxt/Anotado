package com.yharim.anotado

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.taskmanager.ui.navigation.AppViewModel
import com.yharim.anotado.di.ViewModelFactory
import com.example.taskmanager.ui.navigation.AppNavigation
import com.yharim.anotado.ui.theme.AnotadoTheme

class MainActivity : ComponentActivity() {
    private val factory by lazy { ViewModelFactory((application as TaskManagerApplication).container) }
    private val appViewModel: AppViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.WHITE, android.graphics.Color.WHITE)
        )
        setContent { AnotadoTheme { AppNavigation(appViewModel, factory) } }
    }
}
