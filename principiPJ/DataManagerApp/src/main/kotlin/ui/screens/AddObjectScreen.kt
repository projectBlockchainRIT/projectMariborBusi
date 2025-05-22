package ui.screens
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ui.components.*
@Composable
fun AddObjectScreen() {
    var selectedEntity by remember { mutableStateOf("Stop") }
    val entities = listOf("Stop", "Line", "Route", "Direction", "Departure", "User")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            entities.forEach { entity ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedEntity = entity }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = entity,
                        style = if (selectedEntity == entity) {
                            MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.primary)
                        } else {
                            MaterialTheme.typography.body1
                        },
                        maxLines = 1
                    )
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 0.dp))

        when (selectedEntity) {
            "Stop" -> AddStopForm()
            "Line" -> AddLineForm()
            "User" -> AddUserForm()
            "Route" -> AddRouteForm()
            "Direction" -> AddDirectionForm()
            "Departure" -> AddDepartureForm()
            else -> Text("Izberi entiteto za dodajanje")
        }
    }
}



