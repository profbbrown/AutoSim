/* Copyright (c) 2006, Carl Burch. License information is located in the
 * com.cburch.autosim.Main source code and at www.cburch.com/proj/autosim/. */

package com.cburch.autosim;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.MouseEvent;

abstract class Tool {
    private Canvas canvas;
    private Cursor cursor;

    public Tool(Canvas canvas) {
        this.canvas = canvas;
        this.cursor = Cursor.getDefaultCursor();
    }
    
    public Canvas getCanvas() {
        return canvas;
    }
    
    protected void setCursor(Cursor value) {
        cursor = value == null ? Cursor.getDefaultCursor() : value;
    }

    public void draw(Graphics g) { }
    public void select(Graphics g) {
        canvas.getAutomaton().doStop();
        canvas.setCursor(cursor);
    }
    public void deselect(Graphics g) {
        canvas.setCursor(Cursor.getDefaultCursor());
    }
    public void keyTyped(Graphics g, char c) { }
    public void mouseExited(Graphics g, MouseEvent e) { }
    public void mouseMoved(Graphics g, MouseEvent e) { }
    public void mousePressed(Graphics g, MouseEvent e) { }
    public void mouseReleased(Graphics g, MouseEvent e) { }
    public void mouseDragged(Graphics g, MouseEvent e) { }
}
