package com.example.nammahasiru.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.example.nammahasiru.TreeViewModel
import com.example.nammahasiru.TreeViewModelFactory
import com.example.nammahasiru.data.TreeEntity
import com.example.nammahasiru.data.TreeDatabase
import com.example.nammahasiru.worker.StatusReminderWorker
import com.example.nammahasiru.ui.theme.GreenPrimary
import com.example.nammahasiru.ui.theme.NammaHasiruTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import android.content.Context
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.filled.Check
import android.widget.Toast
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantScreen(navController: NavController, viewModel: TreeViewModel) {
    var speciesName by remember { mutableStateOf("") }
    var locationFetched by remember { mutableStateOf(false) }
    var latitude by remember { mutableDoubleStateOf(0.0) }
    var longitude by remember { mutableDoubleStateOf(0.0) }
    var photoTaken by remember { mutableStateOf(false) }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }
    var hasCameraPermission by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    var plantDetectionError by remember { mutableStateOf(false) }
    var plantHealthStatus by remember { mutableStateOf("Unknown") }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val imageCapture = remember { ImageCapture.Builder().build() }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: false
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
    }

    LaunchedEffect(Unit) {
        val cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val locationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        hasCameraPermission = cameraGranted
        hasLocationPermission = locationGranted

        if (!cameraGranted || !locationGranted) {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }

    if (isSubmitted) {
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(100.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text("Plant Successfully Logged!", style = MaterialTheme.typography.headlineMedium, color = GreenPrimary, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            if (plantHealthStatus == "Dead") {
                Text("This plant was detected as dead. No reminder has been set.", style = MaterialTheme.typography.bodyLarge, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
            } else {
                Text("We've successfully set a 90-day alarm to remind you to check on this plant!", style = MaterialTheme.typography.bodyLarge, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    isSubmitted = false
                    speciesName = ""
                    locationFetched = false
                    photoTaken = false
                    photoBitmap = null
                    plantHealthStatus = "Unknown"
                },
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Plant Another", modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = { 
                    navController.navigate("dashboard") {
                        popUpTo("dashboard") { inclusive = false }
                    } 
                },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary)
            ) {
                Text("Back to Dashboard", modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.navigate("dashboard") {
                            popUpTo("dashboard") { inclusive = false }
                        } }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back to Home", tint = GreenPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Record a New Plant \uD83C\uDF31",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = GreenPrimary
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(if (photoTaken) Color.Black else Color(0xFFE8F5E9))
                .border(2.dp, if (photoTaken) GreenPrimary else Color(0xFFC8E6C9), RoundedCornerShape(24.dp))
                .clickable {
                    if (!hasCameraPermission) {


                    } else if (photoTaken) {
                        // Retake photo
                        photoTaken = false
                        photoBitmap = null
                        plantDetectionError = false
                    } else {
                        // Capture photo
                        plantDetectionError = false
                        imageCapture.takePicture(
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val buffer = image.planes[0].buffer
                                    val bytes = ByteArray(buffer.capacity())
                                    buffer.get(bytes)
                                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
                                    val matrix = Matrix()
                                    matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
                                    val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

                                    val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
                                    val inputImage = InputImage.fromBitmap(rotatedBitmap, 0)

                                    labeler.process(inputImage)
                                        .addOnSuccessListener { labels ->
                                            val plantKeywords = listOf("Plant", "Tree", "Flower", "Leaf", "Houseplant", "Vegetation", "Grass", "Nature", "Flora", "Pot plant", "House plant")
                                            val isPlant = labels.any { label ->
                                                plantKeywords.any { keyword -> label.text.contains(keyword, ignoreCase = true) }
                                            }

                                            val deadKeywords = listOf("Dead", "Dried", "Withered", "Brown", "Dying", "Stump", "Dry")
                                            val isDead = labels.any { label ->
                                                deadKeywords.any { keyword -> label.text.contains(keyword, ignoreCase = true) }
                                            }

                                            if (isPlant) {
                                                plantHealthStatus = if (isDead) "Dead" else "Alive"
                                                photoBitmap = rotatedBitmap
                                                photoTaken = true
                                                plantDetectionError = false
                                            } else {
                                                plantDetectionError = true
                                            }
                                            image.close()
                                        }
                                        .addOnFailureListener { e ->
                                            // Fallback if detection fails for some reason
                                            photoBitmap = rotatedBitmap
                                            photoTaken = true
                                            image.close()
                                        }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    // Handle exception
                                }
                            }
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (photoTaken && photoBitmap != null) {
                Image(
                    bitmap = photoBitmap!!.asImageBitmap(),
                    contentDescription = "Captured Plant Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Overlay health status
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.8f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (plantHealthStatus == "Dead") "Status: Dead 🔴" else "Status: Alive 🟢",
                        fontWeight = FontWeight.Bold,
                        color = if (plantHealthStatus == "Dead") Color.Red else GreenPrimary
                    )
                }
                // Overlay a checkmark icon to still show it's successfully captured
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.7f))
                        .padding(4.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(32.dp))
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Tap to Retake", color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else if (hasCameraPermission) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = CameraPreview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageCapture
                                )
                            } catch (exc: Exception) {
                                // handle error
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    }
                )
                // Overlay capture hint
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Tap to Capture", color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(48.dp))
                    Text("Tap to Enable Camera Permission", color = GreenPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

        if (plantDetectionError) {
            Text(
                text = "Could not detect a plant. Please take a clear photo of the plant.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = speciesName,
            onValueChange = { speciesName = it },
            label = { Text("Plant Species (e.g., Neem, Mango)") },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = GreenPrimary
            ),
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
            ),
            trailingIcon = {
                if (speciesName.isNotEmpty()) {
                    IconButton(onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "OK", tint = GreenPrimary)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { 
                if (hasLocationPermission) {
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                        .addOnSuccessListener { location ->
                            if (location != null) {
                                latitude = location.latitude
                                longitude = location.longitude
                                locationFetched = true
                            } else {
                                Toast.makeText(context, "Location not found. Please turn on your phone's GPS.", Toast.LENGTH_LONG).show()
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Error fetching location.", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                }
            },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (locationFetched) GreenPrimary else Color.DarkGray
            )
        ) {
            Icon(if (locationFetched) Icons.Default.CheckCircle else Icons.Default.LocationOn, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(if (locationFetched) "GPS: ${String.format("%.4f", latitude)}°, ${String.format("%.4f", longitude)}°" else "Fetch GPS Coordinates", fontWeight = FontWeight.Bold)
        }

        if (locationFetched) {
            Spacer(modifier = Modifier.height(16.dp))
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(LatLng(latitude, longitude), 15f)
            }
            LaunchedEffect(latitude, longitude) {
                cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(latitude, longitude), 15f)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, GreenPrimary, RoundedCornerShape(16.dp))
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState
                ) {
                    Marker(
                        state = MarkerState(position = LatLng(latitude, longitude)),
                        title = speciesName.ifEmpty { "Plant" }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                coroutineScope.launch {
                    var photoPath: String? = null
                    if (photoBitmap != null) {
                        photoPath = saveBitmapToStorage(context, photoBitmap!!)
                    }

                    val statusToSave = if (plantHealthStatus == "Dead") "Died" else "Planted"
                    val entity = TreeEntity(
                        speciesName = speciesName,
                        latitude = latitude,
                        longitude = longitude,
                        datePlanted = System.currentTimeMillis(),
                        status = statusToSave,
                        photoUri = photoPath
                    )
                    viewModel.insertTree(entity)
                    
                    if (plantHealthStatus != "Dead") {
                        // Schedule 90-day reminder
                        val workRequest = OneTimeWorkRequestBuilder<StatusReminderWorker>()
                            .setInitialDelay(90, TimeUnit.DAYS)
                            .setInputData(workDataOf("TREE_NAME" to speciesName))
                            .build()
                        WorkManager.getInstance(context).enqueue(workRequest)
                    }

                    isSubmitted = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
            shape = RoundedCornerShape(30.dp),
            enabled = speciesName.isNotEmpty() && photoTaken && locationFetched
        ) {
            Text("Complete Geotagging", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun PlantScreenPreview() {
    val context = LocalContext.current
    val database = TreeDatabase.getDatabase(context)
    val viewModel: TreeViewModel = viewModel(
        factory = TreeViewModelFactory(database.treeDao())
    )
    NammaHasiruTheme {
        PlantScreen(navController = rememberNavController(), viewModel = viewModel)
    }
}

fun saveBitmapToStorage(context: Context, bitmap: Bitmap): String {
    val filename = "plant_${System.currentTimeMillis()}.jpg"
    val file = File(context.filesDir, filename)
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
    }
    return file.absolutePath
}
