/* Copyright (c) 2006, Carl Burch. License information is located in the
 * com.cburch.autosim.Main source code and at www.cburch.com/proj/autosim/. */

package com.cburch.autosim;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

class Tape extends JPanel {
    private static final int BLANK_HEIGHT = 40; // height of each square
    private static final int BLANK_WIDTH  = 40; // width of each square
    private static final int BLANKS_SHOW =  2; // extra blanks on each side

    private static class Contents {
        ArrayList<Character> positives = new ArrayList<Character>();
        ArrayList<Character> negatives = new ArrayList<Character>();
        Character blank = new Character(Alphabet.BLANK);

        public Contents() { }

        public char get(int position) {
            ArrayList<Character> which = position < 0 ? negatives : positives;
            if(position < 0) position = -position;
            if(position >= which.size()) return Alphabet.BLANK;
            Character ret = which.get(position);
            return ret == null ? Alphabet.BLANK : ret.charValue();
        }
        public void set(int position, char value) {
            ArrayList<Character> which = position < 0 ? negatives : positives;
            if(position < 0) position = -position;
            while(which.size() <= position) which.add(blank);
            which.set(position, value);
        }
        public int getMaximumPosition() {
            for(int i = positives.size() - 1; i >= 0; i--) {
                Character val = positives.get(i);
                if(val != null && !val.equals(blank)) return i;
            }
            return 0;
        }
        public int getMinimumPosition() {
            for(int i = negatives.size() - 1; i >= 0; i--) {
                Character val = negatives.get(i);
                if(val != null && !val.equals(blank)) return -i;
            }
            return 0;
        }
    }

    private class Representation extends JPanel {
        private class Listener implements FocusListener, MouseListener,
                KeyListener, ComponentListener {
            private int pressed_loc = Integer.MIN_VALUE;

            public void focusGained(FocusEvent e) {
                expose(cursor);
            }
            public void focusLost(FocusEvent e) {
                expose(cursor);
            }

            public void mouseClicked(MouseEvent e) { }
            public void mouseEntered(MouseEvent e) { }
            public void mouseExited(MouseEvent e) { }
            public void mousePressed(MouseEvent e) {
                representation.grabFocus();
                pressed_loc = representation.locatePosition(e.getX(), e.getY());
            }
            public void mouseReleased(MouseEvent e) {
                int loc = representation.locatePosition(e.getX(), e.getY());
                if(loc == pressed_loc) {
                    for(Iterator<TapeListener> it = listeners.iterator(); it.hasNext(); ) {
                        TapeListener listener = it.next();
                        listener.positionClicked(Tape.this, loc);
                    }
                }
                pressed_loc = Integer.MIN_VALUE;
            }

            public void keyPressed(KeyEvent e) { }
            public void keyReleased(KeyEvent e) { }
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                for(TapeListener listener : listeners) {
                    listener.keyTyped(Tape.this, c);
                }
            }

            public void componentHidden(ComponentEvent e) { }
            public void componentMoved(ComponentEvent e) { }
            public void componentResized(ComponentEvent e) {
                representation.computeSize();
            }
            public void componentShown(ComponentEvent e) {
                representation.computeSize();
            }
        }

        private int last_length = -1;

        public Representation() {
            setBackground(Color.white);
            Listener l = new Listener();
            addFocusListener(l);
            addMouseListener(l);
            addKeyListener(l);
            addComponentListener(l);
            computeSize();
        }
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            int base = scroll.getValue();
            int width = this.getSize().width;
            int x;

            // draw background for cursor
            if(hasFocus()) {
                x = (cursor - base) * BLANK_WIDTH;
                g.setColor(Color.black);
                g.fillRect(x, 0, BLANK_WIDTH, BLANK_HEIGHT);
            }

            // draw dividing lines
            GraphicsUtil.switchToWidth(g, 3);
            g.setColor(Color.gray);
            g.drawLine(0, 0, width, 0);
            g.drawLine(0, BLANK_HEIGHT, width, BLANK_HEIGHT);
            for(int i = 0; i * BLANK_WIDTH < width; i++) {
                x = i * BLANK_WIDTH;
                g.drawLine(x, 0, x, BLANK_HEIGHT);
            }
            GraphicsUtil.switchToWidth(g, 1);

            // draw head of reader
            if(show_head) {
                x = (int) ((head_draw - base) * BLANK_WIDTH
                    + BLANK_WIDTH / 2);
                int[] xp = { x - 10, x + 10, x };
                int[] yp = { 3, 3, 13 };
                g.setColor(Color.green);
                g.fillPolygon(xp, yp, xp.length);
                g.setColor(Color.black);
                g.drawPolygon(xp, yp, xp.length);
            }

            // draw characters
            g.setColor(Color.black);
            x = BLANK_WIDTH / 2;
            for(int i = 0; i * BLANK_WIDTH < width; i++, x += BLANK_WIDTH) {
                char c = contents.get(base + i);
                if(c == Alphabet.BLANK) continue;
                boolean is_cur = (base + i == cursor && hasFocus());
                if(is_cur) g.setColor(Color.white);
                GraphicsUtil.drawText(g, "" + c, x, BLANK_HEIGHT / 2,
                    GraphicsUtil.H_CENTER, GraphicsUtil.V_CENTER);
                if(is_cur) g.setColor(Color.black);
            }
        }
        public void expose(int position) {
            repaint();
        }
        public int locatePosition(int x, int y) {
            return scroll.getValue() + x / BLANK_WIDTH;
        }
        public void computeSize() {
            int len = contents.getMaximumPosition() - contents.getMinimumPosition() + 1;
            len += BLANKS_SHOW;
            if(extends_left) len += BLANKS_SHOW;
            if(len == last_length) return;

            setMinimumSize(new Dimension(BLANK_WIDTH, BLANK_HEIGHT));
            setPreferredSize(new Dimension(BLANK_WIDTH * len, BLANK_HEIGHT));
            if(scroll != null) scroll.computeValues();
        }
    }

    private class Scroll extends JScrollBar
            implements ComponentListener, ChangeListener {
        public Scroll() {
            setOrientation(HORIZONTAL);
            setUnitIncrement(1);
            addComponentListener(this);
            getModel().addChangeListener(this);
        }
        public void computeValues() {
            int block = Math.max(1, getSize().width / BLANK_WIDTH - 1);
            int min = contents.getMinimumPosition();
            if(extends_left) min -= BLANKS_SHOW;
            int max = contents.getMaximumPosition() - block + 1;
            max += BLANKS_SHOW;
            max = Math.max(min, max);

            setBlockIncrement(block);
            setMinimum(min);
            setMaximum(max);
        }

        // ChangeListener methods
        public void stateChanged(ChangeEvent e) {
            representation.repaint();
        }

        // ComponentListener methods
        public void componentHidden(ComponentEvent e) { }
        public void componentMoved(ComponentEvent e) { }
        public void componentResized(ComponentEvent e) {
            computeValues();
        }
        public void componentShown(ComponentEvent e) {
            computeValues();
        }
    }

    private class MyFocusListener implements FocusListener {
        public void focusGained(FocusEvent e) {
            representation.grabFocus();
        }
        public void focusLost(FocusEvent e) { }
    }
    
    private class HeadAnimation extends Animation {
        private int src;
        private int dst;
        private double dist;

        public HeadAnimation(int src, int dst) {
            this.src = src;
            this.dst = dst;
            dist = (double) (dst - src) / Automaton.NUM_FRAMES;
        }
        public boolean step(int frames) {
            if(head != dst) {
                return false;
            } else if(frames <= Automaton.NUM_FRAMES) {
                head_draw = src + dist * frames;
                representation.expose((int) head_draw);
                representation.expose((int) head_draw + 1);
                representation.expose((int) head_draw - 1);
                return true;
            } else {
                head_draw = dst;
                representation.expose(dst);
                return false;
            }
        }
    }

    // private variables
    private LinkedList<TapeListener> listeners = new LinkedList<TapeListener>();
    private Contents contents = new Contents();
    private int cursor = 0;
    private int head = 0;
    private double head_draw = 0;
    private boolean show_head = false;
    private boolean extends_left = false;

    private Representation representation = new Representation();
    private Scroll scroll = new Scroll();

    // constructor method
    public Tape() {
        addFocusListener(new MyFocusListener());
        reset();
        setLayout(new BorderLayout());
        add(representation, BorderLayout.CENTER);
        add(scroll, BorderLayout.SOUTH);
    }

    // instance methods
    public void addTapeListener(TapeListener listener) {
        if(!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    public void removeTapeListener(TapeListener listener) {
        listeners.remove(listener);
    }
    public void completeReset() {
        listeners = new LinkedList<TapeListener>();
        show_head = false;
        extends_left = false;
        cursor = 0;
        reset();
    }
    public void reset() {
        contents = new Contents();
        representation.computeSize();
        setHeadPosition(0);
        repaint();
    }

    public void setExtendsLeft(boolean value) {
        if(extends_left != value) {
            extends_left = value;
            scroll.computeValues();
            representation.computeSize();
        }
    }

    public void write(int pos, char value) {
        char val = (value == ' ' ? Alphabet.BLANK : value);
        contents.set(pos, val);
        representation.expose(pos);
    }
    public char read(int pos) {
        return contents.get(pos);
    }

    public int getHeadPosition() { return head; }
    public void setHeadPosition(int value) {
        representation.expose(head);
        head = value;
        head_draw = value;
        representation.expose(head);
        moveIntoView(head);
        representation.computeSize();
    }
    public Animation setHeadPositionAnimate(int value) {
        representation.expose(head);
        int old_head = head;
        head = value;
        return new HeadAnimation(old_head, value);
    }

    public void setShowHead(boolean value) {
        show_head = value;
        representation.expose(head);
    }

    public int getCursorPosition() { return cursor; }
    public void setCursorPosition(int value) {
        representation.expose(cursor);
        cursor = value;
        moveIntoView(cursor);
        representation.expose(cursor);
    }

    private void moveIntoView(int pos) {
        int val = scroll.getValue();
        int block = scroll.getBlockIncrement();
        if(pos < val) scroll.setValue(pos);
        if(pos >= val + block) scroll.setValue(pos - block + 1);
    }
}
