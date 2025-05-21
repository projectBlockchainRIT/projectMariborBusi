package ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dao.postgres.*
import ui.components.StopRow

@Composable
fun ListScreen() {
    val stopDao = PostgreStopDao()
    val stops = remember { stopDao.getAll() }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn {
            items(stops.size) { index ->
                val stop = stops[index]
                StopRow(stop)
            }
        }
    }
}