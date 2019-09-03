package com.colin.videocutview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

/**
 * create by colin 2019-07-16
 *
 * 剪辑的拖动框. 这里用不着考虑测量的情况。也不考虑padding的情况。
 *
 * 使用时候需要设置固定大小高度。使用默认实现即可。
 *
 */

typealias OnCutListener = (left: Float, right: Float, state: Int, orientation: Int) -> Unit

typealias OnCutDurationListener = (startMs: Long, endMs: Long, state: Int, orientation: Int) -> Unit

const val STATE_MOVE = 0x11
const val STATE_IDLE = 0x12

const val ORIENTATION_LEFT = 0X21
const val ORIENTATION_RIGHT = 0X22


class VideoCutView : View {

    constructor(context: Context) : super(context)

    constructor(context: Context, attr: AttributeSet) : super(context, attr)


    private var mLeftPadding = 0f //左边界的padding值
    private var mRightPadding = 0f //有边界的padding值
    private var mRectF = RectF() //选中部分的矩形范围
    private val mPaint = Paint() //画笔

    private var mLeftBitmap: Bitmap
    private var mRightBitmap: Bitmap

    private var mLastX = 0f
    private var mDownX = 0f

    var minDuration = 0f
    var durationPx = 0f

    private var mListener: OnCutListener? = null

    init {
        mPaint.style = Paint.Style.STROKE
        mPaint.color = Color.WHITE
        mPaint.strokeWidth = 5f

        //todo 处理自定义属性，线的颜色，边界的切图
        mLeftBitmap = vector2bitmap(resources, R.drawable.ic_video_control)
        mRightBitmap = vector2bitmap(resources, R.drawable.ic_video_control_right)
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
//        val saved = canvas.saveLayer(null, null, ALL_SAVE_FLAG)

        mPaint.style = Paint.Style.FILL
        mPaint.color = Color.parseColor("#80000000")
        canvas.drawRect(getLeftWidth().toFloat(), 0f, mLeftPadding, height.toFloat(), mPaint)
        canvas.drawRect(
            mRightPadding,
            0f,
            width.toFloat() - getRightWidth().toFloat(),
            height.toFloat(),
            mPaint
        )

        //先画上下两根线
        mPaint.style = Paint.Style.STROKE
        mPaint.color = ContextCompat.getColor(context, R.color.colorAccent)
        canvas.drawLine(mRectF.left, 0f, mRectF.right, 0f, mPaint)
        canvas.drawLine(mRectF.left, height.toFloat(), mRectF.right, height.toFloat(), mPaint)

        //再画边界
        canvas.drawBitmap(mLeftBitmap, mLeftPadding, 0f, mPaint)
        canvas.drawBitmap(mRightBitmap, mRightPadding - mRightBitmap.width, 0f, mPaint)

        //画高亮
//        mPaint.xfermode = mode
//        mPaint.style = Paint.Style.FILL
//        canvas.drawRect(mRectF, mPaint)
//        mPaint.xfermode = null

//        canvas.restoreToCount(saved)

    }

    private var click = -1

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        var consumed = false
        if (!isEnabled)
            return consumed
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                //判断是否点击点在边界上，如果是则处理此次事件。
                val downX = event.x
                mDownX = downX
                mLastX = downX
                consumed = if (downX > mLeftPadding && downX < mLeftPadding + mLeftBitmap.width) {
                    click = ORIENTATION_LEFT
                    mLastX = downX
                    true
                } else if (downX > mRightPadding - mRightBitmap.width && downX < mRightPadding) {
                    click = ORIENTATION_RIGHT
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
                    ORIENTATION_LEFT -> {
                        val newPadding = max(mLeftPadding + dx, 0f)
                        if (newPadding + mLeftBitmap.width < mRectF.right) {
                            val curDuration =
                                (mRectF.right - newPadding - mLeftBitmap.width) * durationPx
                            if (!(curDuration <= minDuration + 1 && dx > 0)) {
                                mLeftPadding = newPadding
                            }
                        }
                        true
                    }
                    ORIENTATION_RIGHT -> {
                        val newPadding = min(mRightPadding + dx, width.toFloat())
                        if (newPadding - mRightBitmap.width > mRectF.left) {
                            val curDuration =
                                (newPadding - mRightBitmap.width - mRectF.left) * durationPx
                            if (!(curDuration <= minDuration + 1 && dx < 0)) {
                                mRightPadding = newPadding
                            }
                        }
                        true
                    }
                    else -> {

                        false
                    }
                }
                if (consumed) {
                    mLastX = moveX
                    mRectF.left = mLeftPadding + mLeftBitmap.width
                    mRectF.right = mRightPadding - mRightBitmap.width
                    invalidate()
                    mListener?.invoke(mRectF.left, mRectF.right, STATE_MOVE, click)
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                //这里不考虑长按等事件，简单处理。 有需要可以再处理。
                consumed = true
//                val upX = event.x
//                if (abs(upX - mDownX) < 5) {
//                    performClick()
//                } else {
                mListener?.invoke(mRectF.left, mRectF.right, STATE_IDLE, click)
//                }
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