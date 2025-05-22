package ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dao.postgres.PostgreUserDao
import model.User
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun AddUserForm() {
    val userDao = PostgreUserDao()

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
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
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Uporabniško ime") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Geslo") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage.isNotBlank()) {
                    Text(
                        text = errorMessage,
                        color = if (errorMessage.contains("uspešno")) Color(0xFF2E7D32) else Color.Red,
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        if (username.isBlank() || email.isBlank() || password.isBlank()) {
                            errorMessage = "Vsa polja morajo biti izpolnjena."
                        } else {
                            val now = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                            val user = User(
                                username = username.trim(),
                                email = email.trim(),
                                password = password,
                                createdAt = now,
                                lastLogin = null
                            )
                            userDao.insert(user)
                            errorMessage = "Uporabnik uspešno dodan."

                            username = ""
                            email = ""
                            password = ""
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
