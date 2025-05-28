package ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import kotlinx.coroutines.withContext
import model.Departure
import model.Direction
import model.Stop

@Composable
fun AddDepartureForm() {
    val directionDao = PostgreDirectionDao()
    val stopDao = PostgreStopDao()
    val departureDao = PostgreDepartureDao()

    var directions = remember { mutableStateListOf<Direction>() }
    var stops = remember { mutableStateListOf<Stop>() }

    var selectedDirectionId by remember { mutableStateOf<Int?>(null) }
    var selectedStopId by remember { mutableStateOf<Int?>(null) }
    var departureTime by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }


    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val fetchedDirections = directionDao.getAll()
            val fetchedStops = stopDao.getAll()

            directions.addAll(fetchedDirections)

            stops.addAll(fetchedStops)
        }
    }

    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Scrollabilen del z inputi in dropdowni
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = departureTime,
                    onValueChange = { departureTime = it },
                    label = { Text("Čas odhoda (HH:mm:ss)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF990000),
                        focusedLabelColor = Color(0xFF990000),
                        cursorColor = Color(0xFF990000)
                    )
                )

                DirectionDropdown(
                    selectedId = selectedDirectionId,
                    onSelect = { selectedDirectionId = it }
                )

                StopDropdown(
                    selectedId = selectedStopId,
                    onSelect = { selectedStopId = it }
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
                    if (departureTime.isBlank()) {
                        errorMessage = "Vnesi čas odhoda."
                    } else if (selectedDirectionId == null) {
                        errorMessage = "Izberi smer."
                    } else if (selectedStopId == null) {
                        errorMessage = "Izberi postajo."
                    } else {
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

