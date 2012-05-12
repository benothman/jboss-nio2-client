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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@code JioClient}
 *
 * Created on Nov 11, 2011 at 3:38:26 PM
 *
 * @author <a href="mailto:nbenothm@redhat.com">Nabil Benothman</a>
 */
public class TestClient extends Thread {

    private static final AtomicInteger connections = new AtomicInteger(0);
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
    public static final int MAX = 1000;
    /**
     * Default wait delay 1000ms
     */
    public static final int DEFAULT_DELAY = 1000;
    private static int NB_CLIENTS = 100;
    private long max_time = Long.MIN_VALUE;
    private long min_time = Long.MAX_VALUE;
    private double avg_time = 0;
    private int max;
    private int delay;
    private Socket channel;
    private URL url;
    private BufferedReader reader;
    private OutputStream os;
    private String jsessionid = null;

    /**
     * Create a new instance of {@code JioClient}
     *
     * @param url
     * @param d_max
     * @param delay
     */
    public TestClient(URL url, int d_max, int delay) {
        this.max = d_max;
        this.delay = delay;
        this.url = url;
    }

    @Override
    public void run() {
        try {
            // Connect to the server
            this.connect();
            while (connections.get() < NB_CLIENTS) {
                // wait until all clients connects
                sleep(100);
            }
            // wait for 2 seconds until all threads are ready
            sleep(DEFAULT_DELAY);
            runit();
        } catch (Exception exp) {
            System.err.println("Exception: " + exp.getMessage());
            exp.printStackTrace();
        } finally {
            try {
                this.channel.close();
            } catch (IOException ioex) {
                System.err.println("Exception: " + ioex.getMessage());
                ioex.printStackTrace();
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
        System.out.println("Connecting to server on " + this.url.getHost() + ":" + this.url.getPort());
        this.channel = new Socket(this.url.getHost(), this.url.getPort());
        this.channel.setSoTimeout(10000);
        this.os = this.channel.getOutputStream();
        this.reader = new BufferedReader(new InputStreamReader(this.channel.getInputStream()));
        System.out.println("Connection to server established ...");
        connections.incrementAndGet();
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
        int counter = 0;
        int min_count = 10 * 1000 / delay;
        int max_count = 50 * 1000 / delay;
        if (min_count > this.max) {
            min_count = 0;
            max_count = max;
        }
        while ((this.max--) > 0) {
            Thread.sleep(this.delay);
            try {
                time = System.currentTimeMillis();
                sendRequest();
                response = readResponse();
                time = System.currentTimeMillis() - time;
                // System.out.println("Response: " + response + " in " + time);
                // This is one of the mod_cluster perf tests.
                if (Integer.parseInt(response) != counter) {
                    System.out.println("[" + getId() + "] Error: " + Integer.parseInt(response) + " != " + counter);
                    System.out.flush();
                    break;
                }
            } catch (IOException exp) {
                System.out.println("[" + getId() + "] Exception:" + exp.getMessage());
                System.out.flush();
                break;
            }

            if (counter >= min_count && counter <= max_count) {
                // update the average response time
                avg_time += time;
                // update the maximum response time
                if (time > max_time) {
                    max_time = time;
                }
                // update the minimum response time
                if (time < min_time) {
                    min_time = time;
                }
            }
            counter++;
        }
        avg_time /= (max_count - min_count + 1);
        // For each thread print out the maximum, minimum and average response
        // times
        System.out.println(max_time + " \t " + min_time + " \t " + avg_time);
    }

    /**
     *
     * @param os
     * @throws Exception
     */
    private void sendRequest() throws IOException {
        if (jsessionid != null) {
            // System.out.println("jsessionid: *" + jsessionid + "*");
            this.os.write(("GET " + this.url.getPath() + ";jsessionid=" + jsessionid + " HTTP/1.1\n").getBytes());
        } else {
            this.os.write(("GET " + this.url.getPath() + " HTTP/1.1\n").getBytes());
        }
        this.os.write(("User-Agent: " + JioClient.class.getName() + "\n").getBytes());
        this.os.write(("Host: " + this.url.getHost() + "\n").getBytes());
        this.os.write("Connection: keep-alive\n".getBytes());
        this.os.write(CRLF.getBytes());
        this.os.flush();
    }

    /**
     *
     * @return data received from server
     * @throws IOException
     */
    public String readResponse() throws IOException {
        long contentLength = 0;
        String line;
        line = this.reader.readLine();
        if (line == null) {
            throw new IOException("readLine failed nothing");
        }

        // skip CRLF
        while (line != null && line.trim().equals("")) {
            line = this.reader.readLine();
        }

        while (line != null && !line.trim().equals("")) {
            // System.out.println("header: " + line);
            int i = line.indexOf(": ");
            if (i == -1) {
                line = this.reader.readLine();
                continue; // HTTP/1.1 :D
            }
            String header = line.substring(0, i);
            String value = line.substring(i + 2);
            // System.out.println("header: " + header + " : " + value);
            if (header.equalsIgnoreCase("Content-length")) {
                contentLength = Long.parseLong(value);
            }
            if (header.equalsIgnoreCase("Set-Cookie")) {
                int j = value.indexOf("=");
                i = value.indexOf(";");
                if (i == -1) {
                    jsessionid = value.substring(j + 1);
                } else {
                    jsessionid = value.substring(j + 1, i);
                }
                // System.out.println("JSESSIONID: " + jsessionid);
            }
            line = this.reader.readLine();
        }
        if (line == null) {
            throw new IOException("readLine failed");
        }
        if (contentLength == 0) {
            throw new IOException("contentLength==0");
        }

        // System.out.println("reading " + contentLength + " chars");
        long read = 0;
        char[] buff = new char[(int) contentLength];

        while (read < contentLength) {
            int i = this.reader.read(buff);
            read += i;
            // System.out.println("READ: " + read + " : " + contentLength);
        }
        // System.out.println("\n\n**************************************************\n\n");
        return new String(buff);
    }

    /**
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        if (args.length < 1) {
            System.err.println("Usage: java " + TestClient.class.getName() + " URL [n] [delay] [nreqs]");
            System.err.println("\tURL: The url of the service to test.");
            System.err.println("\tn: The number of clients. (default is " + NB_CLIENTS + ")");
            System.err.println("\tdelay: The delay between writes. (default is " + DEFAULT_DELAY + "ms)");
            System.err.println("\tnreqs: Number of request/response to run");
            System.exit(1);
        }

        URL strURL = new URL(args[0]);
        int delay = DEFAULT_DELAY;
        int max = 0;

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
                    max = Integer.parseInt(args[3]);
                    if (max < 1) {
                        throw new IllegalArgumentException("Negative number: max");
                    }
                } else {
                    max = 60 * 1000 / delay;
                }
            } catch (Exception exp) {
                System.err.println("Error: " + exp.getMessage());
                System.exit(1);
            }
        }

        System.out.println("\nRunning test with parameters:");
        System.out.println("\tURL: " + strURL);
        System.out.println("\tn: " + NB_CLIENTS);
        System.out.println("\tdelay: " + delay + " ms");
        System.out.println("\tmax request: " + max);

        JioClient clients[] = new JioClient[NB_CLIENTS];

        for (int i = 0; i < clients.length; i++) {
            clients[i] = new JioClient(strURL, max, delay);
        }

        for (int i = 0; i < clients.length; i++) {
            clients[i].start();
        }

        for (int i = 0; i < clients.length; i++) {
            clients[i].join();
        }
    }
}