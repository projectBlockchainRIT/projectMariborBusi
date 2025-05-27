package ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dao.postgres.PostgreStopDao
import model.Stop


@Composable
fun StopDropdown(
    selectedId: Int,
    onSelect: (Int) -> Unit
) {
    val dao = remember { PostgreStopDao() }
    val all = remember { dao.getAll() }

    var query by remember { mutableStateOf(all.find { it.id == selectedId }?.name ?: "") }
    val filtered = remember(query) {
        all.filter { it.name.contains(query, ignoreCase = true) }
    }

    Column {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Postaja") },
            modifier = Modifier.fillMaxWidth()
        )
        filtered.forEach { stop ->
            Text(
                stop.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onSelect(stop.id!!)
                        query = stop.name
                    }
                    .padding(8.dp)
            )
            Divider()
        }
    }
}
