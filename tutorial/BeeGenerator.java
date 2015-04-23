package tutorial;

import java.util.Collection;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

public class BeeGenerator implements Runnable{
	private int generation;
	
	private Collection<Node> sourceAreaNodes;
	private Collection<Node> sourceNetNodes;
	private Collection<Node> totalNetNodes;
	
	private Node representativeNode;
	
	private BeeQueue senderBeeQueue;
	
	private String typeArea;
	private String typeNet;
	
	private int infinity = Integer.MAX_VALUE;

	public BeeGenerator(
			Collection<Node> sourceAreaNodes,
			Collection<Node> sourceNetNodes,
			Collection<Node> totalNetNodes,
			Node rNode,
			BeeQueue senderBeeQueue) {
		this.generation = 1;
		this.typeArea = "AREA";
		this.typeNet = "NET";
		this.sourceAreaNodes = sourceAreaNodes;
		this.sourceNetNodes = sourceNetNodes;
		this.totalNetNodes = totalNetNodes;
		this.representativeNode = rNode;
		this.senderBeeQueue = senderBeeQueue;
	}

	public void generateBees() {
		while (generation <= 100) {
			for (Node n : this.sourceAreaNodes) {
				Map<Id, ? extends Link> iLinks = n.getInLinks();
				for (Link l : iLinks.values()) {
					if (this.sourceAreaNodes.contains(l.getFromNode())) {
						BeeAgent bee = new BeeAgent(generation, this.typeArea,
								n.getId());
						bee.limit = 1;
						bee.setCurrent(l.getFromNode().getId());
						this.senderBeeQueue.add(bee);
					}
				}
			}
			for (Node n : this.sourceNetNodes) {
				Map<Id, ? extends Link> iLinks = n.getInLinks();
				for (Link l : iLinks.values()) {
					if (this.totalNetNodes.contains(l.getFromNode())) {
						BeeAgent bee = new BeeAgent(generation, this.typeNet,
								n.getId());
						bee.setCurrent(l.getFromNode().getId());
						if (this.representativeNode.getId().equals(n)) {
							bee.limit = 1;
						} else {
							bee.limit = this.infinity;
						}
						this.senderBeeQueue.add(bee);
						;
					}
				}
			}
			this.generation++;
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void run() {
		this.generateBees();
	}
}
