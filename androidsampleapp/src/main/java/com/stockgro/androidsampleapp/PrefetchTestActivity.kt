package com.stockgro.androidsampleapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.stockgro.androidsampleapp.ui.prefetch.DiagnosticInterceptor
import com.stockgro.androidsampleapp.ui.prefetch.PrefetchMonitorDashboardScreen
import com.stockgro.androidsampleapp.ui.prefetch.PrefetchTestViewModel
import com.stockgro.androidsampleapp.ui.prefetch.ProgressiveVideoPlayerScreen
import com.stockgro.androidsampleapp.ui.theme.MediaPodTheme
import com.stockgro.prefetch.MediaPrefetchManager
import com.stockgro.prefetch.data.PrefetchDatabase
import io.ktor.client.plugins.*
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.logging.*
import android.util.Log
import androidx.media3.datasource.DataSource
import com.stockgro.prefetch.MediaPrefetchKit
import com.stockgro.prefetch.initialize

class PrefetchTestActivity : ComponentActivity() {

    private lateinit var prefetchManager: MediaPrefetchManager
    private lateinit var database: PrefetchDatabase
    private lateinit var viewModel: PrefetchTestViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val diagnosticInterceptor = DiagnosticInterceptor()

        val httpClient = HttpClient(Android) {
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 30_000
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Log.d("KtorClient", message)
                    }
                }
                level = LogLevel.HEADERS
            }
        }

        prefetchManager = MediaPrefetchKit.initialize(this, httpClient, diagnosticInterceptor)
        database = prefetchManager.database

        viewModel = PrefetchTestViewModel(prefetchManager, database, diagnosticInterceptor)

        setContent {
            MediaPodTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var selectedUrl by remember { mutableStateOf<String?>(null) }
                    var dataSourceFactory by remember { mutableStateOf<DataSource.Factory?>(null) }

                    Column(modifier = Modifier.fillMaxSize()) {
                        ProgressiveVideoPlayerScreen(
                            url = selectedUrl,
                            dataSourceFactory = dataSourceFactory,
                            modifier = Modifier.weight(0.4f)
                        )

                        PrefetchMonitorDashboardScreen(
                            viewModel = viewModel,
                            modifier = Modifier.weight(0.6f)
                        ) { url ->
                            selectedUrl = url
                            dataSourceFactory = viewModel.createDataSourceFactory(url)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        database.close()
    }
}
