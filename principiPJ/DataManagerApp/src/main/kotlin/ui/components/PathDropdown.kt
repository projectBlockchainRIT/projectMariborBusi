package ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalFocusManager
import dao.postgres.PostgreStopDao
import model.Stop

@Composable
fun PathDropdown(
    selectedStops: List<Stop>,
    onSelectionChanged: (List<Stop>) -> Unit
) {
    val stopDao = remember { PostgreStopDao() }
    val allStops = remember { stopDao.getAll() }

    var searchQuery by remember { mutableStateOf("") }
    var isDropdownOpen by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // Avtomatsko odpiranje dropdowna ob fokusu
    LaunchedEffect(isFocused) {
        if (isFocused) isDropdownOpen = true
    }

    val filteredStops by remember(searchQuery, allStops) {
        derivedStateOf {
            if (searchQuery.isBlank()) allStops
            else allStops.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                isDropdownOpen = true
            },
            label = { Text("Išči postaje po imenu") },
            modifier = Modifier.fillMaxWidth(),
            interactionSource = interactionSource,
            singleLine = true,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFF990000),
                focusedLabelColor = Color(0xFF990000),
                cursorColor = Color(0xFF990000)
            )
        )

        //prikaz izbranih postaj
        if (selectedStops.isNotEmpty()) {
            Text(
                text = "Izbrane postaje: ${selectedStops.joinToString { it.name }}",
                style = MaterialTheme.typography.body2,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (isDropdownOpen) {
            if (filteredStops.isEmpty()) {
                Text(
                    text = "Ni zadetkov",
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    color = Color.Gray
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    filteredStops.forEach { stop ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newSelection = if (selectedStops.contains(stop)) {
                                        selectedStops - stop
                                    } else {
                                        selectedStops + stop
                                    }
                                    onSelectionChanged(newSelection)
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedStops.contains(stop),
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(
                                    checkmarkColor = Color.White,
                                    checkedColor = Color(0xFF990000)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stop.name)
                        }
                        Divider()
                    }
                }
            }
        }
    }
}
