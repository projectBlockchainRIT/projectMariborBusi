package ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dao.postgres.PostgreLineDao
import dao.postgres.PostgreRouteDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import model.Route
import utils.FakeData.generateFakeRoute

@Composable
fun GenerateRoutesForm() {
    val routeDao = PostgreRouteDao()
    val lineDao = PostgreLineDao()
    val coroutineScope = rememberCoroutineScope()

    var count by remember { mutableStateOf(10) }
    var generatedRoutes by remember { mutableStateOf<List<Route>>(emptyList()) }
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
            label = { Text("Število poti") },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFF990000),
                focusedLabelColor = Color(0xFF990000),
                cursorColor = Color(0xFF990000)
            )
        )

        errorMessage?.let {
            Text(it, color = MaterialTheme.colors.error)
        }

        Spacer(modifier = Modifier.weight(1f))

        if (generatedRoutes.isNotEmpty()) {
            Text("Generiranih poti: ${generatedRoutes.size}")
        }

        Button(
            onClick = {
                coroutineScope.launch {
                    val newRoutes = withContext(Dispatchers.IO) {
                        val lines = lineDao.getAll()
                        if (lines.isEmpty()) return@withContext null

                        val routes = (1..count).map {
                            val randomLineId = lines.random().id!!
                            generateFakeRoute(randomLineId)
                        }

                        routes.forEach { routeDao.insert(it) }
                        routes
                    }

                    if (newRoutes == null) {
                        errorMessage = "Najprej generiraj linije."
                        generatedRoutes = emptyList()
                    } else {
                        errorMessage = null
                        generatedRoutes = newRoutes
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
