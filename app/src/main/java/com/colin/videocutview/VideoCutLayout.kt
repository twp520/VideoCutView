package com.colin.videocutview

import android.content.Context
import android.graphics.Color
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
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

    private val mRecyclerView: MyRecyclerView = MyRecyclerView(context)
    private val mLayoutManager: LinearLayoutManager =
        LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    private val mCutView: VideoCutView
    private var mMaxDuration: Long = 15 * 1000 //视频的最大时长
    private var mVideoDuration = 0L //视频的真实时长
    private var mPxDuration = 0f //每个像素代表的视频时长
    private var mFramePxDuration = 0f //帧列表每个像素代表的视频时长
    private var mCutDuration = 0L //剪辑的时长
    private var mListener: OnCutDurationListener? = null
    private var isComplete = false //是否准备完成


    constructor(context: Context) : super(context)

    constructor(context: Context, attr: AttributeSet) : super(context, attr)


    init {
        mRecyclerView.layoutManager = mLayoutManager
        mRecyclerView.isEnabled = false
        mCutView = VideoCutView(context)
        mCutView.isEnabled = false
        mCutView.minDuration = 3000f

    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        val params1 = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        //这里改成padding
//        mRecyclerView.setPadding(mCutView.getLeftWidth(), 0, mCutView.getRightWidth(), 0)
        params1.leftMargin = mCutView.getLeftWidth()
        params1.rightMargin = mCutView.getRightWidth()
        mRecyclerView.setBackgroundColor(Color.TRANSPARENT)
        addView(mRecyclerView, params1)

        val params2 = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addView(mCutView, params2)
    }


    /**
     * 设置帧列表的adapter
     */
    fun setFrameAdapter(adapter: RecyclerView.Adapter<*>) {
        mRecyclerView.adapter = adapter
        mRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    computeDuration(mCutView.getStart(), mCutView.getEnd())
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (childCount > 2) {
                    val lastPos = mLayoutManager.findLastVisibleItemPosition()
                    getChildAt(1).visibility = if (lastPos >= adapter.itemCount - 1) View.GONE else View.VISIBLE
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
        var startMs = (startPx * mPxDuration + offset * mFramePxDuration).toLong() //起始时间
        var endMs = (startMs + (right - left) * mPxDuration).toLong()
        if (endMs > mVideoDuration) {
            startMs -= (endMs - mVideoDuration)
            endMs = mVideoDuration
        }
        startMs = Math.max(0, startMs)
        mCutDuration = endMs - startMs
        mListener?.invoke(startMs, endMs)
    }


    /**
     * 当加载完成的时候进行初始化计算
     * 一定要调用这个方法
     *
     * @param func 需要添加一个透明的item
     */
    fun computeWithDataComplete(itemWidth: Int, func: Runnable) {
        mRecyclerView.post {

            var diff = 0f //裁剪区域和list的宽度差
            var offset = 0 //右边露出的宽度
            val cutRange = mCutView.getEnd() - mCutView.getStart() //拖动条的原始区间
            var width = cutRange //拖动条的区间，用来最终计算
            //给剪辑时长赋初始值
            mCutDuration = if (mVideoDuration <= mMaxDuration) {
                mVideoDuration
            } else {
                //如果视频时长大于了最大时长，那么需要将拖动控件往右边移动，让右边露出一些来。
                //将recyclerView的marginRight 设置为0 ，添加一个透明的item，将cutView的右边margin设为item的宽度
                func.run()
                val paramsList = mRecyclerView.layoutParams
                if (paramsList is MarginLayoutParams) {
                    paramsList.rightMargin = 0
                }
                val params = mCutView.layoutParams
                if (params is MarginLayoutParams) {
                    //改变了cutView的margin后，修正了，所以之后的listener中right的坐标不需要减去offset
                    offset = itemWidth - mCutView.getRightWidth()
                    params.rightMargin += offset
                }
                mCutView.layoutParams = params
                //如果要将后面露出来，加一个透明的item
                width -= offset //宽度需要减去露出的部分
                //第二层加一个半透明的view
                val layerView = View(context)
                layerView.setBackgroundColor(Color.parseColor("#80000000"))
                val layerViewParams = LayoutParams(offset + mCutView.getRightWidth(), LayoutParams.MATCH_PARENT)
                layerViewParams.gravity = Gravity.END
                addViewInLayout(layerView, 1, layerViewParams)
                mMaxDuration
            }
            val range = mRecyclerView.computeHorizontalScrollRange()
            //因为帧的item宽度为int，所以会损失一些精度，导致两者有几个像素的误差。这里计算的时候把它减出来.
            if (cutRange > range) {
                val params = mCutView.layoutParams
                if (params is MarginLayoutParams) {
                    //改变了cutView的padding后，修正了，所以之后的listener中right的坐标不需要减去diff
                    diff = cutRange - range
                    params.rightMargin = diff.toInt()
                    width -= diff
                }
                mCutView.layoutParams = params
            }
            //计算出每个像素代表的时长
            mPxDuration = mCutDuration / width
            mCutView.durationPx = mPxDuration
            mFramePxDuration = mVideoDuration / range.toFloat()
            computeDuration(mCutView.getStart(), mCutView.getEnd() - diff - offset)
            isComplete = true
            mCutView.isEnabled = true
            mRecyclerView.isEnabled = true
            requestLayout()
        }
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
    fun setVideoDuration(duration: Long, maxDuration: Long = 15 * 1000) {
        mVideoDuration = duration
        mMaxDuration = maxDuration
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

    /**
     * 获取指针宽度
     *
     * @param orientation 0 -> 左边  1 -> 右边
     */
    fun getBitmapWidth(orientation: Int): Int {
        return if (orientation == 0)
            mCutView.getLeftWidth()
        else
            mCutView.getRightWidth()
    }
}