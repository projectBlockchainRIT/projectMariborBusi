package ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.io.File
import scraper.*
import utils.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@Composable
fun RunScraperForm() {
    var directory by remember { mutableStateOf<File?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val date = "2025-05-22" // možnost vnosa kasneje
    var isScraping by remember { mutableStateOf(false) }
    var waitingDots by remember { mutableStateOf(".") }
    val scope = rememberCoroutineScope()

    // Animacija pikic med čakanjem
    LaunchedEffect(isScraping) {
        while (isScraping) {
            waitingDots = when (waitingDots) {
                "." -> ".."
                ".." -> "..."
                "..." -> "."
                else -> "."
            }
            kotlinx.coroutines.delay(500)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Izbira mape
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    directory = chooseDirectory("Mapa")
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Izberi", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }

            Text(
                text = directory?.absolutePath ?: "Izberi mapo za shranjevanje",
                modifier = Modifier
                    .weight(3f)
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            )
        }

        Spacer(Modifier.weight(1f))

        if (isScraping) {
            Text(text = "Čakaj$waitingDots", color = Color.Gray)
        } else if (message != null) {
            Text(text = message!!, color = Color.Blue)
        }

        Button(
            onClick = {
                if (directory != null) {
                    isScraping = true
                    message = null
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                val stopsFile = File(directory, "stops_maribor_$date.json")
                                val routesFile = File(directory, "routes_maribor_$date.json")
                                val departuresFile = File(directory, "departures_maribor_$date.json")

                                runStopsScraperToLocation(stopsFile.absolutePath)
                                runRoutesScraperToLocation(routesFile.absolutePath)
                                runDeparturesScraperToLocation(departuresFile.absolutePath)
                            }

                            message = "Scraperji uspešno zagnani in podatki shranjeni!"
                        } catch (e: Exception) {
                            message = "Napaka pri zagonu scraperjev: ${e.message}"
                            e.printStackTrace()
                        } finally {
                            isScraping = false
                        }
                    }
                } else {
                    message = "Prosimo, najprej izberite lokacijo."
                }
            },
            enabled = !isScraping,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Poženi scraperje")
        }
    }
}
