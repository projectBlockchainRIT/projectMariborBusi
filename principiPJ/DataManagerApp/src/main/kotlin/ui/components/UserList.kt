package ui.components

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

@Composable
fun UserList() {
    val userDao = PostgreUserDao()
    var users by remember { mutableStateOf(userDao.getAll()) }

    LazyColumn {
        items(users.size) { index ->
            val user = users[index]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                shape = RoundedCornerShape(8.dp),
                elevation = 4.dp
            ) {
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
                        Text("Email: ${user.email}", style = MaterialTheme.typography.body2)
                        Text("Tip: ${user.createdAt}", style = MaterialTheme.typography.body2)
                    }

                    IconButton(onClick = {
                        userDao.delete(user.id!!)
                        users = userDao.getAll()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Izbriši")
                    }
                }
            }
        }
    }
}
