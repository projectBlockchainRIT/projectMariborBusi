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
import com.google.gson.JsonParser
import dao.postgres.PostgreLineDao
import dao.postgres.PostgreStopDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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
    val lines = remember { mutableStateListOf<model.Line>() }

    val selectedStops = remember { mutableStateListOf<Stop>() }
    val allStops = remember { mutableStateListOf<Stop>() }

    var expandedStopsDropdown by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val fetchedLines = lineDao.getAll()
            val fetchedStops = stopDao.getAll()

            lines.addAll(fetchedLines)
            allStops.addAll(fetchedStops)

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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = routeName,
                    onValueChange = { routeName = it },
                    label = { Text("Ime poti") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF990000),
                        focusedLabelColor = Color(0xFF990000),
                        cursorColor = Color(0xFF990000)
                    )
                )

                LineDropdown(
                    selectedLineId = selectedLineId,
                    onLineSelected = { selectedLineId = it }
                )

                PathDropdown(
                    selectedStops = selectedStops,
                    onSelectionChanged = {
                        selectedStops.clear()
                        selectedStops.addAll(it)
                    }
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
                    if (routeName.isBlank() || selectedLineId == null) {
                        errorMessage = "Vsa polja morajo biti izpolnjena."
                    } else if (selectedStops.size < 2) {
                        errorMessage = "Dodaj vsaj dve postaji."
                    } else {
                        val geoJsonString = selectedStops.joinToString(
                            separator = ",\n",
                            prefix = "[\n",
                            postfix = "\n]"
                        ) { "[${it.latitude}, ${it.longitude}]" }
                        val geoJsonPath = Json.parseToJsonElement(geoJsonString)
                        val newRoute = Route(
                            name = routeName,
                            path = geoJsonPath,
                            lineId = selectedLineId!!
                        )

                        dao.postgres.PostgreRouteDao().insert(newRoute)

                        errorMessage = "Pot uspešno dodana."
                        routeName = ""
                        selectedLineId = null
                        selectedStops.clear()
                    }

                },
                modifier = Modifier
                    .fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF990000),
                    contentColor = Color.White
                )
            ) {
                Text("Dodaj pot")
            }
        }
    }
}

