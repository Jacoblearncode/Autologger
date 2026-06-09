package com.nibm.autocare

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourcePolicy
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/**
 * Displays an OSMDroid map using Geoapify tiles.
 * Shows trip details in a bottom card and the user's current GPS location.
 * Trip odometer data is shown as context — actual route is not tracked.
 */
class TripMapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var locationOverlay: MyLocationNewOverlay

    companion object {
        private const val REQUEST_LOCATION = 1001
        private const val API_KEY = "8190237cd66b4a3bae90ce32a27a5d58"
        // Default center: Kuala Lumpur
        private val DEFAULT_CENTER = GeoPoint(3.1390, 101.6869)
        private const val DEFAULT_ZOOM = 13.0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName
        setContentView(R.layout.activity_trip_map)

        val purpose = intent.getStringExtra("purpose") ?: "Trip"
        val date = intent.getStringExtra("date") ?: ""
        val startOdo = intent.getStringExtra("startOdometer") ?: ""
        val endOdo = intent.getStringExtra("endOdometer") ?: ""
        val distance = intent.getDoubleExtra("distance", 0.0)
        val notes = intent.getStringExtra("notes") ?: ""

        setupToolbar(purpose, date)
        bindInfoCard(startOdo, endOdo, distance, notes)
        setupMap()
        requestLocationIfNeeded()
    }

    private fun setupToolbar(purpose: String, date: String) {
        findViewById<TextView>(R.id.tvMapTitle).text =
            if (date.isNotEmpty()) "$purpose · $date" else purpose
        findViewById<View>(R.id.btnMapBack).setOnClickListener { finish() }
    }

    private fun bindInfoCard(startOdo: String, endOdo: String, distance: Double, notes: String) {
        findViewById<TextView>(R.id.tvMapDistance).text = "%.0f km".format(distance)
        findViewById<TextView>(R.id.tvMapStart).text = "$startOdo km"
        findViewById<TextView>(R.id.tvMapEnd).text = "$endOdo km"

        val tvNotes = findViewById<TextView>(R.id.tvMapNotes)
        if (notes.isNotEmpty()) {
            tvNotes.text = notes
            tvNotes.visibility = View.VISIBLE
        } else {
            tvNotes.visibility = View.GONE
        }
    }

    private fun setupMap() {
        mapView = findViewById(R.id.mapView)
        mapView.setMultiTouchControls(true)

        val tileSource = object : OnlineTileSourceBase(
            "Geoapify",
            1, 19, 256, ".png",
            arrayOf("https://maps.geoapify.com"),
            "© OpenStreetMap contributors",
            TileSourcePolicy()
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                return "https://maps.geoapify.com/v1/tile/dark-matter/" +
                    "${MapTileIndex.getZoom(pMapTileIndex)}/" +
                    "${MapTileIndex.getX(pMapTileIndex)}/" +
                    "${MapTileIndex.getY(pMapTileIndex)}.png?apiKey=$API_KEY"
            }
        }
        mapView.setTileSource(tileSource)

        val controller = mapView.controller
        controller.setZoom(DEFAULT_ZOOM)
        controller.setCenter(DEFAULT_CENTER)

        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), mapView)
        locationOverlay.enableMyLocation()
        locationOverlay.runOnFirstFix {
            val loc = locationOverlay.myLocation
            if (loc != null) {
                runOnUiThread {
                    mapView.controller.animateTo(loc, DEFAULT_ZOOM, 500L)
                }
            }
        }
        mapView.overlays.add(locationOverlay)
    }

    private fun requestLocationIfNeeded() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                REQUEST_LOCATION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            locationOverlay.enableMyLocation()
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        locationOverlay.enableMyLocation()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        locationOverlay.disableMyLocation()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDetach()
    }
}
