package edu.uwo.csd.dcsim.examples;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.math3.distribution.*;
import org.apache.log4j.Logger;

import edu.uwo.csd.dcsim.*;
import edu.uwo.csd.dcsim.application.*;
import edu.uwo.csd.dcsim.application.workload.*;
import edu.uwo.csd.dcsim.common.SimTime;
import edu.uwo.csd.dcsim.common.Tuple;
import edu.uwo.csd.dcsim.core.Simulation;
import edu.uwo.csd.dcsim.core.metrics.Metric;
import edu.uwo.csd.dcsim.examples.management.ConsolidationPolicy;
import edu.uwo.csd.dcsim.examples.management.RelocationPolicy;
import edu.uwo.csd.dcsim.host.*;
import edu.uwo.csd.dcsim.host.resourcemanager.*;
import edu.uwo.csd.dcsim.host.scheduler.DefaultResourceSchedulerFactory;
import edu.uwo.csd.dcsim.management.*;
import edu.uwo.csd.dcsim.management.capabilities.HostManager;
import edu.uwo.csd.dcsim.management.capabilities.HostPoolManager;
import edu.uwo.csd.dcsim.management.policies.HostMonitoringPolicy;
import edu.uwo.csd.dcsim.management.policies.HostOperationsPolicy;
import edu.uwo.csd.dcsim.management.policies.HostStatusPolicy;
import edu.uwo.csd.dcsim.management.policies.VmPlacementPolicy;

public class DynamicServiceSpawning extends SimulationTask {

	private static Logger logger = Logger.getLogger(DynamicServiceSpawning.class);
	
	public static void main(String args[]) {
		Simulation.initializeLogging();
		
		SimulationTask task = new DynamicServiceSpawning("dynamic-service-spawn", -5217230306070299805l);
		
		task.run();
		
		//get the results of the simulation
		Collection<Metric> metrics = task.getResults();
		
		//output metric values
		for (Metric metric : metrics) {
			logger.info(metric.getName() + "=" + metric.toString()); //metric.getValue() returns the raw value, while toString() provides formatting
		}
		
	}
	
	public DynamicServiceSpawning(String name, long randomSeed) {
		super(name, SimTime.days(1));
		this.setRandomSeed(randomSeed);
	}

	@Override
	public void setup(Simulation simulation) {
		
		//create DC
		DataCentre dc = new DataCentre(simulation);
		simulation.addDatacentre(dc);
		
		HostPoolManager hostPool = new HostPoolManager();
		AutonomicManager dcAM = new AutonomicManager(simulation, hostPool);
		dcAM.installPolicy(new HostStatusPolicy(5));
		dcAM.installPolicy(new VmPlacementPolicy(0.5, 0.9, 0.85));
		
		//create Hosts
		Host.Builder proLiantDL160G5E5420 = HostModels.ProLiantDL160G5E5420(simulation).privCpu(500).privBandwidth(131072)
				.resourceManagerFactory(new DefaultResourceManagerFactory())
				.resourceSchedulerFactory(new DefaultResourceSchedulerFactory());
		
		//add 10 hosts
		for (int i = 0; i < 10; ++i) {
			Host host = proLiantDL160G5E5420.build();  
			
			AutonomicManager hostAM = new AutonomicManager(simulation, new HostManager(host));
			hostAM.installPolicy(new HostMonitoringPolicy(dcAM), SimTime.minutes(5), 0);
			hostAM.installPolicy(new HostOperationsPolicy());
			
			host.installAutonomicManager(hostAM);
			
			dc.addHost(host);
			hostPool.addHost(host, hostAM);
		}

		/*
		 * We now create a ServiceProducer, which generates incoming Services a specified rate.
		 * 
		 * Create a 'trace' for the ServiceProducer to follow that specifies (time, rate) tuples, where rate is the average number of
		 * new services to spawn per hour. If the first entry is not at time 0, then a rate of '0' will be used until the first
		 * time. Time is relative to the simulation time at which ServiceProducer.start() is called (usually 0). The 'trace' is looped
		 * once immediately upon reaching the end, thus if the first rate was at time '0', the last rate will never really be used. If you want
		 * to maintain the last rate for some period of time, simply add another entry with the same rate at a later time, as has been done below.
		 * In this example, a rate of '5' is set at 28800000, set again (with no effect) at time 43200000, and then 1000ms later (time 43201000)
		 * the rate of '10' is set.
		 */
		ArrayList<Tuple<Long, Double>> serviceRates = new ArrayList<Tuple<Long, Double>>();
		serviceRates.add(new Tuple<Long, Double>(1000l, 10d));
		serviceRates.add(new Tuple<Long, Double>(14400000l, 30d));
		serviceRates.add(new Tuple<Long, Double>(28800000l, 5d));
		serviceRates.add(new Tuple<Long, Double>(43200000l, 5d));
		
		/*
		 * Create the ServiceProducer by extending the abstract class and specifying the buildService() method. This method is responsible
		 * for creating a new service each time the ServiceProducer wishes to spawn a new service. The services created should be entirly 
		 * independent (i.e. should not use the same workload). The ServiceProducer constructor takes the DataCenterSimulation (not the
		 * Simulation superclass), the datacentre to submit Services to, a distribution describing the lifespan of services, and either
		 * a static rate to create services given in services-per-hour, or a list of (time, rate) tuples.
		 */
		ServiceProducer serviceProducer = new ServiceProducer(simulation, dcAM, new NormalDistribution(14400000, 900000), serviceRates) {

			@Override
			public Service buildService() {
				Workload workload = new TraceWorkload(simulation, "traces/clarknet", 2200, 0);
				simulation.addWorkload(workload);
				
				return Services.singleTierInteractiveService(workload, 1, 2500, 1024, 12800, 1024, 1, 300, 1, Integer.MAX_VALUE);
			}
			
		};
		
		//start the ServiceProducer
		serviceProducer.start();
		
		//Add a dynamic management policies to perform relocation and consolidation
		dcAM.installPolicy(new RelocationPolicy(0.5, 0.8, 0.7), SimTime.hours(1), SimTime.hours(1) + 1);
		dcAM.installPolicy(new ConsolidationPolicy(0.5, 0.8, 0.7), SimTime.hours(2), SimTime.hours(2) + 2);
		
	}

}
