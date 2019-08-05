package com.colin.videocutview

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Build

/**
 * create by colin 2019-08-05
 */

fun vetor2bitmap(resources: Resources, id: Int): Bitmap {
    val bitmap: Bitmap
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
        val vectorDrawable = resources.getDrawable(id)
        bitmap = Bitmap.createBitmap(
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        vectorDrawable.draw(canvas)
    } else {
        bitmap = BitmapFactory.decodeResource(resources, id)
    }
    return bitmap
}