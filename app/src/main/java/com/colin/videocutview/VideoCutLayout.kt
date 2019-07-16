package com.colin.videocutview

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
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

    constructor(context: Context) : super(context)

    constructor(context: Context, attr: AttributeSet) : super(context, attr)


    init {
        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        mRecyclerView.layoutManager = layoutManager
        mCutView = VideoCutView(context)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addView(mRecyclerView, params)
        addView(mCutView, params)
    }


    /**
     * 设置帧列表的adapter
     */
    fun  setFrameAdapter(adapter: RecyclerView.Adapter<*>) {
        mRecyclerView.adapter = adapter
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