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
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

class DPDA extends Automaton {
    private class DPDAState extends State {
        public DPDAState() {
            super(DPDA.this);
        }
        public boolean canBeInitial() {
            return isInitial() || getInitialStates().size() == 0;
        }
    }

    private class DPDATransition extends Transition {
        public static final int STACK_POP = -1;
        public static final int STACK_KEEP = 0;
        public static final int STACK_PUSH = 1;

        private class StackTopItem extends JRadioButtonMenuItem
                implements ActionListener {
            public char value;
            public StackTopItem(char value) {
                super(value == Alphabet.ELSE ? "any" : Alphabet.toString(value));
                this.value = value;
                addActionListener(this);
                setSelected(stack_top == value);
            }
            public void actionPerformed(ActionEvent evt) {
                if(!isSelected()) return;
                stack_top = value;
                setLabelText();
            }
        }

        private class StackActionItem extends JRadioButtonMenuItem
                implements ActionListener {
            private int action;
            private char value;
            public StackActionItem(int action) {
                this(action, Alphabet.BLANK);
            }
            public StackActionItem(int action, char value) {
                super(action == STACK_POP ? "Pop"
                    : action == STACK_KEEP ? "No Change"
                    : "Push " + Alphabet.toString(value));
                this.action = action;
                this.value = value;
                addActionListener(this);
                if(stack_action == STACK_PUSH) {
                    setSelected(action == STACK_PUSH
                        && value == stack_push);
                } else {
                    setSelected(action == stack_action);
                }
            }
            public void actionPerformed(ActionEvent evt) {
                if(!isSelected()) return;
                stack_action = action;
                stack_push = value;
                setLabelText();
            }
        }

        private char stack_top = Alphabet.ELSE;
        private int stack_action = STACK_KEEP;
        private char stack_push;

        public DPDATransition(State src, State dst) {
            super(DPDA.this, src, dst);
        }
        public boolean canBeTransit(char what) {
            if(this.transitsOn(Alphabet.EPSILON)) return false;
            if(what == Alphabet.EPSILON && this.canTransit()) {
                return false;
            }

            for(Iterator it = getTransitions(); it.hasNext(); ) {
                DPDATransition transition = (DPDATransition) it.next();
                if(this != transition
                        && transition.getSource() == this.getSource()) {
                    if(transition.stack_top == this.stack_top
                            || transition.stack_top == Alphabet.ELSE
                            || this.stack_top == Alphabet.ELSE) {
                        if(transition.transitsOn(what)
                                || transition.transitsOn(Alphabet.EPSILON)
                                || what == Alphabet.EPSILON) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }

        public String determineLabelText() {
            StringBuffer ret = new StringBuffer();
            if(stack_top == Alphabet.ELSE) ret.append("any");
            else ret.append(Alphabet.toString(stack_top));
            ret.append(" / ");
            ret.append(super.determineLabelText());
            if(stack_action == STACK_POP) {
                ret.append(": pop");
            } else if(stack_action == STACK_PUSH) {
                ret.append(": push " + Alphabet.toString(stack_push));
            }
            return ret.toString();
        }
        public void createMenu(JPopupMenu menu) {
            JMenu submenu;
            JMenuItem item;
            ButtonGroup bgroup;
            String dict = getAutomaton().getAlphabet().toString();

            submenu = new JMenu("Stack Top");
            menu.add(submenu);
            bgroup = new ButtonGroup();
            for(int i = 0; i < dict.length(); i++) {
                char c = dict.charAt(i);
                item = new StackTopItem(c);
                submenu.add(item);
                bgroup.add(item);
            }

            submenu = new JMenu("Input Symbols");
            menu.add(submenu);
            for(int i = 0; i < dict.length(); i++) {
                submenu.add(createTransitItem(dict.charAt(i)));
            }
            submenu.add(createTransitItem(Alphabet.EPSILON));

            submenu = new JMenu("Stack Action");
            menu.add(submenu);
            bgroup = new ButtonGroup();
            item = new StackActionItem(STACK_POP);
            submenu.add(item); bgroup.add(item);
            item = new StackActionItem(STACK_KEEP);
            submenu.add(item); bgroup.add(item);
            for(int i = 0; i < dict.length(); i++) {
                char c = dict.charAt(i);
                if(c != Alphabet.ELSE) {
                    item = new StackActionItem(STACK_PUSH, c);
                    submenu.add(item); bgroup.add(item);
                }
            }

            menu.addSeparator();
            menu.add(createDeleteItem());
        }

        public void print(GroupedWriter fout) {
            super.print(fout);
            if(stack_top != Alphabet.ELSE) {
                fout.print("stacktop ");
                fout.printlnGroup("" + stack_top);
            }
            if(stack_action == STACK_POP) {
                fout.print("action "); fout.printlnGroup("pop");
            } else if(stack_action == STACK_PUSH) {
                fout.print("action ");
                fout.printlnGroup("push " + stack_push);
            }
        }
        public boolean setKey(String key, GroupedReader fin)
                throws IOException {
            if(key.equals("stacktop")) {
                stack_top = fin.readGroup().charAt(0);
                setLabelText();
                return true;
            } else if(key.equals("action")) {
                String action = fin.readGroup();
                if(action.equals("pop")) {
                    stack_action = STACK_POP;
                } else if(action.startsWith("push ")) {
                    stack_action = STACK_PUSH;
                    stack_push = action.charAt(5);
                }
                setLabelText();
                return true;
            } else {
                return super.setKey(key, fin);
            }
        }
    }

    private class MyTapeListener implements TapeListener {
        public void positionClicked(Tape tape, int position) {
            tape.grabFocus();
        }

        public void keyTyped(Tape tape, char what) {
            if(what == Alphabet.EPSILON) return;
            if(what == Alphabet.ELSE) return;
            if(what == ' ') return;

            if(what == '\010') {
                doBackStep();
                return;
            }

            getHistory().add(new MySnapshot());
            transition(tape, what);
        }
    }

    private class MySnapshot implements Snapshot {
        private StateSet current;
        private int head_pos;
        private char head_char;
        private char head_prev;
        private char head_next;

        public MySnapshot() {
            current   = getCurrent();
            Tape tape = getCanvas().getTape();
            head_pos  = tape.getHeadPosition();
            head_char = tape.read(head_pos);
            head_next = tape.read(head_pos + 1);
            if(head_pos >= 0) head_prev = tape.read(head_pos - 1);
        }
        public void restore() {
            setCurrent(current);
            Tape tape = getCanvas().getTape();
            tape.setHeadPosition(head_pos);
            tape.setCursorPosition(head_pos);
            tape.write(head_pos, head_char);
            tape.write(head_pos + 1, head_next);
            if(head_pos >= 0) tape.write(head_pos - 1, head_prev);
        }
    }

    public DPDA() {
        setTapeListener(new MyTapeListener());
    }

    public State createState() {
        return new DPDA.DPDAState();
    }
    public Transition createTransition(State src, State dst) {
        return new DPDA.DPDATransition(src, dst);
    }

    public void doPlay() {
        super.doPlay();
        transition(getCanvas().getTape(), Alphabet.EPSILON);
    }

    public void transition(Tape tape, char what) {
        // determine current situation
        Iterator<State> statesIter = getCurrent().iterator();
        if(!statesIter.hasNext()) return;
        State state = statesIter.next();
        int old_head_pos = tape.getHeadPosition();
        char stack_top = tape.read(old_head_pos);
        LinkedList used = new LinkedList();

        // transition into next (we iterate because we have
        // to go through any EPSILON transitions)
        for(; true; what = Alphabet.EPSILON) {
            // determine next step
            DPDATransition transition = null;
            for(Iterator it = getTransitions(); it.hasNext(); ) {
                DPDATransition delta = (DPDATransition) it.next();
                if(delta.getSource() == state) {
                    if(delta.stack_top == Alphabet.ELSE ||
                            delta.stack_top == stack_top) {
                        if(delta.transitsOn(what)) {
                            transition = delta;
                            break;
                        }
                    }
                }
            }
            if(transition == null) {
                if(what != Alphabet.EPSILON) state = null;
                break;
            }

            // alter stack according to traversed transition
            int pos = tape.getCursorPosition();
            if(pos == 0 && tape.read(0) == Alphabet.BLANK) pos = -1;
            if(transition.stack_action == DPDATransition.STACK_POP) {
                if(pos >= 0) {
                    tape.write(pos, Alphabet.BLANK);
                    if(pos > 0) {
                        tape.setCursorPosition(pos - 1);
                        stack_top = tape.read(pos - 1);
                    } else {
                        stack_top = Alphabet.BLANK;
                    }
                }
            } else if(transition.stack_action == DPDATransition.STACK_PUSH) {
                if(pos >= 0) tape.setCursorPosition(pos + 1);
                tape.write(pos + 1, transition.stack_push);
                stack_top = transition.stack_push;
            }

            // transition to next state
            state = transition.getDest();
            used.add(transition);
        }

        StateSet state_set = new StateSet(this);
        if(state != null) state_set.add(state);
        Animation anim = setCurrent(state_set, used);
        if(tape.getHeadPosition() != tape.getCursorPosition()) {
            int pos = tape.getCursorPosition();
            anim = anim.merge(tape.setHeadPositionAnimate(pos));
        }
        anim.start();
    }
}
