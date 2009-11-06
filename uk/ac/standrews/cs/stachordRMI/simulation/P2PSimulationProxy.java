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
/*
 * Created on Jan 19, 2005 at 4:22:40 PM.
 */
package uk.ac.standrews.cs.stachordRMI.simulation;

import uk.ac.standrews.cs.nds.p2p.interfaces.IP2PNode;
import uk.ac.standrews.cs.nds.p2p.simulation.interfaces.IP2PSimulation;
import uk.ac.standrews.cs.nds.util.ErrorHandling;

/**
 * @author al
 */
public class P2PSimulationProxy<T extends IP2PNode> {

    private IP2PSimulation<T> sim;

    public P2PSimulationProxy() {
        sim = null;
    }

    public IP2PSimulation<T> getSim() {
        if (sim == null)
			ErrorHandling.hardError("P2PSim chord_simulation is null.");
        return sim;
    }

    public void setSim(IP2PSimulation<T> sim) {
        if (sim == null)
			this.sim = sim;
    }
}
