package tutorial;

import java.util.LinkedList;

public class BeeQueue {
private LinkedList<BeeAgent> queue;
private String name;

public BeeQueue(
		String name) {
	this.name = name;
	this.queue = new LinkedList<BeeAgent>();
}

public void add(
		BeeAgent bee) {
	this.queue.add(bee);
}

public BeeAgent getFirst() {
	return this.queue.poll();
}

public int getSize() {
	return this.queue.size();
}

public String toString() {
	return this.name;
}
}
