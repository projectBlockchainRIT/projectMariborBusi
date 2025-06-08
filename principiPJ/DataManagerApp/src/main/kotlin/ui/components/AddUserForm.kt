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

    val scrollState = rememberScrollState()

    Surface(
        color = MaterialTheme.colors.surface,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        )  {
            // scrollabilen del z obrazcem
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Uporabniško ime") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF990000),
                        focusedLabelColor = Color(0xFF990000),
                        cursorColor = Color(0xFF990000)
                    )
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF990000),
                        focusedLabelColor = Color(0xFF990000),
                        cursorColor = Color(0xFF990000)
                    )
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Geslo") },
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

            // gumb fiksiran na dnu zaslona
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

