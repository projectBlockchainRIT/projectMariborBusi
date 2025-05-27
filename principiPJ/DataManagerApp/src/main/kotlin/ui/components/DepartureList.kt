package ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dao.postgres.PostgreDepartureDao
import dao.postgres.PostgreDirectionDao
import dao.postgres.PostgreStopDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import model.Departure
import model.Direction
import model.Stop

@Composable
fun DepartureList() {
    val departureDao = PostgreDepartureDao()

    var departures by remember { mutableStateOf<List<Departure>>(emptyList()) }

    LaunchedEffect(Unit) {
        val deps = withContext(Dispatchers.IO) {
            departureDao.getAll()
        }
        departures = deps
    }

    var searchQuery by remember { mutableStateOf("") }
    var searchField by remember { mutableStateOf("STOP_ID") }
    var sortOption by remember { mutableStateOf("ID") }
    val editingStates = remember { mutableStateMapOf<Int, Boolean>() }

    val filteredAndSortedDepartures by remember(departures, searchQuery, searchField, sortOption) {
        derivedStateOf {
            departures
                .filter {
                    when (searchField) {
                        "STOP_ID" -> it.stopId.toString().contains(searchQuery)
                        "DIRECTION_ID" -> it.directionId.toString().contains(searchQuery)
                        else -> true
                    }
                }
                .sortedWith(
                    when (sortOption) {
                        "STOP_ID" -> compareBy { it.stopId }
                        "DIRECTION_ID" -> compareBy { it.directionId }
                        else -> compareBy { it.id ?: Int.MAX_VALUE }
                    }
                )
        }
    }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            OutlinedButton(onClick = {
                searchField = if (searchField == "STOP_ID") "DIRECTION_ID" else "STOP_ID"
            }, modifier = Modifier.width(140.dp)) {
                Text("Search: ${if (searchField == "STOP_ID") "STOP" else "DIR"}")
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Išči po ${if (searchField == "STOP_ID") "postaji" else "smeri"}") },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(onClick = {
                sortOption = when (sortOption) {
                    "ID" -> "STOP_ID"
                    "STOP_ID" -> "DIR_ID"
                    else -> "ID"
                }
            }, modifier = Modifier.width(140.dp)) {
                Text("Sort: $sortOption")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            if (filteredAndSortedDepartures.isEmpty()) {
                item {
                    Text("Ni zadetkov.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        style = MaterialTheme.typography.subtitle1,
                        color = Color.Gray)
                }
            } else {
                items(filteredAndSortedDepartures) { dep ->
                    val isEditing = editingStates[dep.id] == true
                    var editableDeparture by remember(dep.id) { mutableStateOf(dep) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                        shape = RoundedCornerShape(8.dp),
                        elevation = 0.dp
                    ) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("ID: ${dep.id}", style = MaterialTheme.typography.caption)
                                    Text("Odhod: ${dep.departure}", style = MaterialTheme.typography.h6)
                                    Text("Postaja ID: ${dep.stopId}", style = MaterialTheme.typography.body2)
                                    Text("Smer ID: ${dep.directionId}", style = MaterialTheme.typography.body2)
                                }
                                Row {
                                    IconButton(onClick = {
                                        editingStates[dep.id!!] = !(editingStates[dep.id] ?: false)
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Uredi")
                                    }
                                    IconButton(onClick = {
                                        departureDao.delete(dep.id!!)
                                        editingStates.remove(dep.id)
                                        departures = departureDao.getAll()
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Izbriši")
                                    }
                                }
                            }

                            if (isEditing) {
                                OutlinedTextField(
                                    value = editableDeparture.departure,
                                    onValueChange = {
                                        editableDeparture = editableDeparture.copy(departure = it)
                                    },
                                    label = { Text("Odhod (HH:mm:ss)") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                DirectionDropdown(
                                    selectedId = editableDeparture.directionId,
                                    onSelect = { editableDeparture = editableDeparture.copy(directionId = it) }
                                )

                                StopDropdown(
                                    selectedId = editableDeparture.stopId,
                                    onSelect = { editableDeparture = editableDeparture.copy(stopId = it) }
                                )

                                Button(
                                    onClick = {
                                        departureDao.update(editableDeparture)
                                        departures = departureDao.getAll()
                                        editingStates[dep.id!!] = false
                                    },
                                    modifier = Modifier
                                        .padding(top = 8.dp)
                                        .align(Alignment.End)
                                ) {
                                    Text("Shrani spremembe")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
