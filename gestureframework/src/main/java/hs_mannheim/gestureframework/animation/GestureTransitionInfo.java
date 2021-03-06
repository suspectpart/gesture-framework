/*
 * Copyright (C) 2016 Insitute for User Experience and Interaction Design,
 *     Hochschule Mannheim University of Applied Sciences
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License
 *
 */

package hs_mannheim.gestureframework.animation;

import android.hardware.SensorEvent;

import hs_mannheim.gestureframework.gesture.swipe.TouchPoint;

public class GestureTransitionInfo {

    TouchPoint mTouchPoint;
    SensorEvent mSensorEvent;
    boolean isTouchGesture, isSensorGesture;

    public GestureTransitionInfo(TouchPoint touchPoint) {
        mTouchPoint = touchPoint;
        isTouchGesture = true;
    }

    public GestureTransitionInfo(SensorEvent sensorEvent) {
        mSensorEvent = sensorEvent;
        isSensorGesture = true;
    }

    public boolean isTouchGesture() {
        return isTouchGesture;
    }

    public boolean isSensorGesture() {
        return isSensorGesture;
    }

    public TouchPoint getTouchPoint() {
        return mTouchPoint;
    }

    public SensorEvent getmSensorEvent() {
        return mSensorEvent;
    }
}
