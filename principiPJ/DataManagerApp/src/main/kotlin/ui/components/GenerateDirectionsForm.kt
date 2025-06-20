package ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dao.postgres.PostgreDirectionDao
import dao.postgres.PostgreLineDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import model.Direction
import utils.FakeData.generateFakeDirection

@Composable
fun GenerateDirectionsForm() {
    val directionDao = PostgreDirectionDao()
    val lineDao = PostgreLineDao()
    val coroutineScope = rememberCoroutineScope()

    var count by remember { mutableStateOf(10) }
    var generatedDirections by remember { mutableStateOf<List<Direction>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = count.toString(),
            onValueChange = { count = it.toIntOrNull() ?: 0 },
            label = { Text("Število smeri") },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFF990000),
                focusedLabelColor = Color(0xFF990000),
                cursorColor = Color(0xFF990000)
            )
        )

        Spacer(modifier = Modifier.weight(1f))

        errorMessage?.let {
            Text(it, color = MaterialTheme.colors.error)
        }

        if (generatedDirections.isNotEmpty()) {
            Text("Generiranih smeri: ${generatedDirections.size}")
        }


        Button(
            onClick = {
                coroutineScope.launch {
                    errorMessage = null
                    val newDirections = withContext(Dispatchers.IO) {
                        val lines = lineDao.getAll()
                        if (lines.isEmpty()) {
                            return@withContext null
                        }

                        val directions = (1..count).map {
                            val randomLineId = lines.random().id!!
                            generateFakeDirection(randomLineId)
                        }

                        directions.forEach { directionDao.insert(it) }
                        directions
                    }

                    if (newDirections == null) {
                        errorMessage = "Najprej generiraj linije"
                        generatedDirections = emptyList()
                    } else {
                        generatedDirections = newDirections
                    }
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
