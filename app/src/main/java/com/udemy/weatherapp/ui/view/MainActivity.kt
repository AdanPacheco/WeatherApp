package com.udemy.weatherapp.ui.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.udemy.weatherapp.R
import com.udemy.weatherapp.core.Constants
import com.udemy.weatherapp.core.Prefs
import com.udemy.weatherapp.core.SharedPreferenceApplication.Companion.prefs
import com.udemy.weatherapp.data.model.WeatherResponse
import com.udemy.weatherapp.data.network.WeatherService
import com.udemy.weatherapp.databinding.ActivityMainBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var mProgressDialog: Dialog? = null


    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val lastLocation: Location = result.lastLocation
            val latitude = lastLocation.latitude
            val longitude = lastLocation.longitude
            getLocationWeatherDetails(latitude, longitude)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkForLocationPermissions()
        setupUI()
    }

    private fun setupUI() {
        val weatherResponseJsonString = prefs.getWeather()

        if (!weatherResponseJsonString.isEmpty()) {
            val weatherData = Gson().fromJson(weatherResponseJsonString, WeatherResponse::class.java)
            weatherData.weather.indices.forEach { index ->
                binding.tvWeather.text = weatherData.weather[index].main
                binding.tvWeatherDescription.text = weatherData.weather[index].description
                val units: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    getUnits(application.resources.configuration.locales[0].country.toString())
                } else {
                    getUnits(application.resources.configuration.locale.country.toString())
                }

                binding.tvTemperatureMaximum.text = weatherData.main.temp_max.toString() + units + " max"
                binding.tvTemperatureMinimum.text = weatherData.main.temp_min.toString() + units + " min"
                binding.tvHumidity.text = weatherData.main.temp.toString() + units
                binding.tvHumidityPercent.text = weatherData.main.humidity.toString() + " per cent"
                binding.tvWind.text = weatherData.wind.speed.toString()
                binding.tvSunrise.text = getUnixTime(weatherData.sys.sunrise)
                binding.tvSunset.text = getUnixTime(weatherData.sys.sunset)
                binding.tvCountry.text = weatherData.sys.country
                binding.tvLocation.text = weatherData.name

                when (weatherData.weather[index].icon) {
                    "01d" -> binding.ivWeather.setImageResource(R.drawable.sunny)
                    "02d" -> binding.ivWeather.setImageResource(R.drawable.cloud)
                    "03d" -> binding.ivWeather.setImageResource(R.drawable.cloud)
                    "04d" -> binding.ivWeather.setImageResource(R.drawable.cloud)
                    "04n" -> binding.ivWeather.setImageResource(R.drawable.cloud)
                    "10d" -> binding.ivWeather.setImageResource(R.drawable.rain)
                    "11d" -> binding.ivWeather.setImageResource(R.drawable.storm)
                    "13d" -> binding.ivWeather.setImageResource(R.drawable.snowflake)
                    "01n" -> binding.ivWeather.setImageResource(R.drawable.cloud)
                    "02n" -> binding.ivWeather.setImageResource(R.drawable.cloud)
                    "03n" -> binding.ivWeather.setImageResource(R.drawable.cloud)
                    "10n" -> binding.ivWeather.setImageResource(R.drawable.cloud)
                    "11n" -> binding.ivWeather.setImageResource(R.drawable.rain)
                    "13n" -> binding.ivWeather.setImageResource(R.drawable.snowflake)
                }
            }
        }


    }

    @SuppressLint("SimpleDateFormat")
    private fun getUnixTime(time: Long): String {
        val date = Date(time * 1000L)
        val sdf = SimpleDateFormat("HH:mm")
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }

    private fun getUnits(value: String): String {
        var value = "°C"
        if (value == "US" || value == "LR" || value == "MM") {
            value = "°F"
        }
        return value
    }

    private fun getLocationWeatherDetails(latitude: Double, longitude: Double) {
        if (Constants.isNetworkAvailable(this)) {

            val retrofit: Retrofit =
                Retrofit.Builder().baseUrl(Constants.BASE_URL).addConverterFactory(GsonConverterFactory.create()).build()
            val service: WeatherService = retrofit.create(WeatherService::class.java)

            val listCall: Call<WeatherResponse> = service.getWeather(
                latitude, longitude, Constants.METRIC_UNIT, Constants.APP_ID
            )

            showProgressDialog()
            listCall.enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {

                    if (response.isSuccessful) {

                        hideProgressDialog()

                        val weatherList: WeatherResponse = response.body()!!

                        val weatherResponseJsonString = Gson().toJson(weatherList)
                        prefs.saveWeather(weatherResponseJsonString)

                        setupUI()

                    } else {
                        val responseCode = response.code()
                        when (responseCode) {
                            400 -> {
                            }
                            404 -> {
                            }
                            else -> {
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) { //error
                    hideProgressDialog()
                }
            })

        } else {
            Toast.makeText(this, "No internet connection available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkForLocationPermissions() {
        if (!isLocationEnabled()) {
            Toast.makeText(this, "Your location provider is turned off. Please turn it on", Toast.LENGTH_SHORT).show()

            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        } else {
            Dexter.withContext(this).withPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report!!.areAllPermissionsGranted()) {
                            requestLocationData()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(request: MutableList<PermissionRequest>?, p1: PermissionToken?) {
                        showRationalDialogForPermissions()
                    }
                }).onSameThread().check()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper()!!)
    }

    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this)
            .setMessage("It Looks like you have turned off permissions required for this feature. It can be enable under Application settings")
            .setPositiveButton("GO TO SETTINGS") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun showProgressDialog() {
        mProgressDialog = Dialog(this)
        mProgressDialog!!.setContentView(R.layout.dialog_custom_progress)
        mProgressDialog!!.show()
    }

    private fun hideProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog!!.dismiss()
        }


    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                requestLocationData()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}