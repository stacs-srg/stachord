/*
 *  ASA Library
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
/*
 * Created on 09-May-2005
 */
package uk.ac.standrews.cs.stachordRMI.impl;

import java.util.Collection;
import java.util.HashMap;

import uk.ac.standrews.cs.nds.p2p.exceptions.ApplicationRegistryException;
import uk.ac.standrews.cs.nds.p2p.exceptions.P2PApplicationException;
import uk.ac.standrews.cs.nds.p2p.exceptions.P2PNodeException;
import uk.ac.standrews.cs.nds.p2p.impl.AID;
import uk.ac.standrews.cs.nds.p2p.impl.P2PStatus;
import uk.ac.standrews.cs.nds.p2p.interfaces.IApplicationComponentLocator;
import uk.ac.standrews.cs.nds.p2p.interfaces.IApplicationRegistry;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.interfaces.IP2PApplicationComponent;

/**
 * @author stuart
 */
public class ApplicationRegistryImpl implements IApplicationRegistry {

	private static final String EXCEPTION_MESSAGE_DUPLICATE = "An ApplicationComponent with a matching key has already been registered";

	private final HashMap<AID, IP2PApplicationComponent> registered_components;

	public ApplicationRegistryImpl(){
		registered_components = new HashMap<AID, IP2PApplicationComponent>();
	}

	public Object locateApplicationComponent(IKey key, AID application_id) throws P2PNodeException {

		// This performs a local application up-call.

		IP2PApplicationComponent application_component = registered_components.get(application_id);

		if (application_component != null) {

			IApplicationComponentLocator handler = application_component.getApplicationUpcallHandler();
			return handler.locateApplicationComponent(key, application_id);
		} else
			throw new P2PApplicationException(P2PStatus.APPLICATION_FAILURE, "no application component with AID " + application_id);
	}

	public void registerApplicationComponent(IP2PApplicationComponent component) throws ApplicationRegistryException {

		if(registered_components.containsKey(component.getApplicationID()))
			throw new ApplicationRegistryException(component, EXCEPTION_MESSAGE_DUPLICATE);
		else {
			registered_components.put(component.getApplicationID(), component);
		}
	}

	public void unregisterApplicationComponent(IP2PApplicationComponent component) {
		registered_components.remove(component.getApplicationID());
	}

	public Collection<IP2PApplicationComponent> getRegisteredComponents() {
		return registered_components.values();
	}
}
