/* Copyright (c) 2006, Carl Burch. License information is located in the
 * com.cburch.autosim.Main source code and at www.cburch.com/proj/autosim/. */

package com.cburch.autosim;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

//
// DRAWING TOOLS
//
class ToolText extends Tool {
    private Label current = null;
    private int halign = GraphicsUtil.H_LEFT;
    private int valign = GraphicsUtil.V_BASELINE;

    public ToolText(Canvas canvas) {
        super(canvas);
        setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
    }

    public void deselect(Graphics g) {
        super.deselect(g);
        if(current != null) {
            current.exposeCursor(getCanvas(), g);
            current = null;
        }
    }

    public void setHAlign(int value) { halign = value; }
    public void setVAlign(int value) { valign = value; }

    public void mouseReleased(Graphics g, MouseEvent e) {
        int x = e.getX();
        int y = e.getY();

        // deselect current label
        if(current != null) {
            current.exposeCursor(getCanvas(), g);
            current = null;
        }

        // if nothing found, create a new label
        if(current == null) {
            AutomatonLabel item = new AutomatonLabel(getCanvas().getAutomaton());
            item.move(x, y).setAlignment(halign, valign);
            current = item.getLabel();
            getCanvas().getAutomaton().addComponent(item);
        }
        if(current != null) {
            current.exposeCursor(getCanvas(), g);
            getCanvas().expose(current.getBounds(g));
        }
        getCanvas().commitTransaction(false);
    }
    public void keyTyped(Graphics g, char c) {
        if(current != null) {
            Rectangle prev = current.getBounds(g);
            current.exposeCursor(getCanvas(), g);
            boolean changed = current.addLetter(c);
            if(changed) {
                getCanvas().expose(prev);
                getCanvas().expose(current.getBounds(g));
                current.exposeCursor(getCanvas(), g);
                getCanvas().commitTransaction(false);
            }
        }
    }

    public void draw(Graphics g) {
        super.draw(g);
        if(current != null) current.drawCursor(g);
    }
}
