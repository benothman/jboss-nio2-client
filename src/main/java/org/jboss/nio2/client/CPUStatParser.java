/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.nio2.client;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;

/**
 * {@code CPUStatParser}
 * <p/>
 *
 * Created on May 14, 2012 at 2:39:31 PM
 *
 * @author <a href="mailto:nbenothm@redhat.com">Nabil Benothman</a>
 */
public class CPUStatParser {

    /**
     * Create a new instance of {@code CPUStatParser}
     */
    public CPUStatParser() {
        super();
    }

    public static void main(String args[]) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java " + CPUStatParser.class.getName() + " file");
            System.exit(-1);
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(args[0])));

        String line = null;
        ArrayList<Double> list = new ArrayList<Double>();

        while ((line = br.readLine()) != null) {
            if ("".equals(line.trim())) {
                continue;
            }

            String tab[] = line.split("\\s+");
            try {
                String s = "".equals(tab[0]) ? tab[9] : tab[8];
                double cpu = Double.parseDouble(s);
                list.add(cpu);
            } catch (Exception exp) {
                // NOPE
                //System.out.println("Exception: " + exp.getMessage());
            }
        }
        Collections.sort(list);
        int x = list.size() * 25 / 100;
        double sum = 0;
        int counter = 0;
        System.out.println("\n *** Total = " + list.size() + ", x = " + x + " ***\n");
        for (int i = x; i < list.size() - x; i++) {
            System.out.println(" --> " + list.get(i));
            sum += list.get(i);
            counter++;
        }
        System.out.println("\n");
        System.out.println("SUM = " + sum + ", COUNT = " + counter);
        System.out.println("AVG %CPU = " + (sum / counter));
        br.close();
    }
}
