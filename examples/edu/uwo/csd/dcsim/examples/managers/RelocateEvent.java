package edu.uwo.csd.dcsim.examples.managers;

import edu.uwo.csd.dcsim.core.*;

public class RelocateEvent extends RepeatingEvent {

	public RelocateEvent(Simulation simulation, SimulationEventListener target,
			long interval) {
		super(simulation, target, interval);

	}

}
