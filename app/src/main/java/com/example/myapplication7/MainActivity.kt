package com.example.myapplication7

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication7.ui.theme.AppTheme
import com.example.myapplication7.ui.screens.AppNavigation
import com.example.myapplication7.ui.viewmodels.BeatStreamViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: BeatStreamViewModel = viewModel()
                    // ЛР №9: Навігація з NavHost
                    AppNavigation(viewModel = viewModel)
                }
            }
        }
    }
}