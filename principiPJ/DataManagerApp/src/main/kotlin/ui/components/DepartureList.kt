package ui.components

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
import model.Route

@Composable
fun DepartureList() {
    val departureDao = PostgreDepartureDao()
    var departures by remember { mutableStateOf(departureDao.getAll()) }

    LazyColumn {
        items(departures.size) { index ->
            val dep = departures[index]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                shape = RoundedCornerShape(8.dp),
                elevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ID: ${dep.id ?: "-"}", style = MaterialTheme.typography.caption)
                        Text("Odhod: ${dep.departure}", style = MaterialTheme.typography.h6)
                        Text("Postaja ID: ${dep.stopId}", style = MaterialTheme.typography.body2)
                        Text("Smer ID: ${dep.directionId}", style = MaterialTheme.typography.body2)
                    }

                    IconButton(onClick = {
                        departureDao.delete(dep.id!!)
                        departures = departureDao.getAll()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Izbriši")
                    }
                }
            }
        }
    }
}

