package com.calenpen.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A reusable template that pre-fills a new [Note] with a specific layout / structure.
 *
 * Built-in templates ship with the app; users can also create custom templates from any
 * existing note.
 */
@Entity(tableName = "templates")
data class Template(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Display name shown in the template picker. */
    val name: String,

    /** Brief description of the template. */
    val description: String = "",

    /** Template type key (maps to [TemplateType] constants). */
    val type: String = TemplateType.BLANK,

    /** Paper background style (maps to [PaperStyle] constants). */
    val paperStyle: String = PaperStyle.BLANK,

    /**
     * Serialised stroke data (JSON) that forms the template's guide lines or pre-drawn
     * elements.  Null for fully blank templates.
     */
    val strokesJson: String? = null,

    /**
     * Serialised list of text regions / prompts that the user should fill in.
     * Each region specifies position, size, and placeholder text.
     */
    val textRegionsJson: String? = null,

    /** Whether this is a built-in template shipped with the app. */
    val isBuiltIn: Boolean = false,

    /** Thumbnail image path (file-system path or asset path) for the template picker. */
    val thumbnailPath: String? = null,

    /** Creation timestamp (epoch millis). */
    val createdAt: Long = System.currentTimeMillis()
)
