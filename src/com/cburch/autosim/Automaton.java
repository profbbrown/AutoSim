/* Copyright (c) 2006, Carl Burch. License information is located in the
 * com.cburch.autosim.Main source code and at www.cburch.com/proj/autosim/. */

package com.cburch.autosim;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;

abstract class Automaton {
    public static final int NUM_FRAMES = 15;

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

            // transition to new state
            history.add(new MySnapshot());
            Object[] data = getCurrent().advance(what);
            Animation anim = setCurrent((StateSet) data[0], (LinkedList) data[1]);

            // put character onto tape
            int pos = tape.getCursorPosition();
            tape.write(pos, what);
            tape.setCursorPosition(pos + 1);
            anim = anim.merge(tape.setHeadPositionAnimate(pos + 1));

            anim.start();
        }
    }

    private class MySnapshot implements Snapshot {
        private StateSet current;

        public MySnapshot() {
            this.current = getCurrent();
        }
        public void restore() {
            setCurrent(this.current);
            int pos = canvas.getTape().getCursorPosition() - 1;
            canvas.getTape().setCursorPosition(pos);
            canvas.getTape().setHeadPositionAnimate(pos).start();
            canvas.getTape().write(pos, Alphabet.BLANK);
        }
    }

    private TapeListener tape_listener;

    private LinkedList states = new LinkedList();
    private LinkedList transitions = new LinkedList();
    private LinkedList components = new LinkedList();
    private Alphabet alphabet = new Alphabet(Alphabet.alphabet + Alphabet.ELSE);

    private StateSet current = new StateSet(this);
    private StateSet current_draw = current;
    private Canvas canvas = null;
    private Rectangle bounding = null;

    private LinkedList history = new LinkedList();
        // for storing StateSets previously stepped through

    public Automaton() {
        tape_listener = new MyTapeListener();
    }

    //
    // ABSTRACT METHODS
    //
    public abstract State createState();
    public abstract Transition createTransition(State src, State dst);

    //
    // ACCESS METHODS
    //
    public Alphabet getAlphabet() { return alphabet; }
    public Canvas getCanvas() { return canvas; }
    public List getHistory() { return history; }

    public Iterator getStates() {
        return states.iterator();
    }
    public StateSet getInitialStates() {
        StateSet ret = new StateSet(this);
        for(Iterator it = getStates(); it.hasNext(); ) {
            State state = (State) it.next();
            if(state.isInitial()) ret.add(state);
        }
        return ret;
    }
    public Iterator getTransitions() {
        return transitions.iterator();
    }
    public Iterator getComponents() {
        return components.iterator();
    }
    public Iterator getAllComponents() {
        return Iterators.join(getTransitions(),
            Iterators.join(getStates(), getComponents()));
    }
    public Iterator getAllComponentsReverse() {
        return Iterators.join(Iterators.reverse(components),
            Iterators.join(Iterators.reverse(states),
                Iterators.reverse(transitions)));
    }

    //
    // CONFIGURATION METHODS
    //
    public void setTapeListener(TapeListener listen) {
        tape_listener = listen;
    }
    public void setCanvas(Canvas canvas) { this.canvas = canvas; }
    public void setToolBoxTape(ToolBox toolbox, Tape tape) {
        toolbox.setPlayControlsVisible(false);
    }

    public void exposeConnections(Graphics g, State what) {
        for(Iterator it = getTransitions(); it.hasNext(); ) {
            Transition transition = (Transition) it.next();
            if(transition.getSource() == what
                    || transition.getDest() == what) {
                transition.expose(g);
            }
        }
    }

    public AutomatonComponent addComponent(AutomatonComponent what) {
        components.add(what);
        invalidateBounds();
        return what;
    }
    public void removeComponent(AutomatonComponent what) {
        components.remove(what);
    }

    public State addState() {
        State q = createState();
        if(q != null) {
            states.add(q);
            invalidateBounds();
        }
        return q;
    }
    public void removeState(State what) {
        current.remove(what);
        current_draw.remove(what);
        states.remove(what);

        Graphics g = null;
        if(canvas != null) g = canvas.getGraphics();

        LinkedList to_remove = new LinkedList();
        for(Iterator it = getTransitions(); it.hasNext(); ) {
            Transition transition = (Transition) it.next();
            if(transition.getSource() == what || transition.getDest() == what) {
                to_remove.add(transition);
            }
        }
        for(Iterator it = to_remove.iterator(); it.hasNext(); ) {
            Transition transition = (Transition) it.next();
            if(g != null) transition.expose(g);
            transitions.remove(transition);
        }
    }

    public Transition addTransition(State src, State dst) {
        Transition delta = createTransition(src, dst);
        if(delta != null) {
            transitions.add(delta);
            invalidateBounds();
        }
        return delta;
    }
    public void removeTransition(Transition what) {
        transitions.remove(what);
    }

    public void remove(AutomatonComponent comp) {
        if(comp instanceof State) {
            removeState((State) comp);
        } else if(comp instanceof Transition) {
            removeTransition((Transition) comp);
        } else {
            removeComponent(comp);
        }
    }

    //
    // SIMULATION METHODS
    //
    public void doPlay() {
        StateSet states = getInitialStates();
        if(states.size() == 0) {
            JOptionPane.showMessageDialog(null, "No initial state is selected.");
            return;
        }
        setCurrent(states);
        history.clear();

        Tape tape = canvas.getTape();
        if(tape != null) {
            tape.completeReset();
            tape.addTapeListener(tape_listener);
            tape.setShowHead(true);
            tape.grabFocus();
        }
    }
    public void doStop() {
        setCurrent(new StateSet(this));
        Tape tape = canvas.getTape();
        if(tape != null) tape.completeReset();
    }
    public void doPause() { }
    public void doStep() { }
    public void doBackStep() {
        if(history.size() > 0) {
            ((Snapshot) history.removeLast()).restore();
        }
    }
    public void doResetSimulation() { }

    public StateSet getCurrent() {
        return current;
    }
    public StateSet getCurrentDraw() {
        return current_draw;
    }
    public void setCurrent(StateSet data) {
        if(data == null) data = new StateSet(this);
        StateSet old_draw = current_draw;
        current = data;
        current_draw = data;

        Graphics g = canvas.getGraphics();
        current_draw.expose(g);
        old_draw.expose(g);
    }
    public Animation setCurrent(StateSet data, LinkedList traversed) {
        if(data == null) data = new StateSet(this);
        StateSet old_draw = current_draw;
        current = data;
        current_draw = new StateSet(this);

        Graphics g = canvas.getGraphics();
        Animation ret = new AnimationThread(g, traversed);
        current_draw.expose(g);
        old_draw.expose(g);
        return ret;
    }
    private class AnimationThread extends Animation {
        private Graphics g;
        private LinkedList traversed;
        private StateSet old;

        public AnimationThread(Graphics g, LinkedList traversed) {
            this.g = g;
            this.traversed = traversed;
            this.old = current;

            for(Iterator it = traversed.iterator(); it.hasNext(); ) {
                Transition trans = (Transition) it.next();
                trans.setCursorProgress(0.0);
                trans.setCursorExists(true);
            }
        }
        public boolean step(int frames) {
            if(traversed.size() == 0 || frames >= NUM_FRAMES
                    || current != old) {
                canvas.setSuppressRepaint(true);
                for(Iterator it = traversed.iterator(); it.hasNext(); ) {
                    Transition trans = (Transition) it.next();
                    trans.setCursorExists(false);
                }

                if(current == old) {
                    current_draw = current;
                    current.expose(g);
                }
                canvas.setSuppressRepaint(false);

                return false;
            } else {
                canvas.setSuppressRepaint(true);
                for(Iterator it = traversed.iterator(); it.hasNext(); ) {
                    Transition trans = (Transition) it.next();
                    trans.setCursorProgress((double) frames / NUM_FRAMES);
                }
                canvas.setSuppressRepaint(false);
                return true;
            }
        }
    }

    //
    // GUI METHODS
    //
    public AutomatonComponent find(int x, int y, Graphics g) {
        for(Iterator it = getAllComponentsReverse(); it.hasNext(); ) {
            AutomatonComponent comp = (AutomatonComponent) it.next();
            if(comp.isIn(x, y, g)) return comp;
        }
        return null;
    }
    public State findState(int x, int y, Graphics g) {
        for(Iterator it = Iterators.reverse(states); it.hasNext(); ) {
            State state = (State) it.next();
            if(state.isIn(x, y, g)) return state;
        }
        return null;
    }
    public void draw(Graphics g) {
        for(Iterator it = getAllComponents(); it.hasNext(); ) {
            ((AutomatonComponent) it.next()).draw(g);
        }
    }

    //
    // BOUNDING BOX METHODS
    //
    public Dimension getDimensions(Graphics g) {
        if(bounding == null) computeBoundingBox(g);

        int width = bounding.width;
        if(bounding.x > 0) width = bounding.x + bounding.width;
        int height = bounding.height;
        if(bounding.y > 0) height = bounding.y + bounding.height;
        return new Dimension(width, height);
    }
    public Rectangle getBounds(Graphics g) {
        if(bounding == null) computeBoundingBox(g);
        return new Rectangle(bounding);
    }
    public void invalidateBounds() { bounding = null; }
    private void computeBoundingBox(Graphics g) {
        bounding = null;
        Rectangle box = new Rectangle();
        for(Iterator it = getAllComponents(); it.hasNext(); ) {
            AutomatonComponent comp = (AutomatonComponent) it.next();
            comp.getBounds(box, g);
            if(bounding == null) {
                bounding = new Rectangle(box);
            } else {
                bounding.add(box);
            }
        }
        if(bounding == null) bounding = new Rectangle();
        bounding.grow(5, 5);
    }

    //
    // FILE METHODS
    //
    public void print(GroupedWriter fout) {
        fout.println("Automaton Simulator, " + Main.FILE_VERSION_NAME);
        if(this instanceof TuringMachine) fout.print("turing");
        else if(this instanceof DFA) fout.print("dfa");
        else if(this instanceof NFA) fout.print("nfa");
        else if(this instanceof DPDA) fout.print("dpda");
        else fout.print("??");
        fout.print(" "); fout.beginGroup(); fout.println();
        printAutomaton(fout);
        fout.endGroup(); fout.println();
    }
    private void printAutomaton(GroupedWriter fout) {
        fout.print("alphabet ");
        fout.printlnGroup(alphabet.toString());

        for(Iterator it = getStates(); it.hasNext(); ) {
            State state = (State) it.next();
            fout.print("state "); fout.beginGroup(); fout.println();
            state.print(fout);
            fout.endGroup(); fout.println();
        }

        for(Iterator it = getTransitions(); it.hasNext(); ) {
            Transition transition = (Transition) it.next();
            int i = states.indexOf(transition.getSource());
            int j = states.indexOf(transition.getDest());
            fout.print("edge " + i + " " + j + " ");
            fout.beginGroup(); fout.println();
            transition.print(fout);
            fout.endGroup(); fout.println();
        }

        for(Iterator it = getComponents(); it.hasNext(); ) {
            AutomatonComponent comp = (AutomatonComponent) it.next();
            if(comp instanceof AutomatonLabel) {
                fout.print("label "); fout.beginGroup(); fout.println();
                comp.print(fout);
                fout.endGroup(); fout.println();
            }
        }

    }
    public static Automaton read(GroupedReader fin) throws IOException {
        String what = fin.readLine();
        if(!what.equals("Automaton Simulator, " + Main.FILE_VERSION_NAME)) {
            throw new IOException("unrecognized file version");
        }

        Automaton ret;
        String type = fin.readLine().trim();
        if(type.equals("dfa")) ret = new DFA();
        else if(type.equals("nfa")) ret = new NFA();
        else if(type.equals("dpda")) ret = new DPDA();
        else if(type.equals("turing")) ret = new TuringMachine();
        else throw new IOException("unknown automaton type");
        fin.beginGroup();
        while(!fin.atGroupEnd()) {
            String key = fin.readLine().trim();
            if(key != null && key.length() > 0) {
                if(!ret.setKey(key, fin)) {
                    fin.readGroup();
                }
            }
        }
        fin.endGroup();
        return ret;
    }
    public boolean setKey(String key, GroupedReader fin) throws IOException {
        if(key.equals("alphabet")) {
            alphabet.set(fin.readGroup());
            return true;
        } else if(key.equals("state")) {
            fin.beginGroup();
            State state = addState();
            state.read(fin);
            fin.endGroup();
            return true;
        } else if(key.equals("label")) {
            fin.beginGroup();
            AutomatonLabel label = new AutomatonLabel(this);
            addComponent(label);
            label.read(fin);
            fin.endGroup();
            return true;
        } else if(key.startsWith("edge ")) {
            StringTokenizer tokens = new StringTokenizer(key);
            try {
                tokens.nextToken();

                int src_i = Integer.parseInt(tokens.nextToken());
                State src = (State) states.get(src_i);
                if(src == null) {
                    throw new IOException("source " + src_i + " not defined");
                }

                int dst_i = Integer.parseInt(tokens.nextToken());
                State dst = (State) states.get(dst_i);
                if(dst == null) {
                    throw new IOException("dest " + dst_i + " not defined");
                }

                Transition transition = addTransition(src, dst);
                fin.beginGroup();
                transition.read(fin);
                fin.endGroup();
                return true;
            } catch(NumberFormatException e) {
                throw new IOException("ill-formatted edge (" + key + ")");
            } catch(IndexOutOfBoundsException e) {
                throw new IOException("ill-formatted edge ("
                    + key + ")");
            }
        } else {
            return false;
        }
    }
}
