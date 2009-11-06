/*
 *  StAChord Library
 *  Copyright (C) 2004-2008 Distributed Systems Architecture Research Group
 *  http://asa.cs.st-andrews.ac.uk/
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.standrews.cs.stachordRMI.testharness.impl;

import java.util.ArrayList;
import java.util.List;

import uk.ac.standrews.cs.nds.p2p.exceptions.P2PNodeException;
import uk.ac.standrews.cs.nds.p2p.impl.P2PStatus;
import uk.ac.standrews.cs.nds.p2p.util.SHA1KeyFactory;
import uk.ac.standrews.cs.stachordRMI.testharness.interfaces.IChordNetwork;
import uk.ac.standrews.cs.stachordRMI.testharness.interfaces.IChordNetworkNodeHandle;
import uk.ac.standrews.cs.stachordRMI.testharness.logic.Util;

public abstract class AbstractChordNetwork<HandleType> implements IChordNetwork<HandleType> {

	private static final int DEFAULT_FIRST_PORT = 1111;
	private static int port = DEFAULT_FIRST_PORT;
	protected List<IChordNetworkNodeHandle<HandleType>> nodes = null;
	protected SHA1KeyFactory keyFac = new SHA1KeyFactory();
	protected boolean killed;

	private AbstractChordNetwork() throws Exception{
		init();
		nodes=new ArrayList<IChordNetworkNodeHandle<HandleType>>();
	}

	public AbstractChordNetwork(int n) throws Exception {
		this();
		if (n<0) {
			throw new IllegalArgumentException();
		}
		for(int i=0;i<n;i++) {
			addNode(null);
		}
	}

	public AbstractChordNetwork(String[] keyStrings) throws Exception {
		this();
		if (keyStrings==null || keyStrings.length==0) {
			throw new IllegalArgumentException();
		}
		for(String k:keyStrings) {
			addNode(k);
		}
	}

	public static int serverPort() {
		return port++;
	}

	protected abstract void init();

	public void addNode(String k) throws Exception {
		if(!killed){
			if(nodes.size()!=0){
				IChordNetworkNodeHandle<HandleType> known = nodes.get(0);
				makeNode(k,known);
			} else {
				makeNode(k);
			}
		} else {
			throw new P2PNodeException(P2PStatus.GENERAL_NODE_DEPLOYMENT_FAILURE, "The network has been killed. A node cannot be added.");
		}
	}

	public void addNode() throws Exception {
		addNode(null);
	}

	protected abstract void makeNode(String k, IChordNetworkNodeHandle<HandleType> known) throws Exception;

	protected void makeNode(String k) throws Exception {
		makeNode(k,null);
	}

	public List<IChordNetworkNodeHandle<HandleType>> getNodes() {
		List<IChordNetworkNodeHandle<HandleType>> nodelistcopy = new ArrayList<IChordNetworkNodeHandle<HandleType>>();
		for(IChordNetworkNodeHandle<HandleType> n : nodes) {
			nodelistcopy.add(n);
		}
		return nodelistcopy;
	}

	public void killNetwork() {
		if(!killed){
			killed=true;
			if(nodes!=null){
				IChordNetworkNodeHandle<?>[] nodeArray = nodes.toArray(new IChordNetworkNodeHandle[]{});
				for(IChordNetworkNodeHandle<?> n : nodeArray) {
					n.killNode();
				}
			}
			nodes=null;
		}
	}

	public void killNode(IChordNetworkNodeHandle<HandleType> node) {//throws Exception {
		nodes.remove(node);
	}

	public void waitForStableNetwork() {
		while(!isNetworkStable()){}
	}

	public boolean isNetworkStable() {
		return Util.isChordRingStable(Util.networkNodeHandles2IChordRemote(this));
	}

	public static void resetPortNumber(){
		port=DEFAULT_FIRST_PORT;
	}
}
