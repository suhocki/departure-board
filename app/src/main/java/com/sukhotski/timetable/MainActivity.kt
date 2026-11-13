package com.sukhotski.timetable

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sukhotski.timetable.ui.theme.TimetableTheme
import com.sukhotski.timetable.ui.timetable.TimetableScreen
import com.sukhotski.timetable.ui.timetable.TimetableViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: TimetableViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TimetableTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                TimetableScreen(
                    state = state,
                    onRefresh = viewModel::refresh,
                    apiKeyMissing = BuildConfig.DEBUG && BuildConfig.TIMETABLE_API_KEY.isEmpty(),
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.refreshIfStale()
    }
}
