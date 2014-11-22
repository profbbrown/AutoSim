/* Copyright (c) 2006, Carl Burch. License information is located in the
 * com.cburch.autosim.Main source code and at www.cburch.com/proj/autosim/. */

package com.cburch.autosim;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

class TuringMachine extends DFA {
    private class MyTapeListener implements TapeListener {
        public void positionClicked(Tape tape, int position) {
            tape.setCursorPosition(position);
            tape.grabFocus();
        }

        public void keyTyped(Tape tape, char what) {
            if(what == Alphabet.EPSILON) return;
            if(what == Alphabet.ELSE) return;
            if(what == ' ') what = Alphabet.BLANK;
            if(what == '\010') {
                int pos = tape.getCursorPosition();
                tape.setCursorPosition(pos - 1);
                tape.write(pos - 1, Alphabet.BLANK);
                return;
            }

            int pos = tape.getCursorPosition();
            tape.write(pos, what);
            tape.setCursorPosition(pos + 1);
        }
    }

    private class TMTransition extends DFA.DFATransition {
        private int direction = 1;
        private char output = Alphabet.ELSE;

        private class DirectionMenuItem extends JRadioButtonMenuItem
                implements ActionListener {
            public int value;
            public DirectionMenuItem(int value, String name) {
                super(name);
                this.value = value;
                addActionListener(this);
                setSelected(direction == value);
            }
            public void actionPerformed(ActionEvent evt) {
                if(!isSelected()) return;
                direction = value;
                setLabelText();
            }
        }

        private class OutputSymbolItem extends JRadioButtonMenuItem
                implements ActionListener {
            public char value;
            public OutputSymbolItem(char value) {
                super(value == Alphabet.ELSE ? "no change" : Alphabet.toString(value));
                this.value = value;
                addActionListener(this);
                setSelected(output == value);
            }
            public void actionPerformed(ActionEvent evt) {
                if(!isSelected()) return;
                output = value;
                setLabelText();
            }
        }

        public TMTransition(State src, State dst) {
            super(src, dst);
        }
        public String determineLabelText() {
            String ret = super.determineLabelText() + ": ";
            if(output != Alphabet.ELSE || output == 0) {
                ret += output + ",";
            }
            ret += (direction == 1 ? ">" : "<");
            return ret;
        }
        public void createMenu(JPopupMenu menu) {
            JMenu submenu;
            JMenuItem item;
            ButtonGroup bgroup;

            submenu = new JMenu("Input Symbols");
            menu.add(submenu);
            String dict = getAutomaton().getAlphabet().toString();
            for(int i = 0; i < dict.length(); i++) {
                item = createTransitItem(dict.charAt(i));
                submenu.add(item);
            }

            submenu = new JMenu("Output Symbol");
            menu.add(submenu);
            bgroup = new ButtonGroup();
            for(int i = 0; i < dict.length(); i++) {
                char c = dict.charAt(i);
                item = new OutputSymbolItem(c);
                submenu.add(item);
                bgroup.add(item);
            }

            submenu = new JMenu("Head Movement");
            menu.add(submenu);
            DirectionMenuItem[] movements = {
                new DirectionMenuItem(-1, "Left"),
                new DirectionMenuItem(1, "Right"),
            };
            bgroup = new ButtonGroup();
            for(int i = 0; i < movements.length; i++) {
                submenu.add(movements[i]);
                bgroup.add(movements[i]);
            }

            menu.addSeparator();
            menu.add(createDeleteItem());
        }

        public void print(GroupedWriter fout) {
            super.print(fout);
            fout.print("direction "); fout.printlnGroup(direction == 1 ? "R" : "L");
            if(output != Alphabet.ELSE) {
                fout.print("output "); fout.printlnGroup("" + output);
            }
        }
        public boolean setKey(String key, GroupedReader fin)
                throws IOException {
            if(key.equals("direction")) {
                direction = (fin.readGroup().startsWith("R") ? 1 : -1);
                return true;
            } else if(key.equals("output")) {
                output = fin.readGroup().charAt(0);
                return true;
            } else {
                return super.setKey(key, fin);
            }
        }
    }

    private class MySnapshot implements Snapshot {
        private StateSet current;
        private int head_pos;
        private char head_char;

        public MySnapshot() {
            this.current   = getCurrent();
            this.head_pos  = getCanvas().getTape().getHeadPosition();
            this.head_char = getCanvas().getTape().read(this.head_pos);
        }
        public void restore() {
            setCurrent(this.current);
            getCanvas().getTape().setHeadPosition(this.head_pos);
            getCanvas().getTape().write(this.head_pos, this.head_char);
        }
    }

    public TuringMachine() {
        getAlphabet().add(Alphabet.BLANK);
    }
    public void setToolBoxTape(ToolBox toolbox, Tape tape) {
        toolbox.setPlayControlsVisible(true);
        tape.completeReset();
        tape.setExtendsLeft(true);
        tape.setShowHead(true);
        tape.addTapeListener(new MyTapeListener());
    }

    public Transition createTransition(State src, State dst) {
        return new TMTransition(src, dst);
    }

    //
    // SIMULATION METHODS
    //
    private class PlayThread extends Animation {
        int count = 0;
        Animation current = advance();
        boolean going = true;
        public boolean step(int frames) {
            if(current == null) {
                if(!going || getCurrent().size() == 0) {
                    return false;
                } else {
                    current = advance();
                    count = 0;
                    return true;
                }
            } else if(current.step(count)) {
                ++count;
                return true;
            } else {
                current = null;
                return true;
            }
        }
    }
    private PlayThread thread_going = null;
    public void doPlay() {
        if(getCurrent().size() == 0) if(!startPlay()) return;
        if(thread_going == null) {
            thread_going = new PlayThread();
            thread_going.start();
        }
    }
    public void doStop() {
        setCurrent(new StateSet(this));
        Tape tape = getCanvas().getTape();
        if(tape != null) tape.reset();
        getHistory().clear();
    }
    public void doPause() {
        if(thread_going != null) {
            thread_going.going = false;
            thread_going = null;
            setCurrent(getCurrent());
            Tape tape = getCanvas().getTape();
            tape.setHeadPosition(tape.getHeadPosition());
        }
    }
    public void doStep() {
        doPause();
        if(getCurrent().size() == 0) if(!startPlay()) return;
        advance().start();
    }
    public void doResetSimulation() {
        doPause();
        setCurrent(new StateSet(this));
        getCanvas().getTape().reset();
        getHistory().clear();
    }
    private boolean startPlay() {
        StateSet states = getInitialStates();
        if(states.size() == 0) {
            JOptionPane.showMessageDialog(null, "No initial state is selected.");
            return false;
        }
        setCurrent(states);
        return true;
    }
    private Animation advance() {
        getHistory().add(new MySnapshot());

        Iterator stateIter = getCurrent().iterator();
        if(!stateIter.hasNext()) return Animation.getNull();
        State current = (State) stateIter.next();
        State next = null;
        TMTransition used = null;
        Animation head_move = null;

        Tape tape = getCanvas().getTape();
        char c = tape.read(tape.getHeadPosition());
        for(Iterator it = getTransitions(); it.hasNext(); ) {
            TMTransition trans = (TMTransition) it.next();
            if(trans.getSource() == current) {
                if(trans.transitsOn(c)) {
                    used = trans;
                    next = trans.getDest();
                    break;
                }
                if(trans.transitsOn(Alphabet.ELSE)) {
                    used = trans;
                    next = trans.getDest();
                }
            }
        }

        if(used != null) {
            int pos = tape.getHeadPosition();
            if(used.output != Alphabet.ELSE) {
                tape.write(pos, used.output);
            }
            head_move = tape.setHeadPositionAnimate(pos + used.direction);
        }

        StateSet new_current = new StateSet(this);
        if(next != null) new_current.add(next);
        LinkedList traverse = new LinkedList();
        if(used != null) traverse.add(used);
        return setCurrent(new_current, traverse).merge(head_move);
    }
}
