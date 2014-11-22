/* Copyright (c) 2006, Carl Burch. License information is located in the
 * com.cburch.autosim.Main source code and at www.cburch.com/proj/autosim/. */

package com.cburch.autosim;

import java.util.Iterator;

class DFA extends Automaton {
    class DFAState extends State {
        public DFAState() {
            super(DFA.this);
        }
        public boolean canBeInitial() {
            return isInitial() || getInitialStates().size() == 0;
        }
    }

    class DFATransition extends Transition {
        public DFATransition(State src, State dst) {
            super(DFA.this, src, dst);
        }
        public boolean canBeTransit(char what) {
            if(what == Alphabet.EPSILON) return false;

            for(Iterator it = getTransitions(); it.hasNext(); ) {
                Transition transition = (Transition) it.next();
                if(this != transition
                        && transition.getSource() == this.getSource()
                        && transition.transitsOn(what)) {
                    return false;
                }
            }
            return true;
        }
    }

    public State createState() {
        return new DFA.DFAState();
    }
    public Transition createTransition(State src, State dst) {
        for(Iterator it = getTransitions(); it.hasNext(); ) {
            Transition transition = (Transition) it.next();
            if(transition.getSource() == src
                    && transition.getDest() == dst) {
                return null;
            }
        }
        return new DFA.DFATransition(src, dst);
    }
}
