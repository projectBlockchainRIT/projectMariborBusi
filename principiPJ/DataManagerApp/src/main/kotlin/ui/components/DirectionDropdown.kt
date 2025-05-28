package ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import dao.postgres.PostgreDirectionDao
import model.Direction

@Composable
fun DirectionDropdown(
    selectedId: Int?,
    onSelect: (Int) -> Unit
) {
    val dao = remember { PostgreDirectionDao() }
    val all = remember { dao.getAll() }

    var query by remember {
        mutableStateOf(all.find { it.id == selectedId }?.name ?: "")
    }

    val filtered = remember(query, all) {
        all.filter { it.name.contains(query, ignoreCase = true) }
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
            value = query,
            onValueChange = {
                query = it
                isDropdownOpen = true
            },
            label = { Text("Smer") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            interactionSource = interactionSource,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFF990000),
                focusedLabelColor = Color(0xFF990000),
                cursorColor = Color(0xFF990000)
            )
        )

        if (isDropdownOpen && filtered.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                filtered.forEach { dir ->
                    Text(
                        text = dir.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                query = dir.name
                                onSelect(dir.id!!)
                                isDropdownOpen = false
                                focusManager.clearFocus()  // skrije tipkovnico in odstrani fokus
                            }
                            .padding(8.dp)
                    )
                    Divider()
                }
            }
        } else if (isDropdownOpen && filtered.isEmpty()) {
            Text(
                "Ni rezultatov",
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

