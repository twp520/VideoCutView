package com.colin.videocutview

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                100
            )
        } else {
            initView()
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initView()
            }
        }
    }

    private fun initView() {
        val adapter = TestAdapter(this)
        main_cut.setFrameAdapter(adapter)
        main_cut.setVideoDuration(20000)
        main_cut.setOnCutDurationListener { startMs, endMs, state, orientation ->

        }
        main_btn.setOnClickListener {


        }

        main_test.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        main_test.adapter = TestAdapter(this)

        val creator = NativeFrameCreator()
        val stringByNative = creator.getStringByNative()

        Toast.makeText(this, stringByNative, Toast.LENGTH_SHORT).show()

        val path =
            Environment.getExternalStorageDirectory().absolutePath + "/tencent/MicroMsg/WeiXin/1564324950181.mp4"
        creator.openVideoFile(path)
    }
}
