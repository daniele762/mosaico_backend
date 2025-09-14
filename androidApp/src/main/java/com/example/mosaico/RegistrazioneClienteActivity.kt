package com.example.mosaico

import android.os.Bundle
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.mosaico.data.AppDatabase
import com.example.mosaico.data.ClientEntity
import com.example.mosaico.data.Repository
import kotlinx.coroutines.launch

class RegistrazioneClienteActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RegistrazioneClienteScreen()
                }
            }
        }
    }
}

@Composable
fun RegistrazioneClienteScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val repo = remember { Repository(db) }
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var answer1 by remember { mutableStateOf("") }
    var answer2 by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }

    val instagramUrl = "https://www.instagram.com/amodobakery/?utm_source=registrazione&utm_medium=android&utm_campaign=crm"

    if (showSuccess) {
        // Mostra ringraziamento per 2 secondi, poi redirect
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2000)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(instagramUrl))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(28.dp)
                .fillMaxWidth(0.95f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo (opzionale, se presente in res/drawable/ic_logo.xml/png)
            // Image(painter = painterResource(id = R.drawable.ic_logo), contentDescription = "Logo", modifier = Modifier.size(72.dp))
            Text(
                "Benvenuto!",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF2E7D32)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Registrati per ricevere offerte e promozioni esclusive",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF555555)
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Telefono") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text("Domande opzionali:", style = MaterialTheme.typography.titleMedium, color = Color(0xFF2E7D32))
            OutlinedTextField(
                value = answer1,
                onValueChange = { answer1 = it },
                label = { Text("Cosa ti piace di più del locale?") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = answer2,
                onValueChange = { answer2 = it },
                label = { Text("Allergie o preferenze?") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (name.isNotBlank() && (email.isNotBlank() || phone.isNotBlank())) {
                        scope.launch {
                            repo.addOrUpdateClient(ClientEntity(name = name, email = email, phone = phone))
                            showSuccess = true
                        }
                    } else {
                        Toast.makeText(context, "Inserisci almeno nome e email o telefono", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32), contentColor = Color.White)
            ) {
                Text("Registrati", style = MaterialTheme.typography.titleMedium)
            }
            if (showSuccess) {
                Spacer(Modifier.height(16.dp))
                Text("Grazie per la registrazione!", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                Text("Verrai reindirizzato alla nostra pagina Instagram...", color = Color(0xFF2E7D32))
            }
        }
    }
}