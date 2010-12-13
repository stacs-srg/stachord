package uk.ac.standrews.cs.stachord.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;

public class StreamPair {

    private BufferedReader input_stream;
    private PrintStream output_stream;

    public StreamPair(final Socket socket) throws IOException {

        setupStreams(socket);
    }

    private void setupStreams(final Socket socket) throws IOException {

        input_stream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        output_stream = new PrintStream(socket.getOutputStream(), true);
    }

    public void tearDownStreams() {

        try {
            input_stream.close();
        }
        catch (final IOException e) {
            Diagnostic.trace(DiagnosticLevel.RUN, "error closing input stream");
        }
        finally {
            output_stream.close();
        }
    }

    public BufferedReader getInputStream() {

        return input_stream;
    }

    public PrintStream getOutputStream() {

        return output_stream;
    }

    public String readLine() throws IOException {

        return input_stream.readLine();
    }

    public void println() {

        output_stream.println();
    }

    public void println(final String s) {

        output_stream.println(s);
    }
}
