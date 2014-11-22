/* Copyright (c) 2006, Carl Burch. License information is located in the
 * com.cburch.autosim.Main source code and at www.cburch.com/proj/autosim/. */

package com.cburch.autosim;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

class MainFrame extends JFrame {
    private ToolBox toolbox = null;
    private Canvas canvas = new Canvas();
    private Tape tape = new Tape();
    private File curFile = null;
    private JMenuBar menubar = null;

    public MainFrame(Automaton initial) {
        setSize(790, 565);
        setTitle("Automaton Simulator");
        setBackground(Color.white);
        Listener l = new Listener();
        addWindowListener(l);
        addComponentListener(l);

        toolbox = new ToolBox(canvas, tape);
        canvas.setToolBox(toolbox);
        canvas.setTape(tape);

        menubar = new JMenuBar();
        menubar.add(new MenuFile());
        menubar.add(new MenuHelp());
        setJMenuBar(menubar);

        getContentPane().add(toolbox, BorderLayout.NORTH);

        JScrollPane scroll_pane = new JScrollPane(canvas);
        getContentPane().add(scroll_pane, BorderLayout.CENTER);
        canvas.setScrollPane(scroll_pane);
        getContentPane().add(tape, BorderLayout.SOUTH);

        toolbox.selectButton(toolbox.getStateTool());

        if(initial != null) canvas.setAutomaton(initial);
        canvas.commitTransaction(true);
        computeTitle();
    }

    private void computeTitle() {
        String new_title = "Automaton Simulator";
        if(curFile != null) {
            new_title += ": " + curFile.getName();
        }
        setTitle(new_title);
    }

    public void openAutomaton(File f) throws IOException {
        FileReader fread = new FileReader(f);
        GroupedReader fin = new GroupedReader(fread);

        tape.completeReset();
        Automaton automaton = Automaton.read(fin);
        canvas.setAutomaton(automaton);
        canvas.exposeAll();
        computeTitle();
    }

    public void doQuit() {
        int confirm = JOptionPane.showConfirmDialog(null,
                "Are you sure you want to exit?",
                "Confirm Exit",
                JOptionPane.YES_NO_OPTION);
        if(confirm == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    private class PrintItem implements Pageable {
        public int getNumberOfPages() {
            return 1;
        }
        public PageFormat getPageFormat(int which)
                throws IndexOutOfBoundsException {
            if(which < 0 || which >= getNumberOfPages()) {
                throw new IndexOutOfBoundsException();
            }
            PageFormat format;
            format = new PageFormat();
            format.setOrientation(PageFormat.LANDSCAPE);
            return format;
        }
        public Printable getPrintable(int which)
                throws IndexOutOfBoundsException {
            if(which < 0 || which >= getNumberOfPages()) {
                throw new IndexOutOfBoundsException();
            }
            return (Printable) canvas;
        }
    }

    private class MenuFile extends JMenu implements ActionListener {
        private JFileChooser chooser
            = new JFileChooser(System.getProperty("user.dir"));

        private JMenuItem newDfa;
        private JMenuItem newNfa;
        private JMenuItem newDpda;
        private JMenuItem newTuring;
        private JMenuItem open;
        private JMenuItem save;
        private JMenuItem print;
        private JMenuItem quit;

        public MenuFile() {
            super("File");
            setPopupMenuVisible(true);

            JMenu newMenu = new JMenu("New");
            newDfa = create(newMenu, "Deterministic Finite Automaton");
            newNfa = create(newMenu, "Nondeterministic Finite Automaton");
            newDpda = create(newMenu, "Deterministic Push-Down Automaton");
            newTuring = create(newMenu, "Turing Machine");

            this.add(newMenu);
            open = create(this, "Open", KeyEvent.VK_O);
            save = create(this, "Save", KeyEvent.VK_S);
            print = create(this, "Print", KeyEvent.VK_P);
            quit = create(this, "Quit", KeyEvent.VK_Q);
        }

        private JMenuItem create(JMenu dest, String title) {
            JMenuItem ret = new JMenuItem(title);
            ret.addActionListener(this);
            dest.add(ret);
            return ret;
        }

        private JMenuItem create(JMenu dest, String title, int accel) {
            JMenuItem ret = create(dest, title);
            int mask = ret.getToolkit().getMenuShortcutKeyMask();
            ret.setAccelerator(KeyStroke.getKeyStroke(accel, mask));
            return ret;
        }

        public void actionPerformed(ActionEvent event) {
            Object src = event.getSource();
            if(src == newDfa)       doNew(DFA.class);
            else if(src == newNfa)  doNew(NFA.class);
            else if(src == newDpda) doNew(DPDA.class);
            else if(src == newTuring) doNew(TuringMachine.class);
            else if(src == open)    doOpen();
            else if(src == save)    doSave();
            else if(src == print)   doPrint();
            else if(src == quit)    doQuit();
        }

        private void doNew(Class source) {
            int confirm = JOptionPane.showConfirmDialog(null,
                    "Are you sure you want to clear everything for a new project?",
                    "Confirm New",
                    JOptionPane.YES_NO_OPTION);
            if(confirm == JOptionPane.YES_OPTION) {
                curFile = null;
                Automaton a;
                try {
                    a = (Automaton) source.newInstance();
                } catch(Exception e) {
                    System.err.println(e.getMessage());
                    e.printStackTrace();
                    return;
                }
                tape.completeReset();
                canvas.setAutomaton(a);
                canvas.exposeAll();
                canvas.commitTransaction(true);
            }
        }

        private void doOpen() {
            int returnVal = chooser.showOpenDialog(null);
            if(returnVal == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                try {
                    openAutomaton(f);
                    canvas.commitTransaction(true);
                } catch(IOException e) {
                    JOptionPane.showMessageDialog(null,
                        "Could not open file" + f.toString()
                        + ": " + e.getMessage());
                }
            }
        }

        private void doSave() {
            int returnVal = chooser.showSaveDialog(null);
            if(returnVal != JFileChooser.APPROVE_OPTION) return;

            File f = chooser.getSelectedFile();
            if(f.exists()) {
                int confirm = JOptionPane.showConfirmDialog(this,
                    "The file already exists. Do you want to overwrite it?",
                    "Confirm Overwrite",
                    JOptionPane.YES_NO_OPTION);
                if(confirm != JOptionPane.YES_OPTION) return;
            }
            saveFile(f);
            curFile = f;
            computeTitle();
            canvas.commitTransaction(true);
        }

        private void doPrint() {
            PrinterJob job = PrinterJob.getPrinterJob();
            if(job.printDialog() == false) return;
            job.setPageable(new PrintItem());
            try {
                job.print();
            } catch(PrinterException e) {
                JOptionPane.showMessageDialog(null,
                    "Error during printing: " + e.toString());
            }
        }

        public void saveFile(File f) {
            try {
                FileOutputStream fwrite = new FileOutputStream(f);
                GroupedWriter fout = new GroupedWriter(fwrite);
                canvas.getAutomaton().print(fout);
                fout.close();
            } catch(IOException e) {
                JOptionPane.showMessageDialog(null,
                    "Could not open file.");
            }
        }
    }

    private class MenuHelp extends JMenu implements ActionListener {
        private JMenuItem help;
        private JMenuItem about;

        public MenuHelp() {
            super("Help");
            help = create(this, "Help...");
            about = create(this, "About...");
        }

        private JMenuItem create(JMenu dest, String title) {
            JMenuItem ret = new JMenuItem(title);
            ret.addActionListener(this);
            dest.add(ret);
            return ret;
        }

        public void actionPerformed(ActionEvent event) {
            Object src = event.getSource();
            if(src == help)       doHelp();
            else if(src == about) doAbout();
        }

        private void doHelp() {
            HelpFrame help = new HelpFrame("com/cburch/autosim/doc/index.html");
            URL index = help.getCurrent();
            help.setTitle("Help: Automaton Simulator");
            help.addContentsItem("Contents", index);
            try {
                help.addContentsItem("About", new URL(index, "about.html"));
            } catch(Exception e) { }
            help.setVisible(true);
        }

        private void doAbout() {
            JOptionPane.showMessageDialog(null,
                    "Automaton Simulator " + Main.VERSION_NAME + ". "
                    + "(c) 2001, Carl Burch.\n"
                    + "See Help for details.\n");
        }
    }

    private class Listener implements WindowListener, ComponentListener {
        public void windowActivated(WindowEvent e) { }
        public void windowClosed(WindowEvent e) {
            System.exit(0);
        }
        public void windowClosing(WindowEvent e) {
            doQuit();
        }
        public void windowDeactivated(WindowEvent e) { }
        public void windowDeiconified(WindowEvent e) { }
        public void windowIconified(WindowEvent e) { }
        public void windowOpened(WindowEvent e) { }

        public void componentHidden(ComponentEvent e) { }
        public void componentMoved(ComponentEvent e) { }
        public void componentResized(ComponentEvent e) {
            canvas.computeSize();
        }
        public void componentShown(ComponentEvent e) {
            canvas.computeSize();
        }
    }
}
