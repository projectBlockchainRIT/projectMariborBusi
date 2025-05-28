package ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dao.postgres.PostgreLineDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import model.Line
import utils.FakeData.generateFakeLine

@Composable
fun GenerateLinesForm() {
    val lineDao = PostgreLineDao()
    val coroutineScope = rememberCoroutineScope()

    var count by remember { mutableStateOf(10) }
    var generatedLines by remember { mutableStateOf<List<Line>>(emptyList()) }

    val scrollState = rememberScrollState()

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = count.toString(),
            onValueChange = { count = it.toIntOrNull() ?: 0 },
            label = { Text("Število linij") },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFF990000),
                focusedLabelColor = Color(0xFF990000),
                cursorColor = Color(0xFF990000)
            )
        )

        Spacer(modifier = Modifier.weight(1f))

        if (generatedLines.isNotEmpty()) {
            Text("Generiranih in shranjenih linij: ${generatedLines.size}")
        }

        Button(
            onClick = {
                coroutineScope.launch {
                    val newLines = withContext(Dispatchers.IO) {
                        val lines = List(count) { generateFakeLine() }
                        lines.forEach { lineDao.insert(it) }
                        lines
                    }
                    generatedLines = newLines
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFF990000),
                contentColor = Color.White
            )
        ) {
            Text("Generiraj in Shrani")
        }
    }
}
