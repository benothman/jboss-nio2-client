/**
 * JBoss, Home of Professional Open Source. Copyright 2012, Red Hat, Inc., and
 * individual contributors as indicated by the @author tags. See the
 * copyright.txt file in the distribution for a full listing of individual
 * contributors.
 * 
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
package org.jboss.nio2.client;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.HashMap;

/**
 * {@code HttpdCPUStatParser}
 * 
 * Created on Aug 20, 2012 at 3:57:00 PM
 * 
 * @author <a href="mailto:nbenothm@redhat.com">Nabil Benothman</a>
 */
public class HttpdCPUStatParser {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println("Usage: java " + CPUStatParser.class.getName()
					+ " file");
			System.exit(-1);
		}

		DecimalFormat df = new DecimalFormat("#.##");

		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(args[0])));
		String line = null;
		HashMap<Integer, Tuple> data = new HashMap<Integer, HttpdCPUStatParser.Tuple>();

		while ((line = br.readLine()) != null) {
			line = line.trim();
			if (line.equals("")) {
				continue;
			}

			String tab[] = line.split("\\s+");

			int pid = Integer.valueOf(tab[0]);
			double mem = 0;
			if (tab[5].matches("[0-9]+m")) {
				mem = Double.valueOf(tab[5].substring(0, tab[5].length() - 1)) * 1024;
			} else {
				mem = Double.valueOf(tab[5]);
			}
			double cpu = Double.valueOf(tab[8]);

			Tuple t = data.get(pid);
			if (t == null) {
				t = new Tuple();
				t.pid = pid;
				data.put(pid, t);
			}

			t.cpu += cpu;
			t.mem += mem;
			t.count++;
		}

		br.close();

		double cpu = 0, mem = 0;

		for (int pid : data.keySet()) {
			Tuple t = data.get(pid);
			System.out.println("PID = " + t.pid + ", CPU = " + t.getCPU()
					+ ", MEM = " + t.getMEM());

			cpu += data.get(pid).getCPU();
			mem += data.get(pid).getMEM();
		}

		System.out.println("\nCPU = " + df.format(cpu) + "%, MEM = "
				+ df.format(mem / 1024) + "m\n");
	}

	private static class Tuple {
		int pid;
		double cpu;
		double mem;
		int count;

		public double getCPU() {
			return cpu / count;
		}

		public double getMEM() {
			return mem / count;
		}
	}

}
