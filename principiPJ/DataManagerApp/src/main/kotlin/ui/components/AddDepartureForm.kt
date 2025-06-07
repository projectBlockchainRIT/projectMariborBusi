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
import dao.postgres.PostgreArrivalDao
import dao.postgres.PostgreDepartureDao
import dao.postgres.PostgreDirectionDao
import dao.postgres.PostgreStopDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import model.Arrival
import model.Departure
import model.Direction
import model.Stop
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun AddDepartureForm() {
    val directionDao = PostgreDirectionDao()
    val stopDao = PostgreStopDao()
    val departureDao = PostgreDepartureDao()
    val arrivalDao = PostgreArrivalDao()

    var directions = remember { mutableStateListOf<Direction>() }
    var stops = remember { mutableStateListOf<Stop>() }

    var selectedDirectionId by remember { mutableStateOf<Int?>(null) }
    var selectedStopId by remember { mutableStateOf<Int?>(null) }
    var selectedDate by remember { mutableStateOf(java.time.LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) }
    var departureTimes by remember { mutableStateOf("") }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Dodaj nov odhod",
            style = MaterialTheme.typography.h5,
            color = Color(0xFF990000)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = selectedDate,
            onValueChange = { selectedDate = it },
            label = { Text("Datum (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFF990000),
                focusedLabelColor = Color(0xFF990000),
                cursorColor = Color(0xFF990000)
            )
        )

        OutlinedTextField(
            value = departureTimes,
            onValueChange = { departureTimes = it },
            label = { Text("Časi odhodov (HH:mm:ss, HH:mm:ss, ...)") },
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

        if (errorMessage.isNotEmpty()) {
            Text(
                errorMessage,
                color = MaterialTheme.colors.error,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Button(
            onClick = {
                try {
                    val times = departureTimes.split(",")
                        .map { it.trim() }
                        .map { LocalTime.parse(it) }

                    if (times.isEmpty()) {
                        errorMessage = "Vnesi vsaj en čas odhoda."
                        return@Button
                    }

                    if (selectedDirectionId == null) {
                        errorMessage = "Izberi smer."
                        return@Button
                    }

                    if (selectedStopId == null) {
                        errorMessage = "Izberi postajo."
                        return@Button
                    }

                    val departure = Departure(
                        stopId = selectedStopId!!,
                        directionId = selectedDirectionId!!,
                        date = selectedDate
                    )

                    runBlocking {
                        withContext(Dispatchers.IO) {
                            if (departureDao.insert(departure)) {
                                val insertedDeparture = departureDao.getAll()
                                    .find { it.stopId == departure.stopId && 
                                           it.directionId == departure.directionId && 
                                           it.date == departure.date }
                                
                                insertedDeparture?.id?.let { id ->
                                    val arrival = Arrival(
                                        departureTimes = times.map { it.format(DateTimeFormatter.ISO_LOCAL_TIME) },
                                        departuresId = id
                                    )
                                    arrivalDao.insert(arrival)
                                }
                            }
                        }
                    }

                    errorMessage = "Odhod uspešno dodan!"
                    selectedDate = java.time.LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    departureTimes = ""
                    selectedDirectionId = null
                    selectedStopId = null
                } catch (e: Exception) {
                    errorMessage = "Napaka: ${e.message}"
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFF990000),
                contentColor = Color.White
            )
        ) {
            Text("Dodaj")
        }
    }
}

