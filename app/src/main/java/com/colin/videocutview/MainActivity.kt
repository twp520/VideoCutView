package com.colin.videocutview

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val adapter = TestAdapter(this)
        main_cut.setFrameAdapter(adapter)
        main_cut.setVideoDuration(20000)
        main_cut.setOnCutDurationListener { startMs, endMs ->

        }

        main_btn.setOnClickListener {


        }
    }
}
