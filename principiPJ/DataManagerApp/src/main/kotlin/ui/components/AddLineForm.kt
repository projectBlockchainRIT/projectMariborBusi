package ui.components

import androidx.compose.foundation.layout.*
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

    Box(
        modifier = Modifier
            .fillMaxSize(),
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
                    .padding(24.dp)
            ) {
                OutlinedTextField(
                    value = lineCode,
                    onValueChange = {
                        lineCode = it
                    },
                    label = { Text("Line code") },
                    modifier = Modifier.fillMaxWidth()
                )


                Spacer(Modifier.weight(1f))

                if (errorMessage.isNotBlank()) {
                    Text(
                        text = errorMessage,
                        color = if (errorMessage.contains("uspešno")) Color(0xFF2E7D32) else Color.Red,
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Button(
                    onClick = {
                        if (lineCode.isBlank()) {
                            errorMessage = "Line code ne sme biti prazen."
                        } else {
                            val line = Line (
                                lineCode = lineCode
                            )
                            lineDao.insert(line)
                            errorMessage = "Linija uspešno dodana"
                            lineCode = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Dodaj")
                }
            }
        }
    }
}
