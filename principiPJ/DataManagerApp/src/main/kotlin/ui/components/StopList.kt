package ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dao.postgres.PostgreDepartureDao
import dao.postgres.PostgreRouteDao
import dao.postgres.PostgreStopDao
import model.Route
import model.Departure

@Composable
fun StopList() {
    val stopDao = PostgreStopDao()
    val departureDao = PostgreDepartureDao()
    var stops by remember { mutableStateOf(stopDao.getAll()) }

    // za vsako postajo hranimo razširjeno stanje in pripadajoče departure
    val expandedStops = remember { mutableStateMapOf<Int, List<Departure>>() }

    LazyColumn {
        items(stops.size) { index ->
            val stop = stops[index]
            val departures = expandedStops[stop.id] ?: emptyList()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                shape = RoundedCornerShape(8.dp),
                elevation = 4.dp
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
                            Text("ID: ${stop.id}", style = MaterialTheme.typography.caption)
                            Text(stop.name, style = MaterialTheme.typography.h6)
                            Text(
                                "Lat: ${stop.latitude}, Lon: ${stop.longitude}",
                                style = MaterialTheme.typography.body2
                            )
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

                    // Če so povezani departure, jih prikaži
                    if (departures.isNotEmpty()) {
                        Divider()

                        Text(
                            "Najprej izbriši ${departures.size} povezanih odhodov:",
                            style = MaterialTheme.typography.subtitle2,
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                        )

                        // Gumb "Izbriši vse"
                        Button(
                            onClick = {
                                departureDao.deleteAllForStop(stop.id!!)
                                stopDao.delete(stop.id!!)
                                expandedStops.remove(stop.id)
                                stops = stopDao.getAll()
                            },
                            modifier = Modifier
                                .padding(8.dp)
                        ) {
                            Text("Izbriši vse odhode in postajo")
                        }

                        departures.forEach { departure ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("• ${departure.departure} (stopId=${departure.stopId})", style = MaterialTheme.typography.body2)
                            }
                        }
                    }
                }
            }
        }
    }
}
