package tutorial;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

public class Navigator implements Runnable{
	private static Map<Integer, Navigator> navigators = new HashMap<Integer, Navigator>();
	
	private static Map<Integer, Map<Id, Node>> areaNodes; 
	private static Map<Integer, Map<Id, Node>> netNodes;
	
	private static Map<Integer, Map<Id, Link>> areaLinks;
	private static Map<Integer, Map<Id, Link>> netLinks;
	
	private static Map<Integer, Node> representativeNodes;
	
	private static Map<Id, Node> totalNetNodes = new HashMap<Id, Node>();
	
	private int id;
	
	private BeeQueue senderBeeQueue;
	private BeeQueue receiverBeeQueue;
	
	private BeeTransmitter bTransmitter;
	private BeeGenerator bGenerator;
	private BeeReceiver bReceiver;
	
	public Navigator(
			Map<Integer, Map<Id, Node>> areaNodes,
			Map<Integer, Map<Id, Node>> netNodes,
			Map<Integer, Map<Id, Link>> areaLinks,
			Map<Integer, Map<Id, Link>> netLinks,
			Map<Integer, Node> rNodes,
			int id) {
		if (Navigator.areaNodes == null) Navigator.areaNodes = areaNodes;
		if (Navigator.netNodes == null) Navigator.netNodes = netNodes;
		if (Navigator.areaLinks == null) Navigator.areaLinks = areaLinks;
		if (Navigator.netLinks == null) Navigator.netLinks = netLinks;
		if (Navigator.representativeNodes == null) Navigator.representativeNodes = rNodes;
		Navigator.totalNetNodes.putAll(Navigator.netNodes.get(id));
		Navigator.navigators.put(id, this);
		this.id = id;
		
		this.senderBeeQueue = new BeeQueue("senderQueue" + id);
		this.receiverBeeQueue = new BeeQueue("receiverQueue" + id);
		
		this.bTransmitter = new BeeTransmitter(this.id, Navigator.netNodes, Navigator.navigators, this.senderBeeQueue, this.receiverBeeQueue);
		this.bGenerator = new BeeGenerator(Navigator.areaNodes.get(id).values(), Navigator.netNodes.get(id).values(), 
									Navigator.totalNetNodes.values(), Navigator.representativeNodes.get(id), this.senderBeeQueue);
		this.bReceiver = new BeeReceiver(this.id, Navigator.areaNodes, Navigator.netNodes, Navigator.navigators, Navigator.totalNetNodes, 
									this.receiverBeeQueue);
	}

	public BeeQueue getReceiverQueue() {
		return this.receiverBeeQueue;
	}

	@Override
	public void run() {
		new Thread(this.bGenerator).start();
		new Thread(this.bTransmitter).start();
		new Thread(this.bReceiver).start();
	}

}
