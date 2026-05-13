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
import androidx.compose.ui.unit.sp
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
import coil.compose.AsyncImage
import java.io.File
import android.location.Geocoder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(navController: NavController, viewModel: TreeViewModel) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedTree by remember { mutableStateOf<TreeEntity?>(null) }
    val context = LocalContext.current
    
    val trees by viewModel.allTrees.collectAsState(initial = emptyList())
    
    var mapType by remember { mutableStateOf(MapType.NORMAL) }
    var filterStatus by remember { mutableStateOf("All") }
    var showLayersSheet by remember { mutableStateOf(false) }
    val layerSheetState = rememberModalBottomSheetState()
    
    val filteredTrees = remember(trees, filterStatus) {
        if (filterStatus == "All") {
            trees
        } else {
            trees.filter { it.status == filterStatus }
        }
    }
    
    var hasLocationPermission by remember { mutableStateOf(false) }
    var showGpsDialog by remember { mutableStateOf(false) }

    fun checkGpsEnabled(ctx: Context) {
        val locationManager = ctx.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
            showGpsDialog = true
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    
    val finalTrees = remember(filteredTrees, searchQuery) {
        if (searchQuery.isEmpty()) {
            filteredTrees
        } else {
            filteredTrees.filter { it.speciesName.contains(searchQuery, ignoreCase = true) }
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(12.9716, 77.5946), 12f)
    }

    fun performSearch(query: String) {
        if (query.isEmpty()) return
        
        // Search in plants first
        val matchedPlant = trees.find { it.speciesName.contains(query, ignoreCase = true) }
        if (matchedPlant != null) {
            scope.launch {
                cameraPositionState.animate(
                    com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                        LatLng(matchedPlant.latitude, matchedPlant.longitude),
                        18f
                    )
                )
            }
            return
        }

        // Otherwise use Geocoder for location search
        try {
            val geocoder = Geocoder(context)
            val addresses = geocoder.getFromLocationName(query, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                scope.launch {
                    cameraPositionState.animate(
                        com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                            LatLng(address.latitude, address.longitude),
                            15f
                        )
                    )
                }
            } else {
                Toast.makeText(context, "Location or species not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error searching: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    


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
            properties = MapProperties(
                isMyLocationEnabled = hasLocationPermission,
                mapType = mapType
            ),
            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false)
        ) {
            finalTrees.forEach { tree ->
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
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search location or species...", style = MaterialTheme.typography.bodyMedium, color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = GreenPrimary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { performSearch(searchQuery) })
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            FloatingActionButton(
                onClick = { showLayersSheet = true },
                containerColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.List, contentDescription = "Layers", tint = GreenPrimary)
            }
        }
    }

    // Layers and Filters Bottom Sheet
    if (showLayersSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLayersSheet = false },
            sheetState = layerSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp)
            ) {
                Text(
                    text = "Map Layers & Filters \uD83D\uDDFA️",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = GreenPrimary
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text("Map Type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MapTypeButton(
                        label = "Default",
                        isSelected = mapType == MapType.NORMAL,
                        onClick = { mapType = MapType.NORMAL }
                    )
                    MapTypeButton(
                        label = "Satellite",
                        isSelected = mapType == MapType.SATELLITE,
                        onClick = { mapType = MapType.SATELLITE }
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text("Filter Plants by Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(12.dp))
                
                val statuses = listOf("All", "Planted", "Survived", "Needs Care")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    statuses.chunked(2).forEach { rowStatuses ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowStatuses.forEach { status ->
                                FilterChip(
                                    selected = filterStatus == status,
                                    onClick = { filterStatus = status },
                                    label = { Text(status) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GreenPrimary.copy(alpha = 0.1f),
                                        selectedLabelColor = GreenPrimary,
                                        selectedLeadingIconColor = GreenPrimary
                                    )
                                )
                            }
                        }
                    }
                }
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
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "${selectedTree!!.speciesName} \uD83C\uDF32",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = GreenPrimary
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Plant Image from Geotagging
                if (!selectedTree!!.photoUri.isNullOrEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        AsyncImage(
                            model = File(selectedTree!!.photoUri!!),
                            contentDescription = "Plant Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
                            Text(
                                " Location: ${String.format("%.4f", selectedTree!!.latitude)}, ${String.format("%.4f", selectedTree!!.longitude)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val date = Date(selectedTree!!.datePlanted)
                        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        Text(
                            text = "Planted on: ${dateFormat.format(date)} at ${timeFormat.format(date)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Current Status: ${selectedTree!!.status} \uD83C\uDF1F",
                    style = MaterialTheme.typography.titleMedium,
                    color = GreenPrimary,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth().height(56.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            val nextStatus = when(selectedTree!!.status) {
                                "Planted" -> "Survived"
                                "Survived" -> "Needs Care"
                                else -> "Planted"
                            }
                            viewModel.updateTreeStatus(selectedTree!!, nextStatus)
                            selectedTree = selectedTree!!.copy(status = nextStatus)
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary.copy(alpha = 0.1f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GreenPrimary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Cycle Status", color = GreenPrimary, fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                showBottomSheet = false
                                navController.navigate("plant?treeId=${selectedTree!!.id}")
                            }
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Detailed Update", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
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

@Composable
fun MapTypeButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .height(48.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) GreenPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, GreenPrimary) else null
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                text = label,
                color = if (isSelected) GreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
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
