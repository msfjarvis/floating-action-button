/*
 * Copyright © 2014 Jerzy Chalupski
 * Copyright © 2018 Jason A. Donenfeld <Jason@zx2c4.com>
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com> All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
@file:Suppress("Unused", "WeakerAccess")
package me.msfjarvis.floatingactionbutton

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.TouchDelegate
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.annotation.Keep
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.math.max
import kotlin.math.min

class FloatingActionsMenu : ViewGroup {
    private var mExpandDirection: Int = 0
    private var mButtonSpacing: Int = 0
    private var mLabelsMargin: Int = 0
    private var mLabelsVerticalOffset: Int = 0
    var isExpanded: Boolean = false
        private set
    private val mExpandAnimation = AnimatorSet().setDuration(ANIMATION_DURATION)
    private val mCollapseAnimation = AnimatorSet().setDuration(ANIMATION_DURATION)
    private var mAddButton: FloatingActionButton? = null
    private var mRotatingDrawable: RotatingDrawable? = null
    private var mMaxButtonWidth: Int = 0
    private var mMaxButtonHeight: Int = 0
    private var mLabelsStyle: Int = 0
    private var mLabelsPosition: Int = 0
    private var mForceWhiteAddIcon: Boolean = false
    private var mButtonsCount: Int = 0
    private var mCustomDrawable: Int = 0
    private var mDisableRotationOnExpand: Boolean = false
    private var mTouchDelegateGroup: TouchDelegateGroup? = null
    private var mListener: OnFloatingActionsMenuUpdateListener? = null
    private val touchArea = Rect(0, 0, 0, 0)

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(context, attrs)
    }

    private fun init(context: Context, attributeSet: AttributeSet?) {
        mButtonSpacing = resources.getDimension(R.dimen.fab_actions_spacing).toInt()
        mLabelsMargin = resources.getDimensionPixelSize(R.dimen.fab_labels_margin)
        mLabelsVerticalOffset = resources.getDimensionPixelSize(R.dimen.fab_shadow_offset)

        mTouchDelegateGroup = TouchDelegateGroup(this)
        touchDelegate = mTouchDelegateGroup

        val attr = context.obtainStyledAttributes(attributeSet, R.styleable.FloatingActionsMenu, 0, 0)
        mExpandDirection = attr.getInt(R.styleable.FloatingActionsMenu_fab_expandDirection, EXPAND_UP)
        mLabelsStyle = attr.getResourceId(R.styleable.FloatingActionsMenu_fab_labelStyle, 0)
        mLabelsPosition = attr.getInt(R.styleable.FloatingActionsMenu_fab_labelsPosition, LABELS_ON_LEFT_SIDE)
        mForceWhiteAddIcon = attr.getBoolean(R.styleable.FloatingActionsMenu_fab_forceWhiteAddIcon, false)
        mCustomDrawable = attr.getResourceId(R.styleable.FloatingActionsMenu_fab_drawable, if (mForceWhiteAddIcon) R.drawable.ic_action_add_white else R.drawable.ic_action_add_inverse)
        mDisableRotationOnExpand = attr.getBoolean(R.styleable.FloatingActionsMenu_fab_disableRotationOnExpand, false)
        attr.recycle()

        if (mLabelsStyle != 0 && expandsHorizontally()) {
            throw IllegalStateException("Action labels in horizontal expand orientation is not supported.")
        }

        createAddButton(context)
    }

    fun setOnFloatingActionsMenuUpdateListener(listener: OnFloatingActionsMenuUpdateListener) {
        mListener = listener
    }

    private fun expandsHorizontally(): Boolean {
        return mExpandDirection == EXPAND_LEFT || mExpandDirection == EXPAND_RIGHT
    }

    private fun createAddButton(context: Context) {
        val addIconDrawable = ResourcesCompat.getDrawable(context.resources, mCustomDrawable, context.theme) as Drawable
        mAddButton = if (mDisableRotationOnExpand) {
            FloatingActionButton(context).apply {
                setImageDrawable(addIconDrawable)
                id = R.id.fab_expand_menu_button
                setOnClickListener { toggle() }
            }
        } else {
            val rotatingDrawable = RotatingDrawable(addIconDrawable)
            mRotatingDrawable = rotatingDrawable

            val interpolator = OvershootInterpolator()

            val collapseAnimator = ObjectAnimator.ofFloat(rotatingDrawable, "rotation", EXPANDED_PLUS_ROTATION, COLLAPSED_PLUS_ROTATION)
            val expandAnimator = ObjectAnimator.ofFloat(rotatingDrawable, "rotation", COLLAPSED_PLUS_ROTATION, EXPANDED_PLUS_ROTATION)

            collapseAnimator.interpolator = interpolator
            expandAnimator.interpolator = interpolator

            mExpandAnimation.play(expandAnimator)
            mCollapseAnimation.play(collapseAnimator)

            FloatingActionButton(context).apply {
                setImageDrawable(rotatingDrawable)
                id = R.id.fab_expand_menu_button
                setOnClickListener { toggle() }
            }
        }
        addView(mAddButton, super.generateDefaultLayoutParams())
        mButtonsCount++
    }

    fun addButton(button: LabeledFloatingActionButton) {
        addView(button, mButtonsCount - 1)
        mButtonsCount++

        if (mLabelsStyle != 0) {
            createLabels()
        }
    }

    fun removeButton(button: LabeledFloatingActionButton) {
        removeView(button.labelView)
        removeView(button)
        button.setTag(R.id.fab_label, null)
        mButtonsCount--
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        measureChildren(widthMeasureSpec, heightMeasureSpec)

        var width = 0
        var height = 0

        mMaxButtonWidth = 0
        mMaxButtonHeight = 0
        var maxLabelWidth = 0

        for (i in 0 until mButtonsCount) {
            val child = getChildAt(i)

            if (child.visibility == View.GONE) {
                continue
            }

            when (mExpandDirection) {
                EXPAND_UP, EXPAND_DOWN -> {
                    mMaxButtonWidth = max(mMaxButtonWidth, child.measuredWidth)
                    height += child.measuredHeight
                }
                EXPAND_LEFT, EXPAND_RIGHT -> {
                    width += child.measuredWidth
                    mMaxButtonHeight = max(mMaxButtonHeight, child.measuredHeight)
                }
            }

            if (!expandsHorizontally()) {
                val label = child.getTag(R.id.fab_label) as TextView?
                if (label != null) {
                    maxLabelWidth = max(maxLabelWidth, label.measuredWidth)
                }
            }
        }

        if (expandsHorizontally()) {
            height = mMaxButtonHeight
        } else {
            width = mMaxButtonWidth + if (maxLabelWidth > 0) maxLabelWidth + mLabelsMargin else 0
        }

        when (mExpandDirection) {
            EXPAND_UP, EXPAND_DOWN -> {
                height += mButtonSpacing * (mButtonsCount - 1)
                height = adjustForOvershoot(height)
            }
            EXPAND_LEFT, EXPAND_RIGHT -> {
                width += mButtonSpacing * (mButtonsCount - 1)
                width = adjustForOvershoot(width)
            }
        }

        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        when (mExpandDirection) {
            EXPAND_UP, EXPAND_DOWN -> {
                val expandUp = mExpandDirection == EXPAND_UP

                if (changed) {
                    mTouchDelegateGroup!!.clearTouchDelegates()
                }

                val addButtonY = if (expandUp) b - t - mAddButton!!.measuredHeight else 0
                // Ensure mAddButton is centered on the line where the buttons should be
                val buttonsHorizontalCenter = if (mLabelsPosition == LABELS_ON_LEFT_SIDE)
                    r - l - mMaxButtonWidth / 2
                else
                    mMaxButtonWidth / 2
                val addButtonLeft = buttonsHorizontalCenter - mAddButton!!.measuredWidth / 2
                mAddButton!!.layout(addButtonLeft, addButtonY, addButtonLeft + mAddButton!!.measuredWidth, addButtonY + mAddButton!!.measuredHeight)

                val labelsOffset = mMaxButtonWidth / 2 + mLabelsMargin
                val labelsXNearButton = if (mLabelsPosition == LABELS_ON_LEFT_SIDE)
                    buttonsHorizontalCenter - labelsOffset
                else
                    buttonsHorizontalCenter + labelsOffset

                var nextY = if (expandUp)
                    addButtonY - mButtonSpacing
                else
                    addButtonY + mAddButton!!.measuredHeight + mButtonSpacing

                for (i in mButtonsCount - 1 downTo 0) {
                    val child = getChildAt(i)

                    if (child === mAddButton || child.visibility == View.GONE) continue

                    val childX = buttonsHorizontalCenter - child.measuredWidth / 2
                    val childY = if (expandUp) nextY - child.measuredHeight else nextY
                    child.layout(childX, childY, childX + child.measuredWidth, childY + child.measuredHeight)

                    val collapsedTranslation = (addButtonY - childY).toFloat()
                    val expandedTranslation = 0f

                    child.translationY = if (isExpanded) expandedTranslation else collapsedTranslation
                    child.alpha = if (isExpanded) 1f else 0f

                    val params = child.layoutParams as LayoutParams
                    params.mCollapseDir.setFloatValues(expandedTranslation, collapsedTranslation)
                    params.mExpandDir.setFloatValues(collapsedTranslation, expandedTranslation)
                    params.setAnimationsTarget(child)

                    val label = child.getTag(R.id.fab_label) as View?
                    if (label != null) {
                        val labelXAwayFromButton = if (mLabelsPosition == LABELS_ON_LEFT_SIDE)
                            labelsXNearButton - label.measuredWidth
                        else
                            labelsXNearButton + label.measuredWidth

                        val labelLeft = if (mLabelsPosition == LABELS_ON_LEFT_SIDE)
                            labelXAwayFromButton
                        else
                            labelsXNearButton

                        val labelRight = if (mLabelsPosition == LABELS_ON_LEFT_SIDE)
                            labelsXNearButton
                        else
                            labelXAwayFromButton

                        val labelTop = childY - mLabelsVerticalOffset + (child.measuredHeight - label.measuredHeight) / 2

                        label.layout(labelLeft, labelTop, labelRight, labelTop + label.measuredHeight)

                        touchArea.set(min(childX, labelLeft),
                                childY - mButtonSpacing / 2,
                                max(childX + child.measuredWidth, labelRight),
                                childY + child.measuredHeight + mButtonSpacing / 2)
                        mTouchDelegateGroup!!.addTouchDelegate(TouchDelegate(Rect(touchArea), child))

                        label.translationY = if (isExpanded) expandedTranslation else collapsedTranslation
                        label.alpha = if (isExpanded) 1f else 0f

                        val labelParams = label.layoutParams as LayoutParams
                        labelParams.mCollapseDir.setFloatValues(expandedTranslation, collapsedTranslation)
                        labelParams.mExpandDir.setFloatValues(collapsedTranslation, expandedTranslation)
                        labelParams.setAnimationsTarget(label)
                    }

                    nextY = if (expandUp)
                        childY - mButtonSpacing
                    else
                        childY + child.measuredHeight + mButtonSpacing
                }
            }

            EXPAND_LEFT, EXPAND_RIGHT -> {
                val expandLeft = mExpandDirection == EXPAND_LEFT

                val addButtonX = if (expandLeft) r - l - mAddButton!!.measuredWidth else 0
                // Ensure mAddButton is centered on the line where the buttons should be
                val addButtonTop = b - t - mMaxButtonHeight + (mMaxButtonHeight - mAddButton!!.measuredHeight) / 2
                mAddButton!!.layout(addButtonX, addButtonTop, addButtonX + mAddButton!!.measuredWidth, addButtonTop + mAddButton!!.measuredHeight)

                var nextX = if (expandLeft)
                    addButtonX - mButtonSpacing
                else
                    addButtonX + mAddButton!!.measuredWidth + mButtonSpacing

                for (i in mButtonsCount - 1 downTo 0) {
                    val child = getChildAt(i)

                    if (child === mAddButton || child.visibility == View.GONE) continue

                    val childX = if (expandLeft) nextX - child.measuredWidth else nextX
                    val childY = addButtonTop + (mAddButton!!.measuredHeight - child.measuredHeight) / 2
                    child.layout(childX, childY, childX + child.measuredWidth, childY + child.measuredHeight)

                    val collapsedTranslation = (addButtonX - childX).toFloat()
                    val expandedTranslation = 0f

                    child.translationX = if (isExpanded) expandedTranslation else collapsedTranslation
                    child.alpha = if (isExpanded) 1f else 0f

                    val params = child.layoutParams as LayoutParams
                    params.mCollapseDir.setFloatValues(expandedTranslation, collapsedTranslation)
                    params.mExpandDir.setFloatValues(collapsedTranslation, expandedTranslation)
                    params.setAnimationsTarget(child)

                    nextX = if (expandLeft)
                        childX - mButtonSpacing
                    else
                        childX + child.measuredWidth + mButtonSpacing
                }
            }
        }
    }

    override fun generateDefaultLayoutParams(): ViewGroup.LayoutParams {
        return LayoutParams(super.generateDefaultLayoutParams())
    }

    override fun generateLayoutParams(attrs: AttributeSet): ViewGroup.LayoutParams {
        return LayoutParams(super.generateLayoutParams(attrs))
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams): ViewGroup.LayoutParams {
        return LayoutParams(super.generateLayoutParams(p))
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        bringChildToFront(mAddButton)
        mButtonsCount = childCount

        if (mLabelsStyle != 0) {
            createLabels()
        }
    }

    private fun createLabels() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1 && Build.BRAND == "ASUS")
            return
        val context = ContextThemeWrapper(context, mLabelsStyle)

        for (i in 0 until mButtonsCount) {
            val button = getChildAt(i) as FloatingActionButton

            if (button is LabeledFloatingActionButton) {
                val title = button.title

                val label = AppCompatTextView(context)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    label.setTextAppearance(mLabelsStyle)
                } else {
                    label.setTextAppearance(context, mLabelsStyle)
                }
                label.text = title
                addView(label)

                button.setTag(R.id.fab_label, label)
            }
        }
    }

    fun collapse() {
        collapse(false)
    }

    fun collapseImmediately() {
        collapse(true)
    }

    private fun collapse(immediately: Boolean) {
        if (isExpanded) {
            isExpanded = false
            mTouchDelegateGroup!!.setEnabled(false)
            mCollapseAnimation.duration = (if (immediately) 0 else ANIMATION_DURATION).toLong()
            mCollapseAnimation.start()
            mExpandAnimation.cancel()

            mListener?.onMenuCollapsed()
        }
    }

    fun toggle() {
        if (isExpanded) {
            collapse()
        } else {
            expand()
        }
    }

    fun expand() {
        if (!isExpanded) {
            isExpanded = true
            mTouchDelegateGroup!!.setEnabled(true)
            mCollapseAnimation.cancel()
            mExpandAnimation.start()

            mListener?.onMenuExpanded()
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)

        mAddButton!!.isEnabled = enabled
    }

    public override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        val savedState = SavedState(superState)
        savedState.mExpanded = isExpanded

        return savedState
    }

    public override fun onRestoreInstanceState(state: Parcelable) {
        if (state is SavedState) {
            isExpanded = state.mExpanded
            mTouchDelegateGroup!!.setEnabled(isExpanded)

            mRotatingDrawable?.rotation = if (isExpanded) EXPANDED_PLUS_ROTATION else COLLAPSED_PLUS_ROTATION

            super.onRestoreInstanceState(state.superState)
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    interface OnFloatingActionsMenuUpdateListener {
        fun onMenuExpanded()

        fun onMenuCollapsed()
    }

    private class RotatingDrawable internal constructor(drawable: Drawable) : LayerDrawable(arrayOf(drawable)) {
        var rotation: Float = 0.toFloat()
            @Keep
            set(rotation) {
                field = rotation
                invalidateSelf()
            }

        override fun draw(canvas: Canvas) {
            canvas.save()
            canvas.rotate(rotation, bounds.centerX().toFloat(), bounds.centerY().toFloat())
            super.draw(canvas)
            canvas.restore()
        }
    }

    class SavedState : View.BaseSavedState {
        var mExpanded: Boolean = false

        constructor(parcel: Parcelable?) : super(parcel)

        private constructor(`in`: Parcel) : super(`in`) {
            mExpanded = `in`.readInt() == 1
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(if (mExpanded) 1 else 0)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {

                override fun createFromParcel(`in`: Parcel): SavedState {
                    return SavedState(`in`)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    private inner class LayoutParams internal constructor(source: ViewGroup.LayoutParams) : ViewGroup.LayoutParams(source) {

        val mExpandDir = ObjectAnimator()
        private val mExpandAlpha = ObjectAnimator()
        val mCollapseDir = ObjectAnimator()
        private val mCollapseAlpha = ObjectAnimator()
        private var animationsSetToPlay: Boolean = false

        init {

            mExpandDir.interpolator = EXPAND_INTERPOLATOR
            mExpandAlpha.interpolator = ALPHA_EXPAND_INTERPOLATOR
            mCollapseDir.interpolator = COLLAPSE_INTERPOLATOR
            mCollapseAlpha.interpolator = COLLAPSE_INTERPOLATOR

            mCollapseAlpha.setProperty(View.ALPHA)
            mCollapseAlpha.setFloatValues(1f, 0f)

            mExpandAlpha.setProperty(View.ALPHA)
            mExpandAlpha.setFloatValues(0f, 1f)

            when (mExpandDirection) {
                EXPAND_UP, EXPAND_DOWN -> {
                    mCollapseDir.setProperty(View.TRANSLATION_Y)
                    mExpandDir.setProperty(View.TRANSLATION_Y)
                }
                EXPAND_LEFT, EXPAND_RIGHT -> {
                    mCollapseDir.setProperty(View.TRANSLATION_X)
                    mExpandDir.setProperty(View.TRANSLATION_X)
                }
            }
        }

        fun setAnimationsTarget(view: View) {
            mCollapseAlpha.target = view
            mCollapseDir.target = view
            mExpandAlpha.target = view
            mExpandDir.target = view

            // Now that the animations have targets, set them to be played
            if (!animationsSetToPlay) {
                addLayerTypeListener(mExpandDir, view)
                addLayerTypeListener(mCollapseDir, view)

                mCollapseAnimation.play(mCollapseAlpha)
                mCollapseAnimation.play(mCollapseDir)
                mExpandAnimation.play(mExpandAlpha)
                mExpandAnimation.play(mExpandDir)
                animationsSetToPlay = true
            }
        }

        private fun addLayerTypeListener(animator: Animator, view: View) {
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.setLayerType(View.LAYER_TYPE_NONE, null)
                }

                override fun onAnimationStart(animation: Animator) {
                    view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                }
            })
        }
    }

    companion object {
        const val EXPAND_UP = 0
        const val EXPAND_DOWN = 1
        const val EXPAND_LEFT = 2
        const val EXPAND_RIGHT = 3

        const val LABELS_ON_LEFT_SIDE = 0
        const val LABELS_ON_RIGHT_SIDE = 1

        private const val ANIMATION_DURATION = 300L
        private const val COLLAPSED_PLUS_ROTATION = 0f
        private const val EXPANDED_PLUS_ROTATION = 90f + 45f
        private val EXPAND_INTERPOLATOR = OvershootInterpolator()
        private val COLLAPSE_INTERPOLATOR = DecelerateInterpolator(3f)
        private val ALPHA_EXPAND_INTERPOLATOR = DecelerateInterpolator()

        private fun adjustForOvershoot(dimension: Int): Int {
            return dimension * 12 / 10
        }
    }
}
