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
	List<Worker> factories;
	List<ConstructionTask> factoryTasks; // for constructors building factories
	List<ProductionTask> facTasks; // for factories building units
	List<ConstructionTask> radarTasks;
	List<WorkerTask> workerTasks;
	List<Wreck> features;
	List<Wreck> invalidWrecks;
	List<Unit> radars;
	List<Unit> porcs;
	List<Unit> nanos;
	List<Unit> fusions;
	List<Unit> mexes;
	List<Unit> solars;
	List<Unit> pylons;
	List<Worker> idlers;
	
	float effectiveIncomeMetal = 0;
	float effectiveIncomeEnergy = 0;

	float totalBuildpower = 0;

	boolean buildingFusion = false;
	
	float effectiveIncome = 0;
	float effectiveExpenditure = 0;
	
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
		this.factories = new ArrayList<Worker>();
		this.factoryTasks = new ArrayList<ConstructionTask>();
		this.radarTasks = new ArrayList<ConstructionTask>();
		this.workerTasks = new ArrayList<WorkerTask>();
		this.features = new ArrayList<Wreck>();
		this.invalidWrecks = new ArrayList<Wreck>();
		this.radars = new ArrayList<Unit>();
		this.porcs = new ArrayList<Unit>();
		this.nanos = new ArrayList<Unit>();
		this.fusions = new ArrayList<Unit>();
		this.mexes = new ArrayList<Unit>();
		this.solars = new ArrayList<Unit>();
		this.pylons = new ArrayList<Unit>();
		this.facTasks = new ArrayList<ProductionTask>();
		this.idlers = new ArrayList<Worker>();

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
	public int update(int frame) {
		this.frame = frame;

		effectiveIncomeMetal = eco.getIncome(m);
		effectiveIncomeEnergy = eco.getIncome(e);

		metal = eco.getCurrent(m);
		energy = eco.getCurrent(e);

		float expendMetal = eco.getUsage(m);
		float expendEnergy = eco.getUsage(e);

		effectiveIncome = Math.min(effectiveIncomeMetal, effectiveIncomeEnergy);
		effectiveExpenditure = Math.min(expendMetal, expendEnergy);

		if (frame % 30 == 0) {
			List<Feature> feats = callback.getFeatures();
			features = new ArrayList<Wreck>();

			for (Feature f : feats) {
				features.add(new Wreck(f, f.getDef().getContainedResource(m)));
			}
		}

		// check idlers, since they might not actually be idle.
		for (Worker i : idlers){
			int orders = i.getUnit().getCurrentCommands().size();
			if (orders == 0){
				i.getTask().setCompleted();
			}
		}

		// reset the idle list.
		idlers = new ArrayList<>();

		// randomly stop workers from walking to distant jobs
		// so that they can find something more productive to do nearby.
		/*if (effectiveIncome > 18){
			stochasticWalk();
		}*/

		if (frame % 15 == 0) {
			// assign workers to jobs.
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
					}
					workerTasks.remove(wt);
					assignWorkerTask(w);
				}
			}

			for (Worker f:factories){
				WorkerTask wt = f.getTask();
				int orders = f.getUnit().getCurrentCommands().size();
				// facs don't seem to idle, so checking their orders is
				// the only way to know if they need reassignment.
				if (wt.isCompleted() || orders == 0) {
					facTasks.remove(wt);
					assignFactoryTask(f);
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

			for (Worker w : workers) {
				Unit u = w.getUnit();
				if (u.getMaxSpeed() > 0 && effectiveIncome > 30) {
					ArrayList<Float> params = new ArrayList<>();
					params.add((float) 1);
					u.executeCustomCommand(CMD_PRIORITY, params, (short) 0, parent.currentFrame);
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
    	
    	if(defName.equals("armcom1") || defName.equals("commbasic") || (unit.getMaxSpeed()>0 && def.getBuildSpeed()>=10)){
    		ArrayList<Float> params = new ArrayList<Float>();
    		params.add((float) 2);
    		unit.executeCustomCommand(CMD_PRIORITY, params, (short)0, parent.currentFrame);
    	}

    	if (unit.getMaxSpeed() > 0) {
			checkWorker(unit);
		}
    	
    	for (WorkerTask wt:workerTasks){
    		if(wt instanceof ConstructionTask){
    			ConstructionTask ct = (ConstructionTask)wt;
    			if (ct.building != null){
	    			if(ct.building.getUnitId() == unit.getUnitId()){
	    				ct.setCompleted();
						if(ct.buildType.getName().equals("cormex")){
							graphManager.getClosestMetalSpot(ct.location).clearColonists();
						}
	    			}
    			}
    		}
    	}
		for (WorkerTask wt:facTasks){
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
		mexes.remove(unit);
		solars.remove(unit);
		pylons.remove(unit);

		// If it was a building under construction, reset the builder's target
		if(unit.getMaxSpeed() == 0){
			for (WorkerTask wt:workerTasks){
				if(wt instanceof ConstructionTask){
					ConstructionTask ct = (ConstructionTask)wt;
					if (ct.getBuilding().getUnitId() == unit.getUnitId()){
						ct.setBuilding(null);
					}
				}
			}
		}

		// if we have a dead worker or factory, remove them and their tasks.
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
				if(radarTasks.contains(wt)){
					radarTasks.remove(wt);
				}
	    	}
	    }
		for (Worker fac : factories) {
			if (fac.getUnit().getUnitId() == unit.getUnitId()) {
				deadWorker = fac;
				WorkerTask ft = fac.getTask();
				facTasks.remove(ft);
			}
		}
	    if (deadWorker != null){
	    	workers.remove(deadWorker);
			factories.remove(deadWorker);
	    }

	    for(ConstructionTask ct:factoryTasks){
	    	if(ct.building != null && ct.building.getUnitId() == unit.getUnitId()){
				factoryTasks.remove(ct);
				radarTasks.remove(ct);
	    	}
	    }
	    radars.remove(unit);
        return 0; // signaling: OK
    }

    @Override
    public int unitIdle(Unit unit) {
	    for (Worker worker : workers) {
	    	if(worker.getUnit().getUnitId() == unit.getUnitId()){
				idlers.add(worker);
	    	}
	    } 
        return 0; // signaling: OK
    }
    
    @Override
    public int unitCreated(Unit unit, Unit builder) {
    	if(builder != null && unit.isBeingBuilt()){
			if(unit.getMaxSpeed() == 0){
		    	for (WorkerTask wt:workerTasks){
		    		if(wt instanceof ConstructionTask){
		    			ConstructionTask ct = (ConstructionTask)wt;
		    			if (ct.getWorker().getUnit().getUnitId() == builder.getUnitId()){
		    				ct.setBuilding(unit);
		    			}
		    		}
		    	}
			}
    	} else if (builder != null && !unit.isBeingBuilt()){
			// instant factory plops only call unitcreated, not unitfinished.
			for (Worker w : workers){
				if (w.getUnit().getUnitId() == builder.getUnitId()){
					w.getTask().setCompleted();
				}
			}
		}

		if (unit.getMaxSpeed() == 0 && unit.getDef().getBuildOptions().size() > 0) {
			checkWorker(unit);
		}
		String defName = unit.getDef().getName();

		if(defName.equals("corrl") || defName.equals("corllt") || defName.equals("corhlt") || defName.equals("armartic")){
			porcs.add(unit);
		}

		if (defName.equals("armnanotc")){
			nanos.add(unit);
			AIFloat3 shiftedPosition = unit.getPos();
			shiftedPosition.x+=30;
			shiftedPosition.z+=30;
			unit.patrolTo(shiftedPosition, (short) 0, frame + 900000);
			unit.setRepeat(true, (short) 0, 900000);
		}

		if(defName.equals("cormex")){
			mexes.add(unit);
		}

		if(defName.equals("armsolar")){
			solars.add(unit);
		}

		if(defName.equals("armestor")){
			pylons.add(unit);
		}

		if (defName.equals("armfus")){
			fusions.add(unit);
		}

		if (defName.equals("cafus")){
			fusions.add(unit);
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
			else if (rand > 0.4){
				return "armzeus";
			}
			else if (rand > 0.2) {
				return "armwar";
			}
			else if (rand > 0.05){
				return "spherepole";
			}
			else if (fusions.size() > 0){
				return "armsnipe";
			}
			else{
				return "spherepole";
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

    void checkWorker(Unit unit){
		UnitDef def = unit.getDef();
		if (def.isBuilder()){
			if(def.getName().contains("factory")){
				factories.add(new Worker(unit));
				unit.setMoveState(2, (short) 0, frame+10);
			}
			else if (unit.getMaxSpeed() > 0){
				workers.add(new Worker(unit));
				numWorkers++;
				if (unit.getMaxSpeed() > 0 && effectiveIncome < 30) {
					ArrayList<Float> params = new ArrayList<Float>();
					params.add((float) 2);
					unit.executeCustomCommand(CMD_PRIORITY, params, (short) 0, parent.currentFrame);
				}
			}
		}
    }

	void assignFactoryTask(Worker fac){
		ProductionTask ptask;
		if((float) numWorkers < Math.floor(((effectiveIncome+2)/5))) {
			ptask = createUnitTask(fac, "armrectr");
			facTasks.add(ptask);
			fac.setTask(ptask);
		}else{
			ptask = createUnitTask(fac, getRandomAttacker());
			facTasks.add(ptask);
			fac.setTask(ptask);
		}
		return;
	}

    void assignWorkerTask(Worker worker){
		ConstructionTask task = null;
		AIFloat3 position = worker.getUnit().getPos();

    	// is there a factory? a queued one maybe?
    	if (factories.size() == 0 || effectiveIncome / factories.size() > 80){
    		if(factoryTasks.size() == 0){
    			task = createFactoryTask(worker);
    			factoryTasks.add(task);
    			worker.setTask(task);
    			return;
    		}
    	}

		//Don't build crap right in front of the fac.
		boolean tooCloseToFac = false;
		for(Worker w:factories){
			Unit u = w.getUnit();
			float dist = distance(position,u.getPos());
			if (dist<200){
				tooCloseToFac = true;
			}
		}

		// do we need defense?
		if (needPorc(position) && effectiveIncomeMetal > 10 && !tooCloseToFac){
			task = createPorcTask(worker);
			worker.setTask(task);
			return;
		}

    	// is there sufficient energy to cover metal income?
		if (Math.floor(mexes.size() * 1.2) > solars.size() || effectiveIncome > 15 && metal > 150 || (effectiveIncome > 35 && canBuildFusion(position))) {
			task = createEnergyTask(worker);
			if (task != null) {
				worker.setTask(task);
				return;
			}
		}

    	
    	// do we need caretakers?
    	if((float)nanos.size() < Math.floor(effectiveIncomeMetal/5)-2 && factories.size() > 0){
    		if (factories.size() == 1) {
				for (Worker f : factories) {
					float dist = distance(position, f.getUnit().getPos());
					if (dist < 600) {
						task = createNanoTurretTask(worker, f.getUnit());
						if (task != null) {
							worker.setTask(task);
							return;
						}
					}
				}
			}
			else{
				Worker target = getCaretakerTarget();
				task = createNanoTurretTask(worker, target.getUnit());
				worker.setTask(task);
				return;
			}
    	}

		// do we need radar?
		if (needRadar(position) && effectiveIncomeMetal > 10 && !tooCloseToFac){
    		task = createRadarTask(worker);
    		radarTasks.add(task);
    		worker.setTask(task);
    		return;
    	}

		// maybe find the closest expensive job and assist it.
		if (effectiveIncome > 25 && Math.random() > 0.66) {
			float dist = Float.MAX_VALUE;
			for (WorkerTask t : workerTasks) {
				if (t instanceof ConstructionTask){
					ConstructionTask ct = ((ConstructionTask) t);
					UnitDef def = ct.buildType;
					float cost = def.getCost(m);
					AIFloat3 pos = ct.location;
					float tmpdist = distance(position, pos);
					if (cost > 199 && tmpdist < dist){
						dist = tmpdist;
						task = ct;
					}
				}
			}
			if (task != null && dist < 1200){
				worker.getUnit().build(task.buildType,task.location,(short) 0, (short) 0, frame+1000);
				worker.setTask(task);
				return;
			}
		}

		// do we need pylons?
		if (needGrid(position) && fusions.size() > 1 && !tooCloseToFac){
			task = createGridTask(worker);
			if (task != null) {
				worker.setTask(task);
				return;
			}
		}

    	// are there uncolonized metal spots? or is reclaim more worth it?
    	// or maybe just overdrive more
    	float minWeight = Float.MAX_VALUE;
    	MetalSpot spot = null;
    	List<MetalSpot> metalSpots = graphManager.getNeutralSpots();
    	
    	float fMinWeight = Float.MAX_VALUE;
    	Wreck bestFeature = null;
    	
    	for(MetalSpot ms:metalSpots){
	    		float weight = distance(ms.getPosition(), position) + ((ms.getColonists().size()-1) * 1200);
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
					float weight = distance(f.position, position);
					//weight = weight * (1 + warManager.getThreat(f.position));
					weight *= 2;
					if(weight < fMinWeight){
						bestFeature = f;
						fMinWeight = weight;
					}
				}
			}
		}

		if(minWeight<fMinWeight && spot != null){
			task = createColonizeTask(worker, spot);
			worker.setTask(task);
			spot.addColonist(worker);
			return;
		}else if(bestFeature != null){
			ReclaimTask rtask = createReclaimTask(worker, bestFeature);
			worker.setTask(rtask);;
			return;
		}

    	// are there damaged nearby ally units?
    	// is it useful to assist?
		
		// final fallback: make even more energy
		parent.debug("all hope is lost, spam solars!");
		task = createEnergyTask(worker);
		worker.setTask(task);
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
        worker.getUnit().build(requisite, worker.getUnit().getPos(), (short) 0, (short) 0, frame + 1000);
        ProductionTask ct =  new ProductionTask(worker,requisite,priority,constructionPriority);
    	workerTasks.add(ct);
    	return ct;
    }
    
    ConstructionTask createRadarTask(Worker worker){
    	UnitDef radar = callback.getUnitDefByName("corrad");
    	AIFloat3 position = worker.getUnit().getPos();
    	
    	MetalSpot closest = graphManager.getClosestNeutralSpot(position);

		if (distance(closest.getPosition(),position)<100){
    		AIFloat3 mexpos = closest.getPosition();
			float distance = distance(mexpos, position);
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
		double rand = Math.random();
		if (effectiveIncome < 20) {
			porc = callback.getUnitDefByName("corllt");
		}
		else if (effectiveIncome > 40 && rand > 0.9){
			porc = callback.getUnitDefByName("corhlt");
		}
		else if (rand > 0.33) {
			porc = callback.getUnitDefByName("corrl");
		}
		else {
			porc = callback.getUnitDefByName("corllt");
		}
    	AIFloat3 position = worker.getUnit().getPos();
    	
    	MetalSpot closest = graphManager.getClosestNeutralSpot(position);

		if (distance(closest.getPosition(),position)<100){
    		AIFloat3 mexpos = closest.getPosition();
			float distance = distance(mexpos, position);
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
    	ConstructionTask ct =  new ConstructionTask(worker,porc,priority,constructionPriority,position);
    	workerTasks.add(ct);
    	return ct;
    }
    
    ConstructionTask createFactoryTask(Worker worker){
    	UnitDef factory = callback.getUnitDefByName("factorycloak");
    	AIFloat3 position = worker.getUnit().getPos();
		position.x = position.x+120;
		position.z = position.z+120;

    	
    	MetalSpot closest = graphManager.getClosestNeutralSpot(position);

		if (distance(closest.getPosition(),position)<100){
    		AIFloat3 mexpos = closest.getPosition();
			float distance = distance(mexpos, position);
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

	Boolean needPorc(AIFloat3 position){
		float porcdist = Float.MAX_VALUE;
		for(Unit u:porcs){
			float dist = distance(position,u.getPos());
			if (dist<porcdist){
				porcdist = dist;
			}
		}

		int minporcdist = 300;
		if (warManager.getThreat(position) > 0.7){
			minporcdist = minporcdist-150;
		}
		if (effectiveIncome > 20){
			minporcdist = minporcdist-100;
		}

		if(porcdist > minporcdist){
			return true;
		}
		return false;
	}

	Boolean needRadar(AIFloat3 position){
		float closestRadarDistance = Float.MAX_VALUE;
		for(Unit r:radars){
			float distance = distance(r.getPos(),position);
			if(distance < closestRadarDistance){
				closestRadarDistance = distance;
			}
		}

		if(closestRadarDistance > 1500 && radarTasks.size() == 0){
			return true;
		}
		return false;
	}

	Boolean needGrid(AIFloat3 position){
		float gdist = Float.MAX_VALUE;
		for(Unit u:pylons){
			float dist = distance(position,u.getPos());
			if (dist<gdist){
				gdist = dist;
			}
		}

		if(gdist > 500){
			return true;
		}
		return false;
	}

	Boolean canBuildFusion(AIFloat3 position){
		Boolean tooFar = false;
		for (Worker f:factories){
			float dist = distance(position, f.getUnit().getPos());
			if (dist > 2000){
				tooFar = true;
			}
		}
		if (!tooFar && fusions.size() < (effectiveIncome-30)/10){
			return true;
		}
		return false;
	}

    ConstructionTask createColonizeTask(Worker worker, MetalSpot ms){
    	UnitDef mex = callback.getUnitDefByName("cormex");
    	AIFloat3 position = ms.getPosition();
    	int priority = 100;
    	int constructionPriority = 3;
        worker.getUnit().build(mex, position, (short) 0, (short) 0, frame + 1000);
    	ConstructionTask ct =  new ConstructionTask(worker,mex,priority,constructionPriority, position);
    	workerTasks.add(ct);
    	return ct;
    }
    
    ConstructionTask createEnergyTask(Worker worker){
    	
    	// TODO: implement overdrive housekeeping in graphmanager, get buildpos from there 
    	
    	UnitDef solar = callback.getUnitDefByName("armsolar");
		UnitDef fusion = callback.getUnitDefByName("armfus");
		UnitDef singu = callback.getUnitDefByName("cafus");
    	AIFloat3 position;
		AIFloat3 fpos = new AIFloat3();

    	/*List<Unit>stuffNearby = callback.getFriendlyUnitsIn(position, 400);
    	for (Unit u:stuffNearby){
			if(u.getMaxSpeed() == 0 && u.getDef().getBuildOptions().size()>0){
				float distance = distance(u.getPos(), position);
				float extraDistance = Math.max(50,300-distance);
				float vx = (position.x - u.getPos().x)/distance;
				float vz = (position.z - u.getPos().z)/distance;
				position.x = position.x+vx*extraDistance;
				position.z = position.z+vz*extraDistance;
			}
    	}*/

		// find the nearest fac
		Float dist = Float.MAX_VALUE;
		Worker nearestFac = null;
		for (Worker f:factories){
			float tdist = distance(worker.getUnit().getPos(), f.getUnit().getPos());
			if (tdist < dist && tdist < 2000){
				dist = tdist;
				nearestFac = f;
			}
		}

		if (nearestFac != null){
			fpos = nearestFac.getUnit().getPos();
			float radius = 400;
			double angle = Math.random()*2*Math.PI;

			double vx = Math.cos(angle);
			double vz = Math.sin(angle);

			fpos.x += radius*vx;
			fpos.z += radius*vz;
		}


		ConstructionTask ct;
    	int priority = 100;
    	int constructionPriority = 3;

		if (effectiveIncome > 30 && fusions.size() < 3 && nearestFac != null && fusions.size() < Math.floor((effectiveIncome-30)/10)){
			position = graphManager.getOverdriveSweetSpot(fpos);
			position = callback.getMap().findClosestBuildSite(fusion,position,600f, 3, 0);
			worker.getUnit().build(fusion, position, (short) 0, (short) 0, frame + 1000);
			ct = new ConstructionTask(worker, solar, priority, constructionPriority, position);
			workerTasks.add(ct);
			buildingFusion = true;
		}
		else if (effectiveIncome > 40 && fusions.size() >= 3 && fusions.size() < 8 && nearestFac != null && fusions.size() < Math.floor((effectiveIncome-40)/15)){
			position = graphManager.getOverdriveSweetSpot(fpos);
			position = callback.getMap().findClosestBuildSite(singu,position,600f, 3, 0);
			worker.getUnit().build(singu, position, (short) 0, (short) 0, frame + 1000);
			ct = new ConstructionTask(worker, solar, priority, constructionPriority, position);
			workerTasks.add(ct);
			buildingFusion = true;
		}
		else {
			// we need to count nearby solars to avoid a solar parking lot.
			List<Unit> nearUnits = callback.getFriendlyUnitsIn(worker.getUnit().getPos(), 600);
			int numSolars = 0;
			for (Unit u : nearUnits){
				if (u.getDef().getName().equals("armsolar")){
					numSolars++;
				}
			}
			if (numSolars > 6){
				return null;
			}

			position = graphManager.getOverdriveSweetSpot(worker.getUnit().getPos());
			position = callback.getMap().findClosestBuildSite(solar,position,600f, 3, 0);
			worker.getUnit().build(solar, position, (short) 0, (short) 0, frame + 1000);
			ct = new ConstructionTask(worker, solar, priority, constructionPriority, position);
			workerTasks.add(ct);
		}
		return ct;
    }

	ConstructionTask createGridTask(Worker worker){
		ConstructionTask ct;
		int priority = 100;
		int constructionPriority = 3;
		UnitDef pylon = callback.getUnitDefByName("armestor");
		AIFloat3 position = graphManager.getOverdriveSweetSpot(worker.getUnit().getPos());
		position = callback.getMap().findClosestBuildSite(pylon,position,600f, 3, 0);

		// check the build site for existing pylons, since getOverdriveSweetSpot may cluster them.
		float gdist = Float.MAX_VALUE;
		for(Unit u:pylons){
			float dist = distance(position,u.getPos());
			if (dist<gdist){
				gdist = dist;
			}
		}

		if(gdist > 500) {
			worker.getUnit().build(pylon, position, (short) 0, (short) 0, frame + 1000);
			ct = new ConstructionTask(worker, pylon, priority, constructionPriority, position);
			workerTasks.add(ct);
			return ct;
		}
		return null;
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
        worker.getUnit().build(nano, position, (short) 0, (short) 0, frame + 1000);
    	ConstructionTask ct =  new ConstructionTask(worker,nano,priority,constructionPriority, position);
    	workerTasks.add(ct);
    	return ct;
    }

	Worker getCaretakerTarget(){
		Worker target = null;
		int ctCount = 9001;
		for (Worker f:factories){
			// Try to spread caretakers evenly between facs and to catch singus.
			List<Unit> nearUnits = callback.getFriendlyUnitsIn(f.getUnit().getPos(), 800);
			int numCT = 0;
			for (Unit u : nearUnits){
				if (u.getDef().getName().equals("armnanotc")){
					numCT++;
				}
			}
			if (numCT < ctCount){
				target = f;
				ctCount = numCT;
			}
		}
		return target;
	}

	/*void stochasticWalk(){
		for (Worker w: workers){
			WorkerTask wt = w.getTask();
			if (!wt.isCompleted()){
				if (wt instanceof ConstructionTask){
					ConstructionTask ct = (ConstructionTask) wt;
					AIFloat3 pos1 = w.getUnit().getPos();
					AIFloat3 pos2 = ct.location;
					if (distance(pos1, pos2) > 600 && Math.random() > 0.99){
						w.getUnit().stop((short) 0, frame+60);
						ct.setCompleted();
					}
				} else if (wt instanceof ReclaimTask){
					ReclaimTask rt = (ReclaimTask) wt;
					AIFloat3 pos1 = w.getUnit().getPos();
					AIFloat3 pos2 = rt.location;
					if (distance(pos1, pos2) > 600 && Math.random() > 0.99){
						w.getUnit().stop((short) 0, frame+60);
						rt.setCompleted();
					}
				}
			}
		}
	}*/

	float distance(AIFloat3 pos1, AIFloat3 pos2){
		float x1 = pos1.x;
		float z1 = pos1.z;
		float x2 = pos2.x;
		float z2 = pos2.z;
		return (float) Math.sqrt((x1-x2)*(x1-x2)+(z1-z2)*(z1-z2));
	}
    
	public void setMilitaryManager(MilitaryManager militaryManager) {
		this.warManager = militaryManager;
	}
    
	public void setGraphManager(GraphManager graphManager) {
		this.graphManager = graphManager;
	}
}
