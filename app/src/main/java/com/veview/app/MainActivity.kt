package com.veview.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.veview.app.ui.theme.VeViewTheme
import com.veview.veview_sdk.presentation.VeViewSDK
import com.veview.veview_sdk.presentation.voice_review.VoiceReviewState
import com.veview.veview_sdk.data.configs.VoiceReviewConfig
import com.veview.veview_sdk.domain.model.ReviewContext
import kotlin.time.Duration.Companion.minutes

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        VeViewSDK.init("API", isDebug = true)
        val config = VoiceReviewConfig.Builder()
            .setRecordDuration(5.minutes)
            .build()
        val voiceReviewer = VeViewSDK.getInstance()
            .newAudioReviewer(context = this, config = config)


        enableEdgeToEdge()
        setContent {
            VeViewTheme {
                val reviewState by voiceReviewer.state.collectAsStateWithLifecycle()
                Log.d("MainActivity", "======> state [$reviewState]")
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val shouldStop =
                            reviewState !is VoiceReviewState.Idle && reviewState !is VoiceReviewState.Success
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            text = "Curr: $reviewState",
                            textAlign = TextAlign.Center
                        )
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            onClick = {
                                if (shouldStop) {
                                    voiceReviewer.stop()
                                } else {
                                    voiceReviewer.start(
                                        ReviewContext(
                                            reviewId = "12345",
                                            field = "Coffee"
                                        )
                                    )
                                }
                            }) {
                            Text(if (shouldStop) "Stop" else "Start")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    VeViewTheme {
        Greeting("Android")
    }
}