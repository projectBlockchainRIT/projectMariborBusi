package ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.toSize
import dao.postgres.PostgreLineDao
import model.Line

@Composable
fun LineDropdown(
    selectedLineId: Int?,
    onLineSelected: (Int) -> Unit
) {
    val lineDao = remember { PostgreLineDao() }
    val allLines = remember { lineDao.getAll() }

    var searchQuery by remember {
        mutableStateOf(allLines.find { it.id == selectedLineId }?.lineCode ?: "")
    }

    val filteredLines by remember(searchQuery, allLines) {
        derivedStateOf {
            if (searchQuery.isBlank()) allLines
            else allLines.filter { it.lineCode.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Linija") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (filteredLines.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                filteredLines.forEach { line ->
                    Text(
                        text = line.lineCode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                searchQuery = line.lineCode
                                onLineSelected(line.id ?: -1)
                            }
                            .padding(8.dp)
                    )
                    Divider()
                }
            }
        } else {
            Text(
                "Ni rezultatov",
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}
