package ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dao.postgres.PostgreDepartureDao
import dao.postgres.PostgreDirectionDao
import dao.postgres.PostgreStopDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import model.Departure
import model.Direction
import model.Stop

@Composable
fun AddDepartureForm() {
    val directionDao = PostgreDirectionDao()
    val stopDao = PostgreStopDao()
    val departureDao = PostgreDepartureDao()

    val directions = remember { mutableStateListOf<Direction>() }
    val stops = remember { mutableStateListOf<Stop>() }

    var selectedDirectionId by remember { mutableStateOf<Int?>(null) }
    var expandedDirectionDropdown by remember { mutableStateOf(false) }

    var selectedStopId by remember { mutableStateOf<Int?>(null) }
    var expandedStopDropdown by remember { mutableStateOf(false) }

    var departureTime by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        runBlocking(Dispatchers.IO) {
            directions.clear()
            directions.addAll(directionDao.getAll())
            stops.clear()
            stops.addAll(stopDao.getAll())
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(
            color = MaterialTheme.colors.surface,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
            ) {
                // Input za čas odhoda
                OutlinedTextField(
                    value = departureTime,
                    onValueChange = { departureTime = it },
                    label = { Text("Čas odhoda (HH:mm:ss)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Dropdown za izbiro smeri (Direction)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = directions.find { it.id == selectedDirectionId }?.name ?: "Izberi smer",
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedDirectionDropdown = true },
                        enabled = false,
                        label = { Text("Smer") }
                    )
                    DropdownMenu(
                        expanded = expandedDirectionDropdown,
                        onDismissRequest = { expandedDirectionDropdown = false }
                    ) {
                        if (directions.isEmpty()) {
                            DropdownMenuItem(onClick = {}) {
                                Text("Najprej dodaj smer")
                            }
                        } else {
                            directions.forEach { direction ->
                                DropdownMenuItem(onClick = {
                                    selectedDirectionId = direction.id
                                    expandedDirectionDropdown = false
                                }) {
                                    Text(direction.name)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dropdown za izbiro postaje (Stop)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = stops.find { it.id == selectedStopId }?.name ?: "Izberi postajo",
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedStopDropdown = true },
                        enabled = false,
                        label = { Text("Postaja") }
                    )
                    DropdownMenu(
                        expanded = expandedStopDropdown,
                        onDismissRequest = { expandedStopDropdown = false }
                    ) {
                        if (stops.isEmpty()) {
                            DropdownMenuItem(onClick = {}) {
                                Text("Najprej dodaj postajo")
                            }
                        } else {
                            stops.forEach { stop ->
                                DropdownMenuItem(onClick = {
                                    selectedStopId = stop.id
                                    expandedStopDropdown = false
                                }) {
                                    Text(stop.name)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (errorMessage.isNotBlank()) {
                    Text(
                        text = errorMessage,
                        color = if (errorMessage.contains("uspešno")) Color(0xFF2E7D32) else Color.Red,
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        if (departureTime.isBlank()) {
                            errorMessage = "Vnesi čas odhoda."
                        } else if (selectedDirectionId == null) {
                            errorMessage = "Izberi smer."
                        } else if (selectedStopId == null) {
                            errorMessage = "Izberi postajo."
                        } else {
                            // Shrani Departure
                            val departure = Departure(
                                stopId = selectedStopId!!,
                                directionId = selectedDirectionId!!,
                                departure = departureTime.trim()
                            )

                            departureDao.insert(departure)

                            errorMessage = "Departure uspešno dodan!"
                            departureTime = ""
                            selectedDirectionId = null
                            selectedStopId = null
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
