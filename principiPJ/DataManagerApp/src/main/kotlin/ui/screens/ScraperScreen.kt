package ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import db.*

@Composable
fun ScraperScreen() {
    var stopsFile by remember { mutableStateOf<File?>(null) }
    var departuresFile by remember { mutableStateOf<File?>(null) }
    var routesFile by remember { mutableStateOf<File?>(null) }
    var importDone by remember { mutableStateOf(false) }

    fun chooseFile(title: String): File? {
        val fd = FileDialog(Frame(), title, FileDialog.LOAD)
        fd.isVisible = true
        val dir = fd.directory
        val file = fd.file
        return if (dir != null && file != null) File(dir, file) else null
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Izberi datoteke za uvoz")

        Button(onClick = {
            stopsFile = chooseFile("Izberi datoteko bus_stops_maribor2.json")
        }) {
            Text("Izberi Stops datoteko")
        }
        Text("Izbrano: ${stopsFile?.name ?: "Nič" }")

        Button(onClick = {
            departuresFile = chooseFile("Izberi datoteko bus_arrival_times_stops_maribor.json")
        }) {
            Text("Izberi Arrivals datoteko")
        }
        Text("Izbrano: ${departuresFile?.name ?: "Nič" }")

        Button(onClick = {
            routesFile = chooseFile("Izberi datoteko routes_maribor_2025-05-19.json")
        }) {
            Text("Izberi Routes datoteko")
        }
        Text("Izbrano: ${routesFile?.name ?: "Nič" }")

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                // Klic funkcije za uvoz podatkov iz datotek v bazo
                if (stopsFile != null && departuresFile != null && routesFile != null) {
                    importJsonToDatabase(
                        stopsFile = stopsFile!!.absolutePath,
                        arrivalsFile = departuresFile!!.absolutePath,
                        routesFile = routesFile!!.absolutePath
                    )
                    importDone = true
                }
            },
            enabled = stopsFile != null && departuresFile != null && routesFile != null
        ) {
            Text("Uvozi v bazo")
        }

        if (importDone) {
            Text("Podatki so bili uspešno uvoženi.")
        }
    }
}
