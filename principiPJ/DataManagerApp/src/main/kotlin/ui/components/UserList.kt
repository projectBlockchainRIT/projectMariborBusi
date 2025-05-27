package ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dao.postgres.PostgreUserDao
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.graphics.Color

@Composable
fun UserList() {
    val userDao = PostgreUserDao()
    var users by remember { mutableStateOf(userDao.getAll()) }

    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf("ID") }

    var expandedEditUserId by remember { mutableStateOf<Int?>(null) }

    val filteredAndSortedUsers by remember(users, searchQuery, sortOption) {
        derivedStateOf {
            users
                .filter { it.username.contains(searchQuery, ignoreCase = true) }
                .sortedWith(
                    when (sortOption) {
                        "NAME" -> compareBy { it.username.lowercase() }
                        else -> compareBy { it.id ?: Int.MAX_VALUE }
                    }
                )
        }
    }

    Column {
        //search bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Išči po imenu") },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(
                onClick = {
                    sortOption = if (sortOption == "ID") "NAME" else "ID"
                },
                modifier = Modifier.width(140.dp)
            ) {
                Text("Sort: $sortOption")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            if (filteredAndSortedUsers.isEmpty()) {
                item {
                    Text(
                        "Ni zadetkov za iskanje.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        style = MaterialTheme.typography.subtitle1,
                        color = Color.Gray
                    )
                }
            } else {
                items(filteredAndSortedUsers.size) { index ->
                    val user = filteredAndSortedUsers[index]
                    val isEditing = user.id == expandedEditUserId

                    var editedUsername by remember(user.id) { mutableStateOf(user.username) }
                    var editedEmail by remember(user.id) { mutableStateOf(user.email) }
                    var editedPassword by remember(user.id) { mutableStateOf(user.password) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                        shape = RoundedCornerShape(8.dp),
                        elevation = 0.dp
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("ID: ${user.id ?: "-"}", style = MaterialTheme.typography.caption)
                                    Text("Ime: ${user.username}", style = MaterialTheme.typography.h6)
                                    Text("Ustvarjen: ${user.createdAt}", style = MaterialTheme.typography.body2)
                                }

                                Row {
                                    IconButton(onClick = {
                                        expandedEditUserId = if (isEditing) null else user.id
                                        editedUsername = user.username
                                        editedEmail = user.email
                                        editedPassword = user.password
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Uredi")
                                    }

                                    IconButton(onClick = {
                                        user.id?.let {
                                            userDao.delete(it)
                                            users = userDao.getAll()
                                        }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Izbriši")
                                    }
                                }
                            }

                            if (isEditing) {
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    OutlinedTextField(
                                        value = editedUsername,
                                        onValueChange = { editedUsername = it },
                                        label = { Text("Uporabniško ime") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = editedEmail,
                                        onValueChange = { editedEmail = it },
                                        label = { Text("Email") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = editedPassword,
                                        onValueChange = { editedPassword = it },
                                        label = { Text("Geslo") },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Button(
                                        onClick = {
                                            val updatedUser = user.copy(
                                                username = editedUsername,
                                                email = editedEmail,
                                                password = editedPassword
                                            )
                                            userDao.update(updatedUser)
                                            users = userDao.getAll()
                                            expandedEditUserId = null
                                        },
                                        modifier = Modifier
                                            .padding(top = 8.dp)
                                            .align(Alignment.End)
                                    ) {
                                        Text("Shrani spremembe")
                                    }
                                }
                            }
                        }
                    }
                }

            }
        }
    }
}

