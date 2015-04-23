package tutorial;

public class DistancePair {
	private double f, c;
	
	DistancePair(double f, double c) {
		this.f = f;
		this.c = c;
	}
	
	public void updatePair(double f, double c) {
		this.f = f;
		this.c = c;
	}
	
	public double getTotal() {
		return (this.f + this.c);
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
	
	@Override
	public String toString() {
		return (this.f +": " + this.c);
		
	}

}
