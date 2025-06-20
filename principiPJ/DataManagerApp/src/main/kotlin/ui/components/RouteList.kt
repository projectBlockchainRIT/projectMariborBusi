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
import dao.postgres.PostgreLineDao
import dao.postgres.PostgreRouteDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import model.Departure
import model.Route

@Composable
fun RouteList() {
    val routeDao = PostgreRouteDao()
    var routes by remember { mutableStateOf<List<Route>>(emptyList()) }

    LaunchedEffect(Unit) {
        val rts = withContext(Dispatchers.IO) {
            routeDao.getAll()
        }
        routes = rts
    }

    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf("ID") }

    var expandedRouteId by remember { mutableStateOf<Int?>(null) }
    val editableRoutes = remember { mutableStateMapOf<Int, Route>() }

    val filteredAndSortedRoutes by remember(searchQuery, sortOption, routes) {
        derivedStateOf {
            routes
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
                    sortOption = when (sortOption) {
                        "ID" -> "NAME"
                        "NAME" -> "LINE_ID"
                        else -> "ID"
                    }
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
            if (filteredAndSortedRoutes.isEmpty()) {
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
                items(filteredAndSortedRoutes.size) { index ->
                    val route = filteredAndSortedRoutes[index]
                    val isExpanded = expandedRouteId == route.id

                    val editedRoute = editableRoutes.getOrPut(route.id!!) {
                        route.copy()
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
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "ID: ${route.id ?: "-"}",
                                        style = MaterialTheme.typography.caption,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "Ime: ${route.name}",
                                        style = MaterialTheme.typography.h6,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "Linija ID: ${route.lineId}",
                                        style = MaterialTheme.typography.body2,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "GeoJSON: ${route.path.toString().take(60)}...",
                                        style = MaterialTheme.typography.body2,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row {
                                    IconButton(onClick = {
                                        expandedRouteId = if (isExpanded) null else route.id
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Uredi"
                                        )
                                    }

                                    IconButton(onClick = {
                                        routeDao.delete(route.id!!)
                                        routes = routeDao.getAll()
                                        editableRoutes.remove(route.id)
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Izbriši")
                                    }
                                }
                            }

                            if (isExpanded) {
                                val lineDao = PostgreLineDao()
                                val allLineIds = remember { lineDao.getAll().mapNotNull { it.id } }

                                var editedName by remember(route.id) { mutableStateOf(editedRoute.name) }
                                var editedLineId by remember(route.id) { mutableStateOf(editedRoute.lineId) }
                                var editedPathString by remember(route.id) { mutableStateOf(editedRoute.path.toString()) }

                                Column(modifier = Modifier.padding(16.dp)) {
                                    OutlinedTextField(
                                        value = editedName,
                                        onValueChange = { editedName = it },
                                        label = { Text("Ime") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = TextFieldDefaults.outlinedTextFieldColors(
                                            focusedBorderColor = Color(0xFF990000),
                                            focusedLabelColor = Color(0xFF990000),
                                            cursorColor = Color(0xFF990000)
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    var editedLineId by remember { mutableStateOf(editedRoute.lineId) }

                                    LineDropdown(
                                        selectedLineId = editedLineId,
                                        onLineSelected = { newLineId ->
                                            editedLineId = newLineId
                                            editedRoute.lineId = newLineId
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = editedPathString,
                                        onValueChange = { editedPathString = it },
                                        label = { Text("GeoJSON (kot string)") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp),
                                        singleLine = false,
                                        colors = TextFieldDefaults.outlinedTextFieldColors(
                                            focusedBorderColor = Color(0xFF990000),
                                            focusedLabelColor = Color(0xFF990000),
                                            cursorColor = Color(0xFF990000)
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = {
                                            try {
                                                var safeJson = editedPathString.trim()

                                                if (!safeJson.startsWith("[[")) {
                                                    safeJson = "[$safeJson]"
                                                }

                                                val updatedRoute = route.copy(
                                                    name = editedName,
                                                    lineId = editedLineId,
                                                    path = Json.parseToJsonElement(safeJson)
                                                )
                                                routeDao.update(updatedRoute)
                                                routes = routeDao.getAll()
                                                expandedRouteId = null
                                                editableRoutes.remove(route.id)
                                            } catch (e: Exception) {
                                                println("Napaka pri parsiranju JSON-a: ${e.message}")
                                            }
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
                        }
                    }
                }
            }
        }
    }
}

