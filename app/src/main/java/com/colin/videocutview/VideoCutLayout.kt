package com.colin.videocutview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.RectF
import android.os.Build
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import kotlin.math.max
import kotlin.math.min

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
    private val mLine: View //显示进度的线
    private val mLineWidth: Int by lazy { dp2px(context, 6f) }  //进度的线的宽度
    private var mStartTime: Long = 0L
    private var mEndTime: Long = 0L
    private var mLastX = 0f
    private var mDragListener: OnProgressChangedListener? = null


    constructor(context: Context) : super(context)

    constructor(context: Context, attr: AttributeSet) : super(context, attr)


    init {
        mRecyclerView.layoutManager = mLayoutManager
        mRecyclerView.isEnabled = false
        mCutView = VideoCutView(context)
        mCutView.isEnabled = false
        mCutView.minDuration = 3000f
        mLine = View(context)
        mLine.setBackgroundResource(R.drawable.shape_video_cut_progress_line)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        val params1 = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        //这里改成padding
//        mRecyclerView.setPadding(mCutView.getLeftWidth(), 0, mCutView.getRightWidth(), 0)
        params1.leftMargin = mCutView.getLeftWidth()
        params1.rightMargin = mCutView.getRightWidth()
        val topBottomMargin = dp2px(context, 3f)
        params1.topMargin = topBottomMargin
        params1.bottomMargin = topBottomMargin
        mRecyclerView.setBackgroundColor(Color.TRANSPARENT)
        addView(mRecyclerView, params1)

        val params2 = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        params2.topMargin = topBottomMargin
        params2.bottomMargin = topBottomMargin
        addView(mCutView, params2)
    }


    /**
     * 设置帧列表的adapter
     */
    fun setFrameAdapter(adapter: RecyclerView.Adapter<*>) {
        mRecyclerView.adapter = adapter
        val FRAME_COUNT = 10
        mRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE && adapter.itemCount != FRAME_COUNT) {
                    computeDuration(
                        mCutView.getStart(),
                        mCutView.getEnd(),
                        STATE_IDLE,
                        ORIENTATION_LEFT
                    )
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (isComplete) {
                    computeDuration(
                        mCutView.getStart(),
                        mCutView.getEnd(),
                        STATE_MOVE,
                        ORIENTATION_LEFT
                    )
                }
                if (childCount > 2) {
                    val lastPos = mLayoutManager.findLastVisibleItemPosition()
                    getChildAt(1).visibility =
                        if (lastPos >= adapter.itemCount - 1) View.GONE else View.VISIBLE
                }
            }
        })

        //在滑动的时候，计算当前剪辑的时长
        mCutView.setOnCutListener { left, right, state, orientation ->
            computeDuration(left, right, state, orientation)
            //拖动的时候，白条要跟着动
            setLineLeft((if (orientation == ORIENTATION_LEFT) left else right) - mLineWidth / 2f)
        }
    }

    //计算坐标的偏移量
    //由于CutView返回的是高亮部部分的坐标，但是maxWidth是减去了指针宽度的。
    //所以真正的偏移量应该用 高亮部分的坐标-指针宽度，由于CutView的宽度和父容器宽度一致，所以这个值其实等于
    //CutView的 leftPadding . 但注意，仅仅是值相同，其中计算的思想是不一样的。
    private fun computeDuration(left: Float, right: Float, state: Int, orientation: Int) {
        val startPx = left - mCutView.getLeftWidth()
        val offset = mRecyclerView.computeHorizontalScrollOffset()
//        LogUtils.e(NewVideoEditorAct.TAG, "left = $left , right = $right ,leftWidth = ${mCutView.getLeftWidth()} , startPx = $startPx , offset = $offset")
        var startMs = (startPx * mPxDuration + offset * mFramePxDuration).toLong() //起始时间
//        LogUtils.e(NewVideoEditorAct.TAG, "mPxDuration = $mPxDuration ,mFramePxDuration = $mFramePxDuration ,startMs = $startMs")
        var endMs = (startMs + (right - left) * mPxDuration).toLong()
//        LogUtils.e(NewVideoEditorAct.TAG, "endMs = $endMs")
        if (endMs > mVideoDuration) {
            startMs -= (endMs - mVideoDuration)
            endMs = mVideoDuration
        }
        startMs = max(0, startMs)
        mCutDuration = endMs - startMs
        mStartTime = startMs
        mEndTime = endMs
        mListener?.invoke(startMs, endMs, state, orientation)
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
                val layerViewParams =
                    LayoutParams(offset + mCutView.getRightWidth(), LayoutParams.MATCH_PARENT)
                val margin = dp2px(context, 3f)
                layerViewParams.topMargin = margin
                layerViewParams.bottomMargin = margin
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

            //增加一根竖线，表示播放进度
            val params2 = LayoutParams(mLineWidth, LayoutParams.MATCH_PARENT)
//            params2.leftMargin = (mCutView.getStart() - mLineWidth / 2f).toInt()
            addViewInLayout(mLine, childCount, params2)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mLine.elevation = 30f
                mLine.outlineProvider = ViewOutlineProvider.BOUNDS
            }
            mLine.translationX = mCutView.getStart() - mLineWidth / 2f

            computeDuration(
                mCutView.getStart(),
                mCutView.getEnd() - diff - offset,
                STATE_IDLE,
                ORIENTATION_LEFT
            )
            isComplete = true
            mCutView.isEnabled = true
            mRecyclerView.isEnabled = true
            requestLayout()
        }
    }


    /**
     *  更新播放时间，移动小竖线
     *
     *  @param time 当前播放时间
     */
    fun updatePlayTime(time: Long) {
//        if (time < mStartTime)
//            return
        if (time < mStartTime)
            mStartTime = time

        if (!isComplete)
            return
        val duration = max(time - mStartTime, 0)
        val distance = duration / mPxDuration - mLineWidth / 2f
        setLineLeft(min(mCutView.getStart() + distance, mCutView.getEnd() - mLineWidth / 2f))
    }

    /**
     * 根据拖动的距离计算时间
     */
    private fun getProgressTime(): Long {
        //就根据距离start多少来算
        val distance = mLine.x + mLineWidth / 2f - mCutView.getStart()
        val time = distance * mPxDuration + mStartTime
        return time.toLong()
    }

    private fun setLineLeft(left: Float) {
        mLine.translationX = left
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        //如果按下的坐标位于竖线，则拦截此次事件
        if (ev.action == MotionEvent.ACTION_DOWN) {
            return if (isContainsDown(ev.x, ev.y)) {
                true
            } else {
                super.onInterceptTouchEvent(ev)
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    private fun isContainsDown(x: Float, y: Float): Boolean {
        //因为线很窄，所以右增加20px
        val rect =
            RectF(mLine.x, mLine.top.toFloat(), mLine.x + mLineWidth + 20f, mLine.bottom.toFloat())
        return rect.contains(x, y)
    }

    //拖动竖线
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        var consume = false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
//                LogUtils.d(NewVideoEditorAct.TAG, "onTouchEvent ACTION_DOWN")
                mLastX = event.x
                consume = isContainsDown(event.x, event.y)
                if (consume) {
                    mDragListener?.onDragDown(getProgressTime())
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - mLastX
                val newMargin = mLine.translationX + dx
                consume = if (newMargin >= mCutView.getStart() && newMargin <= mCutView.getEnd()) {
                    mLine.translationX = newMargin
                    mLastX = event.x
                    mDragListener?.onDragMove(getProgressTime())
                    true
                } else {
                    false
                }
            }

            MotionEvent.ACTION_UP -> {
                mDragListener?.onDragUp(getProgressTime())
            }

        }

        return consume || super.onTouchEvent(event)
    }

    fun setOnProgressListener(listener: OnProgressChangedListener) {
        mDragListener = listener
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
        mCutDuration = min(mMaxDuration, mVideoDuration)
    }

    /**
     * 获取裁剪后开始的帧在adapter中的索引
     *
     * 若没有找到，则返回-1
     */
//    fun getStartFramePosition(): Int {
//        val child = mRecyclerView.findChildViewUnder(mCutView.getStart(), 0f)
//        child ?: return -1
//        return mRecyclerView.getChildAdapterPosition(child)
//    }

    /**
     * 获取裁剪后结束的帧在adapter中的索引
     *
     * 若没有找到，则返回-1
     */
//    fun getEndFramePosition(): Int {
//        val child = mRecyclerView.findChildViewUnder(mCutView.getEnd(), 0f)
//        child ?: return -1
//        return mRecyclerView.getChildAdapterPosition(child)
//    }

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

    /**
     * 拖动进度的回调接口
     */
    interface OnProgressChangedListener {

        fun onDragDown(time: Long)

        fun onDragMove(time: Long)

        fun onDragUp(time: Long)
    }
}