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
	
	float effectiveIncomeMetal = 0;
	float effectiveIncomeEnergy = 0;
	
	float totalIncomeMetal = 0;
	float totalIncomeEnergy = 0;
	float totalBuildpower = 0;
	
	float effectiveIncome = 0;
	float effectiveExpenditure = 0;
	
	float energyQueued = 0;
	float metalQueued = 0;
	float buildpowerQueued = 0;
	
	float metal = 0;
	float energy = 0;
	
	/* hax */
	int numWarriors = 0;
	int numGlaives = 0;
	
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
		
		energyQueued = 0;
		metalQueued = 0;
		
		effectiveIncomeMetal = eco.getIncome(m);
		effectiveIncomeEnergy = eco.getIncome(e);
		
		metal = eco.getCurrent(m);
		energy = eco.getCurrent(e);
		
		float expendMetal = eco.getUsage(m);
		float expendEnergy = eco.getUsage(e);
		
		effectiveIncome= Math.min(effectiveIncomeMetal, effectiveIncomeEnergy);
		effectiveExpenditure = Math.min(expendMetal, expendEnergy);
		
		if(frame%30 == 0){
			List<Feature>feats = callback.getFeatures();
			features = new ArrayList<Wreck>();
			
			for(Feature f:feats){
				features.add(new Wreck(f,f.getDef().getContainedResource(m)));
			}
			
			inventarizeWorkers();
		}
		
		for(Worker w:workers){
			WorkerTask wt = w.getTask();
			if(wt.isCompleted()){
				if(wt instanceof ConstructionTask){
					ConstructionTask ct = (ConstructionTask)wt;
					if(factoryTasks.contains(ct)){
						factoryTasks.remove(ct);
					}
					
					if(radarTasks.contains(ct)){
						radarTasks.remove(ct);
					}
					
					workerTasks.remove(ct);
					assignWorkerTask(w);
				}else {
					workerTasks.remove(wt);
					assignWorkerTask(w);
				}
			}
		}
		
		for(WorkerTask wt:workerTasks){
			if(wt instanceof TemporaryAssistTask){
				TemporaryAssistTask at = (TemporaryAssistTask) wt;
				if(at.getTimeout() < frame){
					at.setCompleted();
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
    	
    	if(defName.equals("corrl")){
    		porcs.add(unit);
    	}
    	
    	if(defName.equals("armcom1") || defName.equals("commbasic") || (unit.getMaxSpeed()>0 && def.getBuildSpeed()>=10)){
    		ArrayList<Float> params = new ArrayList<Float>();
    		params.add((float) 2);
    		unit.executeCustomCommand(CMD_PRIORITY, params, (short)0, parent.currentFrame);
    	}
    	
		if(defName.equals("armpw")){
			numGlaives--;
		}
		
		if(defName.equals("armwar")){
			numWarriors--;
		}
    	
		if(defName.equals("factorycloak")){
			factories.add(unit.getUnitId());
			unit.setMoveState(2, (short) 0, frame+10);
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
    	
    	Worker deadWorker = null;
	    for (Worker worker : workers) {
	    	if(worker.getUnit().getUnitId() == unit.getUnitId()){
	    		deadWorker = worker;
	    		WorkerTask wt = worker.getTask();
	    		workerTasks.remove(wt);
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
        return 0;
    }
    
    private String getRandomAttacker(){
    	
    	if(numWarriors == 0){
    		return "armwar";
    	}

    	if(numGlaives == 0){
    		return "armpw";
    	}
    	
    	
    	int index = (int) Math.floor(Math.random()*attackers.size());
    	
    	if(Math.random()*20 < 1 && this.frame > 1000){
    		return "armsnipe";
    	}
    	
    	if(Math.random()*30 < 1 && this.frame > 1000){
    		return "spherepole";
    	}
    	
    	return attackers.get(index);
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
			}else{
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
	    		if(totalBuildpower < effectiveIncome  || 2+effectiveExpenditure+totalBuildpower*0.1 < effectiveIncome || Math.random() < 0.1){
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
    	if (factories.size() == 0 || effectiveIncome / factories.size() > 25){
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
    	if(effectiveIncomeMetal*1.1 +1 > effectiveIncomeEnergy + energyQueued/2 || energy < 10){
			ConstructionTask task = createEnergyTask(worker);
			worker.setTask(task);
			return;
    	}
    	
    	// ponder building a nanoturret
    	if(effectiveExpenditure+totalBuildpower*0.2 < effectiveIncome || effectiveIncome > 10  && metal>200){
    		for(Worker w:workers){
    			Unit u = w.getUnit();
    			if(u.getMaxSpeed() == 0 && u.getDef().getBuildOptions().size()>0){
    				float dist = GraphManager.groundDistance(worker.getUnit().getPos(),u.getPos());
    				if (dist<1000){
    					ConstructionTask task = createNanoTurretTask(worker,u);
    					worker.setTask(task);
    					return;
    				}
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
		
		if(porcdist > 1200 && Math.random() > 0.5){
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
    	
    	if(closestRadarDistance > 2100 && radarTasks.size() == 0){
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
	    		float weight = GraphManager.groundDistance(ms.getPosition(), mypos)/(ms.getValue()+0.001f);
        		weight += weight * warManager.getThreat(ms.getPosition()); 
	    		weight += weight*ms.getColonists().size();
	    		if (weight < minWeight){
	    			spot = ms;
	    			minWeight = weight;
	    		}
    	}

    	for(Wreck f:features){
    		float reclaimValue = f.reclaimValue;
    		if(reclaimValue > 0){
    			float weight = (float) Math.pow(GraphManager.groundDistance(f.position, mypos),1.6);
    			weight /= Math.min(f.reclaimValue * f.feature.getReclaimLeft()+1,100);
        		weight *= 1 + warManager.getThreat(f.position); 
        		weight += metalQueued;
        		if(weight < fMinWeight){
        			bestFeature = f;
        			fMinWeight = weight;
        		}
    		}
    	} 
    	
    	AIFloat3 overDrivePos = graphManager.getOverdriveSweetSpot(mypos);
    	
    	WorkerTask task = null;
    	
    	if((!mypos.equals(overDrivePos)) && Math.min(minWeight, fMinWeight) > Math.pow(GraphManager.groundDistance(mypos,overDrivePos ),4)){
    		if(task == null){
    			task = createEnergyTask(worker);
    			worker.setTask(task);
    		}
	
    		return;
    	}

		if(minWeight<fMinWeight){
			task = createColonizeTask(worker, spot);
			worker.setTask(task);
			spot.addColonist(worker.getUnit());
		}else if(fMinWeight < minWeight){
			task = createReclaimTask(worker, bestFeature);
			worker.setTask(task);;
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
    	UnitDef radar = callback.getUnitDefByName("corrl");
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
    	AIFloat3 position = graphManager.getOverdriveSweetSpot(worker.getUnit().getPos());

    	List<Unit>stuffNearby = callback.getFriendlyUnitsIn(position, 300);
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
    	
    	position = callback.getMap().findClosestBuildSite(solar,position,600f, 3, 0);

    	int priority = 100;
    	int constructionPriority = 3;
        worker.getUnit().build(solar, position, (short) Math.floor(Math.random()*4), (short) 0, frame + 1000);
    	ConstructionTask ct =  new ConstructionTask(worker,solar,priority,constructionPriority, position);
    	energyQueued += 2;
    	workerTasks.add(ct);
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
