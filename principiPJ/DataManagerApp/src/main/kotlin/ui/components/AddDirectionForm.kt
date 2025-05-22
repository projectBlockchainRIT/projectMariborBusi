package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.gson.JsonParser
import dao.postgres.PostgreDirectionDao
import dao.postgres.PostgreLineDao
import dao.postgres.PostgreStopDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import model.Route
import model.Stop
import model.Direction
import model.Line
import java.time.OffsetDateTime

@Composable
fun AddDirectionForm() {
    val lineDao = PostgreLineDao()
    val directionDao = PostgreDirectionDao()

    var selectedLineId by remember { mutableStateOf<Int?>(null) }
    var expandedLineDropdown by remember { mutableStateOf(false) }
    val lines = remember { mutableStateListOf<model.Line>() }
    var directionName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("")}

    LaunchedEffect(Unit) {
        runBlocking(Dispatchers.IO) {
            lines.clear()
            lines.addAll(lineDao.getAll())
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colors.surface,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Input za ime smeri
                OutlinedTextField(
                    value = directionName,
                    onValueChange = {
                        directionName = it
                    },
                    label = { Text("Ime smeri") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = lines.find { it.id == selectedLineId }?.lineCode ?: "Izberi linijo",
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth().clickable { expandedLineDropdown = true },
                        enabled = false,
                        label = { Text("Linija") }
                    )
                    DropdownMenu(
                        expanded = expandedLineDropdown,
                        onDismissRequest = { expandedLineDropdown = false }
                    ) {
                        if (lines.isEmpty()) {
                            DropdownMenuItem(onClick = {}) {
                                Text("Najprej dodaj linijo")
                            }
                        } else {
                            lines.forEach { line ->
                                DropdownMenuItem(onClick = {
                                    selectedLineId = line.id
                                    expandedLineDropdown = false
                                }) {
                                    Text(line.lineCode)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (errorMessage.isNotBlank()) {
                    Text(
                        text = errorMessage,
                        color = if (errorMessage.contains("uspešno")) Color(0xFF2E7D32) else Color.Red,
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Button(
                    onClick = {
                        if (directionName.isBlank()) {
                            errorMessage = "Ime smeri ne sme biti prazno."
                        } else if (selectedLineId == null) {
                            errorMessage = "Izberi linijo."
                        } else {
                            val direction = Direction(
                                name = directionName.trim(),
                                lineId = selectedLineId!!
                            )
                                directionDao.insert(direction)

                                errorMessage = "Direction uspešno dodan!"
                                directionName = ""
                                selectedLineId = null

                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Dodaj")
                }
            }
        }
    }
}
