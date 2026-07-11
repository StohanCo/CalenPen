package com.calenpen.ui.editor

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.calenpen.R

/**
 * Displays a list of PDF pages as thumbnails with selection checkboxes.
 */
class PdfPageAdapter(
    private val pages: List<Bitmap>,
    private val onSelectionChanged: (pageIndex: Int, selected: Boolean) -> Unit
) : RecyclerView.Adapter<PdfPageAdapter.ViewHolder>() {

    private val selectedItems = mutableSetOf<Int>()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.iv_pdf_page)
        val checkbox: CheckBox = view.findViewById(R.id.cb_select_page)
        val pageNumber: TextView = view.findViewById(R.id.tv_page_number)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pdf_page, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.thumbnail.setImageBitmap(pages[position])
        holder.pageNumber.text = holder.itemView.context.getString(R.string.page_number, position + 1)
        holder.checkbox.isChecked = selectedItems.contains(position)
        holder.checkbox.setOnCheckedChangeListener { _, checked ->
            if (checked) selectedItems.add(position) else selectedItems.remove(position)
            onSelectionChanged(position, checked)
        }
        holder.itemView.setOnClickListener {
            holder.checkbox.isChecked = !holder.checkbox.isChecked
        }
    }

    override fun getItemCount(): Int = pages.size
}
