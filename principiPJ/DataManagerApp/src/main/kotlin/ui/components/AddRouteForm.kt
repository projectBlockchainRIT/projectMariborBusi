package ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.gson.JsonParser
import dao.postgres.PostgreLineDao
import dao.postgres.PostgreStopDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import model.Route
import model.Stop
import java.time.OffsetDateTime

@Composable
fun AddRouteForm() {
    val lineDao = PostgreLineDao()
    val stopDao = PostgreStopDao()

    var routeName by remember { mutableStateOf("") }
    var selectedLineId by remember { mutableStateOf<Int?>(null) }
    var expandedLineDropdown by remember { mutableStateOf(false) }
    val lines = remember { mutableStateListOf<model.Line>() }

    val selectedStops = remember { mutableStateListOf<Stop>() }
    var expandedStopsDropdown by remember { mutableStateOf(false) }
    val allStops = remember { mutableStateListOf<Stop>() }

    var errorMessage by remember { mutableStateOf("") }

    // Fetch linije in postaje
    LaunchedEffect(Unit) {
        runBlocking(Dispatchers.IO) {
            lines.clear()
            lines.addAll(lineDao.getAll())
            allStops.clear()
            allStops.addAll(stopDao.getAll())
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            color = MaterialTheme.colors.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = routeName,
                    onValueChange = { routeName = it },
                    label = { Text("Ime poti") },
                    modifier = Modifier.fillMaxWidth()
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = lines.find { it.id == selectedLineId }?.lineCode ?: "Izberi linijo",
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth().clickable { expandedLineDropdown = true },
                        enabled = false,
                        label = { Text("Linija") }
                    )
                    DropdownMenu(
                        expanded = expandedLineDropdown,
                        onDismissRequest = { expandedLineDropdown = false }
                    ) {
                        if (lines.isEmpty()) {
                            DropdownMenuItem(onClick = {}) {
                                Text("Najprej dodaj linijo")
                            }
                        } else {
                            lines.forEach { line ->
                                DropdownMenuItem(onClick = {
                                    selectedLineId = line.id
                                    expandedLineDropdown = false
                                }) {
                                    Text(line.lineCode)
                                }
                            }
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = if (selectedStops.isEmpty()) "Izberi postaje" else selectedStops.joinToString { it.name },
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth().clickable { expandedStopsDropdown = true },
                        enabled = false,
                        label = { Text("Postaje poti") }
                    )
                    DropdownMenu(
                        expanded = expandedStopsDropdown,
                        onDismissRequest = { expandedStopsDropdown = false }
                    ) {
                        allStops.forEach { stop ->
                            DropdownMenuItem(onClick = {
                                if (!selectedStops.contains(stop)) {
                                    selectedStops.add(stop)
                                } else {
                                    selectedStops.remove(stop)
                                }
                            }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = selectedStops.contains(stop), onCheckedChange = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stop.name)
                                }
                            }
                        }
                    }
                }

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
                        if (routeName.isBlank() || selectedLineId == null) {
                            errorMessage = "Vsa polja morajo biti izpolnjena."
                        } else if (selectedStops.size < 2) {
                            errorMessage = "Dodaj vsaj dve postaji."
                        } else {
                            val geoJsonString = selectedStops.joinToString(separator = ",\n", prefix = "[\n", postfix = "\n]") {
                                "[${it.latitude}, ${it.longitude}]"
                            }
                            val geoJsonPath: JsonElement = Json.parseToJsonElement(geoJsonString)
                            val newRoute = Route(
                                name = routeName,
                                path = geoJsonPath,
                                lineId = selectedLineId!!
                            )
                            // Shrani v bazo
                            runBlocking(Dispatchers.IO) {
                                dao.postgres.PostgreRouteDao().insert(newRoute)
                            }
                            errorMessage = "Pot uspešno dodana."
                            routeName = ""
                            selectedLineId = null
                            selectedStops.clear()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Dodaj pot")
                }
            }
        }
    }
}
