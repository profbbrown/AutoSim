/* Copyright (c) 2006, Carl Burch. License information is located in the
 * com.cburch.autosim.Main source code and at www.cburch.com/proj/autosim/. */

package com.cburch.autosim;

abstract class Animation extends Thread {
    private static final int FRAME_RATE = 30;
    private static final Animation EMPTY_INSTANCE = new Empty();
    
    private static class Merge extends Animation {
        private Animation first;
        private Animation second;

        public Merge(Animation first, Animation second) {
            this.first = first;
            this.second = second;
        }

        public boolean step(int frames) {
            if(first != null) {
                if(!first.step(frames)) first = null;
            }
            if(second != null) {
                if(!second.step(frames)) second = null;
            }
            return first != null || second != null;
        }
    }
    
    private static class Empty extends Animation {
        public boolean step(int frames) { return false; }
    }

    private boolean is_done = false;

    public abstract boolean step(int frames);

    public void run() {
        FrameRate rate = new FrameRate(FRAME_RATE);
        for(int frames = 0; step(frames); frames++) {
            rate.nextFrame();
        }
        is_done = true;
    }

    public boolean isDone() { return is_done; }

    public Animation merge(Animation other) {
        return new Merge(this, other);
    }

    public static Animation getNull() {
        return EMPTY_INSTANCE;
    }
}
