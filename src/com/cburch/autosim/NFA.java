/* Copyright (c) 2006, Carl Burch. License information is located in the
 * com.cburch.autosim.Main source code and at www.cburch.com/proj/autosim/. */

package com.cburch.autosim;

import java.util.LinkedList;

class NFA extends Automaton {
    private class NFAState extends State {
        public NFAState() {
            super(NFA.this);
        }
        public boolean canBeInitial() {
            return true;
        }
    }

    private class NFATransition extends Transition {
        public NFATransition(State src, State dst) {
            super(NFA.this, src, dst);
        }
        public boolean canBeTransit(char what) {
            return true;
        }
    }

    public NFA() {
        getAlphabet().add(Alphabet.EPSILON);
    }

    public void doPlay() {
        super.doPlay();

        // transition to all initial states
        Object[] data = getCurrent().advance(Alphabet.EPSILON);
        Animation anim = setCurrent((StateSet) data[0], (LinkedList) data[1]);
        anim.start();
    }

    public State createState() {
        return new NFAState();
    }
    public Transition createTransition(State src, State dst) {
        for(Transition transition : transitions) {
            if(transition.getSource() == src
                    && transition.getDest() == dst) {
                return null;
            }
        }
        return new NFATransition(src, dst);
    }
}
