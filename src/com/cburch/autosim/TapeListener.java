/* Copyright (c) 2006, Carl Burch. License information is located in the
 * com.cburch.autosim.Main source code and at www.cburch.com/proj/autosim/. */

package com.cburch.autosim;

interface TapeListener {
    public void positionClicked(Tape tape, int position);
    public void keyTyped(Tape tape, char what);
}
