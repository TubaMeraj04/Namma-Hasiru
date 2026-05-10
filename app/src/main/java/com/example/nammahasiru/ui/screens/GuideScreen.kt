package com.example.nammahasiru.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.nammahasiru.ui.theme.EarthenBrown
import com.example.nammahasiru.ui.theme.GreenPrimary
import com.example.nammahasiru.ui.theme.NammaHasiruTheme

data class PlantGuide(
    val name: String,
    val scientificName: String,
    val soil: String,
    val water: String,
    val successRate: String,
    val recommended: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen() {
    var selectedFilter by remember { mutableStateOf("All") }
    val allGuides = listOf(
        PlantGuide("Neem", "Azadirachta indica", "Sandy, loamy", "Low", "92%", true),
        PlantGuide("Banyan", "Ficus benghalensis", "Well-drained alluvium", "Moderate", "88%", true),
        PlantGuide("Peepal", "Ficus religiosa", "All soil types", "Moderate", "85%", false),
        PlantGuide("Jackfruit", "Artocarpus heterophyllus", "Rich, deep soil", "High", "70%", false),
        PlantGuide("Tamarind", "Tamarindus indica", "Clay, Loam", "Low", "95%", true)
    )
    val guides = when (selectedFilter) {
        "High Survival" -> allGuides.filter { it.successRate.removeSuffix("%").toInt() > 85 }
        "Low Water" -> allGuides.filter { it.water == "Low" }
        "Fast Growing" -> allGuides.filter { it.recommended }
        else -> allGuides
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = GreenPrimary,
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "Species Guide \uD83D\uDCD6",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Data-driven recommendations for your local soil profile. Plant smarter.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filters Pill Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = selectedFilter == "All", onClick = { selectedFilter = "All" }, label = { Text("All", fontWeight = if (selectedFilter == "All") FontWeight.Bold else FontWeight.Normal) })
            FilterChip(selected = selectedFilter == "High Survival", onClick = { selectedFilter = "High Survival" }, label = { Text("High Survival") })
            FilterChip(selected = selectedFilter == "Low Water", onClick = { selectedFilter = "Low Water" }, label = { Text("Low Water") })
            FilterChip(selected = selectedFilter == "Fast Growing", onClick = { selectedFilter = "Fast Growing" }, label = { Text("Fast Growing") })
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(guides) { guide ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = guide.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = EarthenBrown
                                )
                                Text(
                                    text = guide.scientificName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                            if (guide.recommended) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFFFF8E1), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Best Match", color = Color(0xFFF57C00), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        
                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFE0E0E0))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Optimal Soil", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                                Text(guide.soil, fontWeight = FontWeight.Medium)
                            }
                            Column {
                                Text("Water Needs", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                                Text(guide.water, fontWeight = FontWeight.Medium)
                            }
                            Column {
                                Text("Survival Rate", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                                Text(guide.successRate, fontWeight = FontWeight.Bold, color = GreenPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GuideScreenPreview() {
    NammaHasiruTheme {
        GuideScreen()
    }
}
