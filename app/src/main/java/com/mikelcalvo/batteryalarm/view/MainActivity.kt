package com.mikelcalvo.batteryalarm.view

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikelcalvo.batteryalarm.R
import com.mikelcalvo.batteryalarm.databinding.ActivityMainBinding
import com.mikelcalvo.batteryalarm.model.AlarmType
import com.mikelcalvo.batteryalarm.model.BatteryAlarmSettings
import com.mikelcalvo.batteryalarm.receiver.BatteryLevelReceiver

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var batteryAlarms: MutableList<BatteryAlarmSettings>
    private lateinit var batteryAlarmAdapter: BatteryAlarmAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        batteryAlarms = mutableListOf(
            BatteryAlarmSettings(alarmType = AlarmType.BATTERY_FULL),
            BatteryAlarmSettings(
                alarmType = AlarmType.BATTERY_LOW,
                batteryPercentage = 15
            )
        )

        val layoutManager = LinearLayoutManager(this)
        batteryAlarmAdapter = BatteryAlarmAdapter(batteryAlarms)

        with(binding.rvBatteryAlarms) {
            this.layoutManager = layoutManager
            this.adapter = batteryAlarmAdapter
        }

        registerBatteryLevelReceiver()
    }

    override fun onResume() {
        super.onResume()
        checkExactAlarmPermission()
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.exact_alarm_permission_title)
                    .setMessage(R.string.exact_alarm_permission_message)
                    .setPositiveButton(R.string.exact_alarm_permission_open_settings) { _, _ ->
                        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                    }
                    .setNegativeButton(R.string.exact_alarm_permission_dismiss, null)
                    .show()
            }
        }
    }


    private fun registerBatteryLevelReceiver() {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val receiver = BatteryLevelReceiver()
        applicationContext.registerReceiver(receiver, intentFilter)
    }
}