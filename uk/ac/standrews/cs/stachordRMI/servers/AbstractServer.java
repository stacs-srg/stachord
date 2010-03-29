package uk.ac.standrews.cs.stachordRMI.servers;

import java.text.SimpleDateFormat;

import uk.ac.standrews.cs.nds.p2p.impl.Key;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.util.CommandLineArgs;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.nds.util.NetworkUtil;

/**
 * Common setup for StartRing and StartNode.
 * 
 * @author graham
 */
public abstract class AbstractServer  {
	
	private static final DiagnosticLevel DEFAULT_DIAGNOSTIC_LEVEL = DiagnosticLevel.NONE;
	
	static String local_address = "";
	static int local_port = 0;
	static IKey server_key = null;

	public static void setup( String[] args ) {
		
		//this may be overridden by a CLA
		Diagnostic.setLevel(DEFAULT_DIAGNOSTIC_LEVEL);

		Diagnostic.setTimestampFlag(true);
		Diagnostic.setTimestampFormat(new SimpleDateFormat("HH:mm:ss:SSS "));
		Diagnostic.setTimestampDelimiterFlag(false);
		ErrorHandling.setTimestampFlag(false);
		
		String server_address_parameter = CommandLineArgs.getArg(args, "-s"); // This node's address.
		if (server_address_parameter == null) usage();

		local_address = NetworkUtil.extractHostName(server_address_parameter);
		local_port =    NetworkUtil.extractPortNumber(server_address_parameter);

		String server_key_parameter = CommandLineArgs.getArg(args, "-x"); // This node's key.
		if (server_key_parameter != null && !server_key_parameter.equals("null")) server_key = new Key(server_key_parameter);
	}

	private static void usage() {
			
		ErrorHandling.hardError( "Usage: -s[host][:port]" );
	}
}
