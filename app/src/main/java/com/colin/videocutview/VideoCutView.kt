package com.colin.videocutview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.Canvas.ALL_SAVE_FLAG
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View


/**
 * create by colin 2019-07-16
 *
 * 剪辑的拖动框. 这里用不着考虑测量的情况。也不考虑padding的情况。
 *
 * 使用时候需要设置固定大小高度。使用默认实现即可。
 *
 */

typealias OnCutListener = (left: Float, right: Float) -> Unit

typealias OnCutDurationListener = (startMs: Long, endMs: Long) -> Unit

class VideoCutView : View {

    constructor(context: Context) : super(context)

    constructor(context: Context, attr: AttributeSet) : super(context, attr)


    private var mLeftPadding = 0f //左边界的padding值
    private var mRightPadding = 0f //有边界的padding值
    private var mRectF = RectF() //选中部分的矩形范围
    private val mPaint = Paint() //画笔


    private val leftClick = 0
    private val rightClick = 1
    private var mLeftBitmap: Bitmap
    private var mRightBitmap: Bitmap

    private var mLastX = 0f
    private var mDownX = 0f
    private val mode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)

    private var mListener: OnCutListener? = null

    init {
        mPaint.style = Paint.Style.STROKE
        mPaint.color = Color.WHITE
        mPaint.strokeWidth = 5f

        //todo 处理自定义属性，线的颜色，边界的切图
        mLeftBitmap = BitmapFactory.decodeResource(resources, R.drawable.icon_crop_left)
        mRightBitmap = BitmapFactory.decodeResource(resources, R.drawable.icon_crop_right)
        mRectF.left = mLeftPadding + mLeftBitmap.width

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mRightPadding = w.toFloat()
        mRectF.right = mRightPadding - mRightBitmap.width
        mRectF.top = 5f
        mRectF.bottom = h.toFloat() - 5f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        //绘制背景颜色
        val saved = canvas.saveLayer(null, null, ALL_SAVE_FLAG)

        mPaint.color = Color.parseColor("#80000000")
        canvas.drawRect(getLeftWidth().toFloat(), 0f, mLeftPadding, height.toFloat(), mPaint)

        canvas.drawRect(mRightPadding, 0f, width.toFloat() - getRightWidth().toFloat(), height.toFloat(), mPaint)

        //先画上下两根线
        mPaint.style = Paint.Style.STROKE
        mPaint.color = Color.WHITE
        canvas.drawLine(mLeftPadding, 0f, mRightPadding, 0f, mPaint)
        canvas.drawLine(mLeftPadding, height.toFloat() - 5f, mRightPadding, height.toFloat() - 5f, mPaint)

        //再画边界
        canvas.drawBitmap(mLeftBitmap, mLeftPadding, 0f, mPaint)
        canvas.drawBitmap(mRightBitmap, mRightPadding - mRightBitmap.width, 0f, mPaint)

        //画高亮
        mPaint.xfermode = mode
        mPaint.style = Paint.Style.FILL
        canvas.drawRect(mRectF, mPaint)
        mPaint.xfermode = null

        canvas.restoreToCount(saved)

    }

    private var click = -1

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        var consumed = false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                //判断是否点击点在边界上，如果是则处理此次事件。
                val downX = event.x
                mDownX = downX
                mLastX = downX
                consumed = if (downX > mLeftPadding && downX < mLeftPadding + mLeftBitmap.width) {
                    click = leftClick
                    mLastX = downX
                    true
                } else if (downX > mRightPadding - mRightBitmap.width && downX < mRightPadding) {
                    click = rightClick
                    mLastX = downX
                    true
                } else {
                    click = -1
                    false
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val moveX = event.x
                val dx = moveX - mLastX
                consumed = when (click) {
                    leftClick -> {
                        val newPadding = Math.max(mLeftPadding + dx, 0f)
                        if (newPadding + mLeftBitmap.width < mRectF.right - 5) {
                            mLeftPadding = newPadding
                        }
                        true
                    }
                    rightClick -> {
                        val newPadding = Math.min(mRightPadding + dx, width.toFloat())
                        if (newPadding - mRightBitmap.width > mRectF.left + 5) {
                            mRightPadding = newPadding
                        }
                        true
                    }
                    else -> {

                        false
                    }
                }
                mLastX = moveX
                mRectF.left = mLeftPadding + mLeftBitmap.width
                mRectF.right = mRightPadding - mRightBitmap.width
                if (consumed) {
                    invalidate()
                    mListener?.invoke(mRectF.left, mRectF.right)
                }
            }

            MotionEvent.ACTION_UP -> {
                //这里不考虑长按等事件，简单处理。 有需要可以再处理。
                val upX = event.x
                if (Math.abs(upX - mDownX) < 5) {
                    performClick()
                }
                click = -1
            }
        }

        return consumed
    }

    fun setOnCutListener(listener: OnCutListener) {
        mListener = listener
    }

    fun getLeftWidth(): Int {
        return mLeftBitmap.width
    }

    fun getRightWidth(): Int {
        return mRightBitmap.width
    }

    /**
     * 获得左边的坐标
     */
    fun getStart(): Float {
        return mRectF.left
    }

    /**
     * 获得右边的坐标
     */
    fun getEnd(): Float {
        return mRectF.right
    }
}