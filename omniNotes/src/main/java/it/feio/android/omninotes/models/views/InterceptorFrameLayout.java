/*
 * Copyright (C) 2015 Federico Iosue (federico.iosue@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package it.feio.android.omninotes.models.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import it.feio.android.omninotes.models.listeners.OnViewTouchedListener;


/**
 * отличается от обычного FrameLayout тем, что
 * в onInterceptTouchEvent() вызывается то, что назначено через setOnViewTouchedListener().
 */
public class InterceptorFrameLayout extends FrameLayout {

    private OnViewTouchedListener mOnViewTouchedListener;
    protected IInterceptCondition mCondition;


    public InterceptorFrameLayout(Context context) {
        super(context);
    }


    public InterceptorFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mOnViewTouchedListener != null) {
            mOnViewTouchedListener.onViewTouchOccurred(ev);
        }
        if(mCondition != null) {
            boolean superResult = super.onInterceptTouchEvent(ev); //execute legacy behavior
            return mCondition.sureIntercept(ev) || superResult; // return users intention first or ancestor result.
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        Log.d("IFL", "called"); // This callback is called sometimes and outcomes to swipe-to-dismiss. //todo something.
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    public void setOnViewTouchedListener(OnViewTouchedListener mOnViewTouchedListener) {
        this.mOnViewTouchedListener = mOnViewTouchedListener;
    }

    public void setInterceptCondition(IInterceptCondition condition){
        this.mCondition = condition;
    };


    // todo move out
    public interface IInterceptCondition {
        boolean sureIntercept(MotionEvent event);
    }

}
