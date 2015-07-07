package hs_mannheim.pattern_interaction_model;

import android.app.Application;
import android.graphics.Point;
import android.text.TextWatcher;

import hs_mannheim.pattern_interaction_model.gesture.swipe.SwipeDetector;
import hs_mannheim.pattern_interaction_model.model.IPacketReceiver;
import hs_mannheim.pattern_interaction_model.model.InteractionContext;

public class InteractionApplication extends Application {

private InteractionContext mInteractionContext;

    public InteractionContext getInteractionContext() {
        return mInteractionContext;
    }

    public void setInteractionContext(InteractionContext mInteractionContext) {
        this.mInteractionContext = mInteractionContext;
    }
}
