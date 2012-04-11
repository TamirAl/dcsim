package edu.uwo.csd.dcsim2.application.workload;

import edu.uwo.csd.dcsim2.core.Simulation;
import edu.uwo.csd.dcsim2.core.Utility;

public class RandomWorkload extends Workload {

	long stepSize;
	double scaleFactor;
	int workLevel = 0;
	
	public RandomWorkload(double scaleFactor, long stepSize) {
		super();	
		
		this.stepSize = stepSize;
		this.scaleFactor = scaleFactor;
		
		workLevel = generateRandomWorkLevel();
	}
	
	protected int generateRandomWorkLevel() {
		return (int)Math.round(Utility.getRandom().nextDouble() * scaleFactor);
	}
	
	@Override
	protected double retrievePendingWork() {
		return workLevel * Simulation.getInstance().getElapsedSeconds();
	}

	@Override
	protected long updateWorkLevel() {
		workLevel = generateRandomWorkLevel();
		return Simulation.getInstance().getSimulationTime() + stepSize;
	}
		
	
}
