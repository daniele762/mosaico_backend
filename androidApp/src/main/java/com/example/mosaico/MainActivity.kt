package com.example.mosaico

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.provider.OpenableColumns
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import com.example.mosaico.data.AppDatabase
import com.example.mosaico.data.ClientEntity
import com.example.mosaico.data.Repository
import com.example.mosaico.data.ReservationEntity
import kotlinx.coroutines.launch
@Serializable
data class ClientWeb(
    val name: String = "",
    val phone: String = "",
    val email: String = ""
)


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Richiesta permesso runtime per leggere i contatti (per step successivo)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), 1001)
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    App()
                }
            }
        }
    }
}

@Composable

fun PhoneListScreen() {
    val context = LocalContext.current
    var backendBase by remember { mutableStateOf(rememberBackendBase(context)) }
    // Aggiorna se cambia su disco (quando si torna dalle Impostazioni)
    LaunchedEffect(Unit) { backendBase = rememberBackendBase(context) }
    val db = remember { AppDatabase.get(context) }
    val repo = remember { Repository(db) }
    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }

    // Rubrica clienti: Client(name, phone, email)
    var clients by remember { mutableStateOf(listOf<Client>()) }
    var selectedClients by remember { mutableStateOf(setOf<Client>()) }
    var newName by remember { mutableStateOf("") }
    var newNumber by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Ciao, questo è un messaggio di prova!") }

    // Selettore contatti interno
    var showContactsPicker by remember { mutableStateOf(false) }
    val allContacts = remember { loadContacts(context) }
    var selectedFromPicker by remember { mutableStateOf(setOf<Client>()) }

    // Selezione file da inviare (opzionale)
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    val pickFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedFileUri = uri
        selectedFileName = uri?.let { getFileName(context, it) } ?: ""
    }

    // Caricamento iniziale dei clienti dal DB
    LaunchedEffect(Unit) {
        val stored = repo.getClients()
        clients = stored.map { Client(it.name, it.phone, it.email) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Rubrica clienti")
        Spacer(Modifier.height(4.dp))
        Button(onClick = {
            scope.launch {
                try {
                    val base = if (backendBase.isBlank()) defaultBackendBase() else backendBase
                    val url = java.net.URL("$base/api/clients")
                    val json = url.readText()
                    val arr = Json.decodeFromString<List<ClientWeb>>(json)
                    val imported = arr.map { Client(it.name, it.phone, it.email) }
                    val merged = (clients + imported).distinctBy { it.phone }
                    clients = merged
                    repo.saveClients(merged.map { ClientEntity(name = it.name, phone = it.phone, email = it.email) })
                    snackbarHostState.showSnackbar("Import riuscito: ${imported.size} clienti")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Errore import: controlla rete/URL")
                }
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Importa clienti dal web")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { showContactsPicker = true }) { Text("Importa da rubrica") }
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Nome") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = newNumber,
                onValueChange = { newNumber = it },
                label = { Text("Numero") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = newEmail,
                onValueChange = { newEmail = it },
                label = { Text("Email (opzionale)") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                if (newNumber.matches(Regex("^\\+?\\d{7,15}$")) && newName.isNotBlank()) {
                    val added = Client(newName, newNumber, newEmail.trim())
                    val updated = (clients + added).distinctBy { it.phone }
                    clients = updated
                    // Persistenza
                    scope.launch {
                        repo.saveClients(updated.map { ClientEntity(name = it.name, phone = it.phone, email = it.email) })
                    }
                    newName = ""
                    newNumber = ""
                    newEmail = ""
                }
            }) { Text("Aggiungi") }
        }
        Spacer(Modifier.height(8.dp))
        Text("Seleziona destinatari:")
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(clients) { client ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = selectedClients.contains(client),
                            onValueChange = {
                                selectedClients = if (it) selectedClients + client else selectedClients - client
                            }
                        )
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = selectedClients.contains(client),
                        onCheckedChange = null // handled by Row
                    )
                    val right = buildString {
                        append(client.phone)
                        if (client.email.isNotBlank()) append(" • ").append(client.email)
                    }
                    Text("${client.name} (${right})", modifier = Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Messaggio da inviare:")
        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Ciao!") }
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { pickFileLauncher.launch("*/*") }) { Text("Scegli file da inviare") }
            Spacer(Modifier.width(8.dp))
            if (selectedFileName.isNotBlank()) Text(selectedFileName, maxLines = 1)
        }
        if (selectedFileUri != null) {
            Text("File selezionato pronto per l'invio.", color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            selectedClients.forEach { client ->
                val number = client.phone
                if (selectedFileUri != null) {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = context.contentResolver.getType(selectedFileUri!!) ?: "*/*"
                        putExtra(Intent.EXTRA_STREAM, selectedFileUri)
                        putExtra("jid", "${number.replace("+", "")}@s.whatsapp.net")
                        setPackage("com.whatsapp")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        context.startActivity(sendIntent)
                    } catch (e: Exception) {
                        val fallback = Intent(Intent.ACTION_SEND).apply {
                            type = context.contentResolver.getType(selectedFileUri!!) ?: "*/*"
                            putExtra(Intent.EXTRA_STREAM, selectedFileUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(fallback)
                    }
                } else {
                    val url = "https://wa.me/${number.replace("+", "")}?text=" + Uri.encode(message)
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        setPackage("com.whatsapp")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val fallback = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(fallback)
                    }
                }
            }
        }, modifier = Modifier.fillMaxWidth(), enabled = selectedClients.isNotEmpty()) {
            Text(if (selectedFileUri != null) "Invia File a selezionati" else "Invia WhatsApp a selezionati")
        }

        Spacer(Modifier.height(8.dp))
        // Invio SMS ai selezionati (usa composer di sistema, nessun permesso SEND_SMS richiesto)
        Button(
            onClick = {
                selectedClients.forEach { client ->
                    val number = client.phone
                    val smsUri = Uri.parse("smsto:${number}")
                    val smsIntent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
                        putExtra("sms_body", message)
                    }
                    try {
                        context.startActivity(smsIntent)
                    } catch (_: Exception) {
                        // Nessuna app SMS disponibile: non facciamo nulla per ora
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedClients.isNotEmpty()
        ) { Text("Invia SMS a selezionati") }

        Spacer(Modifier.height(8.dp))
        // Invio Email ai selezionati (solo a chi ha email valorizzata)
        Button(
            onClick = {
                val recipients = selectedClients.mapNotNull { c -> c.email.takeIf { it.isNotBlank() } }
                if (recipients.isNotEmpty()) {
                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, recipients.toTypedArray())
                        putExtra(Intent.EXTRA_SUBJECT, "Offerta Speciale")
                        putExtra(Intent.EXTRA_TEXT, message)
                    }
                    try {
                        context.startActivity(emailIntent)
                    } catch (_: Exception) { }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedClients.any { it.email.isNotBlank() }
        ) { Text("Invia Email a selezionati") }

        if (showContactsPicker) {
            AlertDialog(
                onDismissRequest = { showContactsPicker = false },
                confirmButton = {
                    Button(onClick = {
                        val merged = (clients + selectedFromPicker.toList()).distinctBy { it.phone }
                        clients = merged
                        // Salva tutti i clienti
                        scope.launch {
                            repo.saveClients(merged.map { ClientEntity(name = it.name, phone = it.phone, email = it.email) })
                        }
                        selectedFromPicker = emptySet()
                        showContactsPicker = false
                    }) { Text("Aggiungi selezionati") }
                },
                dismissButton = {
                    Button(onClick = { selectedFromPicker = emptySet(); showContactsPicker = false }) { Text("Chiudi") }
                },
                title = { Text("Seleziona contatti") },
                text = {
                    Column(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                        OutlinedTextField(
                            value = "",
                            onValueChange = {},
                            enabled = false,
                            label = { Text("Contatti trovati: ${allContacts.size}") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(allContacts) { c ->
                                val checked = selectedFromPicker.contains(c)
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)) {
                                    Checkbox(checked = checked, onCheckedChange = {
                                        selectedFromPicker = if (it) selectedFromPicker + c else selectedFromPicker - c
                                    })
                                    val right = buildString {
                                        append(c.phone)
                                        if (c.email.isNotBlank()) append(" • ").append(c.email)
                                    }
                                    Text("${c.name} (${right})", modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            )
        }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

// Carica contatti (Nome, Numero) dal dispositivo
data class Client(val name: String, val phone: String, val email: String = "")

fun loadContacts(context: Context): List<Client> {
    val contacts = mutableListOf<Client>()
    val cr = context.contentResolver
    // Prima: raccogliamo tutte le email per ContactId
    val emailMap = mutableMapOf<Long, String>()
    val emailCursor = cr.query(
        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Email.CONTACT_ID,
            ContactsContract.CommonDataKinds.Email.ADDRESS
        ),
        null,
        null,
        null
    )
    emailCursor?.use {
        val idIdx = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
        val emailIdx = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)
        while (it.moveToNext()) {
            val id = it.getLong(idIdx)
            val email = it.getString(emailIdx) ?: ""
            if (email.isNotBlank() && !emailMap.containsKey(id)) {
                emailMap[id] = email
            }
        }
    }

    // Poi: raccogliamo nome, numero e associamo email se presente
    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER,
        ContactsContract.CommonDataKinds.Phone.CONTACT_ID
    )
    val cursor = cr.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        projection,
        null,
        null,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
    )
    cursor?.use {
        val nameIdx = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numberIdx = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val idIdx = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
        while (it.moveToNext()) {
            val name = it.getString(nameIdx) ?: ""
            val rawNumber = it.getString(numberIdx) ?: ""
            val number = rawNumber.replace(" ", "").replace("-", "")
            val id = it.getLong(idIdx)
            val email = emailMap[id] ?: ""
            if (number.isNotBlank()) contacts.add(Client(name, number, email))
        }
    }
    return contacts.distinctBy { it.phone }
}

// Funzione di utilità per ottenere il nome del file da un Uri
fun getFileName(context: Context, uri: Uri): String {
    var result = ""
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                result = it.getString(nameIndex)
            }
        }
    }
    if (result.isEmpty()) {
        result = uri.path?.substringAfterLast('/') ?: ""
    }
    return result
}

@Composable
fun App() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Messaggi", "Prenotazioni", "Statistiche", "Automazioni", "QR Registrazione", "Impostazioni")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }
        when (selectedTab) {
            0 -> PhoneListScreen()
            1 -> ReservationsScreen()
            2 -> StatsScreen()
            3 -> AutomationScreen()
            4 -> QrCodeScreen()
            5 -> SettingsScreen()
        }
    }
}

// Helper: SharedPreferences per URL backend
private const val PREFS_NAME = "mosaico_prefs"
private const val KEY_BACKEND_BASE = "backend_base"

fun defaultBackendBase(): String = "http://192.168.178.121:4000"

fun rememberBackendBase(context: Context): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getString(KEY_BACKEND_BASE, defaultBackendBase()) ?: defaultBackendBase()
}

fun saveBackendBase(context: Context, value: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_BACKEND_BASE, value).apply()
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    var url by remember { mutableStateOf(rememberBackendBase(context)) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Impostazioni", style = MaterialTheme.typography.titleLarge)
        Text("Configura l'URL del backend (base). Deve essere raggiungibile dai dispositivi dei clienti per la pagina web e dal tablet per l'import.")
        OutlinedTextField(
            value = url,
            onValueChange = { url = it.trim() },
            label = { Text("Backend Base URL") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("http://192.168.1.100:4000") }
        )
        Button(onClick = {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                saveBackendBase(context, url)
                Toast.makeText(context, "Salvato", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "URL non valido", Toast.LENGTH_SHORT).show()
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Salva") }
    HorizontalDivider()
        Text("Suggerimenti:")
        Text("- Usa l'IP LAN del PC (es. http://192.168.x.y:4000)")
        Text("- Assicurati che il firewall consenta la porta 4000")
    }
}

@Composable
fun StatsScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val repo = remember { Repository(db) }
    var clients by remember { mutableStateOf(listOf<ClientEntity>()) }
    var reservations by remember { mutableStateOf(listOf<ReservationEntity>()) }
    var today by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        clients = repo.getClients()
        reservations = repo.getReservations()
        today = java.time.LocalDate.now().toString()
    }

    val todayReservations = reservations.filter { it.date == today }
    val futureReservations = reservations.filter { it.date >= today }
    val clientsNoEmail = clients.filter { it.email.isBlank() }
    val clientsNoPhone = clients.filter { it.phone.isBlank() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Statistiche", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("Clienti totali: ${clients.size}")
        Text("Prenotazioni totali: ${reservations.size}")
        Text("Prenotazioni oggi: ${todayReservations.size}")
        Text("Prenotazioni future: ${futureReservations.size}")
        Spacer(Modifier.height(16.dp))
        Text("Clienti senza email: ${clientsNoEmail.size}")
        Text("Clienti senza telefono: ${clientsNoPhone.size}")
        Spacer(Modifier.height(16.dp))
        Text("Prenotazioni di oggi:", style = MaterialTheme.typography.titleMedium)
        if (todayReservations.isEmpty()) Text("Nessuna")
        else LazyColumn(Modifier.heightIn(max = 200.dp)) {
            items(todayReservations) { r ->
                Text("${r.name} (${r.people}p) alle ${r.time}")
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Top clienti (per prenotazioni):", style = MaterialTheme.typography.titleMedium)
        val topClients = reservations.groupBy { it.name }.mapValues { it.value.size }.toList().sortedByDescending { it.second }.take(5)
        if (topClients.isEmpty()) Text("Nessuno")
        else topClients.forEach { (name, count) -> Text("$name: $count prenotazioni") }
    }
}

@Composable
fun AutomationScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val repo = remember { Repository(db) }
    var reservations by remember { mutableStateOf(listOf<ReservationEntity>()) }
    var clients by remember { mutableStateOf(listOf<ClientEntity>()) }
    var today by remember { mutableStateOf("") }
    var yesterday by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        reservations = repo.getReservations()
        clients = repo.getClients()
        val now = java.time.LocalDate.now()
        today = now.toString()
        yesterday = now.minusDays(1).toString()
    }

    val todayReservations = reservations.filter { it.date == today }
    val yesterdayReservations = reservations.filter { it.date == yesterday }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Automazioni", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("Promemoria prenotazioni di oggi:")
        Button(onClick = {
            // Invio promemoria: apre composer SMS per tutti i clienti con prenotazione oggi
            val numbers = todayReservations.mapNotNull { r ->
                clients.find { c -> c.name == r.name }?.phone?.takeIf { it.isNotBlank() }
            }.distinct()
            if (numbers.isNotEmpty()) {
                val smsUri = Uri.parse("smsto:" + numbers.joinToString(","))
                val intent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
                    putExtra("sms_body", "Gentile cliente, ti ricordiamo la prenotazione per oggi. Grazie!")
                }
                try { context.startActivity(intent) } catch (_: Exception) {}
            }
        }) { Text("Invia promemoria SMS") }
        Spacer(Modifier.height(16.dp))
        Text("Follow-up prenotazioni di ieri:")
        Button(onClick = {
            // Invio follow-up: apre composer WhatsApp per tutti i clienti con prenotazione ieri
            val numbers = yesterdayReservations.mapNotNull { r ->
                clients.find { c -> c.name == r.name }?.phone?.takeIf { it.isNotBlank() }
            }.distinct()
            if (numbers.isNotEmpty()) {
                val url = "https://wa.me/" + numbers.first() + "?text=" + Uri.encode("Grazie per essere stati nostri ospiti! A presto.")
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try { context.startActivity(intent) } catch (_: Exception) {}
            }
        }) { Text("Invia follow-up WhatsApp") }
    }
}

@Preview
@Composable
fun PreviewApp() {
    App()
}

// ---------------- Prenotazioni -----------------
data class Reservation(
    val id: Long = 0,
    val name: String,
    val people: Int,
    val date: String, // formato libero: es. 2025-09-14
    val time: String, // es. 20:30
    val notes: String = ""
)

@Composable
fun ReservationsScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val repo = remember { Repository(db) }
    val scope = rememberCoroutineScope()

    var reservations by remember { mutableStateOf(listOf<Reservation>()) }
    var name by remember { mutableStateOf("") }
    var peopleText by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // Caricamento iniziale prenotazioni
    LaunchedEffect(Unit) {
        val stored = repo.getReservations()
        reservations = stored.map { Reservation(id = it.id, name = it.name, people = it.people, date = it.date, time = it.time, notes = it.notes) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Nuova prenotazione", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = peopleText, onValueChange = { peopleText = it.filter { ch -> ch.isDigit() } }, label = { Text("Numero persone") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Data (es. 2025-09-14)") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Ora (es. 20:30)") }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                val people = peopleText.toIntOrNull() ?: 0
                if (name.isNotBlank() && people > 0 && date.isNotBlank() && time.isNotBlank()) {
                    val newR = Reservation(name = name, people = people, date = date, time = time, notes = notes)
                    scope.launch {
                        repo.saveReservation(
                            ReservationEntity(
                                id = if (newR.id == 0L) 0 else newR.id,
                                name = newR.name,
                                people = newR.people,
                                date = newR.date,
                                time = newR.time,
                                notes = newR.notes
                            )
                        )
                        // ricarica da DB per ottenere l'id generato
                        val fresh = repo.getReservations().map { Reservation(id = it.id, name = it.name, people = it.people, date = it.date, time = it.time, notes = it.notes) }
                        reservations = fresh
                    }
                    name = ""; peopleText = ""; date = ""; time = ""; notes = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Aggiungi prenotazione") }

        Spacer(Modifier.height(16.dp))
        Text("Prenotazioni", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (reservations.isEmpty()) {
            Text("Nessuna prenotazione.")
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(reservations, key = { it.id }) { r ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("${r.name} - ${r.people} persone")
                            Text("${r.date} alle ${r.time}")
                            if (r.notes.isNotBlank()) Text("Note: ${r.notes}")
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    scope.launch {
                                        repo.deleteReservation(r.id)
                                        val fresh = repo.getReservations().map { Reservation(id = it.id, name = it.name, people = it.people, date = it.date, time = it.time, notes = it.notes) }
                                        reservations = fresh
                                    }
                                }) { Text("Elimina") }
                                // Placeholder per modifica futura
                                // Button(onClick = { /* TODO: Modifica */ }) { Text("Modifica") }
                            }
                        }
                    }
                }
            }
        }
    }
}
