package uk.ac.standrews.cs.stachord.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.NetworkUtil;

public abstract class ApplicationServer {

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

        setupServerSocket();
        startServer();
    }

    public void stop() throws IOException {

        stopServer();
        tearDownServerSocket();
    }

    // -------------------------------------------------------------------------------------------------------

    abstract Handler getHandler(String method_name);

    // -------------------------------------------------------------------------------------------------------

    private void setupServerSocket() throws IOException {

        server_socket = NetworkUtil.makeReusableServerSocket(local_address, port);
    }

    private void startServer() {

        server = new Server(server_socket);
        server.startServer();
    }

    private void tearDownServerSocket() throws IOException {

        if (server_socket != null) {
            server_socket.close();
            server_socket = null;
        }
    }

    private void stopServer() {

        server.stopServer();
        server = null;
    }

    private String dispatch(final String method_name, final String[] args) throws RemoteChordException {

        final Handler method_handler = getHandler(method_name);

        if (method_handler == null) {
            final String message = "unknown method name: " + method_name;
            Diagnostic.trace(DiagnosticLevel.RUN, message);
            return message;
        }

        return method_handler.execute(args);
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

        private void handleRequest(final Socket socket) throws IOException {

            thread_pool.execute(new Request(socket));
        }
    }

    class Request implements Runnable {

        private Socket socket;
        private StreamPair streams;

        // -------------------------------------------------------------------------------------------------------

        public Request(final Socket socket) throws IOException {

            this.socket = socket;

            setupStreams();
        }

        // -------------------------------------------------------------------------------------------------------

        @Override
        public void run() {

            try {
                try {
                    final String method_name = readMethodName();
                    final String[] args = readArgs();

                    try {
                        final String method_result = dispatch(method_name, args);
                        sendReply(method_result);
                    }
                    catch (final RemoteChordException e) {
                        sendException(e);
                    }
                }
                finally {

                    try {
                        tearDownStreams();
                    }
                    finally {
                        tearDownSocket();
                    }
                }
            }
            catch (final IOException e) {
                Diagnostic.trace(DiagnosticLevel.RUN, "error servicing request");
            }
        }

        // -------------------------------------------------------------------------------------------------------

        private void setupStreams() throws IOException {

            streams = new StreamPair(socket);
        }

        private void tearDownStreams() {

            streams.tearDownStreams();
        }

        private String readMethodName() throws IOException {

            return streams.readLine();
        }

        private String[] readArgs() throws IOException {

            final List<String> arg_list = new ArrayList<String>();

            String line = streams.readLine();

            // End of args indicated by empty line.
            while (line.length() > 0) {
                arg_list.add(line);
                line = streams.readLine();
            }

            return arg_list.toArray(new String[0]);
        }

        private void sendReply(final String method_result) {

            streams.println(method_result);
        }

        private void sendException(final RemoteChordException e) {

            streams.println("exception: " + e.getMessage());
        }

        private void tearDownSocket() throws IOException {

            if (socket != null) {
                socket.close();
                socket = null;
            }
        }
    }

    interface Handler {

        String execute(String[] args) throws RemoteChordException;
    }
}
