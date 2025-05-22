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
import dao.postgres.PostgreRouteDao
import model.Route

@Composable
fun RouteList() {
    val routeDao = PostgreRouteDao()
    var routes by remember { mutableStateOf(routeDao.getAll()) }

    LazyColumn {
        items(routes.size) { index ->
            val route = routes[index]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 8.dp),
                shape = RoundedCornerShape(8.dp),
                elevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "ID: ${route.id ?: "-"}", style = MaterialTheme.typography.caption)
                        Text(text = "Ime: ${route.name}", style = MaterialTheme.typography.h6)
                        Text(text = "Linija ID: ${route.lineId}", style = MaterialTheme.typography.body2)
                        Text(text = "GeoJSON: ${route.path.toString().take(60)}...", style = MaterialTheme.typography.body2)
                    }

                    IconButton(onClick = {
                        routeDao.delete(route.id!!)
                        routes = routeDao.getAll() //refresh
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Izbriši"
                        )
                    }
                }
            }
        }
    }
}
