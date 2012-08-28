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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
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
			System.err.println("Usage: java " + StatCalculator.class.getName()
					+ " path");
			System.exit(1);
		}

		DecimalFormat df = new DecimalFormat("#.####");
		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(args[0])));
		HashMap<Integer, StatCalculator.Tuple> stats = new HashMap<Integer, StatCalculator.Tuple>();
		HashMap<Integer, List<Double>> data = new HashMap<Integer, List<Double>>();
		String line = null;

		while ((line = br.readLine()) != null) {
			if ("".equals(line.trim())) {
				continue;
			}

			String tab[] = line.split("\\s+");
			int key = Integer.parseInt(tab[0]);
			double value = Double.parseDouble(tab[1]);
			if (stats.get(key) == null) {
				stats.put(key, new StatCalculator.Tuple());
			}

			if (data.get(key) == null) {
				data.put(key, new ArrayList<Double>());
			}

			StatCalculator.Tuple tuple = stats.get(key);
			tuple.add(value);
			data.get(key).add(value);
		}

		br.close();

		FileWriter fw = new FileWriter(new File(args[0] + "_cal.txt"));
		List<Integer> keys = new ArrayList<Integer>(stats.keySet());
		// Sorting keys in ascending order
		Collections.sort(keys);
		StatCalculator.Tuple tuple = null;
		fw.write("Req/Sec\tSamples\tDelta\tAVG\n");
		System.out.println("\nReq/Sec\tSamples\tDelta\tAVG");
		for (int key : keys) {
			tuple = stats.get(key);
			double avg = tuple.getAvg();
			double delta = delta(data.get(key), avg);
			System.out.println(key + "\t" + tuple.samples() + "\t" + df.format(delta) + "\t" + df.format(avg));
			fw.write(key + "\t" + tuple.samples() + "\t" + delta + "\t" + tuple.getAvg() + "\n");
		}
		fw.flush();
		fw.close();
		System.out.println("\n");
	}

	/**
	 * 
	 * @param data
	 * @return
	 */
	protected static double delta(List<Double> data, double avg) {
		double delta = 0;
		int n = data.size();
		for (double x : data) {
			delta += Math.pow(x - avg, 2);
		}

		delta = Math.sqrt(delta / (n * (n - 1)));

		return delta;
	}

	/**
     *
     */
	private static class Tuple {

		private int counter = 0;
		private double sum = 0;
		private double delta = 0;

		double getAvg() {
			return sum / counter;
		}

		int samples() {
			return counter;
		}

		void add(double value) {
			sum += value;
			counter++;
		}
	}
}
