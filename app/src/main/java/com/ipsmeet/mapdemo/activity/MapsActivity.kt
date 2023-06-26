package com.ipsmeet.mapdemo.activity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.ipsmeet.mapdemo.R
import com.ipsmeet.mapdemo.databinding.ActivityMapsBinding
import com.ipsmeet.mapdemo.viewmodel.MapViewModel

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapsBinding

    private lateinit var viewModel: MapViewModel
    var longitude: Double = 0.0
    var latitude: Double = 0.0
    lateinit var locationManager: LocationManager
    val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar!!.hide()
        viewModel = ViewModelProvider(this)[MapViewModel::class.java]

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        viewModel.checkPermission(this)
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0F, locationListener)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("notification_channel", "channelName", NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        binding.btnMapTypes.setOnClickListener {
            viewModel.enableBottomSheet(this, true)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        viewModel.onMapReady(this, googleMap)
    }

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            longitude = location.longitude
            latitude = location.latitude

            val runnable = object : Runnable {
                override fun run() {
                    viewModel.getCoordinates(latitude, longitude)
                    handler.postDelayed(this, 1000)
                    Log.d("Current Location", " $longitude + $latitude")
                }
            }
            handler.post(runnable)
            locationManager.removeUpdates(this)

            val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
            mapFragment.getMapAsync(this@MapsActivity)

            if (mapFragment.requireView().findViewById<View?>("1".toInt()) != null) {
                // Get the button view
                val locationButton: View = (mapFragment.requireView().findViewById<View>("1".toInt()).parent as View).findViewById("2".toInt())
                // and next place it, on bottom right (as Google Maps app)
                val layoutParams: RelativeLayout.LayoutParams = locationButton.layoutParams as RelativeLayout.LayoutParams
                // position on right bottom
                layoutParams.apply {
                    addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
                    addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                    setMargins(0, 0, 0, 120)
                }
            }

            if (mapFragment.requireView().findViewById<View?>("2".toInt()) != null) {
                // Get the button view
                val locationButton: View = (mapFragment.requireView().findViewById<View>("2".toInt()).parent as View).findViewById("5".toInt())
                // and next place it, on top right (as Google Maps app)
                val layoutParams: RelativeLayout.LayoutParams = locationButton.layoutParams as RelativeLayout.LayoutParams
                // position on right top
                layoutParams.apply {
                    addRule(RelativeLayout.ALIGN_PARENT_START, 0)
                    addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE)
                    setMargins(0, 190, 50, 0)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermission(this)
    }
}