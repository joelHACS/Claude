package com.example.servicereminder

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.servicereminder.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) checkExactAlarmAndActivate()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnToggle.setOnClickListener {
            if (AlarmScheduler.isActive(this)) {
                AlarmScheduler.deactivate(this)
            } else {
                requestPermissionsAndActivate()
            }
            updateUi()
        }
    }

    override fun onResume() {
        super.onResume()
        updateUi()
    }

    private fun requestPermissionsAndActivate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        checkExactAlarmAndActivate()
    }

    private fun checkExactAlarmAndActivate() {
        if (Build.VERSION.SDK_INT in Build.VERSION_CODES.S..Build.VERSION_CODES.S_V2) {
            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
                return
            }
        }
        AlarmScheduler.scheduleInitial(this)
        updateUi()
    }

    private fun updateUi() {
        val active = AlarmScheduler.isActive(this)
        val doneUntil = AlarmScheduler.getDoneUntil(this)
        val now = System.currentTimeMillis()

        binding.btnToggle.text = if (active)
            getString(R.string.btn_deactivate)
        else
            getString(R.string.btn_activate)

        binding.tvStatus.text = when {
            !active -> getString(R.string.status_inactive)
            doneUntil > now -> {
                val fmt = SimpleDateFormat("dd.MM. HH:mm", Locale.GERMANY)
                getString(R.string.status_done_until, fmt.format(Date(doneUntil)))
            }
            else -> getString(R.string.status_active)
        }
    }
}
