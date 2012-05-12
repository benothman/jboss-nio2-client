/**
 * JBoss, Home of Professional Open Source. Copyright 2011, Red Hat, Inc., and individual
 * contributors as indicated by the
 *
 * @author tags. See the copyright.txt file in the distribution for a full listing of individual
 * contributors.
 *
 * This is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation; either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * software; if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.nio2.client;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * {@code StatCalculator}
 * <p/>
 *
 * Created on May 3, 2012 at 11:42:28 AM
 *
 * @author <a href="mailto:nbenothm@redhat.com">Nabil Benothman</a>
 */
public class StatCalculator {

    /**
     * Create a new instance of {@code StatCalculator}
     */
    public StatCalculator() {
        super();
    }

    /**
     *
     * @param args
     * @throws Exception
     */
    public static void main(String args[]) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java " + StatCalculator.class.getName() + " path");
            System.exit(1);
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(args[0])));
        HashMap<Integer, StatElement> stats = new HashMap<Integer, StatElement>();
        String line = null;

        while ((line = br.readLine()) != null) {
            if ("".equals(line.trim())) {
                continue;
            }

            String tab[] = line.split("\\s+");
            int key = Integer.parseInt(tab[0]);
            double value = Double.parseDouble(tab[1]);
            if (stats.get(key) == null) {
                stats.put(key, new StatElement());
            }

            StatElement p = stats.get(key);
            p.add(value);
        }
        br.close();

        FileWriter fw = new FileWriter(new File(args[0] + "_cal.txt"));
        List<Integer> keys = new ArrayList<Integer>(stats.keySet());
        // Sorting keys in ascending order
        Collections.sort(keys);
        StatElement p = null;
        fw.write("Req/Sec \t AVG\n");
        System.out.println("\n Req/Sec \t AVG");
        for (int key : keys) {
            p = stats.get(key);
            System.out.println(" " + key + " \t " + p.getAvg());
            fw.write(key + "  \t  " + p.getAvg() + "\n");
        }
        fw.flush();
        fw.close();
        System.out.println("\n");
    }

    /**
     *
     */
    private static class StatElement {

        private int counter = 0;
        private double sum = 0;

        /**
         * @return the average of the
         */
        double getAvg() {
            return sum / counter;
        }

        /**
         *
         * @param value
         */
        void add(double value) {
            sum += value;
            counter++;
        }

        /**
         * @return the number of samples used
         */
        int samples() {
            return counter;
        }
    }
}
