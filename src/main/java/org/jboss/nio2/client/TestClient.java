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

import java.io.*;
import java.net.Socket;
import java.net.URL;

/**
 * {@code TestClient}
 * <p/>
 *
 * Created on Feb 22, 2012 at 4:19:38 PM
 *
 * @author <a href="mailto:nbenothm@redhat.com">Nabil Benothman</a>
 */
public class TestClient extends Thread {

    public static final String STR_URL = "http://neo6.gva.redhat.com:8080/index.html";
    public static final int MAX = 1000;
    public static final int N_THREADS = 100;
    public static final int DEFAULT_DELAY = 1000; // default wait delay 1000ms
    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_HOST = "localhost";
    private static final String CRLF = "\r\n";
    private static final String LF = "\n";
    private String hostname;
    private int port;
    private Socket socket;
    private boolean running = true;
    private long max_time = Long.MIN_VALUE;
    private long min_time = Long.MAX_VALUE;
    private double avg_time = 0;
    private URL url;
    private int max;
    private int delay;

    /**
     * Create a new instance of {@code TestClient}
     */
    public TestClient() {
        super();
    }

    /**
     * Create a new instance of {@code TestClient}
     *
     * @param url the service URL
     * @param d_max the maximum number of requests
     * @param delay
     * @throws Exception if the URL is MalFormed
     */
    public TestClient(URL url, int d_max, int delay) throws Exception {
        this.url = url;
        this.max = 55 * 1000 / delay;
        /*
         * Random generator = new Random(); while (this.max < MIN_REQ_NUM) { this.max =
         * generator.nextInt(d_max); }
         */
        this.delay = delay;
    }

    /**
     * Create a new instance of {@code TestClient}
     *
     * @param max the maximum number of requests
     * @throws Exception
     */
    public TestClient(int max) throws Exception {
        this(new URL(STR_URL), max, DEFAULT_DELAY);
    }

    @Override
    public void run() {
        System.out.println("[" + getId() + "] - HELLO WORLD");
    }

    /**
     *
     * @throws IOException
     */
    public void close() throws IOException {
        this.socket.close();
    }

    /**
     *
     * @throws Exception
     */
    public void runit() throws Exception {
        OutputStream os = this.socket.getOutputStream();
        InputStream is = this.socket.getInputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(is));

        while ((max--) > 0) {
            long time = System.currentTimeMillis();
            sendRequest(os, url);
            readResponse();
            time = System.currentTimeMillis() - time;
            sleep(DEFAULT_DELAY);
        }

        close();
    }

    /**
     *
     * @param os
     * @throws Exception
     */
    private static void sendRequest(OutputStream os, URL url) throws Exception {
        os.write(("GET " + url.getPath() + " HTTP/1.1\n").getBytes());
        os.write(("User-Agent: " + TestClient.class.getName() + " (chunked-test)\n").getBytes());
        os.write(("Host: " + url.getHost() + "\n").getBytes());
        os.write("Connection: keep-alive\n".getBytes());
        os.write("\n".getBytes());
        os.flush();
    }

    /**
     * 
     * @throws Exception 
     */
    private static void readResponse() throws Exception {
        // TODO
    }
    
    /**
     *
     * @param args
     * @throws Exception
     */
    public static void main(String args[]) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java " + TestClient.class.getName() + " URL [n] [max] [delay]");
            System.err.println("\tURL: The url of the service to test.");
            System.err.println("\tn: The number of threads. (default is " + N_THREADS + ")");
            System.err.println("\tdelay: The delay between writes. (default is " + DEFAULT_DELAY + "ms)");
            System.exit(1);
        }

        URL strURL = new URL(args[0]);
        int n = 100, max = 1000, delay = DEFAULT_DELAY;

        if (args.length > 1) {
            try {
                n = Integer.parseInt(args[1]);
                if (args.length > 2) {
                    max = Integer.parseInt(args[2]);
                    if (max < 1) {
                        throw new IllegalArgumentException("Negative number: max");
                    }
                }
                if (args.length > 3) {
                    delay = Integer.parseInt(args[3]);
                    if (delay < 1) {
                        throw new IllegalArgumentException("Negative number: delay");
                    }
                }
            } catch (Exception exp) {
                System.err.println("Error: " + exp.getMessage());
                System.exit(1);
            }
        }

        System.out.println("\nRunning test with parameters:");
        System.out.println("\tURL: " + strURL);
        System.out.println("\tn: " + n);
        System.out.println("\tmax: " + max);
        System.out.println("\tdelay: " + delay);

        TestClient clients[] = new TestClient[n];
        for (int i = 0; i < clients.length; i++) {
            clients[i] = new TestClient(strURL, max, delay);
        }
        for (int i = 0; i < clients.length; i++) {
            clients[i].start();
        }
        for (int i = 0; i < clients.length; i++) {
            clients[i].join();
        }

    }
}
