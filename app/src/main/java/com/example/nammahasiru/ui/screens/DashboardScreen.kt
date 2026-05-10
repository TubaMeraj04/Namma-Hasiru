package com.example.nammahasiru.ui.screens

import androidx.compose.ui.platform.LocalContext

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.tooling.preview.Preview
import com.example.nammahasiru.TreeViewModel
import com.example.nammahasiru.ui.theme.EarthenBrown
import com.example.nammahasiru.ui.theme.GreenPrimary
import com.example.nammahasiru.ui.theme.NammaHasiruTheme
import com.example.nammahasiru.data.TreeDatabase
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nammahasiru.TreeViewModelFactory

import androidx.compose.material.icons.filled.ExitToApp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun DashboardScreen(navController: NavController, viewModel: TreeViewModel) {
    DashboardContent(navController = navController, viewModel = viewModel)
}

@Composable
fun DashboardContent(navController: NavController, viewModel: TreeViewModel) {
    var animationPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = true) {
        animationPlayed = true
    }
    
    val totalTrees by viewModel.totalTrees.collectAsState()
    val survivedTrees by viewModel.survivedTrees.collectAsState()
    val trees by viewModel.allTrees.collectAsState(initial = emptyList())
    
    val targetProgress = if (totalTrees > 0) survivedTrees.toFloat() / totalTrees.toFloat() else 0f
    val survivalRatePercent = if (totalTrees > 0) (survivedTrees * 100) / totalTrees else 0

    val progressAnimation by animateFloatAsState(
        targetValue = if (animationPlayed) targetProgress else 0f,
        animationSpec = tween(durationMillis = 1500),
        label = "progress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp, top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Welcome to Namma Hasiru \uD83C\uDF3F",
                style = MaterialTheme.typography.headlineMedium,
                color = GreenPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = GreenPrimary)
            }
        }

        // Survival Score Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Village Overall Impact \uD83C\uDF0D",
                    style = MaterialTheme.typography.titleMedium,
                    color = EarthenBrown,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${survivalRatePercent}%",
                        style = MaterialTheme.typography.displayLarge,
                        color = GreenPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = " survival rate",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                LinearProgressIndicator(
                    progress = progressAnimation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = GreenPrimary,
                    trackColor = Color(0xFFC8E6C9)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("$totalTrees planted", color = Color.DarkGray, style = MaterialTheme.typography.bodySmall)
                    Text("$survivedTrees survived", color = GreenPrimary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
        }

        Text(
            text = "Upcoming Actions \uD83D\uDD14",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Reminders List
        if (trees.isEmpty()) {
            Text("No plants geotagged yet. Add your first plant!", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(trees.size) { index ->
                    val tree = trees[index]
                    val daysSincePlanted = ((System.currentTimeMillis() - tree.datePlanted) / (1000 * 60 * 60 * 24)).toInt()
                    val isUrgent = daysSincePlanted >= 90 && tree.status != "Survived" && tree.status != "Died" && tree.status != "Dead"
                    
                    ReminderItem(
                        treeName = "${tree.speciesName} (Lat: ${String.format("%.2f", tree.latitude)}, Lon: ${String.format("%.2f", tree.longitude)})",
                        daysSincePlanted = daysSincePlanted,
                        isUrgent = isUrgent,
                        status = tree.status,
                        onClick = { navController.navigate("map") }
                    )
                }
            }
        }
    }
}

@Composable
fun ReminderItem(treeName: String, daysSincePlanted: Int, isUrgent: Boolean, status: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUrgent) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(if (isUrgent) Color(0xFFFF9800) else GreenPrimary, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isUrgent) Icons.Default.Warning else Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = treeName, fontWeight = FontWeight.Bold)
                    Text(
                        text = when {
                            status == "Survived" -> "Status: Survived \uD83C\uDF1F"
                            status == "Died" || status == "Dead" -> "Status: Died 🔴"
                            isUrgent -> "Needs update now ($daysSincePlanted Days)"
                            else -> "Check in ${90 - daysSincePlanted} Days"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            status == "Survived" -> GreenPrimary
                            status == "Died" || status == "Dead" -> Color.Red
                            isUrgent -> Color(0xFFE65100)
                            else -> Color.DarkGray
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        status == "Survived" -> GreenPrimary
                        status == "Died" || status == "Dead" -> Color.Gray
                        isUrgent -> Color(0xFFFF9800)
                        else -> GreenPrimary
                    }
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isUrgent && status != "Survived" && status != "Died" && status != "Dead") "Update" else "View")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    val context = LocalContext.current
    val database = TreeDatabase.getDatabase(context)
    val viewModel: TreeViewModel = viewModel(
        factory = TreeViewModelFactory(database.treeDao())
    )
    NammaHasiruTheme {
        DashboardScreen(navController = rememberNavController(), viewModel = viewModel)
    }
}
