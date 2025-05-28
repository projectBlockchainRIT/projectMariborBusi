package ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dao.postgres.PostgreLineDao
import model.Line

@Composable
fun AddLineForm() {
    val lineDao = PostgreLineDao()

    var lineCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                OutlinedTextField(
                    value = lineCode,
                    onValueChange = { lineCode = it },
                    label = { Text("Line code") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF990000),
                        focusedLabelColor = Color(0xFF990000),
                        cursorColor = Color(0xFF990000)
                    )
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
                    if (lineCode.isBlank()) {
                        errorMessage = "Line code ne sme biti prazen."
                    } else {
                        val line = Line(
                            lineCode = lineCode
                        )
                        lineDao.insert(line)
                        errorMessage = "Linija uspešno dodana"
                        lineCode = ""
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

