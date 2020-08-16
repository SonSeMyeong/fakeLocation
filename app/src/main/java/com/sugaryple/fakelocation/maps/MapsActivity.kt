package com.sugaryple.fakelocation.maps

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import com.eazypermissions.common.model.PermissionResult
import com.eazypermissions.livedatapermission.PermissionManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.maps.SupportMapFragment
import com.google.android.libraries.maps.model.Marker
import com.sugaryple.fakelocation.R
import com.sugaryple.fakelocation.feature.fakeGps.FakeGpsWorkManager
import com.sugaryple.fakelocation.feature.fakeGps.FakeGpsWorkSate
import com.sugaryple.fakelocation.feature.fakeGps.FakeGpsWorker
import com.sugaryple.fakelocation.model.*
import com.sugaryple.fakelocation.showOnlyOne
import com.sugaryple.fakelocation.toSimpleLatLng
import kotlinx.android.synthetic.main.activity_maps.*
import org.koin.android.ext.android.inject

class MapsActivity : AppCompatActivity(), PermissionManager.PermissionObserver {

    companion object {
        const val PERMISSION_REQUEST_ID = 100
        const val TAG_REQUIRED_MOCK_LOCATION_DIALOG = "TAG_REQUIRED_MOCK_LOCATION_DIALOG"
    }

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private val mapModel: MapModel by lazy {
        GoogleMapModel(
            supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        )
    }
    private var centerMarker: Marker? = null
    private val fakeGpsManager: FakeGpsWorkManager by inject()
    private val gpsProviderModel: GpsProviderModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        fakeGpsManager.state.observe(this) {
            when (it) {
                is FakeGpsWorkSate.On -> { }
                is FakeGpsWorkSate.Failed -> startRequiredMockLocationDialog()
                else -> { }
            }
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mapObserve()
        button_play.setOnClickListener {
            val centerLocation = mapModel.getCenterLocation()
            if (centerLocation != null) {
                centerMarker?.remove()
                centerMarker = mapModel.addMarker(centerLocation)
            }
            fakeGpsManager.start(centerLocation!!)
        }
        gpsProviderObserve()
    }

    override fun onResume() {
        super.onResume()
        if (gpsProviderModel.isGPSProviderEnabled()) {
            gpsProviderModel.initMockLocationProvider()
        }
    }

    private fun mapObserve() {
        mapModel.mapReadyEvent.observe(this) {
            enableMyLocation()
            moveMyLocation()
            mapModel.setCompassEnabled(true)
            mapModel.setZoomControlsEnabled(true)
            button_play.isEnabled = true
        }
    }

    private fun gpsProviderObserve() {
        gpsProviderModel.eventMockLocationRequest.observe(this) {
            it.getContentIfNotHandled()?.let {
                startRequiredMockLocationDialog()
            }
        }
    }

    private fun moveMyLocation() {
        if (checkSelfPositionPermission()) {
            fusedLocationClient?.lastLocation?.addOnSuccessListener {
                mapModel.moveCamera(it.toSimpleLatLng(), 15f)
            }
        }
    }

    private fun requestPermission() {
        PermissionManager.requestPermissions(
            this,
            PERMISSION_REQUEST_ID,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    private fun enableMyLocation() {
        if (checkSelfPositionPermission()) {
            mapModel.setIsMyLocationEnabled(true)
        } else {
            requestPermission()
        }
    }

    // 위치를
    private fun checkSelfPositionPermission(): Boolean {
        return !(ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED)
    }


    override fun setupObserver(permissionResultLiveData: LiveData<PermissionResult>) {
        permissionResultLiveData.observe(this, Observer<PermissionResult> {
            when (it) {
                is PermissionResult.PermissionGranted ->

                {
                    if (it.requestCode == PERMISSION_REQUEST_ID) {
                        // 사용자가 권한을 부여한 후 여기에 논리를 추가하십시오.
                        enableMyLocation()
                        Toast.makeText(this@MapsActivity, "PermissionGranted", Toast.LENGTH_SHORT).show()
                    }
                }
                is PermissionResult.PermissionDenied -> {
                    if (it.requestCode == PERMISSION_REQUEST_ID) {
                        // 권한 거부 처리를위한 논리 추가
                        Toast.makeText(this@MapsActivity, "PermissionDenied", Toast.LENGTH_SHORT).show()
                    }
                }
                is PermissionResult.PermissionDeniedPermanently -> {
                    if (it.requestCode == PERMISSION_REQUEST_ID) {
                        // 사용자가 권한을 영구적으로 거부 한 경우 여기에 논리를 추가하십시오.
                        // 이상적으로 사용자에게 설정으로 이동하여 권한을 활성화하도록 요청해야합니다.
                        Toast.makeText(this@MapsActivity, "PermissionDeniedPermanently", Toast.LENGTH_SHORT).show()
                    }
                }
                is PermissionResult.ShowRational -> {
                    if (it.requestCode == PERMISSION_REQUEST_ID) {
                        // 사용자가 권한을 자주 거부하면이 권한을 요청한 이유가 확실하지 않습니다.
                        // 권한이 필요한 이유를 설명 할 수있는 기회입니다.
                        Toast.makeText(this@MapsActivity, "ShowRational", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    fun startRequiredMockLocationDialog() {
        RequiredMockLocationDialog.newInstance()
            .showOnlyOne(supportFragmentManager, TAG_REQUIRED_MOCK_LOCATION_DIALOG)
    }

}