package ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dao.postgres.PostgreUserDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import model.User
import utils.FakeData.generateFakeUser

@Composable
fun GenerateUsersForm() {
    val userDao = PostgreUserDao()
    val coroutineScope = rememberCoroutineScope()

    var count by remember { mutableStateOf(10) }
    var generatedUsers by remember { mutableStateOf<List<User>>(emptyList()) }

    val scrollState = rememberScrollState()

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp)
    ) {
        OutlinedTextField(
            value = count.toString(),
            onValueChange = { count = it.toIntOrNull() ?: 0 },
            label = { Text("Število uporabnikov") },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFF990000),
                focusedLabelColor = Color(0xFF990000),
                cursorColor = Color(0xFF990000)
            )
        )

        Spacer(modifier = Modifier.weight(1f))

        if (generatedUsers.isNotEmpty()) {
            Text("Generiranih in shranjenih uporabnikov: ${generatedUsers.size}")
        }

        Button(
            onClick = {
                coroutineScope.launch {
                    val (existingIds, newUsers) = withContext(Dispatchers.IO) {
                        val existingIds = userDao.getAll().mapNotNull { it.id }.toSet()
                        val newUsers = mutableListOf<User>()
                        var nextId = 1

                        repeat(count) {
                            while (existingIds.contains(nextId)) {
                                nextId++
                            }
                            val user = generateFakeUser(nextId)
                            newUsers.add(user)
                            nextId++
                        }

                        newUsers.forEach { user ->
                            userDao.insert(user)
                        }

                        existingIds to newUsers
                    }

                    generatedUsers = newUsers
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
