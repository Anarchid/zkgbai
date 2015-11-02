package zkgbai.economy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import zkgbai.Module;
import zkgbai.ZKGraphBasedAI;
import zkgbai.economy.tasks.ConstructionTask;
import zkgbai.economy.tasks.ProductionTask;
import zkgbai.economy.tasks.ReclaimTask;
import zkgbai.economy.tasks.TemporaryAssistTask;
import zkgbai.economy.tasks.WorkerTask;
import zkgbai.graph.GraphManager;
import zkgbai.graph.Link;
import zkgbai.graph.MetalSpot;
import zkgbai.graph.Pylon;
import zkgbai.military.MilitaryManager;

import com.springrts.ai.Enumerations.UnitCommandOptions;
import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Economy;
import com.springrts.ai.oo.clb.Feature;
import com.springrts.ai.oo.clb.FeatureDef;
import com.springrts.ai.oo.clb.OOAICallback;
import com.springrts.ai.oo.clb.Resource;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;

public class EconomyManager extends Module {
	ZKGraphBasedAI parent;
	List<Worker> workers;
	Set<Integer> factories;
	List<ConstructionTask> factoryTasks;
	List<ConstructionTask> radarTasks;
	List<WorkerTask> workerTasks;
	List<Wreck> features;
	List<Wreck> invalidWrecks;
	List<Unit> radars;
	List<Unit> porcs;
	List<Unit> nanos;
	List<Unit> fusions;
	
	float effectiveIncomeMetal = 0;
	float effectiveIncomeEnergy = 0;

	float totalBuildpower = 0;

	boolean buildingFusion = false;
	
	float effectiveIncome = 0;
	float effectiveExpenditure = 0;
	
	float energyQueued = 2;
	float metalQueued = 2;
	
	float metal = 0;
	float energy = 0;

	int numWorkers = 0;
	
	int frame = 0;
	static int CMD_PRIORITY = 34220;
	
	Economy eco;
	Resource m;
	Resource e;
	
	private int myTeamID;
	private OOAICallback callback;
	private GraphManager graphManager;
	private MilitaryManager warManager;
	private ArrayList<String> attackers;
	
	public EconomyManager(ZKGraphBasedAI parent){
		this.parent = parent;
		this.callback = parent.getCallback();
		this.myTeamID = parent.teamID;
		this.workers = new ArrayList<Worker>();
		this.factories = new HashSet<Integer>();
		this.factoryTasks = new ArrayList<ConstructionTask>();
		this.radarTasks = new ArrayList<ConstructionTask>();
		this.workerTasks = new ArrayList<WorkerTask>();
		this.features = new ArrayList<Wreck>();
		this.invalidWrecks = new ArrayList<Wreck>();
		this.radars = new ArrayList<Unit>();
		this.porcs = new ArrayList<Unit>();
		this.nanos = new ArrayList<Unit>();
		this.fusions = new ArrayList<Unit>();

		this.eco = callback.getEconomy();
		
		/*
		Map<String,String> customParams = callback.getUnitDefByName("armsolar").getCustomParams();
		
		String pylonRange = customParams.get("pylonrange");
		
		parent.debug("length of solar customparams: " +customParams.size());
		for(String s:customParams.keySet()){
			parent.debug("solar customparam "+s+" => "+customParams.get(s));
		}
		*/
		
		this.m = callback.getResourceByName("Metal");
		this.e = callback.getResourceByName("Energy");
		
		this.attackers = new ArrayList<String>();
		attackers.add("armrock");
		attackers.add("armrock");
		attackers.add("armwar");
		attackers.add("armpw");
		attackers.add("armpw");
		attackers.add("armpw");
		attackers.add("armzeus");
	}
	
	@Override
	public String getModuleName() {
		// TODO Auto-generated method stub
		return "EconomyManager";
	}
	
	@Override
	public int update(int frame){
		this.frame = frame;

		if (metalQueued < 2){
				metalQueued = 2;
		}

		if (energyQueued < 2){
			energyQueued = 2;
		}

		effectiveIncomeMetal = eco.getIncome(m);
		effectiveIncomeEnergy = eco.getIncome(e);
		
		metal = eco.getCurrent(m);
		energy = eco.getCurrent(e);
		
		float expendMetal = eco.getUsage(m);
		float expendEnergy = eco.getUsage(e);
		
		effectiveIncome= Math.min(effectiveIncomeMetal, effectiveIncomeEnergy);
		effectiveExpenditure = Math.min(expendMetal, expendEnergy);
		
		if(frame%15 == 0) {
			List<Feature> feats = callback.getFeatures();
			features = new ArrayList<Wreck>();

			for (Feature f : feats) {
				features.add(new Wreck(f, f.getDef().getContainedResource(m)));
			}

			inventarizeWorkers();


			for (Worker w : workers) {
				WorkerTask wt = w.getTask();
				if (wt.isCompleted()) {
					if (wt instanceof ConstructionTask) {
						ConstructionTask ct = (ConstructionTask) wt;
						if (factoryTasks.contains(ct)) {
							factoryTasks.remove(ct);
						}

						if (radarTasks.contains(ct)) {
							radarTasks.remove(ct);
						}

						workerTasks.remove(ct);
						assignWorkerTask(w);
					} else {
						workerTasks.remove(wt);
						assignWorkerTask(w);
					}
				}
			}

			for (WorkerTask wt : workerTasks) {
				if (wt instanceof TemporaryAssistTask) {
					TemporaryAssistTask at = (TemporaryAssistTask) wt;
					if (at.getTimeout() < frame) {
						at.setCompleted();
					}
				}
			}
			for(Worker w:workers) {
				Unit u = w.getUnit();
				if (u.getMaxSpeed() > 0 && effectiveIncome > 30) {
					ArrayList<Float> params = new ArrayList<Float>();
					params.add((float) 1);
					u.executeCustomCommand(CMD_PRIORITY, params, (short)0, parent.currentFrame);
				}
			}
		}
		return 0;
	}
	
	@Override
    public int init(int teamId, OOAICallback callback) {
        return 0;
    }
	
    @Override
    public int unitFinished(Unit unit) { 
    	
    	UnitDef def = unit.getDef(); 
    	String defName = def.getName();
    	
    	if(defName.equals("corrad")){
    		radars.add(unit);
    	}

		if(defName.equals("armsolar")){
			energyQueued += 2;
		}

		if(defName.equals("cormex")){
			metalQueued += 2;
		}
    	
    	if(defName.equals("armcom1") || defName.equals("commbasic") || (unit.getMaxSpeed()>0 && def.getBuildSpeed()>=10)){
    		ArrayList<Float> params = new ArrayList<Float>();
    		params.add((float) 2);
    		unit.executeCustomCommand(CMD_PRIORITY, params, (short)0, parent.currentFrame);
    	}

		if(defName.equals("factorycloak")){
			factories.add(unit.getUnitId());
			unit.setMoveState(2, (short) 0, frame+10);
		}

		if (defName.equals("armfus")){
			fusions.add(unit);
			buildingFusion = false;
		}

		if (defName.equals("cafus")){
			fusions.add(unit);
			buildingFusion = false;
		}

    	checkWorker(unit);
    	
    	for (WorkerTask wt:workerTasks){
    		if(wt instanceof ProductionTask){
    			ProductionTask ct = (ProductionTask)wt;
    			if (ct.building != null){
	    			if(ct.building.getUnitId() == unit.getUnitId()){
	    				ct.setCompleted();
	    			}
    			}
    		}
    	}
        return 0;
    }
    
    @Override
    public int unitDestroyed(Unit unit, Unit attacker) {
    	
    	radars.remove(unit);
    	porcs.remove(unit);
		nanos.remove(unit);
		fusions.remove(unit);
    	
    	Worker deadWorker = null;
	    for (Worker worker : workers) {
	    	if(worker.getUnit().getUnitId() == unit.getUnitId()){
	    		deadWorker = worker;
	    		WorkerTask wt = worker.getTask();
	    		workerTasks.remove(wt);
				numWorkers--;
				if(factoryTasks.contains(wt)){
					factoryTasks.remove(wt);
				}
	    	}
	    } 
	    if (deadWorker != null){
	    	workers.remove(deadWorker);
	    }

	    for(ConstructionTask ct:factoryTasks){
	    	if(ct.building != null && ct.building == unit){
				factoryTasks.remove(ct);
				radarTasks.remove(ct);
	    	}
	    }
	    radars.remove(unit);
	    factories.remove(unit.getUnitId());
        return 0; // signaling: OK
    }

    @Override
    public int unitIdle(Unit unit) {
	    for (Worker worker : workers) {
	    	if(worker.getUnit().getUnitId() == unit.getUnitId()){
				if(factoryTasks.contains(worker.getTask())){
					parent.debug("unitIdle: "+worker.getTask());
				}else{
		    		worker.getTask().setCompleted();
		    		WorkerTask wt = new WorkerTask(worker); 
		    		worker.setTask(wt);
		    		workerTasks.add(wt);
				}
	    	}
	    } 
        return 0; // signaling: OK
    }
    
    @Override
    public int unitCreated(Unit unit, Unit builder) {
    	if(builder != null){
			if(unit.isBeingBuilt()){
		    	for (WorkerTask wt:workerTasks){
		    		if(wt instanceof ProductionTask){
		    			ProductionTask ct = (ProductionTask)wt;
		    			if (ct.getWorker().getUnit().getUnitId() == builder.getUnitId()){
		    				ct.setBuilding(unit);
		    			}
		    		}
		    	}
			}else{	
		    	for (WorkerTask wt:workerTasks){
		    		if(wt instanceof ProductionTask){
		    			ProductionTask ct = (ProductionTask)wt;
		    			if (ct.getWorker().getUnit().getUnitId() == builder.getUnitId()){
		    				ct.setCompleted();
		    			}
		    		}
		    	}
			}
    	}
		String defName = unit.getDef().getName();

		if(defName.equals("corrl") || defName.equals("corllt") || defName.equals("corhlt")){
			porcs.add(unit);
		}

		if (defName.equals("armnanotc")){
			nanos.add(unit);
		}

		if(defName.equals("cormex") && metalQueued > 2){
			metalQueued -= 2;
		}

		if(defName.equals("armsolar")){
			energyQueued -= 2;
		}

        return 0;
    }
    
    private String getRandomAttacker(){
		if (effectiveIncome < 18){
			if (Math.random() > 0.9){
				return "armwar";
			}
			else{
				return "armpw";
			}
		}
		else{
			double rand = Math.random();
			if (rand > 0.6){
				return "armrock";
			}
			else if (rand > 0.5){
				return "armzeus";
			}
			else if (rand > 0.4) {
				return "armwar";
			}
			else if (rand > 0.25){
				return "armham";
			}
			else if (rand > 0.1){
				return "spherepole";
			}
			else if (fusions.size() > 0){
				return "armsnipe";
			}
			else{
				return "armpw";
			}
		}

    	/*if(numWarriors == 0){
    		return "armwar";
    	}

    	if(numGlaives == 0){
    		return "armpw";
    	}
    	
    	
    	int index = (int) Math.floor(Math.random()*attackers.size());
    	
    	if(Math.random()*20 < 1 && effectiveIncomeMetal > 25){
    		return "armsnipe";
    	}
    	
    	if(Math.random()*30 < 1 && effectiveIncomeMetal > 25){
    		return "spherepole";
    	}
    	
    	return attackers.get(index);*/
    }
    
    /**
     * Manually check for workers without orders
     */
    private void inventarizeWorkers(){
    	for(Worker w:workers){
    		Unit u = w.getUnit();
    		if (u.getCurrentCommands().size() == 0){
				if(workerTasks.contains(w.getTask()) && w.getTask() instanceof ConstructionTask){
					ConstructionTask c = (ConstructionTask) w.getTask();
					Unit building = c.getBuilding();
					
					if(building != null && building.getDef() != null){
						u.repair(building, (short)0, frame+1);
						parent.marker(building.getPos(),"interrupted: "+w.getTask());
					}else if(!factoryTasks.contains(c)){
						
						if(c.buildType.getName().equals("cormex")){
							graphManager.getClosestMetalSpot(c.location).removeColonist(u);;
						}
						
			    		w.getTask().setCompleted();
			    		WorkerTask wt = new WorkerTask(w); 
			    		w.setTask(wt);
			    		workerTasks.add(wt);	
					}else{
						factoryTasks.remove(c);
						workerTasks.remove(c);
			    		WorkerTask wt = new WorkerTask(w); 
			    		w.setTask(wt);
			    		workerTasks.add(wt);	
					}		
				}else{
		    		w.getTask().setCompleted();
		    		WorkerTask wt = new WorkerTask(w); 
		    		w.setTask(wt);
		    		workerTasks.add(wt);	
				}
    		}
    	}
    }
    
    void checkWorker(Unit unit){
		UnitDef def = unit.getDef();
		if (def.isBuilder()){
			if (def.getBuildOptions().size()>0){
				workers.add(new Worker(unit));
				numWorkers++;
				if (unit.getMaxSpeed() > 0 && effectiveIncome < 30) {
					ArrayList<Float> params = new ArrayList<Float>();
					params.add((float) 2);
					unit.executeCustomCommand(CMD_PRIORITY, params, (short) 0, parent.currentFrame);
				}
			}
			else{
				AIFloat3 shiftedPosition = unit.getPos();
		    	shiftedPosition.x+=30;
		    	shiftedPosition.z+=30;
		        unit.patrolTo(shiftedPosition, (short)0, frame+900000);
		        unit.setRepeat(true, (short)0, 900000);
			}
			totalBuildpower += def.getBuildSpeed();
		}
    }
    
    void assignWorkerTask(Worker worker){    	
    	// factories get special treatment
    	if(worker.getUnit().getMaxSpeed() == 0){
    		if (!worker.getUnit().getDef().getName().equals("armnanotc")){
	    		if((float)numWorkers < ((effectiveIncomeMetal+2)/5) + (2 * fusions.size())) {
	    			ProductionTask task = createUnitTask(worker, "armrectr");
	    			workerTasks.add(task);
	    			worker.setTask(task);
	    		}else{
	    			ProductionTask task = createUnitTask(worker, getRandomAttacker());
	    			workerTasks.add(task);
	    			worker.setTask(task);
	    		}
    		}
    		return;
    	}
    	
    	// is there a factory? a queued one maybe?
    	if (factories.size() == 0 || effectiveIncome / factories.size() > 80){
    		if(factoryTasks.size() == 0){
    			ConstructionTask task = createFactoryTask(worker);
    			factoryTasks.add(task);
    			worker.setTask(task);
    			return;
    		}else{
    			// check if assisting thet  to assist this factory?
    		}
    	}

    	// is there sufficient energy to cover metal income?
    	if(effectiveIncomeMetal + metalQueued + 2 > effectiveIncomeEnergy + energyQueued || energy < 10 || (effectiveIncomeMetal > 25 && !buildingFusion)){
			if (effectiveIncomeMetal >= 6) {
				ConstructionTask task = createEnergyTask(worker);
				worker.setTask(task);
				return;
			}
    	}
    	
    	// ponder building a nanoturret
    	if(((float)nanos.size() < (effectiveIncomeMetal/10) + fusions.size()) && effectiveIncome > 18){
    		for(Worker w:workers){
    			Unit u = w.getUnit();
    			if((u.getMaxSpeed() == 0 && u.getDef().getBuildOptions().size()>0) || fusions.contains(u)){
    				float dist = GraphManager.groundDistance(worker.getUnit().getPos(),u.getPos());
    				if (dist<1000){
    					ConstructionTask task = createNanoTurretTask(worker,u);
    					worker.setTask(task);
    					return;
    				}
    			}
    		}
    	}

		boolean tooCloseToFac = false;
		for(Worker w:workers){
			Unit u = w.getUnit();
			if(u.getMaxSpeed() == 0 && u.getDef().getBuildOptions().size()>0){
				float dist = GraphManager.groundDistance(worker.getUnit().getPos(),u.getPos());
				if (dist<300){
					tooCloseToFac = true;
				}
			}
		}
    	// ponder building a defender
    	float porcdist = Float.MAX_VALUE;
		for(Unit u:porcs){
			float dist = GraphManager.groundDistance(worker.getUnit().getPos(),u.getPos());
			if (dist<porcdist){
				porcdist = dist;
			}
		}

		int minporcdist = 400;
		if (warManager.getThreat(worker.getUnit().getPos()) > 0.7){
			minporcdist /= 2;
		}
		if (effectiveIncome > 20){
			minporcdist /= 2;
		}

		if(porcdist > minporcdist && effectiveIncomeMetal > 10 && !tooCloseToFac){
			ConstructionTask task = createPorcTask(worker);
			worker.setTask(task);
			return;
		}
    	
    	float closestRadarDistance = Float.MAX_VALUE;
    	for(Unit r:radars){
    		float distance = GraphManager.groundDistance(r.getPos(),worker.getUnit().getPos());
    		if(distance < closestRadarDistance){
    			closestRadarDistance = distance;
    		}
    	}
    	
    	if(closestRadarDistance > 1800 && radarTasks.size() == 0 && effectiveIncomeMetal > 10 && !tooCloseToFac){
    		ConstructionTask task = createRadarTask(worker);
    		radarTasks.add(task);
    		worker.setTask(task);
    		return;
    	}

    	// are there uncolonized metal spots? or is reclaim more worth it?
    	// or maybe just overdrive more
    	float minWeight = Float.MAX_VALUE;
    	MetalSpot spot = null;
    	AIFloat3 mypos = worker.getUnit().getPos();
    	List<MetalSpot> metalSpots = graphManager.getNeutralSpots();
    	
    	float fMinWeight = Float.MAX_VALUE;
    	Wreck bestFeature = null;
    	
    	for(MetalSpot ms:metalSpots){
	    		float weight = GraphManager.groundDistance(ms.getPosition(), mypos) + ((ms.getColonists().size()-1) * 1200);
			//weight += weight * (1 + warManager.getThreat(ms.getPosition()));
	    		if (weight < minWeight){
	    			spot = ms;
					minWeight = weight;
	    		}
    	}

		if (effectiveIncomeEnergy > 20){
			for(Wreck f:features){
				float reclaimValue = f.reclaimValue;
				if(reclaimValue > 0){
					float weight = GraphManager.groundDistance(f.position, mypos);
					//weight = weight * (1 + warManager.getThreat(f.position));
					weight += metalQueued;
					if(weight < fMinWeight){
						bestFeature = f;
						fMinWeight = weight;
					}
				}
			}
		}
    	
    	AIFloat3 overDrivePos = graphManager.getOverdriveSweetSpot(mypos);
    	
    	WorkerTask task = null;

		if(minWeight<fMinWeight && spot != null){
			task = createColonizeTask(worker, spot);
			worker.setTask(task);
			spot.addColonist(worker.getUnit());
			return;
		}else if(fMinWeight < minWeight && bestFeature != null){
			task = createReclaimTask(worker, bestFeature);
			worker.setTask(task);;
			return;
		}

    	if((!mypos.equals(overDrivePos)) && Math.min(minWeight, fMinWeight) > Math.pow(GraphManager.groundDistance(mypos,overDrivePos ),4) && effectiveIncomeMetal > 30){
			task = createEnergyTask(worker);
			worker.setTask(task);
			parent.debug("Spamming energy for no reason!");
			return;
    	}

    	// are there damaged nearby ally units?
    	// is it useful to assist?
		
		// final fallback: make even more energy
		if(task == null){
			parent.debug("all hope is lost, spam solars!");
			task = createEnergyTask(worker);
			worker.setTask(task);
		}

		
		return;
    }
    
    ReclaimTask createReclaimTask(Worker worker, Wreck f){
        worker.getUnit().reclaimInArea(f.position,200, (short)0, frame+1000);
    	//worker.getUnit().reclaimFeature(f, (short)0, frame+100);
        ReclaimTask rt =  new ReclaimTask(worker,f);
    	workerTasks.add(rt);
    	return rt;
    }
    
    ProductionTask createUnitTask(Worker worker, String unitname){
    	UnitDef requisite = callback.getUnitDefByName(unitname);
    	int priority = 100;
    	int constructionPriority = 3;
        worker.getUnit().build(requisite, worker.getUnit().getPos(), (short) Math.floor(Math.random()*4), (short) 0, frame + 1000);
        ProductionTask ct =  new ProductionTask(worker,requisite,priority,constructionPriority);
    	workerTasks.add(ct);
    	return ct;
    }
    
    ConstructionTask createRadarTask(Worker worker){
    	UnitDef radar = callback.getUnitDefByName("corrad");
    	AIFloat3 position = worker.getUnit().getPos();
    	
    	MetalSpot closest = graphManager.getClosestNeutralSpot(position);
    	
    	if (GraphManager.groundDistance(closest.getPosition(),position)<100){
    		AIFloat3 mexpos = closest.getPosition();
			float distance = GraphManager.groundDistance(mexpos, position);
			float extraDistance = 100;
			float vx = (position.x - mexpos.x)/distance; 
			float vz = (position.z - mexpos.z)/distance; 
			position.x = position.x+vx*extraDistance;
			position.z = position.z+vz*extraDistance;
    	}
    	
    	position = callback.getMap().findClosestBuildSite(radar,position,600f, 3, 0);
    	
    	int priority = 100;
    	int constructionPriority = 3;
    	
        worker.getUnit().build(radar, position, (short)0, (short) 0, frame+30);
    	ConstructionTask ct =  new ConstructionTask(worker,radar,priority,constructionPriority, position);
    	workerTasks.add(ct);
    	return ct;
    }
    
    ConstructionTask createPorcTask(Worker worker){
		UnitDef porc;
		if (effectiveIncome < 20) {
			porc = callback.getUnitDefByName("corllt");
		}
		else if (Math.random() > 0.9) {
			porc = callback.getUnitDefByName("corhlt");
		}
		else if (Math.random() > 0.85) {
			//faraday
			porc = callback.getUnitDefByName("armartic");
		}
		else if (Math.random() > 0.5) {
			porc = callback.getUnitDefByName("corrl");
		}
		else {
			porc = callback.getUnitDefByName("corllt");
		}
    	AIFloat3 position = worker.getUnit().getPos();
    	
    	MetalSpot closest = graphManager.getClosestNeutralSpot(position);
    	
    	if (GraphManager.groundDistance(closest.getPosition(),position)<100){
    		AIFloat3 mexpos = closest.getPosition();
			float distance = GraphManager.groundDistance(mexpos, position);
			float extraDistance = 100;
			float vx = (position.x - mexpos.x)/distance; 
			float vz = (position.z - mexpos.z)/distance; 
			position.x = position.x+vx*extraDistance;
			position.z = position.z+vz*extraDistance;
    	}
    	
    	position = callback.getMap().findClosestBuildSite(porc,position,600f, 3, 0);
    	
    	int priority = 100;
    	int constructionPriority = 3;
    	
        worker.getUnit().build(porc, position, (short)0, (short) 0, frame+30);
    	ConstructionTask ct =  new ConstructionTask(worker,porc,priority,constructionPriority, position);
    	workerTasks.add(ct);
    	return ct;
    }
    
    ConstructionTask createFactoryTask(Worker worker){
    	UnitDef factory = callback.getUnitDefByName("factorycloak");
    	AIFloat3 position = worker.getUnit().getPos();
    	
    	List<Unit>stuffNearby = callback.getFriendlyUnitsIn(position, 1000);
    	for (Unit u:stuffNearby){
			if(u.getDef().getName().equals("factorycloak")){
				float distance = GraphManager.groundDistance(u.getPos(), position);
				float extraDistance = Math.max(50,1000-distance);
				float vx = (position.x - u.getPos().x)/distance; 
				float vz = (position.z - u.getPos().z)/distance; 
				position.x = position.x+vx*extraDistance;
				position.z = position.z+vz*extraDistance;
			}
    	}
    	
    	MetalSpot closest = graphManager.getClosestNeutralSpot(position);
    	
    	if (GraphManager.groundDistance(closest.getPosition(),position)<100){
    		AIFloat3 mexpos = closest.getPosition();
			float distance = GraphManager.groundDistance(mexpos, position);
			float extraDistance = 100;
			float vx = (position.x - mexpos.x)/distance; 
			float vz = (position.z - mexpos.z)/distance; 
			position.x = position.x+vx*extraDistance;
			position.z = position.z+vz*extraDistance;
    	}
    	
    	position = callback.getMap().findClosestBuildSite(factory,position,600f, 3, 0);
    	
    	int priority = 100;
    	int constructionPriority = 3;
    	
    	short facing = 0;
    	int mapWidth = callback.getMap().getWidth() *8;
    	int mapHeight = callback.getMap().getHeight() *8;
    	
		if (Math.abs(mapWidth - 2*position.x) > Math.abs(mapHeight - 2*position.z)){
			if (2*position.x>mapWidth){
				// facing="west"
				facing=3;
			}else{
				// facing="east"
				facing=1;
			}
		}else{
			if (2*position.z>mapHeight){
				// facing="north"
				facing=2;
			}else{
				// facing="south"
				facing=0;
			}
		}

        worker.getUnit().build(factory, position, facing, (short) 0, frame+30);
    	ConstructionTask ct =  new ConstructionTask(worker,factory,priority,constructionPriority, position);
    	workerTasks.add(ct);
    	return ct;
    }
    
    ConstructionTask createColonizeTask(Worker worker, MetalSpot ms){
    	UnitDef mex = callback.getUnitDefByName("cormex");
    	AIFloat3 position = ms.getPosition();
    	int priority = 100;
    	int constructionPriority = 3;
        worker.getUnit().build(mex, position, (short) Math.floor(Math.random()*4), (short) 0, frame + 1000);
    	ConstructionTask ct =  new ConstructionTask(worker,mex,priority,constructionPriority, position);
    	workerTasks.add(ct);
    	return ct;
    }
    
    ConstructionTask createEnergyTask(Worker worker){
    	
    	// TODO: implement overdrive housekeeping in graphmanager, get buildpos from there 
    	
    	UnitDef solar = callback.getUnitDefByName("armsolar");
		UnitDef fusion = callback.getUnitDefByName("armfus");
		UnitDef singu = callback.getUnitDefByName("cafus");
    	AIFloat3 position = graphManager.getOverdriveSweetSpot(worker.getUnit().getPos());

    	List<Unit>stuffNearby = callback.getFriendlyUnitsIn(position, 400);
    	for (Unit u:stuffNearby){
			if(u.getMaxSpeed() == 0 && u.getDef().getBuildOptions().size()>0){
				float distance = GraphManager.groundDistance(u.getPos(), position);
				float extraDistance = Math.max(50,300-distance);
				float vx = (position.x - u.getPos().x)/distance;
				float vz = (position.z - u.getPos().z)/distance;
				position.x = position.x+vx*extraDistance;
				position.z = position.z+vz*extraDistance;
			}
    	}

		boolean nearFac = false;
		for(Worker w:workers) {
			Unit u = w.getUnit();
			if (u.getMaxSpeed() == 0 && u.getDef().getBuildOptions().size() > 0) {
				float dist = GraphManager.groundDistance(worker.getUnit().getPos(), u.getPos());
				if (dist < 1200 && dist > 450) {
					nearFac = true;
				}
			}
		}
		ConstructionTask ct;
    	int priority = 100;
    	int constructionPriority = 3;

		if (effectiveIncome > 25 && !buildingFusion && fusions.size() < 3 && nearFac){
			position = callback.getMap().findClosestBuildSite(fusion,position,600f, 3, 0);
			worker.getUnit().build(fusion, position, (short) Math.floor(Math.random() * 4), (short) 0, frame + 1000);
			ct = new ConstructionTask(worker, solar, priority, constructionPriority, position);
			workerTasks.add(ct);
			buildingFusion = true;
		}
		else if (effectiveIncome > 30 && !buildingFusion && fusions.size() >= 3 && fusions.size() < 6 && nearFac){
			position = callback.getMap().findClosestBuildSite(singu,position,600f, 3, 0);
			worker.getUnit().build(singu, position, (short) Math.floor(Math.random() * 4), (short) 0, frame + 1000);
			ct = new ConstructionTask(worker, solar, priority, constructionPriority, position);
			workerTasks.add(ct);
			buildingFusion = true;
		}
		else {
			position = callback.getMap().findClosestBuildSite(solar,position,600f, 3, 0);
			worker.getUnit().build(solar, position, (short) Math.floor(Math.random() * 4), (short) 0, frame + 1000);
			ct = new ConstructionTask(worker, solar, priority, constructionPriority, position);
			energyQueued += 2;
			workerTasks.add(ct);
		}
		return ct;
    }
    
    ConstructionTask createNanoTurretTask(Worker worker, Unit target){ 	
    	UnitDef nano = callback.getUnitDefByName("armnanotc");
    	AIFloat3 position = target.getPos();
    	float buildDist = (float) (nano.getBuildDistance() * 0.9);
		double angle = Math.random()*2*Math.PI;
    	
    	double vx = Math.cos(angle);
    	double vz = Math.sin(angle);
    	
    	position.x += buildDist*vx;
    	position.z += buildDist*vz;
    	
    	position = callback.getMap().findClosestBuildSite(nano,position,600f, 3, 0);
    	int priority = 100;
    	int constructionPriority = 3;
        worker.getUnit().build(nano, position, (short) Math.floor(Math.random()*4), (short) 0, frame + 1000);
    	ConstructionTask ct =  new ConstructionTask(worker,nano,priority,constructionPriority, position);
    	workerTasks.add(ct);
    	return ct;
    }
    
	public void setMilitaryManager(MilitaryManager militaryManager) {
		this.warManager = militaryManager;
	}
    
	public void setGraphManager(GraphManager graphManager) {
		this.graphManager = graphManager;
	}
}
