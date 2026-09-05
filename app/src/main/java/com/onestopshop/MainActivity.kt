package com.onestopshop

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    companion object {
        var daemonPort: Int = 0
    }

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Dynamically assign a free ephemeral port on first launch
        if (daemonPort == 0) {
            try {
                val serverSocket = java.net.ServerSocket(0, 50, java.net.InetAddress.getByName("127.0.0.1"))
                daemonPort = serverSocket.localPort
                serverSocket.close()
            } catch (e: Exception) {
                e.printStackTrace()
                daemonPort = 3000 // Fallback
            }
        }

        // Initialize WebView
        webView = findViewById(R.id.webView)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
        }

        // Prevent background sleep cycles, ensuring persistent WebSocket communication
        webView.keepScreenOn = true

        webView.webViewClient = object : WebViewClient() {
            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url != null && url.startsWith("opencode://")) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                    return true
                }
                return false
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                if (failingUrl == "http://localhost:$daemonPort/") {
                    val fallbackHtml = """
                        <html>
                            <body style="background-color: #121212; color: #00ffcc; font-family: monospace; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0;">
                                <div style="text-align: center;">
                                    <h1>Waiting for OSS Daemon...</h1>
                                    <p>The container environment is booting or extracting.</p>
                                    <p style="font-size: 0.8em; color: #888;">(Auto-reloading in 3 seconds)</p>
                                    <button onclick="window.NativeHost.installNow()" style="margin-top: 20px; padding: 10px 20px; background-color: #00ffcc; color: #121212; border: none; font-weight: bold; cursor: pointer;">Install Now (Manual Trigger)</button>
                                </div>
                                <script>
                                    setTimeout(() => location.reload(), 3000);
                                </script>
                            </body>
                        </html>
                    """.trimIndent()
                    view?.loadDataWithBaseURL(failingUrl, fallbackHtml, "text/html", "UTF-8", null)
                } else {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                }
            }
        }

        webView.loadUrl("http://localhost:$daemonPort/")

        // Inject JavascriptInterface to trigger extraction manually
        webView.addJavascriptInterface(WebAppInterface(this), "NativeHost")

        handleIntent(intent)
    }

    inner class WebAppInterface(private val context: MainActivity) {
        @JavascriptInterface
        fun installNow() {
            // Extract assets
            val extractor = AssetExtractor(context)
            extractor.extractAssets()

            // Start foreground service
            val serviceIntent = Intent(context, ContainerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val action = intent?.action
        val data: Uri? = intent?.data

        if (Intent.ACTION_VIEW == action && data != null) {
            if (data.scheme == "opencode" && data.host == "oauth-callback") {
                val code = data.getQueryParameter("code")
                if (code != null) {
                    exchangeCodeForToken(code)
                }
            }
        }
    }

    private fun exchangeCodeForToken(code: String) {
        thread {
            try {
                val url = URL("https://github.com/login/oauth/access_token")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true

                val clientId = BuildConfig.GITHUB_CLIENT_ID
                val clientSecret = BuildConfig.GITHUB_CLIENT_SECRET
                val postData = "client_id=$clientId&client_secret=$clientSecret&code=$code"

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(postData)
                    writer.flush()
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)
                    if (jsonObject.has("access_token")) {
                        val accessToken = jsonObject.getString("access_token")
                        writeTokenToGitConfig(accessToken)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun writeTokenToGitConfig(token: String) {
        try {
            val rootFsDir = File(filesDir, "ubuntu_rootfs")
            val rootHomeDir = File(rootFsDir, "root")
            if (!rootHomeDir.exists()) {
                rootHomeDir.mkdirs()
            }
            val gitConfigFile = File(rootHomeDir, ".gitconfig")

            val gitConfigContent = """
                [url "https://$token@github.com/"]
                    insteadOf = https://github.com/
            """.trimIndent()

            gitConfigFile.writeText(gitConfigContent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
