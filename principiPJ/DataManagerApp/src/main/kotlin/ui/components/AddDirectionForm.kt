package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.gson.JsonParser
import dao.postgres.PostgreDirectionDao
import dao.postgres.PostgreLineDao
import dao.postgres.PostgreStopDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import model.Route
import model.Stop
import model.Direction
import model.Line
import java.time.OffsetDateTime@Composable
fun AddDirectionForm() {
    val lineDao = PostgreLineDao()
    val directionDao = PostgreDirectionDao()

    var selectedLineId by remember { mutableStateOf<Int?>(null) }
    val lines = remember { mutableStateListOf<Line>() }
    var directionName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val fetchedLines = lineDao.getAll()
            lines.clear()
            lines.addAll(fetchedLines)
        }
    }

    val scrollState = rememberScrollState()

    Surface(
        color = MaterialTheme.colors.surface,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            //scrollabl del
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                OutlinedTextField(
                    value = directionName,
                    onValueChange = { directionName = it },
                    label = { Text("Ime smeri") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF990000),
                        focusedLabelColor = Color(0xFF990000),
                        cursorColor = Color(0xFF990000)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                LineDropdown(
                    selectedLineId = selectedLineId,
                    onLineSelected = { selectedLineId = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (errorMessage.isNotBlank()) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.fillMaxWidth()
                )
            }


            Button(
                onClick = {
                    if (directionName.isBlank()) {
                        errorMessage = "Ime smeri ne sme biti prazno."
                    } else if (selectedLineId == null) {
                        errorMessage = "Izberi linijo."
                    } else {
                        val direction = Direction(
                            name = directionName.trim(),
                            lineId = selectedLineId!!
                        )
                        directionDao.insert(direction)

                        errorMessage = "Direction uspešno dodan!"
                        directionName = ""
                        selectedLineId = null
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF990000),
                    contentColor = Color.White
                )
            ) {
                Text("Dodaj")
            }
        }
    }
}

