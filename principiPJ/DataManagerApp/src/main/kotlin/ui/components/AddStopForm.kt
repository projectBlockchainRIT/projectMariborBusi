package ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dao.postgres.PostgreStopDao
import model.Stop

@Composable
fun AddStopForm() {
    val stopDao = PostgreStopDao()

    var id by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize(),
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
                    .padding(24.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
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

                if (errorMessage.isNotBlank()) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Button(
                    onClick = {
                        if (id.isBlank() || number.isBlank() || name.isBlank() || latitude.isBlank() || longitude.isBlank()) {
                            errorMessage = "Vsa polja morajo biti izpolnjena."
                            return@Button
                        }

                        val stopId = id.toIntOrNull()
                        if (stopId == null) {
                            errorMessage = "ID mora biti številka."
                        } else {
                            val existing = stopDao.getById(stopId)
                            if (existing != null) {
                                errorMessage = "Postaja z ID $stopId že obstaja!"
                            } else {
                                val lat = latitude.toDoubleOrNull()
                                val lon = longitude.toDoubleOrNull()
                                if (lat == null || lon == null) {
                                    errorMessage = "Latitude in longitude morata biti decimalni števili."
                                    return@Button
                                }

                                val stop = Stop(
                                    id = stopId,
                                    number = number,
                                    name = name,
                                    latitude = lat,
                                    longitude = lon
                                )
                                stopDao.insert(stop)
                                errorMessage = "Postaja uspešno dodana."

                                id = ""
                                number = ""
                                name = ""
                                latitude = ""
                                longitude = ""
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Dodaj")
                }
            }
        }
    }

}