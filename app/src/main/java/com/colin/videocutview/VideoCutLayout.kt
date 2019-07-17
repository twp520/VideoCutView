package com.colin.videocutview

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout

/**
 * create by colin 2019-07-16
 *
 * 一个帧布局，包裹了一个RecycleView 和 CutView
 *
 * 用来处理拖动后，获取到当前需要的帧.
 *
 * 请注意，一定要设置高度。
 */
class VideoCutLayout : FrameLayout {

    private val mRecyclerView: RecyclerView = RecyclerView(context)
    private val mCutView: VideoCutView
    private var mMaxDuration: Long = 15 * 1000 //视频的最大时长
    private var mVideoDuration = 0L //视频的真实时长
    private var mPxDuration = 0L //每个像素代表的视频时长
    private var mFramePxDuration = 0L //帧列表每个像素代表的视频时长
    private var mCutDuration = 0L //剪辑的时长
    private var mListener: OnCutDurationListener? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attr: AttributeSet) : super(context, attr)


    init {
        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        mRecyclerView.layoutManager = layoutManager
        mCutView = VideoCutView(context)

    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        val params1 = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        params1.leftMargin = mCutView.getLeftWidth()
        params1.rightMargin = mCutView.getRightWidth()
        addView(mRecyclerView, params1)

        val params2 = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addView(mCutView, params2)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val width = w - mCutView.getLeftWidth() - mCutView.getRightWidth()
        if (mVideoDuration <= mMaxDuration) {
            mCutDuration = mVideoDuration
            mPxDuration = mVideoDuration / width
        } else {
            mCutDuration = mMaxDuration
            mPxDuration = mMaxDuration / width
        }
    }


    /**
     * 设置帧列表的adapter
     */
    fun setFrameAdapter(adapter: RecyclerView.Adapter<*>) {
        mRecyclerView.adapter = adapter
        mRecyclerView.post {
            val range = mRecyclerView.computeHorizontalScrollRange()
            //计算出每个像素代表的时长,并将剪辑时长赋值为最大值
            mFramePxDuration = mVideoDuration / range
        }

        mRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    computeDuration(mCutView.getStart(), mCutView.getEnd())
                }
            }
        })

        //在滑动的时候，计算当前剪辑的时长
        mCutView.setOnCutListener { left, right ->
            computeDuration(left, right)
        }
    }

    //计算坐标的偏移量
    //由于CutView返回的是高亮部部分的坐标，但是maxWidth是减去了指针宽度的。
    //所以真正的偏移量应该用 高亮部分的坐标-指针宽度，由于CutView的宽度和父容器宽度一致，所以这个值其实等于
    //CutView的 leftPadding . 但注意，仅仅是值相同，其中计算的思想是不一样的。
    private fun computeDuration(left: Float, right: Float) {
        val startPx = left - mCutView.getLeftWidth()
        val offset = mRecyclerView.computeHorizontalScrollOffset()
        Log.e("test", "left = $left , right = $right , width = $width offset = $offset ")
        val startMs = startPx * mPxDuration + offset * mFramePxDuration //起始时间
        val endMs = startMs + (right - left) * mPxDuration
        Log.e(
            "test",
            "startMs = $startMs  endMs = $endMs ,pxDuration = $mPxDuration  ,frameDuration = $mFramePxDuration"
        )
        mCutDuration = endMs.toLong() - startMs.toLong()
        mListener?.invoke(startMs.toLong(), endMs.toLong())
    }


    /**
     * 设置监听
     */
    fun setOnCutDurationListener(listener: OnCutDurationListener) {
        mListener = listener
    }


    /**
     * 设置视频的时长
     */
    fun setVideoDuration(duration: Long) {
        mVideoDuration = duration
        mCutDuration = Math.min(mMaxDuration, mVideoDuration)
    }

    /**
     * 获取裁剪后开始的帧在adapter中的索引
     *
     * 若没有找到，则返回-1
     */
    fun getStartFramePosition(): Int {
        val child = mRecyclerView.findChildViewUnder(mCutView.getStart(), 0f)
        child ?: return -1
        return mRecyclerView.getChildAdapterPosition(child)
    }

    /**
     * 获取裁剪后结束的帧在adapter中的索引
     *
     * 若没有找到，则返回-1
     */
    fun getEndFramePosition(): Int {
        val child = mRecyclerView.findChildViewUnder(mCutView.getEnd(), 0f)
        child ?: return -1
        return mRecyclerView.getChildAdapterPosition(child)
    }
}