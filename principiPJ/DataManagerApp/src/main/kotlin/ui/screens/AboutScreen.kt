package ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp


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
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            textAlign = TextAlign.Center
        )

        Spacer(
            Modifier.height(16.dp)
        )

        Text("App version: Beta release 1.0.0")
    }
}