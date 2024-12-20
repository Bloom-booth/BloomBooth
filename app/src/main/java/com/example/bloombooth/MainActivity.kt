// MainActivity.kt
package com.example.bloombooth

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.example.bloombooth.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMainBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var currentLocationMarker: com.google.android.gms.maps.model.Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // FusedLocationClient 초기화
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // MapView 초기화
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)

        // 현재 위치 버튼 클릭 리스너
        binding.icCenter.setOnClickListener {
            getCurrentLocation { location ->
                moveCameraToLocation(location)
            }
        }

        // SearchActivity로 이동하기
        binding.header.btnSearch.setOnClickListener {
            getCurrentLocation { location ->
                // 현재 위치를 SearchActivity로 전달
                val intent = Intent(this, SearchActivity::class.java).apply {
                    putExtra("currentLatitude", location.latitude)
                    putExtra("currentLongitude", location.longitude)
                }
                startActivity(intent)
            }
        }

        // MypageActivity로 이동하기
        binding.header.btnMypage.setOnClickListener {
            val intent = Intent(this, MypageActivity::class.java)
            startActivity(intent)
        }
    }

    // 구글 맵이 준비되었을 때 호출됨
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        // 초기 위치 설정
        getCurrentLocation { location ->
            moveCameraToLocation(location)
        }
    }

    // 현재 위치를 가져오는 함수
    private fun getCurrentLocation(callback: (Location) -> Unit) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                1001 // 요청 코드
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                callback(location)
            } else {
                requestNewLocation(callback)
            }
        }
    }

    // 새 위치 요청
    private fun requestNewLocation(callback: (Location) -> Unit) {
        val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
            10000L // 요청 간격 (밀리초)
        ).apply {
            setMinUpdateIntervalMillis(5000L) // 최소 업데이트 간격
        }.build()

        val locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                locationResult.lastLocation?.let { location ->
                    callback(location)
                    fusedLocationClient.removeLocationUpdates(this) // 위치 업데이트 중지
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    // 카메라를 위치로 이동시키는 함수
    private fun moveCameraToLocation(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)

        // 기존의 현재 위치 마커를 지우고 새로운 마커 추가
        currentLocationMarker?.remove()
        currentLocationMarker = googleMap.addMarker(
            MarkerOptions().position(latLng).title("현재 위치")
        )

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
    }

    // 권한 요청 결과 처리
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation { location ->
                    moveCameraToLocation(location)
                }
            } else {
                Toast.makeText(this, "위치 권한을 허용해야 합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Activity lifecycle 메소드들
    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }
}