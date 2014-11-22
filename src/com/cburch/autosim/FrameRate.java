/* Copyright (c) 2006, Carl Burch. License information is located in the
 * com.cburch.autosim.Main source code and at www.cburch.com/proj/autosim/. */

package com.cburch.autosim;

class FrameRate {
    private long start;
    private int rate;
    private long frame_count = 0;

    public FrameRate(int rate) {
        this.rate = rate;
        this.start = System.currentTimeMillis();
    }

    public void nextFrame() {
        ++frame_count;
        long next = start + frame_count * 1000 / rate;
        long wait = next - System.currentTimeMillis();
        try {
            Thread.sleep(wait < 0 ? 0 : wait);
        } catch(InterruptedException e) { }
    }

    public void restart() {
        frame_count = 0;
        start = System.currentTimeMillis();
    }
}
