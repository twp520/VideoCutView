package com.colin.videocutview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView

/**
 * create by colin 2019-08-20
 *
 * 封面选择的view
 *
 * 计算思路跟裁剪layout一样。这里只需要固定10个item。
 *
 * 采用FrameLayout，底层一个帧列表，上层一个View。
 *
 * 滑动 View 选择时间
 */
class ChooseCoverBar : FrameLayout {

    private val mRecyclerView: RecyclerView = RecyclerView(context)
    private var mPxDuration = 0f //每个像素代表的视频时长
    private var isComplete = false //是否准备完成
    private val mRect: ImageView = ImageView(context) //预览的帧画面
    private val mRectLayout: FrameLayout = FrameLayout(context)
    private var mLastX = 0f
    private var mRectWidth: Int = 0
    private var mVideoDuration: Long = 0L
    private var mWidth = 0
    private var mListener: OnChooseListener? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    init {
        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        mRecyclerView.layoutManager = layoutManager
        isEnabled = isComplete
        mRectLayout.isEnabled = isComplete
        mRectLayout.setBackgroundResource(R.drawable.shape_bord_theme_2)
        val padding = dp2px(context, 2.0f)
        mRectLayout.setPadding(padding, padding, padding, padding)
        mRect.scaleType = ImageView.ScaleType.CENTER_CROP
        mRectLayout.addView(
            mRect,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
    }


    override fun onFinishInflate() {
        super.onFinishInflate()
        val params1 = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        //这里改成padding
        mRecyclerView.setBackgroundColor(Color.TRANSPARENT)
        addView(mRecyclerView, params1)

        val params2 = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        val layer = View(context)
        layer.setBackgroundColor(Color.parseColor("#80000000"))
        addView(layer, params2)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mWidth = w
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            return if (isContainsDown(ev.x, ev.y)) {
                true
            } else {
                super.onInterceptTouchEvent(ev)
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        var consume = false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
//                LogUtils.d(NewVideoEditorAct.TAG, "onTouchEvent ACTION_DOWN")
                mLastX = event.x
                consume = isContainsDown(event.x, event.y)
                if (consume) {
                    mListener?.onChooseDown(getProgressTime())
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - mLastX
                val newMargin = mRectLayout.translationX + dx
                consume = if (newMargin >= 0f && newMargin <= mWidth - mRectWidth) {
                    mRectLayout.translationX = newMargin
                    mLastX = event.x
                    mListener?.onChooseMove(getProgressTime(), getStartFramePosition())
                    true
                } else {
                    false
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mListener?.onChooseUp(getProgressTime(), getStartFramePosition())
            }

        }

        return consume || super.onTouchEvent(event)
    }


    private fun getProgressTime(): Long {
        //就根据距离start多少来算
        val distance = mRectLayout.x
        val time = distance * mPxDuration
        return time.toLong()
    }

    private fun isContainsDown(x: Float, y: Float): Boolean {
        val rect = RectF(
            mRectLayout.x,
            mRect.top.toFloat(),
            mRectLayout.x + mRectWidth,
            mRectLayout.bottom.toFloat()
        )
        return rect.contains(x, y)
    }

    /**
     * 设置帧列表的adapter
     */
    fun setFrameAdapter(adapter: RecyclerView.Adapter<*>?) {
        mRecyclerView.adapter = adapter
    }

    /**
     * 设置视频时长
     */
    fun setVideoDuration(duration: Long) {
        mVideoDuration = duration
    }


    /**
     * 设置监听
     */
    fun setChooseListener(listener: OnChooseListener) {
        this.mListener = listener
    }

    /**
     * 获取裁剪后开始的帧在adapter中的索引
     *
     * 若没有找到，则返回-1
     */
    private fun getStartFramePosition(): Int {
        val child = mRecyclerView.findChildViewUnder(mRectLayout.x, 0f)
        child ?: return -1
        return mRecyclerView.getChildAdapterPosition(child)
    }

    /**
     * 当加载完成的时候进行初始化计算
     * 一定要调用这个方法
     */
    fun computeWithDataComplete(itemWidth: Int, firstFrame: Bitmap?) {
        post {
            mRectWidth = itemWidth
            mRectWidth = dp2px(context, 49f)
            val params2 = LayoutParams(mRectWidth, LayoutParams.MATCH_PARENT)
            //计算每个像素代表的时间
            mPxDuration = mVideoDuration / mWidth.toFloat()
            firstFrame?.let {
                mRect.setImageBitmap(it)
            }
            addView(mRectLayout, params2)
            isComplete = true
            mRectLayout.isEnabled = true
            mRecyclerView.isEnabled = true
        }
    }

    /**
     * 通过设置时间更新封面
     */
    fun updateCoverByTime(time: Long) {
        post {
            if (isComplete && time >= 0 && time <= mVideoDuration) {
                val distance = time / mPxDuration
                mRectLayout.translationX = distance
                mListener?.onChooseUp(getProgressTime(), getStartFramePosition())
            }
        }
    }

    /**
     * 更新封面
     */
    fun updateCover(cover: Bitmap?) {
        cover?.let {
            mRect.setImageBitmap(it)
        }
    }


    interface OnChooseListener {

        fun onChooseDown(time: Long)

        fun onChooseMove(time: Long, index: Int)

        fun onChooseUp(time: Long, index: Int)
    }
}


