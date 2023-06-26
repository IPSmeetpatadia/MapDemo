package com.ipsmeet.mapdemo.viewmodel

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.maps.android.SphericalUtil
import com.ipsmeet.mapdemo.R
import com.ipsmeet.mapdemo.databinding.BottomSheetAddressBinding
import com.ipsmeet.mapdemo.databinding.BottomSheetDifferentViewBinding
import kotlinx.coroutines.Runnable

class MapViewModel: ViewModel() {

    private lateinit var mMap: GoogleMap
    private var longitude: Double = 0.0
    private var latitude: Double = 0.0
    private var marker: Marker? = null

    private lateinit var liveLatLng: LatLng     // user's current LatLng
    private lateinit var markerLatLng: LatLng   // marker's current LatLng

    // flags for Map-Type and Map-Details
    private var defaultView: Boolean = true
    private var satelliteView: Boolean = false
    private var terrainView: Boolean = false
    private var trafficView: Boolean = false

    val handler = Handler()
    lateinit var runnable: Runnable

    //  CHECK APP-PERMISSIONS
    fun checkPermission(activity: Activity) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // system-default permission request permission dialog
            ActivityCompat.requestPermissions(
                activity, arrayOf(
                    "android.permission.ACCESS_COARSE_LOCATION",
                    "android.permission.ACCESS_FINE_LOCATION"
                ), 1)
            return
        }
    }

    //  COORDINATES OF USER'S CURRENT LOCATION
    fun getCoordinates(lat: Double, lng: Double) {
        latitude = lat
        longitude = lng
    }

    fun onMapReady(activity: Activity, googleMap: GoogleMap) {
        mMap = googleMap
        checkPermission(activity)
        mMap.isMyLocationEnabled = true

        liveLatLng = LatLng(latitude, longitude)
        Log.i("Current LatLng", liveLatLng.toString())
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(liveLatLng, 16f))

        mMap.setOnMapClickListener {
            marker?.remove()
            marker = mMap.addMarker(MarkerOptions().position(it))
            markerLatLng = it
            viewAddress(activity, it)
            Log.i("Marker LatLng", it.toString())
        }
    }

    //  BOTTOM SHEET: DISPLAY ADDRESS OF MARKED LOCATION (DROPPED PIN)
    private fun viewAddress(context: Activity, latLng: LatLng) {
        val dialogBinding = BottomSheetAddressBinding.inflate(LayoutInflater.from(context))
        val bottomSheet = BottomSheetDialog(context)
        bottomSheet.setContentView(dialogBinding.root)
        bottomSheet.show()

        dialogBinding.imgVClose.setOnClickListener {
            marker!!.remove()
            bottomSheet.dismiss()
        }

        dialogBinding.txtAddress.text = getAddress(context,latLng)
        findDifferenceBetweenPoints(context)
    }

    //  GET ADDRESS OF MARKED LOCATION (DROPPED PIN)
    private fun getAddress(context: Context,latLng: LatLng): String {
        val geocoder = Geocoder(context)
        val list = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 6)
        Log.d("list", list.toString())
        return list!![0].getAddressLine(0)
    }

    private fun findDifferenceBetweenPoints(context: Activity) {
        runnable = object : Runnable {
            override fun run() {
                val distanceInMeter = SphericalUtil.computeDistanceBetween(liveLatLng, markerLatLng)
                Log.d("Distance", "findDifferenceBetweenPoints: $distanceInMeter")
                handler.postDelayed(this, 2000)

                if (distanceInMeter < 500) {
                    val notificationManager = NotificationManagerCompat.from(context)

                    val notification =NotificationCompat.Builder(context, "notification_channel")
                        .setContentTitle("Near by")
                        .setContentText("You are in 500 meter radius of Dropped Pin")
                        .setSmallIcon(R.drawable.baseline_location_on_24)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .build()

                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(context, arrayOf("android.permission.POST_NOTIFICATIONS"), 1)
                        return
                    }
                    notificationManager.notify(911, notification)

                    handler.removeCallbacks(runnable)
                }
            }
        }
        handler.post(runnable)
    }

    //  BOTTOM SHEET: Map-Types and May-Details
    fun enableBottomSheet(activity: Context, enable: Boolean) {
        val dialogBinding = BottomSheetDifferentViewBinding.inflate(LayoutInflater.from(activity))

        val bottomSheetDialog = BottomSheetDialog(activity)
        bottomSheetDialog.setContentView(dialogBinding.root)

        if (enable) {
            bottomSheetDialog.show()

            //  CHECK FOR SELECTED MAP
            if (defaultView) {
                dialogBinding.mapTypeDefault.background = ContextCompat.getDrawable(activity, R.drawable.selected_map)
                dialogBinding.txtDefault.setTextColor(ContextCompat.getColor(activity, R.color.selected_color))
            }
            else if (satelliteView) {
                dialogBinding.mapTypeSatellite.background = ContextCompat.getDrawable(activity, R.drawable.selected_map)
                dialogBinding.txtSatellite.setTextColor(ContextCompat.getColor(activity, R.color.selected_color))
            }
            else {
                dialogBinding.mapTypeTerrain.background = ContextCompat.getDrawable(activity, R.drawable.selected_map)
                dialogBinding.txtTerrain.setTextColor(ContextCompat.getColor(activity, R.color.selected_color))
            }

            //  CHECK FOR SELECTED DETAIL
            if (trafficView) {
                dialogBinding.mapDetailTraffic.background = ContextCompat.getDrawable(activity, R.drawable.selected_map)
                dialogBinding.txtTraffic.setTextColor(ContextCompat.getColor(activity, R.color.selected_color))
            }

            //  CLOSE BUTTON
            dialogBinding.imgVClose.setOnClickListener {
                bottomSheetDialog.dismiss()
            }

            //  DEFAULT MAP VIEW
            dialogBinding.mapTypeDefault.setOnClickListener {
                mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                defaultView = true
                satelliteView = false
                terrainView = false
                dialogBinding.mapTypeDefault.background = ContextCompat.getDrawable(activity, R.drawable.selected_map)
                dialogBinding.mapTypeSatellite.setBackgroundResource(0)
                dialogBinding.mapTypeTerrain.setBackgroundResource(0)
                dialogBinding.txtDefault.setTextColor(ContextCompat.getColor(activity, R.color.selected_color))
                dialogBinding.txtSatellite.setTextColor(ContextCompat.getColor(activity, R.color.default_color))
                dialogBinding.txtTerrain.setTextColor(ContextCompat.getColor(activity, R.color.default_color))
            }

            //  SATELLITE MAP VIEW
            dialogBinding.mapTypeSatellite.setOnClickListener {
                mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
                defaultView = false
                satelliteView = true
                terrainView = false
                dialogBinding.mapTypeDefault.setBackgroundResource(0)
                dialogBinding.mapTypeSatellite.background = ContextCompat.getDrawable(activity, R.drawable.selected_map)
                dialogBinding.mapTypeTerrain.setBackgroundResource(0)
                dialogBinding.txtDefault.setTextColor(ContextCompat.getColor(activity, R.color.default_color))
                dialogBinding.txtSatellite.setTextColor(ContextCompat.getColor(activity, R.color.selected_color))
                dialogBinding.txtTerrain.setTextColor(ContextCompat.getColor(activity, R.color.default_color))
            }

            //  TERRAIN MAP VIEW
            dialogBinding.mapTypeTerrain.setOnClickListener {
                mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
                defaultView = false
                satelliteView = false
                terrainView = true
                dialogBinding.mapTypeDefault.setBackgroundResource(0)
                dialogBinding.mapTypeSatellite.setBackgroundResource(0)
                dialogBinding.mapTypeTerrain.background = ContextCompat.getDrawable(activity, R.drawable.selected_map)
                dialogBinding.txtDefault.setTextColor(ContextCompat.getColor(activity, R.color.default_color))
                dialogBinding.txtSatellite.setTextColor(ContextCompat.getColor(activity, R.color.default_color))
                dialogBinding.txtTerrain.setTextColor(ContextCompat.getColor(activity, R.color.selected_color))
            }

            //  TRAFFIC MAP VIEW
            dialogBinding.mapDetailTraffic.setOnClickListener {
                if (trafficView) {
                    trafficView = false
                    mMap.isTrafficEnabled = false
                    dialogBinding.mapDetailTraffic.setBackgroundResource(0)
                    dialogBinding.txtTraffic.setTextColor(ContextCompat.getColor(activity, R.color.default_color))
                }
                else {
                    trafficView = true
                    mMap.isTrafficEnabled = true
                    dialogBinding.mapDetailTraffic.background = ContextCompat.getDrawable(activity, R.drawable.selected_map)
                    dialogBinding.txtTraffic.setTextColor(ContextCompat.getColor(activity, R.color.selected_color))
                }
            }
        }
    }
}