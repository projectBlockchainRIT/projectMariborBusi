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
import dao.postgres.PostgreLineDao
import dao.postgres.PostgreRouteDao

import model.Route
@Composable
fun LineList() {
    val lineDao = PostgreLineDao()
    var lines by remember { mutableStateOf(lineDao.getAll()) }

    LazyColumn {
        items(lines.size) { index ->
            val line = lines[index]
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
                        Text("ID: ${line.id ?: "-"}", style = MaterialTheme.typography.caption)
                        Text("Ime: ${line.lineCode}", style = MaterialTheme.typography.h6)
                    }

                    IconButton(onClick = {
                        lineDao.delete(line.id!!)
                        lines = lineDao.getAll()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Izbriši")
                    }
                }
            }
        }
    }
}
