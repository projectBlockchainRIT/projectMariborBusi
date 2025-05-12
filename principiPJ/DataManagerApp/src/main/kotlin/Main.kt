import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application



@Composable
@Preview
fun App() {
    var selectedScreen by remember { mutableStateOf("busStops") }

    MaterialTheme {
        Row (
            Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
            ) {
                NavItem(
                    text = "Add Bus Stop",
                    isSelected = selectedScreen == "busStops",
                    onClick = { selectedScreen = "busStops" }
                )

                NavItem(
                    text = "Bus Stops",
                    isSelected = selectedScreen == "addBusStop",
                    onClick = { selectedScreen = "addBusStop" }
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                NavItem(
                    text = "Scraper",
                    isSelected = selectedScreen == "scraper",
                    onClick = { selectedScreen = "scraper" }
                )

                NavItem(
                    text = "Generator",
                    isSelected = selectedScreen == "generator",
                    onClick = { selectedScreen = "generator" }
                )

                Spacer(modifier = Modifier.weight(1f))

                NavItem(
                    text = "About",
                    isSelected = selectedScreen == "about",
                    onClick = { selectedScreen = "about" }
                )
            }


            Column (
                modifier = Modifier
                    .weight(3f)
                    .padding(8.dp)
                    .border(1.dp, Color.LightGray, shape = RoundedCornerShape(8.dp))
                    .padding(8.dp)
                    .fillMaxSize()
            ) {
                Text("content")

            }
        }
    }
}

@Composable
fun NavItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isSelected) Color.LightGray.copy(alpha = 0.3f) else Color.Transparent
            )
            .padding(16.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            color = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
        )
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Database Manager App") {
        App()
    }
}
