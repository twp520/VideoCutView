package com.colin.videocutview

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

/**
 * create by colin 2019-07-16
 */
class TestAdapter(var context: Context) : RecyclerView.Adapter<TestAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ViewHolder {
        val itemView = LayoutInflater.from(context).inflate(R.layout.item_test, parent, false)
        return ViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return 20
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = "item $position"
    }


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.item_img)
    }
}