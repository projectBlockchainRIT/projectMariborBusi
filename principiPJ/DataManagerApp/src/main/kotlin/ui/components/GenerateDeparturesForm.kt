package ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dao.postgres.PostgreArrivalDao
import dao.postgres.PostgreDepartureDao
import dao.postgres.PostgreDirectionDao
import dao.postgres.PostgreStopDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import model.Arrival
import model.Departure
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun GenerateDeparturesForm() {
    val departureDao = PostgreDepartureDao()
    val arrivalDao = PostgreArrivalDao()
    val directionDao = PostgreDirectionDao()
    val stopDao = PostgreStopDao()
    val coroutineScope = rememberCoroutineScope()

    var count by remember { mutableStateOf(10) }
    var timeRange by remember { mutableStateOf("06:00:00-09:00:00") }
    var selectedDate by remember { mutableStateOf(java.time.LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) }
    var generatedDepartures by remember { mutableStateOf<List<Departure>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
            value = count.toString(),
            onValueChange = { count = it.toIntOrNull() ?: 0 },
            label = { Text("Število odhodov") },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFF990000),
                focusedLabelColor = Color(0xFF990000),
                cursorColor = Color(0xFF990000)
            )
        )

        OutlinedTextField(
            value = timeRange,
            onValueChange = { timeRange = it },
            label = { Text("Časovni interval (npr. 06:00:00-09:00:00)") },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFF990000),
                focusedLabelColor = Color(0xFF990000),
                cursorColor = Color(0xFF990000)
            )
        )

        Spacer(modifier = Modifier.weight(1f))

        errorMessage?.let {
            Text(it, color = MaterialTheme.colors.error)
        }

        if (generatedDepartures.isNotEmpty()) {
            Text("Generiranih odhodov: ${generatedDepartures.size}")
        }

        Button(
            onClick = {
                coroutineScope.launch {
                    try {
                        val directions = withContext(Dispatchers.IO) { directionDao.getAll() }
                        val stops = withContext(Dispatchers.IO) { stopDao.getAll() }

                        if (directions.isEmpty() || stops.isEmpty()) {
                            errorMessage = "Ni na voljo smeri ali postaj."
                            return@launch
                        }

                        val (from, to) = timeRange.split("-").map { it.trim() }
                        val fromTime = LocalTime.parse(from)
                        val toTime = LocalTime.parse(to)

                        val newDepartures = mutableListOf<Departure>()
                        val newArrivals = mutableListOf<Arrival>()

                        repeat(count) {
                            val randomDirectionId = directions.random().id!!
                            val randomStopId = stops.random().id!!
                            
                            val departure = Departure(
                                stopId = randomStopId,
                                directionId = randomDirectionId,
                                date = selectedDate
                            )

                            withContext(Dispatchers.IO) {
                                if (departureDao.insert(departure)) {
                                    val insertedDeparture = departureDao.getAll()
                                        .find { it.stopId == departure.stopId && 
                                               it.directionId == departure.directionId && 
                                               it.date == departure.date }
                                    
                                    insertedDeparture?.let { dep ->
                                        newDepartures.add(dep)
                                        
                                        //generiraj cas med from to
                                        val randomTime = LocalTime.ofSecondOfDay(
                                            (fromTime.toSecondOfDay()..toTime.toSecondOfDay()).random().toLong()
                                        )
                                        
                                        val arrival = Arrival(
                                            departureTimes = listOf(randomTime.format(DateTimeFormatter.ISO_LOCAL_TIME)),
                                            departuresId = dep.id!!
                                        )
                                        
                                        if (arrivalDao.insert(arrival)) {
                                            newArrivals.add(arrival)
                                        }
                                    }
                                }
                            }
                        }

                        generatedDepartures = newDepartures
                        errorMessage = null
                    } catch (e: Exception) {
                        errorMessage = "Napaka: ${e.message}"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFF990000),
                contentColor = Color.White
            )
        ) {
            Text("Generiraj in shrani")
        }
    }
}

