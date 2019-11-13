/*
 * Copyright © 2014 Jerzy Chalupski
 * Copyright © 2018 Jason A. Donenfeld <Jason@zx2c4.com>
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com> All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package me.msfjarvis.floatingactionbutton

import android.graphics.Rect
import android.view.MotionEvent
import android.view.TouchDelegate
import android.view.View
import java.util.ArrayList

class TouchDelegateGroup(uselessHackyView: View) : TouchDelegate(USELESS_HACKY_RECT, uselessHackyView) {
    private val mTouchDelegates = ArrayList<TouchDelegate>()
    private var mCurrentTouchDelegate: TouchDelegate? = null
    private var mEnabled: Boolean = false

    fun addTouchDelegate(touchDelegate: TouchDelegate) {
        mTouchDelegates.add(touchDelegate)
    }

    fun removeTouchDelegate(touchDelegate: TouchDelegate) {
        mTouchDelegates.remove(touchDelegate)
        if (mCurrentTouchDelegate == touchDelegate) {
            mCurrentTouchDelegate = null
        }
    }

    fun clearTouchDelegates() {
        mTouchDelegates.clear()
        mCurrentTouchDelegate = null
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!mEnabled)
            return false

        var delegate: TouchDelegate? = null

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                for (touchDelegate in mTouchDelegates) {
                    if (touchDelegate.onTouchEvent(event)) {
                        mCurrentTouchDelegate = touchDelegate
                        return true
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> delegate = mCurrentTouchDelegate

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                delegate = mCurrentTouchDelegate
                mCurrentTouchDelegate = null
            }
        }

        return delegate != null && delegate.onTouchEvent(event)
    }

    fun setEnabled(enabled: Boolean) {
        mEnabled = enabled
    }

    companion object {
        private val USELESS_HACKY_RECT = Rect()
    }
}
