package ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import db.importDirectToDatabase
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import db.importJsonToDatabase
import kotlinx.coroutines.launch

@Composable
fun ScrapeDirectImport() {
    var message by remember { mutableStateOf("") }
    var isScraping by remember { mutableStateOf(false) }
    var waitingDots by remember { mutableStateOf(".") }

    // Animacija pik
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
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Spacer(Modifier.weight(1f))

                if (isScraping) {
                    Text(text = "Čakaj$waitingDots", color = Color.Gray)
                } else if (message != null) {
                    Text(text = message, color = Color.Blue)
                }

                Button(
                    onClick = {
                        isScraping = true
                        message = "Čakaj"
                        kotlinx.coroutines.GlobalScope.launch {
                            try {
                                importDirectToDatabase()
                                message = "Podatki so bili uspešno uvoženi."
                            } catch (e: Exception) {
                                message = "Napaka pri uvozu: ${e.message}"
                            } finally {
                                isScraping = false
                            }
                        }
                    },
                    enabled = !isScraping,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Scrape + Uvozi v bazo")
                }
            }
        }
    }
}
