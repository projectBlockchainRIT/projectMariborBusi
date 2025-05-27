package ui.components


import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import dao.postgres.PostgreDepartureDao
import dao.postgres.PostgreLineDao
import dao.postgres.PostgreRouteDao
import model.Route

@Composable
fun LineList() {
    val lineDao = PostgreLineDao()
    var lines by remember { mutableStateOf(lineDao.getAll()) }

    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf("ID") } // "ID" ali "NAME"

    var expandedLineEditId by remember { mutableStateOf<Int?>(null) }

    val filteredAndSortedLines by remember(searchQuery, sortOption, lines) {
        derivedStateOf {
            lines
                .filter { it.lineCode.contains(searchQuery, ignoreCase = true) }
                .sortedWith(
                    when (sortOption) {
                        "NAME" -> compareBy { it.lineCode.lowercase() }
                        else -> compareBy { it.id ?: Int.MAX_VALUE }
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
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(
                onClick = {
                    sortOption = if (sortOption == "ID") "NAME" else "ID"
                },
                modifier = Modifier.width(140.dp)
            ) {
                Text("Sort: $sortOption")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        //prikaz linij
        LazyColumn {
            if (filteredAndSortedLines.isEmpty()) {
                item {
                    Text(
                        "Ni zadetkov za iskanje.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        style = MaterialTheme.typography.subtitle1,
                        color = Color.Gray
                    )
                }
            } else {
                items(filteredAndSortedLines.size) { index ->
                    val line = filteredAndSortedLines[index]

                    val isEditing = line.id == expandedLineEditId

                    var editedLineCode by remember(line.id) {
                        mutableStateOf(line.lineCode)
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
                                    Text("ID: ${line.id ?: "-"}", style = MaterialTheme.typography.caption)
                                    Text("Ime: ${line.lineCode}", style = MaterialTheme.typography.h6)
                                }

                                Row {
                                    IconButton(onClick = {
                                        expandedLineEditId = if (isEditing) null else line.id
                                        editedLineCode = line.lineCode
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Uredi")
                                    }

                                    IconButton(onClick = {
                                        line.id?.let {
                                            lineDao.delete(it)
                                            lines = lineDao.getAll()
                                        }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Izbriši")
                                    }
                                }
                            }

                            if (isEditing) {
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    OutlinedTextField(
                                        value = editedLineCode,
                                        onValueChange = { editedLineCode = it },
                                        label = { Text("Koda linije") },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Button(
                                        onClick = {
                                            val updatedLine = line.copy(lineCode = editedLineCode)
                                            lineDao.update(updatedLine)
                                            lines = lineDao.getAll()
                                            expandedLineEditId = null
                                        },
                                        modifier = Modifier
                                            .align(Alignment.End)
                                            .padding(top = 8.dp)
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
