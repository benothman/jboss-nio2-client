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
 * {@code HttpdCPUStatCalculator}
 * 
 * Created on Aug 24, 2012 at 11:19:44 AM
 * 
 * @author <a href="mailto:nbenothm@redhat.com">Nabil Benothman</a>
 */
public class HttpdCPUStatCalculator {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println("Usgae: java "
					+ HttpdCPUStatCalculator.class.getName() + " fileName");
			System.exit(-1);
		}

		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(args[0])));
		HashMap<Integer, HttpdCPUStatCalculator.Tuple> data = new HashMap<Integer, HttpdCPUStatCalculator.Tuple>();
		String line = null;

		while ((line = br.readLine()) != null) {
			line = line.trim();
			if (line.equals("")) {
				continue;
			}

			String tab[] = line.split("\\s+");
			int nReq = Integer.valueOf(tab[0]);
			double cpu = Double.valueOf(tab[1]);
			double mem = Double.valueOf(tab[2]);

			Tuple t = data.get(nReq);
			if (t == null) {
				t = new Tuple(nReq);
				data.put(nReq, t);
			}

			t.cpu += cpu;
			t.mem += mem;
			t.count++;
		}

		br.close();

		DecimalFormat df = new DecimalFormat("#.####");
		System.out.println("Req/Sec\t\tSamples\t\t%CPU\t\tMem (MB)");
		Tuple t = null;
		for (int key : data.keySet()) {
			t = data.get(key);
			System.out.println(t.nReq + "\t\t" + t.count + " \t\t" + df.format(t.getCPU())
					+ "\t\t" + df.format(t.getMEM()));
		}
	}

	private static class Tuple {
		int nReq, count;
		double cpu = 0, mem = 0;

		public Tuple(int nReq) {
			this.nReq = nReq;
		}

		public double getCPU() {
			return this.cpu / count;
		}

		public double getMEM() {
			return this.mem / count;
		}
	}
}
