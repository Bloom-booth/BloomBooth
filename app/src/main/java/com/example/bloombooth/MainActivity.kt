package com.example.bloombooth

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.bloombooth.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMainBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val TAG = "테스트"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ViewBinding을 통해 레이아웃 바인딩
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // FusedLocationClient 초기화
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // MapView 초기화
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)

        // 현재 위치 버튼 클릭 리스너
        binding.icCenter.setOnClickListener {
            Log.d(TAG, "현재 위치 버튼 클릭됨")
            getCurrentLocation { location ->
                Log.d(TAG, "현재 위치 가져오기 성공: ${location.latitude}, ${location.longitude}")
                moveCameraToLocation(location)
            }
        }
    }

    // 구글 맵이 준비되었을 때 호출됨
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        Log.d(TAG, "Google Map 준비 완료")
        // 초기 위치 설정
        getCurrentLocation { location ->
            Log.d(TAG, "초기 위치 가져오기 성공: ${location.latitude}, ${location.longitude}")
            moveCameraToLocation(location)
        }
    }

    // 현재 위치를 가져오는 함수
    private fun getCurrentLocation(callback: (Location) -> Unit) {
        Log.d(TAG, "getCurrentLocation 호출됨")
        // 위치 권한 확인
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // 권한이 없다면 요청
            Log.d(TAG, "위치 권한이 없음, 권한 요청")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                1001 // 요청 코드
            )
            return
        }

        // `lastLocation` 호출
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                Log.d(TAG, "FusedLocationClient로 위치 가져오기 성공")
                callback(location)
            } else {
                Log.w(TAG, "FusedLocationClient로 가져온 위치가 null임, 새 위치 요청 시도")
                requestNewLocation(callback) // 새 위치 요청
            }
        }
    }

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
                    Log.d(TAG, "새 위치 가져오기 성공: ${location.latitude}, ${location.longitude}")
                    callback(location)
                    fusedLocationClient.removeLocationUpdates(this) // 위치 업데이트 중지
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "위치 권한이 부족합니다.")
            return
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }


    // 카메라를 위치로 이동시키는 함수
    private fun moveCameraToLocation(location: Location) {
        Log.d(TAG, "moveCameraToLocation 호출됨: ${location.latitude}, ${location.longitude}")
        val latLng = LatLng(location.latitude, location.longitude)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        googleMap.addMarker(MarkerOptions().position(latLng).title("현재 위치"))
        Log.d(TAG, "카메라 이동 완료")
    }

    // 권한 요청 결과 처리
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1001) {
            // 권한 요청 결과 확인
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "위치 권한 허용됨")
                // 권한이 허용되었으면 현재 위치를 다시 가져옴
                getCurrentLocation { location ->
                    Log.d(TAG, "권한 허용 후 위치 가져오기 성공: ${location.latitude}, ${location.longitude}")
                    moveCameraToLocation(location)
                }
            } else {
                Log.w(TAG, "위치 권한이 거부됨")
                // 권한이 거부되었을 경우 안내 메시지 표시
                Toast.makeText(this, "위치 권한을 허용해야 합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Activity lifecycle 메소드들
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume 호출됨")
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause 호출됨")
        binding.mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy 호출됨")
        binding.mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.d(TAG, "onLowMemory 호출됨")
        binding.mapView.onLowMemory()
    }
}