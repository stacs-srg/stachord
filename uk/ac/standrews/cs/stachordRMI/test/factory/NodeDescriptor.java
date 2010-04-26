package uk.ac.standrews.cs.stachordRMI.test.factory;

import uk.ac.standrews.cs.nds.util.ClassPath;
import uk.ac.standrews.cs.nds.util.SSH2ConnectionWrapper;

public class NodeDescriptor {

	public final SSH2ConnectionWrapper ssh_client_wrapper;
	public final String java_version;
	public final ClassPath class_path;
	
	public NodeDescriptor(SSH2ConnectionWrapper ssh_client_wrapper, String java_version, ClassPath class_path) {

		this.ssh_client_wrapper = ssh_client_wrapper;
		this.java_version = java_version;
		this.class_path = class_path;
	}
}
