package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import db.importJsonToDatabase

@Composable
fun AddJsonForm() {
    var stopsFile by remember { mutableStateOf<File?>(null) }
    var departuresFile by remember { mutableStateOf<File?>(null) }
    var routesFile by remember { mutableStateOf<File?>(null) }
    var message by remember { mutableStateOf("") }

    fun chooseFile(title: String): File? {
        val fd = FileDialog(Frame(), title, FileDialog.LOAD)
        fd.isVisible = true
        val dir = fd.directory
        val file = fd.file
        return if (dir != null && file != null) File(dir, file) else null
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colors.surface,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { stopsFile = chooseFile("Stops file") },
                        modifier = Modifier.weight(1f) // 1/6 širine
                    ) {
                        Text("Stops", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    }
                    Box(
                        modifier = Modifier
                            .weight(3f) // 5/6 širine
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(text = stopsFile?.name ?: "Izberi Stops Datoteko")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { departuresFile = chooseFile("Departures file") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Departures", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    }
                    Box(
                        modifier = Modifier
                            .weight(3f)
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(text = departuresFile?.name ?: "Izberi Departures Datoteko")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { routesFile = chooseFile("Routes file") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Routes", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    }
                    Box(
                        modifier = Modifier
                            .weight(3f)
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(text = routesFile?.name ?: "Izberi Routes Datoteko")
                    }
                }

                Spacer(Modifier.weight(1f))

                if (message.isNotBlank()) {
                    Text(
                        text = message,
                        color = if (message.contains("uspešno")) Color(0xFF2E7D32) else Color.Red,
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Button(
                    onClick = {
                        if (stopsFile != null && departuresFile != null && routesFile != null) {
                            try {
                                importJsonToDatabase(
                                    stopsFile = stopsFile!!.absolutePath,
                                    arrivalsFile = departuresFile!!.absolutePath,
                                    routesFile = routesFile!!.absolutePath
                                )
                                message = "Podatki so bili uspešno uvoženi."
                            } catch (e: Exception) {
                                message = "Napaka pri uvozu: ${e.message}"
                            }
                        } else {
                            message = "zberi vse tri datoteke."
                        }
                    },
                    enabled = stopsFile != null && departuresFile != null && routesFile != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Uvozi v bazo")
                }
            }
        }
    }
}
