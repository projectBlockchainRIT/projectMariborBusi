package ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import model.Stop
import dao.*
import dao.postgres.PostgreStopDao
import model.*


@Composable
fun AddStopScreen() {
    val stopDao = PostgreStopDao()

    var id by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            elevation = 8.dp,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight() // zavzema 90% višine zaslona
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()), // omogoča scroll
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = id,
                        onValueChange = { id = it },
                        label = { Text("ID postaje") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = number,
                        onValueChange = { number = it },
                        label = { Text("Številka postaje") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Ime postaje") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = latitude,
                    onValueChange = { latitude = it },
                    label = { Text("Zemljepisna širina (latitude)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = longitude,
                    onValueChange = { longitude = it },
                    label = { Text("Zemljepisna dolžina (longitude)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = {
                        val stopId = id.toIntOrNull()
                        if (stopId == null) {
                            errorMessage = "ID mora biti številka."
                        } else {
                            val existing = stopDao.getById(stopId)
                            if (existing != null) {
                                errorMessage = "Postaja z ID $stopId že obstaja!"
                            } else {
                                val stop = Stop(
                                    id = stopId,
                                    number = number,
                                    name = name,
                                    latitude = latitude.toDoubleOrNull() ?: 0.0,
                                    longitude = longitude.toDoubleOrNull() ?: 0.0
                                )
                                stopDao.insert(stop)
                                errorMessage = "Postaja uspešno dodana."
                            }
                        }

                        id = ""
                        number = ""
                        name = ""
                        latitude = ""
                        longitude = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Shrani postajo")
                }

                if (errorMessage.isNotBlank()) {
                    Text(
                        text = errorMessage,
                        color = if (errorMessage.contains("uspešno")) Color(0xFF2E7D32) else Color.Red,
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
