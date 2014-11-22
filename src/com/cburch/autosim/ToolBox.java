/* Copyright (c) 2006, Carl Burch. License information is located in the
 * com.cburch.autosim.Main source code and at www.cburch.com/proj/autosim/. */

package com.cburch.autosim;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToolBar;

class ToolBox extends JToolBar {
    private static abstract class ToolButton extends JButton
            implements ActionListener, MouseListener, Icon {
        private ToolBox toolbox;
        private Tool tool;
        private JPopupMenu popup = null;

        public ToolButton(ToolBox toolbox, Tool tool) {
            super();

            this.toolbox = toolbox;
            this.tool = tool;

            setIcon(this);
            addActionListener(this);
            addMouseListener(this);
        }

        protected void setPopupMenu(JPopupMenu popup) {
            this.popup = popup;
        }

        public Tool getTool() {
            return tool;
        }
        
        @SuppressWarnings("unused")
		public void setTool(Tool tool) {
            this.tool = tool;
        }

        public void actionPerformed(ActionEvent e) {
            toolbox.selectButton(this);
        }
        public void mouseClicked(MouseEvent e) { }
        public void mouseEntered(MouseEvent e) { }
        public void mouseExited(MouseEvent e) { }
        public void mousePressed(MouseEvent e) {
            if(popup == null) return;
            int mask = InputEvent.BUTTON2_MASK | InputEvent.BUTTON3_MASK;
            if(e.isPopupTrigger() || (e.getModifiers() & mask) != 0) {
                doClick();
                popup.show(this, e.getX(), e.getY());
            }
        }
        public void mouseReleased(MouseEvent e) { }

        public int getIconWidth() { return 20; }
        public int getIconHeight() { return 20; }
        public void paintIcon(Component c, Graphics g, int x, int y) { }
        public void paint(Graphics g) {
            super.paint(g);
            if(popup != null) {
                GraphicsUtil.switchToWidth(g, 1);
                int ht = getHeight();
                int wd = getWidth();
                int[] xp = { wd - 9, wd - 3, wd - 7 };
                int[] yp = { ht - 6, ht - 6, ht - 3 };
                g.setColor(Color.black);
                g.fillPolygon(xp, yp, xp.length);
            }
        }
    }

    private static class ButtonEdge extends ToolButton {
        public ButtonEdge(Canvas canvas, ToolBox toolbox) {
            super(toolbox, new ToolTransition(canvas));
            setToolTipText("Add state transitions to automaton");
        }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            super.paintIcon(c, g, x, y);
            g.setColor(Color.black);
            g.drawLine(x + 3, y + 13, x + 17, y + 7);
            g.setColor(Color.red);
            g.fillOval(x + 1, y + 11, 5, 5);
            g.fillOval(x + 15, y + 5, 5, 5);
            g.setColor(Color.black);
        }
    }

    private static class ButtonState extends ToolButton {
        public ButtonState(Canvas canvas, ToolBox toolbox) {
            super(toolbox, new ToolState(canvas));
            setToolTipText("Add states to automaton");
        }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            super.paintIcon(c, g, x, y);
            g.setColor(Color.red);
            g.fillOval(x + 4, y + 4, 12, 12);
            g.setColor(Color.black);
            g.drawOval(x + 4, y + 4, 12, 12);
        }
    }

    private static class ButtonText extends ToolButton {
        public ButtonText(Canvas canvas, ToolBox toolbox) {
            super(toolbox, new ToolText(canvas));
            setToolTipText("Add text to diagram");
            setPopupMenu(new AlignmentMenu());
        }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            super.paintIcon(c, g, x, y);
            g.setColor(Color.black);
            GraphicsUtil.switchToWidth(g, 2);
            g.drawLine(x + 5, y + 16, x + 10, y + 2);
            g.drawLine(x + 15, y + 16, x + 10, y + 2);
            g.drawLine(x + 7, y + 11, x + 13, y + 11);
            GraphicsUtil.switchToWidth(g, 1);
        }

        private class AlignmentMenu extends JPopupMenu implements ActionListener {
            ButtonGroup hgroup = new ButtonGroup();
            ButtonGroup vgroup = new ButtonGroup();

            JRadioButtonMenuItem HLeft = new JRadioButtonMenuItem("Left");
            JRadioButtonMenuItem HCenter = new JRadioButtonMenuItem("Center");
            JRadioButtonMenuItem HRight = new JRadioButtonMenuItem("Right");

            JRadioButtonMenuItem VBottom = new JRadioButtonMenuItem("Bottom");
            JRadioButtonMenuItem VCenter = new JRadioButtonMenuItem("Center");
            JRadioButtonMenuItem VTop = new JRadioButtonMenuItem("Top");

            AlignmentMenu() {
                super("Text Alignment");

                hgroup.add(HLeft);   add(HLeft);
                hgroup.add(HCenter); add(HCenter);
                hgroup.add(HRight);  add(HRight);

                addSeparator();

                vgroup.add(VBottom); add(VBottom);
                vgroup.add(VCenter); add(VCenter);
                vgroup.add(VTop);    add(VTop);

                HLeft.addActionListener(this);
                HCenter.addActionListener(this);
                HRight.addActionListener(this);
                VBottom.addActionListener(this);
                VCenter.addActionListener(this);
                VTop.addActionListener(this);

                setHAlign(GraphicsUtil.H_LEFT);
                setVAlign(GraphicsUtil.V_BASELINE);
            }

            public void setHAlign(int value) {
                switch(value) {
                case GraphicsUtil.H_LEFT:   HLeft.setSelected(true); break;
                case GraphicsUtil.H_CENTER: HCenter.setSelected(true); break;
                case GraphicsUtil.H_RIGHT:  HRight.setSelected(true); break;
                }
                ((ToolText) getTool()).setHAlign(value);
            }
            public void setVAlign(int value) {
                switch(value) {
                case GraphicsUtil.V_TOP:    VTop.setSelected(true); break;
                case GraphicsUtil.V_CENTER: VCenter.setSelected(true); break;
                case GraphicsUtil.V_BASELINE: VBottom.setSelected(true); break;
                }
                ((ToolText) getTool()).setVAlign(value);
            }

            public void actionPerformed(ActionEvent evt) {
                JRadioButtonMenuItem src = (JRadioButtonMenuItem) evt.getSource();
                if(!src.isSelected()) return;

                if(src == HLeft) setHAlign(GraphicsUtil.H_LEFT);
                if(src == HCenter) setHAlign(GraphicsUtil.H_CENTER);
                if(src == HRight) setHAlign(GraphicsUtil.H_RIGHT);

                if(src == VTop) setVAlign(GraphicsUtil.V_TOP);
                if(src == VCenter) setVAlign(GraphicsUtil.V_CENTER);
                if(src == VBottom) setVAlign(GraphicsUtil.V_BASELINE);
            }
        }
    }

    private static abstract class SimButton extends JButton
            implements ActionListener, Icon {
        private Canvas canvas;

        public SimButton(Canvas canvas, Tape tape) {
            super();
            this.canvas = canvas;
            setIcon(this);
            addActionListener(this);
        }

        public Canvas getCanvas() {
            return canvas;
        }

        public abstract void select();
        public abstract void paintIcon(Component c, Graphics g, int x, int y);

        public void actionPerformed(ActionEvent e) { select(); }

        public int getIconWidth() { return 20; }
        public int getIconHeight() { return 20; }
    }

    private static class ButtonPlay extends SimButton {
        public ButtonPlay(Canvas canvas, Tape tape) {
            super(canvas, tape);
            setToolTipText("Simulate automaton");
        }

        public void select() {
            getCanvas().getAutomaton().doPlay();
        }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(Color.green);
            int[] xp = { x + 5, x +  5, x + 15 };
            int[] yp = { y + 4, y + 16, y + 10 };
            g.fillPolygon(xp, yp, 3);
            g.setColor(Color.black);
            g.drawPolygon(xp, yp, 3);
        }
    }

    private static class ButtonPause extends SimButton {
        public ButtonPause(Canvas canvas, Tape tape) {
            super(canvas, tape);
            setToolTipText("Pause simulation");
        }

        public void select() { getCanvas().getAutomaton().doPause(); }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(Color.red);
            g.fillRect(x +  4, y + 3, 4, 14);
            g.fillRect(x + 12, y + 3, 4, 14);
            g.setColor(Color.black);
            g.drawRect(x +  4, y + 3, 4, 14);
            g.drawRect(x + 12, y + 3, 4, 14);
        }
    }

    private static class ButtonStep extends SimButton {
        public ButtonStep(Canvas canvas, Tape tape) {
            super(canvas, tape);
            setToolTipText("Step simulation");
        }

        public void select() { getCanvas().getAutomaton().doStep(); }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(Color.green);
            int[] xp = { x + 4, x +  4, x + 12 };
            int[] yp = { y + 4, y + 16, y + 10 };
            g.fillPolygon(xp, yp, 3);
            g.setColor(Color.red);
            g.fillRect(x + 13, y + 3, 4, 14);
            g.setColor(Color.black);
            g.drawRect(x + 13, y + 3, 4, 14);
            g.drawPolygon(xp, yp, 3);
        }
    }

    private static class ButtonBackStep extends SimButton {
        public ButtonBackStep(Canvas canvas, Tape tape) {
            super(canvas, tape);
            setToolTipText("Rewind simulation one step");
        }

        public void select() { getCanvas().getAutomaton().doBackStep(); }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(Color.green);
            int[] xp = { x + 17, x + 17, x + 8 };
            int[] yp = { y + 4, y + 16, y + 10 };
            g.fillPolygon(xp, yp, 3);
            g.setColor(Color.red);
            g.fillRect(x + 4, y + 3, 4, 14);
            g.setColor(Color.black);
            g.drawRect(x + 4, y + 3, 4, 14);
            g.drawPolygon(xp, yp, 3);
        }
    }

    private static class ButtonReset extends SimButton {
        public ButtonReset(Canvas canvas, Tape tape) {
            super(canvas, tape);
            setToolTipText("Reset simulation");
        }

        public void select() { getCanvas().getAutomaton().doResetSimulation(); }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(Color.black);
            GraphicsUtil.switchToWidth(g, 4);
            int[] xp = { x +  9, x + 12, x +  9 };
            int[] yp = { y + 12, y + 15, y + 18 };
            g.drawArc(x + 5, y + 5, 14, 14, 0, 270);
            g.drawPolyline(xp, yp, 3);

            g.setColor(Color.red);
            GraphicsUtil.switchToWidth(g, 2);
            g.drawArc(x + 5, y + 5, 14, 14, 0, 270);
            g.drawPolyline(xp, yp, 3);

            GraphicsUtil.switchToWidth(g, 1);
            g.setColor(Color.black);
        }
    }

    private Canvas canvas = null;
    private ToolButton cur_button = null;

    private ButtonState b_state;
    private ButtonPause b_pause;
    private ButtonStep b_step;
    private ButtonBackStep b_back;
    private ButtonReset b_reset;

    public ToolBox(Canvas canvas, Tape tape) {
        this.canvas = canvas;

        add(b_state = new ButtonState(canvas, this));
        add(new ButtonEdge(canvas, this));
        add(new ButtonText(canvas, this));
        addSeparator();
        add(new ButtonPlay(canvas, tape));
        add(b_pause = new ButtonPause(canvas, tape));
        add(b_step  = new ButtonStep(canvas, tape));
        add(b_back  = new ButtonBackStep(canvas, tape));
        add(b_reset = new ButtonReset(canvas, tape));
    }

    public void setCanvas(Canvas canvas) { this.canvas = canvas; }

    public ButtonState getStateTool() {
        return b_state;
    }

    public void setPlayControlsVisible(boolean value) {
        b_reset.setVisible(value);
        b_back.setVisible(value);
        b_step.setVisible(value);
        b_pause.setVisible(value);
    }

    public void selectButton(ToolButton what) {
        if(what.getTool() == null) return;
        if(cur_button != null) cur_button.setSelected(false);
        cur_button = what;
        cur_button.setSelected(true);
        if(canvas != null) {
            canvas.setTool(cur_button.getTool());
            canvas.commitTransaction(true);
        }
    }
}
