package com.example.dynamic_plotting

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private var clusterManager: ClusterManager<LocationData>? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        clusterManager = ClusterManager(this, map)
        clusterManager?.renderer = CustomClusterRenderer()
        map.setOnCameraIdleListener(clusterManager)
        map.setOnMarkerClickListener(clusterManager)

        loadLocationDataFromCSV()
    }

    private fun loadLocationDataFromCSV() {
        val inputStream: InputStream = assets.open("Coordinates with 3 decimal places.csv")
        val reader = BufferedReader(InputStreamReader(inputStream))
        var line: String?

        try {
            // Skip header line
            reader.readLine()

            // Remove previous clusters from the map
            clusterManager?.clearItems()

            while (reader.readLine().also { line = it } != null) {
                val data = line!!.split(",").toTypedArray()
                if (data.size >= 3) {
                    val latitude = if (data[0].isNotEmpty()) data[0].toDouble() else 0.0
                    val longitude = if (data[1].isNotEmpty()) data[1].toDouble() else 0.0
                    val depth = if (data[2].isNotEmpty()) data[2].toDouble() else 0.0

                    val location = LocationData(latitude, longitude)
                    clusterManager?.addItem(location)
                }
            }

            clusterManager?.cluster()

            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(26.0, 151.0), 1f))

        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                reader.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    inner class LocationData(private val latitude: Double, private val longitude: Double) :
        com.google.maps.android.clustering.ClusterItem {

        private val position: LatLng = LatLng(latitude, longitude)

        override fun getPosition(): LatLng {
            return LatLng(latitude, longitude)
        }

        override fun getTitle(): String? {
           return null
        }

        override fun getSnippet(): String? {
            return null
        }
    }

    inner class CustomClusterRenderer :
        DefaultClusterRenderer<LocationData>(applicationContext, map, clusterManager) {

        override fun onBeforeClusterItemRendered(item: LocationData, markerOptions: MarkerOptions) {
            val color = getColorBasedOnDepth(item)
            val circleOptions = CircleOptions()
                .center(item.position)
                .radius(1000.0)
                .fillColor(color)
                .strokeWidth(0f)
            map.addCircle(circleOptions)
        }

        private fun getColorBasedOnDepth(item: LocationData): Int {
            val depth = item.position.latitude // Assuming depth is based on latitude
            // Define your color ranges and corresponding colors here
            return when (depth) {
                in 0.007..0.142 -> Color.parseColor("#B3B3FF")
                in 0.142..0.277 -> Color.parseColor("#5D5DFF")
                in 0.277..0.411 -> Color.parseColor("#0707FF")
                in 0.411..0.546 -> Color.parseColor("#0000B4")
                in 0.546..1.0 -> Color.parseColor("#00005B")
                else -> Color.parseColor("#e8f1fd")
            }
        }
    }

    override fun onPause() {
        clusterManager?.clearItems()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        clusterManager?.cluster()
    }


    override fun onDestroy() {
        map.clear()
        super.onDestroy()
    }
}
