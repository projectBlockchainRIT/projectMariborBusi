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

    val scrollState = rememberScrollState()

    Surface(
        color = MaterialTheme.colors.surface,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
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
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF990000),
                            focusedLabelColor = Color(0xFF990000),
                            cursorColor = Color(0xFF990000)
                        )
                    )
                    OutlinedTextField(
                        value = number,
                        onValueChange = { number = it },
                        label = { Text("Številka postaje") },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF990000),
                            focusedLabelColor = Color(0xFF990000),
                            cursorColor = Color(0xFF990000)
                        )
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Ime postaje") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF990000),
                        focusedLabelColor = Color(0xFF990000),
                        cursorColor = Color(0xFF990000)
                    )
                )

                OutlinedTextField(
                    value = latitude,
                    onValueChange = { latitude = it },
                    label = { Text("Zemljepisna širina (latitude)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF990000),
                        focusedLabelColor = Color(0xFF990000),
                        cursorColor = Color(0xFF990000)
                    )
                )

                OutlinedTextField(
                    value = longitude,
                    onValueChange = { longitude = it },
                    label = { Text("Zemljepisna dolžina (longitude)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF990000),
                        focusedLabelColor = Color(0xFF990000),
                        cursorColor = Color(0xFF990000)
                    )
                )


            }

            Spacer(modifier = Modifier.height(16.dp))

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
                modifier = Modifier
                    .fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF990000),
                    contentColor = Color.White
                )
            ) {
                Text("Dodaj")
            }
        }
    }
}
