import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Share
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
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
                    icon = Icons.Default.Add,
                    isSelected = selectedScreen == "addBusStop",
                    onClick = { selectedScreen = "addBusStop" }
                )

                NavItem(
                    text = "Bus Stops",
                    icon = Icons.Default.Menu,
                    isSelected = selectedScreen == "busStops",
                    onClick = { selectedScreen = "busStops" }
                )

                Divider(
                    Modifier.padding(vertical = 8.dp)
                )

                NavItem(
                    text = "Scraper",
                    icon = Icons.Outlined.Share,
                    isSelected = selectedScreen == "scraper",
                    onClick = { selectedScreen = "scraper" }
                )

                NavItem(
                    text = "Generator",
                    icon = Icons.Outlined.Edit,
                    isSelected = selectedScreen == "generator",
                    onClick = { selectedScreen = "generator" }
                )

                Spacer(
                    Modifier.weight(1f)
                )

                NavItem(
                    text = "About",
                    icon = Icons.Outlined.Info,
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
                when (selectedScreen) {
                    "addBusStop" -> AddBusStopScreen()
                    "busStops" -> BusStopsListScreen()
                    "scraper" -> ScraperScreen()
                    "generator" -> GeneratorScreen()
                    "about" -> AboutScreen()
                    else -> Text("Select an option")
                }
            }
        }
    }
}

//komponente
@Composable
fun AddBusStopScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Add Bus Stop")
        //dodajanje postaje
    }
}

@Composable
fun BusStopsListScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("List of Bus Stops")
        //seznam postaj
    }
}

@Composable
fun ScraperScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Scraper")
        //scraper
    }
}

@Composable
fun GeneratorScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Generator")
        //generator postaj
    }
}

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Project: M-Busi", style = MaterialTheme.typography.h5)

        Spacer(
            Modifier.height(16.dp)
        )

        Text(
            text = "Aplikacija za upravljanje s podatkovno bazo s funkcijami " +
                    "izpisovanja, urejanja in dodajanja podatkov.",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(
            Modifier.height(16.dp)
        )

        Text("App version: 1.0.0")
    }
}

@Composable
fun NavItem(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isSelected) Color.LightGray.copy(alpha = 0.3f) else Color.Transparent
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface,
            modifier = Modifier.size(24.dp)
        )

        Spacer(
            Modifier.width(8.dp)
        )

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
