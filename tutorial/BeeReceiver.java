package tutorial;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

public class BeeReceiver implements Runnable{
	private Map<Id, Map<Id, Integer>> latestGeneration;
	private Map<Id, Map<Id, Integer>> minHopCount;
	
	private Map<Id, Map<Id, DistancePair>> bestDistance;
	private Map<Id, Map<Id, DistancePair>> routingTable;
	
	private Map<Integer, Map<Id, Node>> areaNodes;
	private Map<Integer, Map<Id, Node>> netNodes;
	
	private Map<Integer, Navigator> navigators;
	
	private Map<Id, Map<Id, Id>> bestSuccessor;
	
	private Map<Id, Map<Id, Boolean>> visited;
	
	private Map<Id, Node> totalNetNodes;
	
	private BeeQueue receiverBeeQueue;
	
	private String typeArea;
	private String typeNet;
	
	private int id;
	
	public BeeReceiver(
			int id,
			Map<Integer, Map<Id, Node>> areaNodes,
			Map<Integer, Map<Id, Node>> netNodes,
			Map<Integer, Navigator> navigators,
			Map<Id, Node> totalNetNodes,
			BeeQueue receiverBeeQueue) {
		this.latestGeneration = new HashMap<Id, Map<Id, Integer>>();
		this.minHopCount = new HashMap<Id, Map<Id, Integer>>();
		this.bestDistance = new HashMap<Id, Map<Id, DistancePair>>();
		this.routingTable = new HashMap<Id, Map<Id, DistancePair>>();
		this.bestSuccessor = new HashMap<Id, Map<Id, Id>>();
		this.visited = new HashMap<Id, Map<Id, Boolean>>();
		this.typeArea = "AREA";
		this.typeNet = "NET";
		this.id = id;
		this.areaNodes = areaNodes;
		this.netNodes = netNodes;
		this.navigators = navigators;
		this.totalNetNodes = totalNetNodes;
		this.receiverBeeQueue = receiverBeeQueue;
	}
	
	public void receiveBee() {
		while (this.receiverBeeQueue.getSize() > 0) {
			BeeAgent bee = this.receiverBeeQueue.getFirst();
			this.initializeTables(bee);
			int realNId = -1;
			for (int i = 0; i < 16; i++) {
				if (this.areaNodes.get(i).containsKey(bee.getSource())) {
					realNId = i;
					break;
				}
			}
			if (!this.latestGeneration.get(bee.getCurrent()).containsKey(
					bee.getSource()))
				this.latestGeneration.get(bee.getCurrent()).put(
						bee.getSource(), 0);
			if (bee.generation < this.latestGeneration.get(bee.getCurrent())
					.get(bee.getSource()))
				return;
			if (bee.generation > this.latestGeneration.get(bee.getCurrent())
					.get(bee.getSource())) {
				Id s1 = this.getGoodNeighbor(bee);
				this.bestDistance.get(bee.getCurrent()).put(bee.getSource(),
						this.routingTable.get(bee.getSource()).get(s1));
				this.bestSuccessor.get(bee.getCurrent()).put(bee.getSource(),
						s1);
				this.setNeighborInfinity(bee);
				this.visited.get(bee.getCurrent()).put(bee.getSource(), false);
			}
			double fDelay = this.getFDelay(bee.getCurrent(), bee.getLast());
			bee.setF(bee.getF() + fDelay);
			// bee.setC();
			this.routingTable.get(bee.getSource()).put(bee.getLast(),
					new DistancePair(bee.getF(), bee.getC()));
			System.out.println("Bee on node: "
					+ bee.getCurrent()
					+ " of: "
					+ this.id
					+ " From: "
					+ realNId
					+ ": "
					//+ this.visited.get(bee.getCurrent()).get(bee.getSource())
					//+ " "
					+ this.bestSuccessor.get(bee.getCurrent()).get(
							bee.getSource())
					+ " "
					+ this.bestDistance.get(bee.getCurrent()).get(
							bee.getSource()) + ": " + bee.type);
			if ((!this.visited.get(bee.getCurrent()).get(bee.getSource()))
					&& ((bee.generation == 1)
							|| (bee.getLast().equals(this.bestSuccessor.get(
									bee.getCurrent()).get(bee.getSource()))) 
							|| (bee.distance < this.bestDistance
							.get(bee.getCurrent()).get(bee.getSource())
							.getTotal()))) {
				if (!this.areaNodes.get(this.id).containsKey(bee.getLast()))
					bee.limit -= 1;

				if (!this.minHopCount.get(bee.getCurrent()).containsKey(
						bee.getSource()))
					this.minHopCount.get(bee.getCurrent()).put(bee.getSource(),
							Integer.MAX_VALUE);
				if (this.minHopCount.get(bee.getCurrent()).get(bee.getSource()) < bee.limit)
					bee.limit = this.minHopCount.get(bee.getCurrent()).get(
							bee.getSource());
				else
					this.minHopCount.get(bee.getCurrent()).put(bee.getSource(),
							bee.limit);

				if (bee.limit > 0) {
					this.visited.get(bee.getCurrent()).put(bee.getSource(),
							true);
					// forward to next node
					this.forwardBee(bee);
				}

			}
		}
	}
	
	private void initializeTables(BeeAgent bee) {
		if (!this.latestGeneration.containsKey(bee.getCurrent()))
			this.latestGeneration.put(bee.getCurrent(), new HashMap<Id, Integer>());
		
		if (!this.bestDistance.containsKey(bee.getCurrent()))
			this.bestDistance.put(bee.getCurrent(), new HashMap<Id, DistancePair>());
		
		if (!this.bestSuccessor.containsKey(bee.getCurrent()))
			this.bestSuccessor.put(bee.getCurrent(), new HashMap<Id, Id>());
		
		if (!this.visited.containsKey(bee.getCurrent()))
			this.visited.put(bee.getCurrent(), new HashMap<Id, Boolean>());
		
		if (!this.routingTable.containsKey(bee.getSource()))
			this.routingTable.put(bee.getSource(), new HashMap<Id, DistancePair>());
		
		if (!this.minHopCount.containsKey(bee.getCurrent()))
			this.minHopCount.put(bee.getCurrent(), new HashMap<Id, Integer>());
	}

	private Id getGoodNeighbor(BeeAgent bee) {
		Map<Id, ? extends Link> oLinks = this.areaNodes.get(this.id).get(bee.getCurrent()).getOutLinks();
		Id temp = null;
		double distance = Double.POSITIVE_INFINITY;
		String print = "";
		for (Link l: oLinks.values()) {
			if (bee.type.equals(this.typeArea)) {
				if (this.areaNodes.get(this.id).containsKey(l.getToNode().getId())) {
					if(this.routingTable.get(bee.getSource()).containsKey(l.getToNode().getId())) {
						if (temp == null) {
							temp = l.getToNode().getId();
							distance = this.routingTable.get(bee.getSource()).get(l.getToNode().getId()).getTotal();
							print += "Area Present and: ";
						}
						if (this.routingTable.get(bee.getSource()).get(l.getToNode().getId()).getTotal() < distance) {
							temp = l.getToNode().getId();
							distance = this.routingTable.get(bee.getSource()).get(l.getToNode().getId()).getTotal();
							print += "Area Present: ";
						}
					}
					else {
						this.routingTable.get(bee.getSource()).put(l.getToNode().getId(), 
								new DistancePair(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
						temp = l.getToNode().getId();
						distance = this.routingTable.get(bee.getSource()).get(l.getToNode().getId()).getTotal();
						print += "Area empty: ";
					}
				}
			}
			else {
				if (this.totalNetNodes.containsKey(l.getToNode().getId())) {
					if (!this.routingTable.containsKey(bee.getSource()))
						this.routingTable.put(bee.getSource(), new HashMap<Id, DistancePair>());
					if(this.routingTable.get(bee.getSource()).containsKey(l.getToNode().getId())) {
						if (temp == null) {
							temp = l.getToNode().getId();
							distance = this.routingTable.get(bee.getSource()).get(l.getToNode().getId()).getTotal();
							print += "Net Present and: ";
						}
						if (this.routingTable.get(bee.getSource()).get(l.getToNode().getId()).getTotal() < distance) {
							temp = l.getToNode().getId();
							distance = this.routingTable.get(bee.getSource()).get(l.getToNode().getId()).getTotal();
							print += "Net present: ";
						}
					}
					else {
						this.routingTable.get(bee.getSource()).put(l.getToNode().getId(), 
								new DistancePair(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
						temp = l.getToNode().getId();
						distance = this.routingTable.get(bee.getSource()).get(l.getToNode().getId()).getTotal();
						print += "Net empty: ";
					}
				}
			}
		}
		//System.out.println(print + "returning temp: " + temp + ": " + bee.type + " with oLinks size: " + oLinks.size());
		return temp;
	}
	
	private void setNeighborInfinity(BeeAgent bee) {
		Map<Id, ? extends Link> oLinks = this.areaNodes.get(this.id).get(bee.getCurrent()).getOutLinks();
		for (Link l: oLinks.values()) {
			this.routingTable.get(bee.getSource()).put(l.getFromNode().getId(), 
					new DistancePair(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
		}
	}
	
	private double getFDelay(Id current, Id last) {
		Map<Id, ? extends Link> oLinks = this.areaNodes.get(this.id).get(current).getOutLinks();
		for (Link l: oLinks.values()) {
			if(l.getToNode().getId().equals(last)) {
				return (l.getLength()/ l.getFreespeed());
			}
		}
		return 0;
	}

	private void forwardBee(BeeAgent bee) {
		Map<Id, ? extends Link> iLinks = this.areaNodes.get(this.id).get(bee.getCurrent()).getInLinks();
		for (Link l: iLinks.values()) {
			if (bee.type.equals(typeArea)) {				
				if (this.areaNodes.get(this.id).containsKey(l.getFromNode().getId())) {
					if (!l.getFromNode().getId().equals(bee.getLast())) {
						bee.setCurrent(l.getFromNode().getId());
						break;
					}
				}
			}
			else {
				if (this.totalNetNodes.containsKey(l.getFromNode().getId())) {
					if (!l.getFromNode().getId().equals(bee.getLast())) {
						bee.setCurrent(l.getFromNode().getId());
						break;
					}
				}
			}
		}
		if ((bee.type.equals(typeArea)) || 
				(bee.type.equals(typeNet) && this.netNodes.get(this.id).containsKey(bee.getCurrent()))) {
			this.receiverBeeQueue.add(bee);;
		}
		else {
			for (int i = 0; i < 16; i++) {
				if(this.netNodes.get(i).containsKey(bee.getCurrent())) {
					this.navigators.get(i).getReceiverQueue().add(bee);
					break;
				}
			}
		}
	}

	@Override
	public void run() {
		this.receiveBee();
	}
}
