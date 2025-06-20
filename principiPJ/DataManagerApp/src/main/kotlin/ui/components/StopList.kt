package ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dao.postgres.PostgreDepartureDao
import dao.postgres.PostgreRouteDao
import dao.postgres.PostgreStopDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import model.Route
import model.Departure
import model.Stop

@Composable
fun StopList() {
    val stopDao = PostgreStopDao()
    val departureDao = PostgreDepartureDao()
    var stops by remember { mutableStateOf<List<Stop>>(emptyList()) }

    LaunchedEffect(Unit) {
        val stps = withContext(Dispatchers.IO) {
            stopDao.getAll()
        }
        stops = stps
    }

    // za vsako postajo hranimo razširjeno stanje in pripadajoče departure
    val expandedStops = remember { mutableStateMapOf<Int, List<Departure>>() }
    var expandedStopEditId by remember { mutableStateOf<Int?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf("ID") } // ID ali NAME

    val filteredAndSortedStops by remember(searchQuery, sortOption, stops) {
        derivedStateOf {
            stops.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }.sortedWith(
                when (sortOption) {
                    "NAME" -> compareBy { it.name.lowercase() }
                    else -> compareBy { it.id }
                }
            )
        }
    }

    Column {
        //search bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Išči po imenu") },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFF990000),
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = Color(0xFF990000),
                    cursorColor = Color(0xFF990000)
                ),
                singleLine = true,
                maxLines = 1
            )

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(
                onClick = {
                    sortOption = if (sortOption == "ID") "NAME" else "ID"
                },
                modifier = Modifier
                    .width(140.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF990000)
                )
            ) {
                Text("Sort: $sortOption")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            if (filteredAndSortedStops.isEmpty()) {
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
                items(filteredAndSortedStops.size) { index ->
                    val stop = filteredAndSortedStops[index]
                    val departures = expandedStops[stop.id] ?: emptyList()
                    val isEditing = stop.id == expandedStopEditId

                    // Editable state (kopija postaje za urejanje)
                    var editedStop by remember(stop.id) {
                        mutableStateOf(stop.copy())
                    }

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
                                    Text(
                                        "ID: ${stop.id}",
                                        style = MaterialTheme.typography.caption,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        stop.name,
                                        style = MaterialTheme.typography.h6,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "Lat: ${stop.latitude}, Lon: ${stop.longitude}",
                                        style = MaterialTheme.typography.body2,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(onClick = {
                                    expandedStopEditId = if (isEditing) null else stop.id
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Uredi")
                                }

                                IconButton(onClick = {
                                    val linkedDepartures = stopDao.getDeparturesForStop(stop.id!!)
                                    if (linkedDepartures.isNotEmpty()) {
                                        expandedStops[stop.id!!] = linkedDepartures
                                    } else {
                                        stopDao.delete(stop.id!!)
                                        stops = stopDao.getAll()
                                        expandedStops.remove(stop.id)
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Izbriši")
                                }
                            }

                            if (isEditing) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    OutlinedTextField(
                                        value = editedStop.number,
                                        onValueChange = { editedStop = editedStop.copy(number = it) },
                                        label = { Text("Številka") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = TextFieldDefaults.outlinedTextFieldColors(
                                            focusedBorderColor = Color(0xFF990000),
                                            focusedLabelColor = Color(0xFF990000),
                                            cursorColor = Color(0xFF990000)
                                        )
                                    )

                                    OutlinedTextField(
                                        value = editedStop.name,
                                        onValueChange = { editedStop = editedStop.copy(name = it) },
                                        label = { Text("Ime") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = TextFieldDefaults.outlinedTextFieldColors(
                                            focusedBorderColor = Color(0xFF990000),
                                            focusedLabelColor = Color(0xFF990000),
                                            cursorColor = Color(0xFF990000)
                                        )
                                    )

                                    OutlinedTextField(
                                        value = editedStop.latitude.toString(),
                                        onValueChange = {
                                            editedStop =
                                                editedStop.copy(latitude = it.toDoubleOrNull() ?: editedStop.latitude)
                                        },
                                        label = { Text("Latitude") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = TextFieldDefaults.outlinedTextFieldColors(
                                            focusedBorderColor = Color(0xFF990000),
                                            focusedLabelColor = Color(0xFF990000),
                                            cursorColor = Color(0xFF990000)
                                        )
                                    )

                                    OutlinedTextField(
                                        value = editedStop.longitude.toString(),
                                        onValueChange = {
                                            editedStop =
                                                editedStop.copy(longitude = it.toDoubleOrNull() ?: editedStop.longitude)
                                        },
                                        label = { Text("Longitude") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = TextFieldDefaults.outlinedTextFieldColors(
                                            focusedBorderColor = Color(0xFF990000),
                                            focusedLabelColor = Color(0xFF990000),
                                            cursorColor = Color(0xFF990000)
                                        )

                                    )

                                    Button(
                                        onClick = {
                                            stopDao.update(editedStop)
                                            stops = stopDao.getAll()
                                            expandedStopEditId = null
                                        },
                                        modifier = Modifier
                                            .padding(top = 8.dp)
                                            .align(Alignment.End),
                                        colors = ButtonDefaults.buttonColors(
                                            backgroundColor = Color(0xFF990000),
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Text("Shrani spremembe")
                                    }
                                }
                            }

                            //če so povezani departure, jih prikaži
                            if (departures.isNotEmpty()) {
                                Divider()

                                Text(
                                    "Na to postajo je vezanih ${departures.size} odhodov:",
                                    style = MaterialTheme.typography.subtitle2,
                                    modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                //gumb "Izbriši vse"
                                Button(
                                    onClick = {
                                        departureDao.deleteAllForStop(stop.id!!)
                                        stopDao.delete(stop.id!!)
                                        expandedStops.remove(stop.id)
                                        stops = stopDao.getAll()
                                    },
                                    modifier = Modifier
                                        .padding(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = Color(0xFF990000),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("Izbriši vse odhode in postajo")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
