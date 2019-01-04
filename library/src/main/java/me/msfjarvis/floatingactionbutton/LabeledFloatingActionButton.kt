/*
 * Copyright © 2014 Jerzy Chalupski
 * Copyright © 2018 Jason A. Donenfeld <Jason@zx2c4.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package me.msfjarvis.floatingactionbutton

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.annotation.ColorInt
import com.google.android.material.floatingactionbutton.FloatingActionButton

class LabeledFloatingActionButton @JvmOverloads
constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
) : FloatingActionButton(context, attrs, defStyle) {

    var title: String? = null

    val labelView: TextView?
        get() = getTag(R.id.fab_label) as TextView

    init {

        val attr = context.obtainStyledAttributes(attrs, R.styleable.LabeledFloatingActionButton, 0, 0)
        title = attr.getString(R.styleable.LabeledFloatingActionButton_fab_title)
        attr.recycle()
    }

    fun setColorNormalResId(@ColorInt colorNormalResId: Int) {
        this.setBackgroundColor(colorNormalResId)
    }

    override fun setVisibility(visibility: Int) {
        val label = labelView
        label?.visibility = visibility

        super.setVisibility(visibility)
    }

}
