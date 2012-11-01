/**
 * JBoss, Home of Professional Open Source. Copyright 2011, Red Hat, Inc., and
 * individual
 * contributors as indicated by the
 * 
 * @author tags. See the copyright.txt file in the distribution for a full
 *         listing of individual
 *         contributors.
 * 
 *         This is free software; you can redistribute it and/or modify it under
 *         the terms of the GNU Lesser
 *         General Public License as published by the Free Software Foundation;
 *         either version 2.1 of the
 *         License, or (at your option) any later version.
 * 
 *         This software is distributed in the hope that it will be useful, but
 *         WITHOUT ANY WARRANTY;
 *         without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *         PARTICULAR PURPOSE. See the
 *         GNU Lesser General Public License for more details.
 * 
 *         You should have received a copy of the GNU Lesser General Public
 *         License along with this
 *         software; if not, write to the Free Software Foundation, Inc., 51
 *         Franklin St, Fifth Floor,
 *         Boston, MA 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.nio2.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@code JioClient}
 * 
 * Created on Nov 11, 2011 at 3:38:26 PM
 * 
 * @author <a href="mailto:nbenothm@redhat.com">Nabil Benothman</a>
 */
public class JioClient extends Thread {

	protected static final AtomicInteger connections = new AtomicInteger(0);
	/**
     *
     */
	public static final int READ_BUFFER_SIZE = 16 * 1024;
	/**
     *
     */
	public static final String CRLF = "\r\n";

	/**
	 * 
	 */
	public static final int DEFAULT_NREQ = 1000000;

	/**
     *
     */
	public static final int MAX = 1000;

	/**
	 * Default wait delay 1000ms
	 */
	public static final int DEFAULT_DELAY = 1000;

	/**
	 * 
	 */
	protected static int NB_CLIENTS = 100;

	//private long max_time = Long.MIN_VALUE;
	//private long min_time = Long.MAX_VALUE;
	//private double avg_time = 0;
	private int max;
	private int delay;
	private Socket channel;
	protected URL url;
	private BufferedReader reader;
	private OutputStream os;
	private byte[] requestBytes;
	private List<Long> times = new ArrayList<Long>();

	/**
	 * Create a new instance of {@code JioClient}
	 * 
	 * @param d_max
	 * @param delay
	 */
	public JioClient(int d_max, int delay) {
		this.max = d_max;
		this.delay = delay;
	}

	/**
	 * Create a new instance of {@code JioClient}
	 * 
	 * @param url
	 * @param d_max
	 * @param delay
	 */
	public JioClient(URL url, int d_max, int delay) {
		this(d_max, delay);
		this.url = url;
	}

	/**
	 * Create a new instance of {@code JioClient}
	 * 
	 * @param url
	 * @param delay
	 */
	public JioClient(URL url, int delay) {
		this(delay);
		this.url = url;
	}

	/**
	 * Create a new instance of {@code JioClient}
	 * 
	 * @param delay
	 */
	public JioClient(int delay) {
		this(60 * 1000 / delay, delay);
	}

	/**
	 * 
	 * @throws Exception
	 */
	public void setup() throws Exception {
		this.connect();
		connections.incrementAndGet();

		this.requestBytes = ("GET " + this.url.getPath() + " HTTP/1.1\n" + "Host: "
				+ this.url.getHost() + "\n" + "User-Agent: " + getClass().getName() + "\n"
				+ "Connection: keep-alive\n" + CRLF).getBytes();
	}

	@Override
	public void run() {
		try {
			// Setting up connection with server
			this.setup();
			while (connections.get() < NB_CLIENTS) {
				// wait until all clients connects
				sleep(100);
			}
			// wait for 2 seconds until all threads are ready
			sleep(DEFAULT_DELAY);
			runit();
		} catch (Throwable exp) {
			System.err.println("Exception: " + exp.getMessage());
			exp.printStackTrace();
		} finally {
			try {
				this.channel.close();
			} catch (Throwable th) {
				// NOPE
			}
		}
	}

	/**
	 * 
	 * @throws Exception
	 */
	protected void connect() throws Exception {
		// Open connection with server
		Thread.sleep(new Random().nextInt(5 * NB_CLIENTS));
		System.out.println("Connecting to server on " + this.url.getHost() + ":"
				+ this.url.getPort());
		setInOut(new Socket(this.url.getHost(), this.url.getPort()));
	}

	/**
	 * 
	 * @throws Exception
	 */
	protected void setInOut(Socket socket) throws Exception {
		this.channel = socket;
		this.channel.setSoTimeout(100000);
		this.os = this.channel.getOutputStream();
		this.reader = new BufferedReader(new InputStreamReader(this.channel.getInputStream()));
		System.out.println("Connection to server established ...");
	}

	/**
	 * 
	 * @throws Exception
	 */
	public void runit() throws Exception {
		Random random = new Random();
		// Wait a delay to ensure that all threads are ready
		sleep(4 * DEFAULT_DELAY + random.nextInt(NB_CLIENTS));
		long time = 0;
		String response = null;
		// int counter = 0, min_count = 10 * 1000 / delay;
		// int max_count = 50 * 1000 / delay;

		while ((this.max--) > 0) {
			Thread.sleep(this.delay);
			try {
				time = System.currentTimeMillis();
				sendRequest();
				response = readResponse();
				time = System.currentTimeMillis() - time;
				times.add(time);
			} catch (IOException exp) {
				System.out.println("[" + getId() + "] Exception:" + exp.getMessage());
				exp.printStackTrace();
				break;
			}

			/*
			 * if (counter >= min_count && counter <= max_count) {
			 * // update the average response time
			 * avg_time += time;
			 * // update the maximum response time
			 * if (time > max_time) {
			 * max_time = time;
			 * }
			 * // update the minimum response time
			 * if (time < min_time) {
			 * min_time = time;
			 * }
			 * }
			 * counter++;
			 */
		}
		// avg_time /= (max_count - min_count + 1);
		// For each thread print out the maximum, minimum and average response
		// times
		// System.out.println(max_time + " \t " + min_time + " \t " + avg_time);
		for (long t : times) {
			System.out.println(t);
		}
	}

	/**
	 * Send request to the server
	 * 
	 * @throws Exception
	 */
	private void sendRequest() throws IOException {
		this.os.write(this.requestBytes);
		this.os.flush();
	}

	/**
	 * Read the response from the server
	 * 
	 * @return data received from server
	 * @throws IOException
	 */
	public String readResponse() throws IOException {
		long contentLength = 0;
		String line;
		while ((line = this.reader.readLine()) != null && !line.trim().equals("")) {
			// System.out.println(line);
			String tab[] = line.split("\\s*:\\s*");
			if (tab[0].equalsIgnoreCase("Content-length")) {
				contentLength = Long.parseLong(tab[1]);
			}
		}

		long read = 0;

		while (read < contentLength && (line = this.reader.readLine()) != null) {
			read += line.length() + 1;
		}

		return "Hello world!";
	}

	/**
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		if (args.length < 1) {
			System.err.println("Usage: java " + JioClient.class.getName()
					+ " URL [n] [delay] [nReq] [nClients]");
			System.err.println("\tURL: The url of the service to test.");
			System.err.println("\tn: The number of clients. (default is " + NB_CLIENTS + ")");
			System.err.println("\tdelay: The delay between writes. (default is " + DEFAULT_DELAY
					+ "ms)");
			System.err.println("\tnReq: The total number of clients. (default is " + DEFAULT_NREQ
					+ ")");
			System.err
					.println("\tnClients: The total number of clients. (default is the number of client 'n')");
			System.exit(1);
		}

		URL strURL = new URL(args[0]);
		int delay = DEFAULT_DELAY;
		int nReq = DEFAULT_NREQ;
		int nClients = NB_CLIENTS;

		if (args.length > 1) {
			try {
				NB_CLIENTS = Integer.parseInt(args[1]);
				if (args.length > 2) {
					delay = Integer.parseInt(args[2]);
					if (delay < 1) {
						throw new IllegalArgumentException("Negative number: delay");
					}
				}

				if (args.length > 3) {
					nReq = Integer.parseInt(args[3]);
					if (nReq < NB_CLIENTS) {
						System.err
								.println("ERROR: the total number of requests canno't be less than the number of clients");
						System.err
								.println("  --> adjusting the total number of requests to the number of clients");
						nReq = NB_CLIENTS;
					}
				}

				if (args.length > 4) {
					nClients = Integer.parseInt(args[4]);
					if (nClients < NB_CLIENTS) {
						System.err
								.println("The total number of clients canno't less than the number of clients");
						System.err
								.println("  --> adjusting the total number of clients to the number of clients");
						nClients = NB_CLIENTS;
					}
				}

			} catch (Exception exp) {
				System.err.println("Error: " + exp.getMessage());
				System.exit(1);
			}
		}

		System.out.println("\nRunning test with parameters:");
		System.out.println("\tURL: " + strURL);
		System.out.println("\tn: " + NB_CLIENTS);
		System.out.println("\tdelay: " + delay);
		System.out.println("\tnReq: " + nReq);
		JioClient clients[] = new JioClient[NB_CLIENTS];

		int d_max = nReq / nClients;

		for (int i = 0; i < clients.length; i++) {
			clients[i] = new JioClient(strURL, d_max, delay);
		}

		for (int i = 0; i < clients.length; i++) {
			clients[i].start();
		}

		for (int i = 0; i < clients.length; i++) {
			clients[i].join();
		}
	}
}
