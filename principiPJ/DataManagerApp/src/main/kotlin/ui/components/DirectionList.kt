package ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dao.postgres.PostgreDirectionDao
import androidx.compose.runtime.*
import dao.postgres.PostgreDepartureDao


@Composable
fun DirectionList() {
    val directionDao = PostgreDirectionDao()
    val departureDao = PostgreDepartureDao()
    var directions by remember { mutableStateOf(directionDao.getAll()) }

    //prikaz povezanih departures
    var expandedDirectionId by remember { mutableStateOf<Int?>(null) }

    LazyColumn {
        items(directions.size) { index ->
            val direction = directions[index]
            val departures = departureDao.getDeparturesForDirection(direction.id!!)
            val hasDepartures = departures.isNotEmpty()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                shape = RoundedCornerShape(8.dp),
                elevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ID: ${direction.id}", style = MaterialTheme.typography.caption)
                            Text("Ime: ${direction.name}", style = MaterialTheme.typography.h6)
                            Text("Linija ID: ${direction.lineId}", style = MaterialTheme.typography.body2)
                        }

                        IconButton(onClick = {
                            if (hasDepartures) {
                                expandedDirectionId = if (expandedDirectionId == direction.id) null else direction.id
                            } else {
                                directionDao.delete(direction.id)
                                directions = directionDao.getAll()
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Izbriši")
                        }
                    }

                    if (expandedDirectionId == direction.id && hasDepartures) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Pred brisanjem je treba odstraniti ${departures.size} odhodov:",
                            style = MaterialTheme.typography.subtitle2,
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                        )

                        Button(onClick = {
                            departureDao.deleteAllForDirection(direction.id!!)
                            directionDao.delete(direction.id!!)
                            directions = directionDao.getAll()
                            expandedDirectionId = null
                            },
                            modifier = Modifier
                                .padding(8.dp)
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
                                Text("• ${departure.departure} (directionId=${departure.directionId})", style = MaterialTheme.typography.body2)
                            }
                        }
                    }
                }
            }
        }
    }
}

