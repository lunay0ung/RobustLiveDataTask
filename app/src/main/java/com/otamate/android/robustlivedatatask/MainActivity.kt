package com.otamate.android.robustlivedatatask

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.otamate.android.robustlivedatatask.R.layout.activity_main
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_CHANNEL = "1"
        const val SHOW_STATUS_BAR_ICON = "SHOW_STATUS_BAR_ICON"
        const val HIDE_STATUS_BAR_ICON = "HIDE_STATUS_BAR_ICON"
    }

    private lateinit var mainViewModel: MainViewModel
    private lateinit var mNotifyMgr: NotificationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activity_main)
        setSupportActionBar(toolbar)

        mainViewModel = ViewModelProviders.of(this,  ViewModelProvider.AndroidViewModelFactory.getInstance(application))[MainViewModel::class.java]

        mainViewModel.getProgressLiveData().observe(this, Observer<MainViewModel.ProgressData> { progressData ->
            progressBar.progress = mainViewModel.getProgressData().progress
        })

        mainViewModel.getViewStateLiveData().observe(this, Observer<MainViewModel.ViewStateData> { viewStateData ->
            updateUIFromModel()
        })

        fabRestart.setOnClickListener {
            startProgress()
        }

        buttonBegin.setOnClickListener {
            startProgress()
        }

        progressBar.max = MainViewModel.ITERATIONS

        val intentFilter = IntentFilter()
        intentFilter.addAction(SHOW_STATUS_BAR_ICON)
        intentFilter.addAction(HIDE_STATUS_BAR_ICON)

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(broadCastReceiver, IntentFilter(intentFilter))

        mNotifyMgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onBackPressed() {
        super.onBackPressed()
        mNotifyMgr.cancel(NOTIFICATION_ID)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadCastReceiver)
    }

    private fun startProgress() {
        mainViewModel.getProgressData().progress = 0
        mainViewModel.setViewStateData(mainViewModel.getViewStateData().copy(isInProgress = true, isFinished = false))
        updateUIFromModel()
    }

    private fun updateUIFromModel() {
        if (mainViewModel.getViewStateData().isBegun && buttonBegin.visibility == View.VISIBLE ) {
            buttonBegin.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
        }

        if (mainViewModel.getViewStateData().isInProgress) {
            fabRestart.visibility = View.GONE
        } else {
            if (mainViewModel.getViewStateData().isFinished) {
                fabRestart.visibility = View.VISIBLE
            }
        }
    }

    private val broadCastReceiver = object: BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
            val mBuilder = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL)
                .setSmallIcon(R.drawable.sb_anim_icon)
                .setContentTitle("Title")
            val resultIntent = Intent(applicationContext, MainActivity::class.java)
            val resultPendingIntent = PendingIntent.getActivity(
                applicationContext,
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            mBuilder.setContentIntent(resultPendingIntent)
            val notification = mBuilder.build()
            notification.flags = notification.flags or
                    (Notification.FLAG_NO_CLEAR or Notification.FLAG_ONGOING_EVENT)

            when (intent?.action) {
                SHOW_STATUS_BAR_ICON -> mNotifyMgr.notify(NOTIFICATION_ID, notification)

                HIDE_STATUS_BAR_ICON -> mNotifyMgr.cancel(NOTIFICATION_ID)
            }
        }
    }
}