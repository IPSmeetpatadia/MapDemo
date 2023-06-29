package com.ipsmeet.mapdemo.activity

import android.Manifest
import android.app.Dialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.ipsmeet.mapdemo.R
import com.ipsmeet.mapdemo.databinding.ActivityMapsBinding
import com.ipsmeet.mapdemo.databinding.DialogNoGpsBinding
import com.ipsmeet.mapdemo.viewmodel.MapViewModel

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapsBinding

    private lateinit var viewModel: MapViewModel
    var longitude: Double = 0.0
    var latitude: Double = 0.0
    lateinit var locationManager: LocationManager
    private lateinit var noGpsDialog: Dialog
    val handler = Handler()
    private lateinit var mapView: Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapView = supportFragmentManager.findFragmentById(R.id.map)!!

        noGpsDialog = Dialog(this)
        supportActionBar!!.hide()
        viewModel = ViewModelProvider(this)[MapViewModel::class.java]

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this@MapsActivity)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0F, locationListener)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("notification_channel", "channelName", NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }


    override fun onMapReady(googleMap: GoogleMap) {
        binding.btnMapTypes.setOnClickListener {
            viewModel.enableBottomSheet(this, googleMap, true)
        }

        binding.btnTrackLocation.setOnClickListener {
            viewModel.checkPermission(this)
            checkForGPS()
        }

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            viewModel.onMapReady(this, googleMap, mapView)
            binding.btnTrackLocation.visibility = View.GONE
        }
        else {
            binding.btnTrackLocation.visibility = View.VISIBLE
        }
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
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private fun checkForGPS() {
        viewModel.checkPermission(this)
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            val dialogBinding = DialogNoGpsBinding.inflate(LayoutInflater.from(this))
            noGpsDialog.setContentView(dialogBinding.root)
            noGpsDialog.setCancelable(false)
            noGpsDialog.show()

            dialogBinding.txtOk.setOnClickListener {
                startActivity(
                    Intent(
                        Settings.ACTION_LOCATION_SOURCE_SETTINGS
                    )
                )
                noGpsDialog.dismiss()
            }

            dialogBinding.txtNo.setOnClickListener {
                noGpsDialog.dismiss()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0F, locationListener)
        }
    }
}