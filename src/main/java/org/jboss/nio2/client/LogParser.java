/**
 * JBoss, Home of Professional Open Source. Copyright 2011, Red Hat, Inc., and
 * individual
 * contributors as indicated by the @author tags. See the copyright.txt file in
 * the distribution for a full listing of individual
 * contributors.
 * 
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation; either
 * version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this
 * software; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor,
 * Boston, MA 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.nio2.client;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Collections;
import java.util.LinkedList;

/**
 * {@code LogParser}
 * 
 * Created on Nov 1, 2011 at 10:30:28 AM
 * 
 * @author <a href="mailto:nbenothm@redhat.com">Nabil Benothman</a>
 */
public class LogParser {

	/**
	 * Create a new instance of {@code LogParser}
	 */
	public LogParser() {
		super();
	}

	/**
	 * 
	 * @param filename
	 * @throws Exception
	 */
	public static void parse(String filename, int n) throws Exception {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
		String line = null;
		// drop the header line
		line = br.readLine();
		int abs_min = Integer.MAX_VALUE, abs_max = Integer.MIN_VALUE, tmp0, tmp1;
		double avg_min = Double.MAX_VALUE, avg_max = Double.MIN_VALUE, avg_avg = 0, tmp2;
		String tab[];
		int counter = 0;

		LinkedList<Integer> max_times = new LinkedList<Integer>();
		LinkedList<Integer> min_times = new LinkedList<Integer>();
		LinkedList<Double> avg_times = new LinkedList<Double>();

		while ((line = br.readLine()) != null) {
			if (line.matches("\\s*")) {
				continue;
			}

			counter++;
			tab = line.split("\\s+");
			// Max
			tmp0 = Integer.parseInt(tab[0]);
			max_times.add(tmp0);
			// Min
			tmp1 = Integer.parseInt(tab[1]);
			min_times.add(tmp1);

			// Avg
			tmp2 = Double.parseDouble(tab[2]);
			avg_times.add(tmp2);

			// Update the absolute maximum
			if (abs_max < tmp0) {
				abs_max = tmp0;
			}
			// Update the absolute minimum
			if (abs_min > tmp1) {
				abs_min = tmp1;
			}
			// Update the average sum
			avg_avg += tmp2;
			// Update the maximum average
			if (tmp2 > avg_max) {
				avg_max = tmp2;
			}
			// Update the minimum average
			if (tmp2 < avg_min) {
				avg_min = tmp2;
			}
		}
		br.close();

		// sort lists
		Collections.sort(max_times);
		Collections.sort(min_times);
		Collections.sort(avg_times);

		double avg = 0;

		int toRemove = 12 * counter / 100;
		for (int i = 0; i < toRemove; i++) {
			max_times.removeFirst();
			max_times.removeLast();

			min_times.removeFirst();
			min_times.removeLast();

			avg_times.removeFirst();
			avg_times.removeLast();
		}

		avg_avg /= counter;
		FileWriter fw = new FileWriter(filename + "_merge.txt");
		int size = min_times.size();
		fw.write("max \t min \t avg\n");
		for (int i = 0; i < size; i++) {
			int x_max = max_times.get(i);
			int x_min = min_times.get(i);
			double x_avg = avg_times.get(i);
			avg += x_avg;
			fw.write(x_max + " \t " + x_min + " \t " + x_avg + "\n");
		}

		avg /= size;

		fw.write("-------------- STATS --------------\n");
		fw.write("ABS MAX: " + max_times.getLast() + " ms\n");
		fw.write("ABS MIN: " + min_times.getFirst() + " ms\n");
		fw.write("AVG MAX: " + avg_times.getLast() + " ms\n");
		fw.write("AVG MIN: " + avg_times.getFirst() + " ms\n");
		fw.write("AVG AVG: " + avg + " ms\n");
		fw.flush();
		fw.close();

		// ---------------------------
		String homeDir = System.getProperty("user.home");
		File file = new File(homeDir + File.separatorChar + "stats.txt");
		FileChannel channel = new RandomAccessFile(file, "rw").getChannel();
		FileLock lock = null;

		try {
			lock = channel.lock();
			ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
			buffer.put((n + "\t" + avg + "\n").getBytes()).flip();
			long pos = channel.size();
			channel.write(buffer, pos);
		} catch (Exception exp) {
			exp.printStackTrace();
		} finally {
			lock.release();
		}
		channel.close();
		// Print out the average max, min and avg
		System.err.println("\n\nAVG MAX: " + avg_times.getLast() + " ms");
		System.err.println("AVG MIN: " + avg_times.getFirst() + " ms");
		System.err.println("AVG AVG: " + avg + " ms\n\n");
	}

	/**
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String args[]) throws Exception {
		if (args.length < 2) {
			System.err.println("Usage: java " + LogParser.class.getName() + " filename n");
			System.err.println("filename: the path of the file to be parsed");
			System.err.println("n: the total number of requests to be considered");
			System.exit(1);
		}

		int n = 0;
		try {
			n = Integer.parseInt(args[1]);
		} catch (NumberFormatException nfe) {
			System.err.println("ERROR: " + nfe.getMessage());
			System.exit(-1);
		}

		parse(args[0], n);
	}
}