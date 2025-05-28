package ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
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

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val focusManager = LocalFocusManager.current

    var isDropdownOpen by remember { mutableStateOf(false) }

    // Odpri dropdown, ko dobi fokus
    LaunchedEffect(isFocused) {
        if (isFocused) isDropdownOpen = true
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                isDropdownOpen = true
            },
            label = { Text("Linija") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            interactionSource = interactionSource,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFF990000),
                focusedLabelColor = Color(0xFF990000),
                cursorColor = Color(0xFF990000)
            )
        )

        if (isDropdownOpen && filteredLines.isNotEmpty()) {
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
                                isDropdownOpen = false
                                focusManager.clearFocus() // Da skrijemo tipkovnico, če je odprta
                            }
                            .padding(8.dp)
                    )
                    Divider()
                }
            }
        }
    }
}



