package uk.ac.standrews.cs.stachord.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.NetworkUtil;

public class ApplicationServer {

    private InetAddress local_address;
    private int port;
    private ServerSocket server_socket;
    private Server server;

    public void setLocalAddress(final InetAddress local_address) {

        this.local_address = local_address;
    }

    public void setPort(final int port) {

        this.port = port;
    }

    public void start() throws IOException {

        setupSocket();
        startServer();
    }

    private void setupSocket() throws IOException {

        server_socket = NetworkUtil.makeReusableServerSocket(local_address, port);
    }

    private void startServer() {

        server = new Server(server_socket);
        server.startServer();
    }

    public void stop() throws IOException {

        stopServer();
        tearDownSocket();
    }

    private void tearDownSocket() throws IOException {

        if (server_socket != null) {
            server_socket.close();
            server_socket = null;
        }
    }

    private void stopServer() {

        server.stopServer();
        server = null;
    }

    class Server extends Thread {

        private static final int MAX_THREADS = 5;
        private final ServerSocket server_socket;
        private boolean running = true;
        private final ExecutorService thread_pool = Executors.newFixedThreadPool(MAX_THREADS);

        public Server(final ServerSocket server_socket) {

            this.server_socket = server_socket;
        }

        public void startServer() {

            start();
        }

        public void stopServer() {

            running = false;
        }

        @Override
        public void run() {

            while (running) {
                try {
                    final Socket socket = server_socket.accept();
                    handleRequest(socket);
                }
                catch (final IOException e) {
                    Diagnostic.trace(DiagnosticLevel.RUN, "error accepting connection");
                }
            }
        }

        private void handleRequest(final Socket socket) {

            thread_pool.execute(new Request(socket));
        }
    }

    class Request implements Runnable {

        public Request(final Socket socket) {

            // TODO Auto-generated constructor stub
        }

        @Override
        public void run() {

            // TODO Auto-generated method stub
        }
    }
}
