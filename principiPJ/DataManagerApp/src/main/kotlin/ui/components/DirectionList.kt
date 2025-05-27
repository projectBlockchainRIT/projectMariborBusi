package ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dao.postgres.PostgreDirectionDao
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import dao.postgres.PostgreDepartureDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import model.Departure
import model.Direction
import model.Line

@Composable
fun DirectionList() {
    val directionDao = PostgreDirectionDao()
    val departureDao = PostgreDepartureDao()
    var directions by remember { mutableStateOf<List<Direction>>(emptyList()) }


    LaunchedEffect(Unit) {
        val dirs = withContext(Dispatchers.IO) {
            directionDao.getAll()
        }
        directions = dirs
    }

    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf("ID") }
    val expandedDirections = remember { mutableStateMapOf<Int, List<Departure>>() }
    var editingDirectionId by remember { mutableStateOf<Int?>(null) }
    val editableDirections = remember { mutableStateMapOf<Int, Direction>() }

    val filteredAndSortedDirections by remember(searchQuery, sortOption, directions) {
        derivedStateOf {
            directions
                .filter { it.name.contains(searchQuery, ignoreCase = true) }
                .sortedWith(
                    when (sortOption) {
                        "NAME" -> compareBy { it.name.lowercase() }
                        "LINE_ID" -> compareBy { it.lineId }
                        else -> compareBy { it.id ?: Int.MAX_VALUE }
                    }
                )
        }
    }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Išči po imenu") },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(
                onClick = {
                    sortOption = when (sortOption) {
                        "ID" -> "NAME"
                        "NAME" -> "LINE_ID"
                        else -> "ID"
                    }
                },
                modifier = Modifier.width(140.dp)
            ) {
                Text("Sort: $sortOption")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            if (filteredAndSortedDirections.isEmpty()) {
                item {
                    Text(
                        "Ni zadetkov.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        style = MaterialTheme.typography.subtitle1,
                        color = Color.Gray
                    )
                }
            } else {
                items(filteredAndSortedDirections) { direction ->
                    val departures = expandedDirections[direction.id] ?: emptyList()
                    val isEditing = editingDirectionId == direction.id
                    val editable = editableDirections.getOrPut(direction.id!!) { direction.copy() }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                        shape = RoundedCornerShape(8.dp),
                        elevation = 0.dp
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("ID: ${direction.id}", style = MaterialTheme.typography.caption)
                                    Text("Ime: ${direction.name}", style = MaterialTheme.typography.h6)
                                    Text("Linija ID: ${direction.lineId}", style = MaterialTheme.typography.body2)
                                }

                                Row {
                                    IconButton(onClick = {
                                        editingDirectionId = if (isEditing) null else direction.id
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Uredi")
                                    }

                                    IconButton(onClick = {
                                        val linkedDepartures = departureDao.getDeparturesForDirection(direction.id!!)
                                        if (linkedDepartures.isNotEmpty()) {
                                            expandedDirections[direction.id!!] = linkedDepartures
                                        } else {
                                            directionDao.delete(direction.id!!)
                                            directions = directionDao.getAll()
                                            expandedDirections.remove(direction.id)
                                        }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Izbriši")
                                    }
                                }
                            }

                            if (isEditing) {
                                var editedName by remember(direction.id) { mutableStateOf(editable.name) }
                                var editedLineId by remember(direction.id) { mutableStateOf(editable.lineId) }

                                Column(modifier = Modifier.padding(16.dp)) {
                                    OutlinedTextField(
                                        value = editedName,
                                        onValueChange = { editedName = it },
                                        label = { Text("Ime") },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    LineDropdown(
                                        selectedLineId = editedLineId,
                                        onLineSelected = {
                                            editedLineId = it
                                            editable.lineId = it
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = {
                                            val updated = direction.copy(name = editedName, lineId = editedLineId)
                                            directionDao.update(updated)
                                            directions = directionDao.getAll()
                                            editingDirectionId = null
                                            editableDirections.remove(direction.id)
                                        },
                                        modifier = Modifier
                                            .padding(top = 8.dp)
                                            .align(Alignment.End)
                                    ) {
                                        Text("Shrani spremembe")
                                    }
                                }
                            }

                            if (departures.isNotEmpty()) {
                                Divider()

                                Text(
                                    "Najprej izbriši ${departures.size} povezanih odhodov:",
                                    style = MaterialTheme.typography.subtitle2,
                                    modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                                )

                                Button(
                                    onClick = {
                                        departureDao.deleteAllForDirection(direction.id!!)
                                        directionDao.delete(direction.id!!)
                                        expandedDirections.remove(direction.id)
                                        directions = directionDao.getAll()
                                    },
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Text("Izbriši vse odhode in smer")
                                }

                                departures.forEach { departure ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("• ${departure.departure} (directionId=${departure.directionId})")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}