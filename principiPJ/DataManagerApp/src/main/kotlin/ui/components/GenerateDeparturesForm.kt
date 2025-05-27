package ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dao.postgres.PostgreDepartureDao
import dao.postgres.PostgreDirectionDao
import dao.postgres.PostgreStopDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import model.Departure
import utils.FakeData.generateFakeDeparture
@Composable
fun GenerateDeparturesForm() {
    val departureDao = PostgreDepartureDao()
    val directionDao = PostgreDirectionDao()
    val stopDao = PostgreStopDao()
    val coroutineScope = rememberCoroutineScope()

    var count by remember { mutableStateOf(10) }
    var timeRange by remember { mutableStateOf("06:00:00-09:00:00") }
    var generatedDepartures by remember { mutableStateOf<List<Departure>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = count.toString(),
            onValueChange = { count = it.toIntOrNull() ?: 0 },
            label = { Text("Število odhodov") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = timeRange,
            onValueChange = { timeRange = it },
            label = { Text("Časovni interval (npr. 06:00:00-09:00:00)") },
            modifier = Modifier.fillMaxWidth()
        )

        errorMessage?.let {
            Text(it, color = MaterialTheme.colors.error)
        }

        if (generatedDepartures.isNotEmpty()) {
            Text("Generiranih odhodov: ${generatedDepartures.size}")
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                coroutineScope.launch {
                    val newDepartures = withContext(Dispatchers.IO) {
                        val directions = directionDao.getAll()
                        val stops = stopDao.getAll()

                        if (directions.isEmpty() || stops.isEmpty()) {
                            return@withContext null
                        }

                        val (from, to) = timeRange.split("-").map { it.trim() }

                        val departures = (1..count).map {
                            val randomDirectionId = directions.random().id!!
                            val randomStopId = stops.random().id!!
                            generateFakeDeparture(randomStopId, randomDirectionId, from, to)
                        }

                        departures.forEach { departureDao.insert(it) }

                        departures
                    }

                    if (newDepartures == null) {
                        generatedDepartures = emptyList()
                        errorMessage = "Najprej generiraj smeri in postaje!"
                    } else {
                        generatedDepartures = newDepartures
                        errorMessage = null
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Generiraj in Shrani")
        }
    }
}

