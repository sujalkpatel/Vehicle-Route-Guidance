package tutorial;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class CreatePopulationAndDemand implements LinkEnterEventHandler, StartupListener{
	
	private final static Logger log = Logger.getLogger(CreatePopulationAndDemand.class);
	private Scenario scenario;
	private Controler controler;
	private MobsimTimer mTimer;
	
	private String facilitiesFile = "./input/facilities.xml.gz";
	private String networkFile = "./input/network.xml";
	
	// --------------------------------------------------------------------------
	/*public static void main(String[] args) {
		CreatePopulationAndDemand creator = new CreatePopulationAndDemand();
		//EventsManager myEManager = EventsUtils.createEventsManager();
		//myEManager.addHandler(creator);
		creator.run();		
	}*/
	
	public void run() {
		this.init();
		CreatePopulation populationCreator = new CreatePopulation();
		populationCreator.run(this.scenario);
		CreateDemand demandCreator = new CreateDemand();
		demandCreator.run(this.scenario, populationCreator.getPersonHomeAndWorkLocations());
		this.write();
		/*Config config = ConfigUtils.loadConfig("input/config.xml");
		this.controler = new Controler ("input/config.xml");
		this.controler.addControlerListener(this);
		this.controler.run();*/
	}
	
	private void init() {
		/*
		 * Create the scenario
		 */
		Config config = ConfigUtils.createConfig();
		this.scenario = ScenarioUtils.createScenario(config);
		/*
		 * Read the network and store it in the scenario
		 */
		new MatsimNetworkReader(this.scenario).readFile(networkFile);
		/*
		 * Read the facilities and store them in the scenario
		 */
		new FacilitiesReaderMatsimV1((ScenarioImpl)this.scenario).readFile(this.facilitiesFile);	
		this.mTimer = new MobsimTimer();
	}
	
	private void write() {
		PopulationWriter populationWriter = new PopulationWriter(this.scenario.getPopulation(), this.scenario.getNetwork());
		populationWriter.write("./input/plans.xml.gz");
		log.info("Number of persons: " + this.scenario.getPopulation().getPersons().size());
	}

	@Override
	public void reset(int arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(LinkEnterEvent lEvent) {
		int hour = (int) (lEvent.getTime() / 3600);
		int min = (int) ((lEvent.getTime() % 3600) / 60);
		int sec = (int) ((lEvent.getTime() % 60) % 60);
		
		double cTime = this.mTimer.getTimeOfDay();
		int cHour = (int) (lEvent.getTime() / 3600);
		int cMin = (int) ((lEvent.getTime() % 3600) / 60);
		int cSec = (int) ((lEvent.getTime() % 60) % 60);
		System.out.println("Person: " + lEvent.getPersonId() + "\tleft link: " + lEvent.getLinkId() + "\tat: " + 
					hour + ": " + min + ": " + sec + " Current time: " + cHour + ": " + cMin + ": " + cSec);
		
		
		
	}

	@Override
	public void notifyStartup(StartupEvent sEvent) {
		sEvent.getControler().getEvents().addHandler(this);
		
	}
}
