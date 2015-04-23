package tutorial;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.Controler.TerminationCriterion;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class Simulation implements StartupListener, ShutdownListener/*BeforeMobsimListener, AfterMobsimListener*/{
	private String networkFile = "./input/network.xml";
	private Controler controler;
	private Scenario scenario;
	private NetworkImpl network;
	public static MobsimTimer mTimer;
	private EventsManager eManager;
	private double minX, minY, maxX, maxY, rangeX, rangeY;
	private Map<Integer, Node> represntativeNodes;							// <area id, node>
	private Map<Id, Node> nodes, routingNodes;								// <node id, node>
	private Map<Id, Link> links, routingLinks;								// <link id, link>
	private Map<Integer, Map<Id, Node>> areaNodes, borderNodes, netNodes;	// <navigator id, <node id, node>>
	private Map<Integer, Map<Id, Link>> areaLinks, borderLinks, netLinks;	// <navigator id, <link id, link>>
	private Navigator[] n;
	private int numOfNavigators = 16;
	private CreatePopulationAndDemand creator;
	public static void main(String[] args) {
		Simulation simulator = new Simulation();
		simulator.run();
	}

	private void run() {
		this.init();
		
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.controler.run();
		//this.n[0].setLaunchStatus(false);
		
	}

	private void init() {
		this.creator = new CreatePopulationAndDemand();
		this.creator.run();
		
		Config config = ConfigUtils.createConfig();
		this.scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(this.scenario).readFile(networkFile);
		this.network = (NetworkImpl) this.scenario.getNetwork();
		this.scenario.getPopulation();
		new QSim(this.scenario, this.eManager);
		this.controler = new Controler("input/config.xml");
		this.controler.addControlerListener(this);
		TerminationCriterion tc = new TerminationCriterion() {
			
			@Override
			public boolean continueIterations(int it) {
				if (it == 10) return false;
				return true;
			}
		};
		this.controler.setTerminationCriterion(tc);
		new Random();
		this.nodes = this.network.getNodes();
		this.links = this.network.getLinks();
		this.setMinMaxCoordinates();
		System.out.println (this.network);
		this.setupRoutingLayer();
		this.setupAreaLayer();
		this.setupBorderNodesAndLinks();
		this.setupNetLayer();		
		this.setUpNavigators();
	}

	private void setupBorderNodesAndLinks() {
		this.borderNodes = new HashMap<Integer, Map<Id, Node>>();
		this.borderLinks = new HashMap<Integer, Map<Id, Link>>();
		for (int i = 0; i < 16; i++) {
			Map<Id, Link> links = new HashMap<Id, Link>();
			this.borderLinks.put(i, links);
			Map<Id, Node> nodes = new HashMap<Id, Node>();
			this.borderNodes.put(i, nodes);
			
			Map<Id, Node> tempNodes = this.areaNodes.get(i);
			for (Node n: tempNodes.values()) {
				Map<Id, ? extends Link> oLinks = n.getOutLinks();
				for (Link l : oLinks.values()) {
					if (!tempNodes.containsKey(l.getToNode().getId())) {
						this.borderNodes.get(i).put(n.getId(), n);
						this.borderLinks.get(i).put(l.getId(), l);
					}
				}
			}
			//System.out.println(this.borderNodes.get(i).size() + ": " + this.borderLinks.get(i).size());
		}
		
	}

	private void setupNetLayer() {
		this.represntativeNodes = new HashMap<Integer, Node>();
		this.netNodes = new HashMap<Integer, Map<Id, Node>>(this.borderNodes);
		this.netLinks = new HashMap<Integer, Map<Id, Link>>(this.borderLinks);
		for (int i = 0; i < 16; i++) {
			Map<Id, Node> tempNodes = this.netNodes.get(i);
			for (Node n: tempNodes.values()) {
				Map<Id, ? extends Link> oLinks = n.getOutLinks();
				for (Link l : oLinks.values()) {
					if (tempNodes.containsKey(l.getToNode().getId())) {
						this.netLinks.get(i).put(l.getId(), l);
					}
				}
			}
			this.represntativeNodes.put(i, this.netNodes.get(i).entrySet().iterator().next().getValue());
			//System.out.println (this.netNodes.get(i).size() + ": " + this.netLinks.get(i).size());
		}
	}

	private void setupRoutingLayer() {
		this.routingNodes = new HashMap<Id, Node>(this.nodes);
		this.routingLinks = new HashMap<Id, Link>(this.links);
		/*Map<Id, Node>tempNodes = new HashMap<Id, Node>();
		
		for (Node n: this.routingNodes.values()) {
			Map<Id, ? extends Link> iLinks = n.getInLinks();
			Map<Id, ? extends Link> oLinks = n.getOutLinks();
			int iNumber = iLinks.size();
			int oNumber = oLinks.size();
			if ((iNumber <= 2) && (oNumber <= 2)) {
				Object[] iArray = iLinks.values().toArray();
				Object[] oArray = oLinks.values().toArray();
				if((iNumber == 2) && (oNumber == 2)) {
					// Change toNode of both inLinks
					Link link1 = (Link) iArray[0];
					Link link2 = (Link) iArray[1];
					Link link3 = (Link) oArray[0];
					Link link4 = (Link) oArray[1];
					link1.setToNode(link2.getFromNode());
					link2.setToNode(link1.getFromNode());
					
					this.routingLinks.remove(link1.getId());
					this.routingLinks.remove(link2.getId());
					this.routingLinks.put(link1.getId(), link1);
					this.routingLinks.put(link2.getId(), link2);
					
					this.routingLinks.remove(link3.getId());
					this.routingLinks.remove(link4.getId());
					
					tempNodes.put(n.getId(), n);
					
					
				}
				else if ((iNumber == 1) && (oNumber == 1)) {
					Link link1 = (Link) iArray[0];
					Link link2 = (Link) oArray[0];
					
					if (link1.getFromNode().toString().equals(link2.getToNode().toString())) {
						this.routingLinks.remove(link1.getId());
						this.routingLinks.remove(link2.getId());
					}
					else {
						link1.setToNode(link2.getToNode());
						
						this.routingLinks.remove(link1.getId());
						this.routingLinks.put(link1.getId(), link1);
						
						this.routingLinks.remove(link2.getId());
						
						//System.out.println(link1.getFromNode().getCoord() + ": " + this.routingLinks.get(link1.getId()).getFromNode().getCoord());
					}
					tempNodes.put(n.getId(), n);
				}
			}
		}
		
		for (Node n: tempNodes.values())
			this.routingNodes.remove(n.getId());*/
		
	}

	private void setUpNavigators() {
		this.n = new Navigator[16];
		for (int i = 0; i < this.numOfNavigators; i++) {
			this.n[i] = new Navigator(this.areaNodes, this.netNodes, this.areaLinks, this.netLinks, this.represntativeNodes, i);
		}
		
	}

	private void setupAreaLayer() {
		this.areaNodes = new HashMap<Integer, Map<Id, Node>>();
		this.areaLinks = new HashMap<Integer, Map<Id, Link>>();
		double difX = 0, difY = 0;
		int xPosition = 0, yPosition = 0;
		
		for (int i = 0; i < 16; i++) {
			Map<Id, Link> links = new HashMap<Id, Link>();
			this.areaLinks.put(i, links);
			Map<Id, Node> nodes = new HashMap<Id, Node>();
			this.areaNodes.put(i, nodes);
		}
		System.out.println(this.routingNodes.size() + ": " + this.routingLinks.size());
		for (Node n : this.routingNodes.values()) {
			difX = n.getCoord().getX() - this.minX;
			difY = n.getCoord().getY() - this.minY;
			
			{if (difX < (this.rangeX / 2)) {
				if (difX < (this.rangeX / 4))
					xPosition = 0;
				else
					xPosition = 1;
			}
			else {
				if (difX < (this.rangeX * 3 / 4))
					xPosition = 2;
				else
					xPosition = 3;
			}}
			
			{if (difY < (this.rangeY / 2)) {
				if (difY < (this.rangeY / 4))
					yPosition = 0;
				else
					yPosition = 1;
			}
			else {
				if (difY < (this.rangeY * 3 / 4))
					yPosition = 2;
				else
					yPosition = 3;
			}}
			
			{if ((xPosition == 0) && (yPosition == 0)) {
					this.areaNodes.get(0).put(n.getId(), n);
					Map<Id, ? extends Link> oLinks = n.getOutLinks();
					for (Link l : oLinks.values())
						this.areaLinks.get(0).put(l.getId(), l);
			}
			if ((xPosition == 0) && (yPosition == 1)) {
					this.areaNodes.get(1).put(n.getId(), n);
					Map<Id, ? extends Link> oLinks = n.getOutLinks();
					for (Link l : oLinks.values())
						this.areaLinks.get(1).put(l.getId(), l);
			}
			if ((xPosition == 0) && (yPosition == 2)) {
					this.areaNodes.get(2).put(n.getId(), n);
					Map<Id, ? extends Link> oLinks = n.getOutLinks();
					for (Link l : oLinks.values())
						this.areaLinks.get(2).put(l.getId(), l);
			}
			if ((xPosition == 0) && (yPosition == 3)) {
					this.areaNodes.get(3).put(n.getId(), n);
					Map<Id, ? extends Link> oLinks = n.getOutLinks();
					for (Link l : oLinks.values())
						this.areaLinks.get(3).put(l.getId(), l);
			}
			if ((xPosition == 1) && (yPosition == 0)) {
					this.areaNodes.get(4).put(n.getId(), n);
					Map<Id, ? extends Link> oLinks = n.getOutLinks();
					for (Link l : oLinks.values())
						this.areaLinks.get(4).put(l.getId(), l);
			}
			if ((xPosition == 1) && (yPosition == 1)) {
					this.areaNodes.get(5).put(n.getId(), n);
					Map<Id, ? extends Link> oLinks = n.getOutLinks();
					for (Link l : oLinks.values())
						this.areaLinks.get(5).put(l.getId(), l);
			}
			if ((xPosition == 1) && (yPosition == 2)) {
					this.areaNodes.get(6).put(n.getId(), n);
					Map<Id, ? extends Link> oLinks = n.getOutLinks();
					for (Link l : oLinks.values())
						this.areaLinks.get(6).put(l.getId(), l);
			}
			if ((xPosition == 1) && (yPosition == 3)) {
					this.areaNodes.get(7).put(n.getId(), n);
					Map<Id, ? extends Link> oLinks = n.getOutLinks();
					for (Link l : oLinks.values())
						this.areaLinks.get(7).put(l.getId(), l);
			}
			if ((xPosition == 2) && (yPosition == 0)) {
					this.areaNodes.get(8).put(n.getId(), n);
					Map<Id, ? extends Link> oLinks = n.getOutLinks();
					for (Link l : oLinks.values())
						this.areaLinks.get(8).put(l.getId(), l);
			}
			if ((xPosition == 2) && (yPosition == 1)) {
					this.areaNodes.get(9).put(n.getId(), n);
					Map<Id, ? extends Link> oLinks = n.getOutLinks();
					for (Link l : oLinks.values())
						this.areaLinks.get(9).put(l.getId(), l);
			}
			if ((xPosition == 2) && (yPosition == 2)) {
					this.areaNodes.get(10).put(n.getId(), n);
					Map<Id, ? extends Link> oLinks = n.getOutLinks();
					for (Link l : oLinks.values())
						this.areaLinks.get(10).put(l.getId(), l);
			}
			if ((xPosition == 2) && (yPosition == 3)) {
					this.areaNodes.get(11).put(n.getId(), n);
					Map<Id, ? extends Link> oLinks = n.getOutLinks();
					for (Link l : oLinks.values())
						this.areaLinks.get(11).put(l.getId(), l);
			}
			if ((xPosition == 3) && (yPosition == 0)) {
					this.areaNodes.get(12).put(n.getId(), n);
					Map<Id, ? extends Link> oLinks = n.getOutLinks();
					for (Link l : oLinks.values())
						this.areaLinks.get(12).put(l.getId(), l);
			}
			if ((xPosition == 3) && (yPosition == 1)) {
					this.areaNodes.get(13).put(n.getId(), n);
					Map<Id, ? extends Link> oLinks = n.getOutLinks();
					for (Link l : oLinks.values())
						this.areaLinks.get(13).put(l.getId(), l);
			}
			if ((xPosition == 3) && (yPosition == 2)) {
					this.areaNodes.get(14).put(n.getId(), n);
					Map<Id, ? extends Link> oLinks = n.getOutLinks();
					for (Link l : oLinks.values())
						this.areaLinks.get(14).put(l.getId(), l);
			}
			if ((xPosition == 3) && (yPosition == 3)) {
					this.areaNodes.get(15).put(n.getId(), n);
					Map<Id, ? extends Link> oLinks = n.getOutLinks();
					for (Link l : oLinks.values())
						this.areaLinks.get(15).put(l.getId(), l);
			}
			}
		}
		System.out.println(this.areaNodes.get(9).size() + ": " + this.areaLinks.get(9).size());
	}

	private void setMinMaxCoordinates() {
		this.minX = Double.POSITIVE_INFINITY;
		this.minY = Double.POSITIVE_INFINITY;
		this.maxX = Double.NEGATIVE_INFINITY;
		this.maxY = Double.NEGATIVE_INFINITY;
		
		for (Node n : this.nodes.values()) {
			if (n.getCoord().getX() < this.minX)	this.minX = n.getCoord().getX();
			if (n.getCoord().getY() < this.minY)	this.minY = n.getCoord().getY();
			if (n.getCoord().getX() > this.maxX)	this.maxX = n.getCoord().getX();
			if (n.getCoord().getY() > this.maxY)	this.maxY = n.getCoord().getY();
		}
		this.minX -= 1;
		this.minY -= 1;
		this.maxX += 1;
		this.maxY += 1;
		this.rangeX = this.maxX - this.minX;
		this.rangeY = this.maxY - this.minY;
		System.out.println(this.minX + ": " + this.minY + ": " + this.maxX + ": " + this.maxY + ": " + this.rangeX + ": " +this.rangeY);
	}

	@Override
	public void notifyStartup(StartupEvent sEvent) {
		System.out.println("NotifyStartup called..........");
		
		for (int i = 0; i < this.numOfNavigators; i++) {
			new Thread(this.n[i]).start();
			//sEvent.getControler().getEvents().addHandler(this.n[i]);
		}
		
	}

	@Override
	public void notifyShutdown(ShutdownEvent sdEvent) {
		System.out.println("ShutdownEventHandler is called.");
		for (int i = 0; i < 16; i++) {
			//this.n[i].setRunStatus(false);
			this.n[i] = null;
		}
		
	}

	/*@Override
	public void notifyAfterMobsim(AfterMobsimEvent amEvent) {
		System.out.println("AfterMobsimEvent is called.");
		for (int i = 0; i < 16; i++) {
			//this.n[i].setRunStatus(false);
			//this.n[i] = null;
		}
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent bmEvent) {
		System.out.println("BeforeMobsimEvent is called.");
		//for (int i = 0; i < 16; i++)
			
	}*/

}
