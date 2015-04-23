package tutorial;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

public class BeeTransmitter implements Runnable{
	private Map<Integer, Map<Id, Node>> netNodes;
	
	private Map<Integer, Navigator> navigators;
	
	private BeeQueue senderBeeQueue;
	private BeeQueue receiverBeeQueue;
	
	private String typeArea;
	private String typeNet;
	
	private int id;
	
	public BeeTransmitter(
			int id,
			Map<Integer, Map<Id, Node>> netNodes,
			Map<Integer, Navigator> navigators,
			BeeQueue senderBeeQueue,
			BeeQueue receiverBeeQueue) {
		this.typeArea = "AREA";
		this.typeNet = "NET";
		this.id = id;
		this.netNodes = netNodes;
		this.navigators = navigators;
		this.senderBeeQueue = senderBeeQueue;
		this.receiverBeeQueue = receiverBeeQueue;
	}
	
	public void transmitBee() {
		while(this.senderBeeQueue.getSize() != 0) {
			BeeAgent bee = this.senderBeeQueue.getFirst();
			if ((bee.type.equals(this.typeArea)) || 
					(bee.type.equals(this.typeNet) && this.netNodes.get(this.id).containsKey(bee.getCurrent()))) {
				this.receiverBeeQueue.add(bee);
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
	}

	@Override
	public void run() {
		this.transmitBee();
	}
}
