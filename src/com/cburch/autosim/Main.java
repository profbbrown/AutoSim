/*
 * Copyright (c) 2006, Carl Burch.
 * 
 * This file is part of the Automaton Simulator source code. The latest
 * version is available at http://www.cburch.com/proj/autosim/.
 *
 * Automaton Simulator is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * Automaton Simulator is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Automaton Simulator; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301  USA
 */
 
package com.cburch.autosim;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Main {
    static final String VERSION_NAME = "1.2";
    static final String FILE_VERSION_NAME = "v1.0";

    private Main() { }

    public static void main(String[] args) {
        // to set up Macintosh menu bar to display application name
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "AutoSim");

        Automaton automaton = null;
        if(args.length > 0) {
            try {
                GroupedReader fin = new GroupedReader(new FileReader(new File(args[0])));
                automaton = Automaton.read(fin);
            } catch(IOException e) {
                System.err.println(e.getMessage());
                return;
            }
        }

        MainFrame win = new MainFrame(automaton);
        win.setVisible(true);
    }
}
