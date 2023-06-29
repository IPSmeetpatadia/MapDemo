package com.ipsmeet.mapdemo.viewmodel

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
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

class MapViewModel : ViewModel() {

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
    private lateinit var runnable: Runnable

    //  CHECK APP-PERMISSIONS
    fun checkPermission(activity: Activity) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // system-default permission request permission dialog
            ActivityCompat.requestPermissions(
                activity, arrayOf(
                    "android.permission.ACCESS_COARSE_LOCATION",
                    "android.permission.ACCESS_FINE_LOCATION"
                ), 1
            )
            return
        }
    }

    //  COORDINATES OF USER'S CURRENT LOCATION
    fun getCoordinates(lat: Double, lng: Double) {
        latitude = lat
        longitude = lng
        Log.d("getCoordinates() ~ latitude", latitude.toString())
        Log.d("getCoordinates() ~ longitude", longitude.toString())
    }

    fun onMapReady(activity: Activity, googleMap: GoogleMap, mapFragment: Fragment) {
        mMap = googleMap
        checkPermission(activity)
        mMap.isMyLocationEnabled = true

        liveLatLng = LatLng(latitude, longitude)
        sharedPref(activity)
        Log.i("Current LatLng", liveLatLng.toString())
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(liveLatLng, 16f))

        mMap.setOnMapClickListener {
            marker?.remove()
            marker = mMap.addMarker(MarkerOptions().position(it))
            markerLatLng = it
            viewAddress(activity, it)
            Log.i("Marker LatLng", it.toString())
        }

        // gps-location button
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

        // compass button
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

        dialogBinding.txtAddress.text = getAddress(context, latLng)
        findDifferenceBetweenPoints(context)
    }

    //  GET ADDRESS OF MARKED LOCATION (DROPPED PIN)
    private fun getAddress(context: Context, latLng: LatLng): String? {
        val geocoder = Geocoder(context)
        return try {
            val list = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 6)
            Log.d("list", list.toString())
            try {
                list!![0].getAddressLine(0)
            } catch (e: IndexOutOfBoundsException) {
                null
            }
        } catch (e: Exception) {
            e.cause?.message.toString()
        }
    }

    private fun findDifferenceBetweenPoints(context: Activity) {
        runnable = object : Runnable {
            override fun run() {
                val distanceInMeter = SphericalUtil.computeDistanceBetween(liveLatLng, markerLatLng)
                Log.d("Distance", "findDifferenceBetweenPoints: $distanceInMeter")
                handler.postDelayed(this, 2000)

                if (distanceInMeter < 500) {
                    val notificationManager = NotificationManagerCompat.from(context)

                    val notification = NotificationCompat.Builder(context, "notification_channel")
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

                    handler.removeCallbacks(this)
                }
            }
        }
        handler.post(runnable)
    }

    //  BOTTOM SHEET: Map-Types and May-Details
    fun enableBottomSheet(activity: Context, mMap: GoogleMap, enable: Boolean) {
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

    private fun sharedPref(context: Context) {
        val sharedPreferences = context.getSharedPreferences("lastLatLng", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("lastLat", latitude.toString())
        editor.putString("lastLng", longitude.toString())
        editor.apply()
    }
}