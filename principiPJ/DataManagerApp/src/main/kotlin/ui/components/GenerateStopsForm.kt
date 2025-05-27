package ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dao.postgres.PostgreStopDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import model.Stop
import utils.FakeData.generateFakeStop
import kotlinx.coroutines.withContext


@Composable
fun GenerateStopsForm() {
    val stopDao = PostgreStopDao()
    val coroutineScope = rememberCoroutineScope()

    var count by remember { mutableStateOf(10) }
    var latRange by remember { mutableStateOf(45.0 to 46.0) }
    var lonRange by remember { mutableStateOf(13.0 to 15.0) }
    var numberStart by remember { mutableStateOf(100) }
    var numberEnd by remember { mutableStateOf(200) }

    var generatedStops by remember { mutableStateOf<List<Stop>>(emptyList()) }

    val scrollState = rememberScrollState()

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {

        //latitude row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = latRange.first.toString(),
                onValueChange = { latRange = (it.toDoubleOrNull() ?: 0.0) to latRange.second },
                label = { Text("Min lat") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = latRange.second.toString(),
                onValueChange = { latRange = latRange.first to (it.toDoubleOrNull() ?: 0.0) },
                label = { Text("Max lat") },
                modifier = Modifier.weight(1f)
            )
        }

        //longitude row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = lonRange.first.toString(),
                onValueChange = { lonRange = (it.toDoubleOrNull() ?: 0.0) to lonRange.second },
                label = { Text("Min lon") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = lonRange.second.toString(),
                onValueChange = { lonRange = lonRange.first to (it.toDoubleOrNull() ?: 0.0) },
                label = { Text("Max lon") },
                modifier = Modifier.weight(1f)
            )
        }

        //count
        OutlinedTextField(
            value = count.toString(),
            onValueChange = { count = it.toIntOrNull() ?: 0 },
            label = { Text("Število postaj") },
            modifier = Modifier.fillMaxWidth()
        )

        //number range
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = numberStart.toString(),
                onValueChange = { numberStart = it.toIntOrNull() ?: 0 },
                label = { Text("Min številka postaje") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = numberEnd.toString(),
                onValueChange = { numberEnd = it.toIntOrNull() ?: 0 },
                label = { Text("Max številka postaje") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        if (generatedStops.isNotEmpty()) {
            Text("Generiranih in shranjenih postaj: ${generatedStops.size}")
        }


        Button(
            onClick = {
                coroutineScope.launch {
                    val (existingIds, newStops) = withContext(Dispatchers.IO) {
                        //pridobi obstoječe ID-je
                        val existingIds = stopDao.getAll().map { it.id }.toSet()

                        //generiraj nove postaje
                        val newStops = mutableListOf<Stop>()
                        var nextId = 1

                        repeat(count) {
                            while (existingIds.contains(nextId)) {
                                nextId++
                            }
                            val stop = generateFakeStop(
                                id = nextId,
                                latRange = latRange.first..latRange.second,
                                lonRange = lonRange.first..lonRange.second,
                                numberRange = numberStart..numberEnd
                            )
                            newStops.add(stop)
                            nextId++
                        }

                        //shrani v bazo
                        newStops.forEach { stop ->
                            stopDao.insert(stop)
                        }

                        existingIds to newStops
                    }

                    //posodobi UI (smo na glavnem threadu)
                    generatedStops = newStops
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Generiraj in Shrani")
        }

    }
}
