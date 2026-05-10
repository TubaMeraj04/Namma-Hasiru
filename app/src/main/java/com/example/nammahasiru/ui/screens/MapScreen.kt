package com.example.nammahasiru.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.nammahasiru.TreeViewModel
import com.example.nammahasiru.data.TreeEntity
import com.example.nammahasiru.ui.theme.GreenPrimary
import com.example.nammahasiru.ui.theme.NammaHasiruTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.example.nammahasiru.data.TreeDatabase
import com.google.android.gms.location.LocationServices
import com.example.nammahasiru.TreeViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(navController: NavController, viewModel: TreeViewModel) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedTree by remember { mutableStateOf<TreeEntity?>(null) }
    val context = LocalContext.current
    
    val trees by viewModel.allTrees.collectAsState(initial = emptyList())
    
    var hasLocationPermission by remember { mutableStateOf(false) }
    var showGpsDialog by remember { mutableStateOf(false) }

    fun checkGpsEnabled(ctx: Context) {
        val locationManager = ctx.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
            showGpsDialog = true
        }
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(12.9716, 77.5946), 12f)
    }

    @SuppressLint("MissingPermission")
    fun fetchLocationAndMoveCamera() {
        if (hasLocationPermission) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    scope.launch {
                        cameraPositionState.animate(
                            com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                                LatLng(location.latitude, location.longitude),
                                15f
                            )
                        )
                    }
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            checkGpsEnabled(context)
            fetchLocationAndMoveCamera()
        }
    }

    LaunchedEffect(Unit) {
        val isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        hasLocationPermission = isGranted
        if (!isGranted) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            checkGpsEnabled(context)
            fetchLocationAndMoveCamera()
        }
    }

    if (showGpsDialog) {
        AlertDialog(
            onDismissRequest = { showGpsDialog = false },
            title = { Text("Enable GPS") },
            text = { Text("GPS is disabled. Please enable it to view your location on the map.") },
            confirmButton = {
                TextButton(onClick = {
                    showGpsDialog = false
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }) {
                    Text("Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGpsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = hasLocationPermission)
        ) {
            trees.forEach { tree ->
                Marker(
                    state = MarkerState(position = LatLng(tree.latitude, tree.longitude)),
                    title = tree.speciesName,
                    snippet = "Status: ${tree.status}",
                    onClick = {
                        selectedTree = tree
                        showBottomSheet = true
                        true
                    }
                )
            }
        }

        // Top UI Layer Over Map
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingActionButton(
                onClick = { navController.navigate("dashboard") {
                    popUpTo("dashboard") { inclusive = false }
                } },
                containerColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Go back to Home", tint = GreenPrimary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Search location or species...", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            FloatingActionButton(
                onClick = { Toast.makeText(context, "Layers feature coming soon!", Toast.LENGTH_SHORT).show() },
                containerColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.List, contentDescription = "Layers", tint = GreenPrimary)
            }
        }
    }

    if (showBottomSheet && selectedTree != null) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    "${selectedTree!!.speciesName} \uD83C\uDF32",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = GreenPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Text(" Lat: ${String.format("%.4f", selectedTree!!.latitude)}, Lon: ${String.format("%.4f", selectedTree!!.longitude)}", color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Status: ${selectedTree!!.status} \uD83C\uDF1F", color = GreenPrimary, fontWeight = FontWeight.Bold)
                
                val date = Date(selectedTree!!.datePlanted)
                val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                Text("Planted: ${format.format(date)}", color = Color.DarkGray)
                
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        // Toggle status for demo
                        val nextStatus = if (selectedTree!!.status == "Planted") "Survived" else "Planted"
                        viewModel.updateTreeStatus(selectedTree!!, nextStatus)
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showBottomSheet = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text("Update Status Log")
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun MapMarker(x: androidx.compose.ui.unit.Dp, y: androidx.compose.ui.unit.Dp, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .offset(x = x, y = y)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Map Marker",
            tint = color,
            modifier = Modifier.size(48.dp)
        )
        // Inner dot
        Box(modifier = Modifier.size(12.dp).offset(y = (-4).dp).clip(CircleShape).background(Color.White))
    }
}

@Preview(showBackground = true)
@Composable
fun MapScreenPreview() {
    val context = LocalContext.current
    val database = TreeDatabase.getDatabase(context)
    val viewModel: TreeViewModel = viewModel(
        factory = TreeViewModelFactory(database.treeDao())
    )
    NammaHasiruTheme {
        MapScreen(navController = rememberNavController(), viewModel = viewModel)
    }
}
