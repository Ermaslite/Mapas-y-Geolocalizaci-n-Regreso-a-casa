package com.example.maps

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.maps.ui.theme.MapsTheme
import com.example.maps.viewmodel.MapViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configuración de OSMdroid
        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        
        setContent {
            MapsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MapScreen()
                }
            }
        }
    }
}

@Composable
fun MapScreen(viewModel: MapViewModel = viewModel()) {
    val context = LocalContext.current
    var hasLocationPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions.values.all { it }
        if (hasLocationPermission) {
            viewModel.updateLocation(context)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.init(context)
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(15.0)
                    controller.setCenter(viewModel.homeLocation)
                }
            },
            update = { mapView ->
                mapView.overlays.clear()
                
                // Permitir configurar la casa tocando largo el mapa
                val mapEventsReceiver = object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean = false
                    override fun longPressHelper(p: GeoPoint?): Boolean {
                        p?.let {
                            viewModel.setHome(context, it.latitude, it.longitude)
                        }
                        return true
                    }
                }
                mapView.overlays.add(MapEventsOverlay(mapEventsReceiver))

                // Marcador Actual
                viewModel.currentLocation?.let { point ->
                    val marker = Marker(mapView)
                    marker.position = point
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    marker.title = "Tu ubicación"
                    mapView.overlays.add(marker)
                }

                // Marcador Casa
                val homeMarker = Marker(mapView)
                homeMarker.position = viewModel.homeLocation
                homeMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                homeMarker.title = "Casa"
                mapView.overlays.add(homeMarker)

                // Dibujar Ruta
                if (viewModel.routePoints.isNotEmpty()) {
                    val polyline = Polyline()
                    polyline.setPoints(viewModel.routePoints)
                    polyline.outlinePaint.color = android.graphics.Color.BLUE
                    mapView.overlays.add(polyline)
                }

                mapView.invalidate()
            },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            viewModel.errorMessage?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(8.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            Button(
                onClick = { viewModel.updateLocation(context) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Trazar Ruta a Casa")
            }

            Button(
                onClick = { 
                    viewModel.currentLocation?.let {
                        viewModel.setHome(context, it.latitude, it.longitude)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Configurar Ubicación Actual como Casa")
            }
            
            Text(
                text = "Mantén presionado en el mapa para elegir otro destino",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
