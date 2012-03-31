package edu.uwo.csd.dcsim2.management;

import java.util.ArrayList;
import java.util.Collections;

import org.apache.log4j.Logger;

import edu.uwo.csd.dcsim2.core.Event;
import edu.uwo.csd.dcsim2.core.Simulation;
import edu.uwo.csd.dcsim2.host.Host;
import edu.uwo.csd.dcsim2.host.comparator.*;
import edu.uwo.csd.dcsim2.vm.VMAllocationRequest;

public class VMPlacementPolicyFixedCount extends VMPlacementPolicy {

	int vmsPerHost;
	
	public VMPlacementPolicyFixedCount(int vmsPerHost) {
		this.vmsPerHost = vmsPerHost;
	}
	
	@SuppressWarnings("unused")
	private static Logger logger = Logger.getLogger(VMPlacementPolicyFFD.class);
	
	@Override
	public boolean submitVM(VMAllocationRequest vmAllocationRequest) {

		ArrayList<Host> sortedHosts = sortHostList();
		Host target = findTargetHost(vmAllocationRequest, sortedHosts);

		if (target != null)
			return submitVM(vmAllocationRequest, target);

		return false;			
	}

	@Override
	public boolean submitVMs(ArrayList<VMAllocationRequest> vmAllocationRequests) {

		for (VMAllocationRequest request : vmAllocationRequests) {
			if (!submitVM(request))
				return false;
		}
		
		return true;
	}
	
	@Override
	public boolean submitVM(VMAllocationRequest vmAllocationRequest, Host host) {

		if (host.hasCapacity(vmAllocationRequest)) {
			sendVM(vmAllocationRequest, host);
			return true;
		} else {
			return false;
		}
		
	}
	
	private void sendVM(VMAllocationRequest vmAllocationRequest, Host host) {
		
		if (host.getState() != Host.HostState.ON && host.getState() != Host.HostState.POWERING_ON) {
			Simulation.getInstance().sendEvent(
					new Event(Host.HOST_POWER_ON_EVENT,
							Simulation.getInstance().getSimulationTime(),
							this,
							host)
					);
		}
		
		//logger.debug("Submitted VM to Host #" + host.getId());
		host.submitVM(vmAllocationRequest);
	}

	private ArrayList<Host> sortHostList() {
		ArrayList<Host> sorted = new ArrayList<Host>();
		
		sorted.addAll(datacentre.getHosts());
		Collections.sort(sorted, new HostVMCountComparator());
		Collections.reverse(sorted); //switch to decreasing order
		return sorted;
	}
	
	private Host findTargetHost(VMAllocationRequest vmAllocationRequest, ArrayList<Host> sortedHosts) {

		for (Host host : sortedHosts) {
			if (host.getVMAllocations().size() < vmsPerHost && host.hasCapacity(vmAllocationRequest)) {
				return host;
			}
		}
		
		return null;
	}

}