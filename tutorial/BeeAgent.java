package tutorial;

import org.matsim.api.core.v01.Id;

public class BeeAgent {
	String type = null;
	Id source = null, last = null, secondLast = null, current = null;
	int area = 1, generation = 0, limit = 0;
	double f, c, distance;
	
	// area, F and C
	public BeeAgent(int generation, String type, Id source) {
		this.generation = generation;
		this.type = type;
		this.source = source;
		this.last = source;
		this.secondLast = source;
		this.current = source;
		this.f = 0;
		this.c = 0;
		this.distance = 0;
		
	}
	
	public double getTotalDistance() {
		this.distance = this.f + this.c;
		return this.distance;
	}
	
	public double getF() {
		return this.f;
	}
	
	public double getC() {
		return this.c;
	}
	
	public void setF(double f) {
		this.f = f;
	}
	
	public void setC(double c) {
		this.c = c;
	}
	
	public void setCurrent(Id current) {
		this.secondLast = this.last;
		this.last = this.current;
		this.current = current;
	}
	
	public Id getCurrent() {
		return this.current;
	}
	
	public Id getLast() {
		return this.last;
	}
	
	public Id getSecondLast() {
		return this.secondLast;
	}
	
	public Id getSource() {
		return this.source;
	}
	
}
