package com.example.mosaico

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.awt.SwingPanel
import javax.swing.JPanel
import java.awt.BorderLayout
import javafx.embed.swing.JFXPanel
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Mosaico - Browser integrato") {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                DesktopApp()
            }
        }
    }
}

@Composable
fun DesktopApp() {
    var url by remember { mutableStateOf("https://www.mosaico-project.com/factory/index.html?1757660596175#/") }
    var textFieldValue by remember { mutableStateOf(TextFieldValue(url)) }
    var reloadKey by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { textFieldValue = it },
            label = { Text("Inserisci URL") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            url = textFieldValue.text
            reloadKey++
        }, modifier = Modifier.align(Alignment.End)) {
            Text("Apri")
        }
        Spacer(modifier = Modifier.height(16.dp))
        // WebView con chiave per forzare reload
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            key(reloadKey) {
                SwingPanel(modifier = Modifier.fillMaxSize(), factory = {
                    val panel = JPanel(BorderLayout())
                    val jfxPanel = JFXPanel()
                    panel.add(jfxPanel, BorderLayout.CENTER)
                    Platform.runLater {
                        val webView = WebView()
                        val engine: WebEngine = webView.engine
                        engine.load(url)
                        jfxPanel.scene = Scene(webView)
                    }
                    panel
                })
            }
        }
    }
}


