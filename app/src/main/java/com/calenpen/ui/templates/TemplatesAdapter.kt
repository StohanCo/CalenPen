package com.calenpen.ui.templates

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.calenpen.R
import com.calenpen.data.database.entities.Template

class TemplatesAdapter(
    private val onTemplateClick: (Template) -> Unit,
    private val onTemplateDelete: ((Template) -> Unit)? = null
) : ListAdapter<Template, TemplatesAdapter.TemplateViewHolder>(TemplateDiffCallback) {

    inner class TemplateViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_template_name)
        val tvDescription: TextView = view.findViewById(R.id.tv_template_description)
        val tvType: TextView = view.findViewById(R.id.tv_template_type)
        val btnDelete: ImageButton = view.findViewById(R.id.btn_delete_template)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemplateViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_template, parent, false)
        return TemplateViewHolder(view)
    }

    override fun onBindViewHolder(holder: TemplateViewHolder, position: Int) {
        val template = getItem(position)
        holder.tvName.text = template.name
        holder.tvDescription.text = template.description
        holder.tvType.text = template.type.replaceFirstChar { it.uppercase() }
            .replace("_", " ")

        holder.itemView.setOnClickListener { onTemplateClick(template) }

        if (onTemplateDelete != null && !template.isBuiltIn) {
            holder.btnDelete.visibility = View.VISIBLE
            holder.btnDelete.setOnClickListener { onTemplateDelete.invoke(template) }
        } else {
            holder.btnDelete.visibility = View.GONE
        }
    }

    private object TemplateDiffCallback : DiffUtil.ItemCallback<Template>() {
        override fun areItemsTheSame(oldItem: Template, newItem: Template) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Template, newItem: Template) =
            oldItem == newItem
    }
}
