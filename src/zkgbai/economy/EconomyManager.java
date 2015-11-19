package zkgbai.economy;

import java.util.ArrayList;
import java.util.List;

import com.springrts.ai.oo.clb.*;
import zkgbai.Module;
import zkgbai.ZKGraphBasedAI;
import zkgbai.economy.tasks.*;
import zkgbai.economy.tasks.RepairTask;
import zkgbai.graph.GraphManager;
import zkgbai.graph.MetalSpot;
import zkgbai.military.MilitaryManager;

import com.springrts.ai.oo.AIFloat3;

public class EconomyManager extends Module {
	ZKGraphBasedAI parent;
	List<Worker> workers;
	List<Worker> factories;
	List<Worker> assigned;
	List<Worker> commanders;
	List<ConstructionTask> factoryTasks; // for constructors building factories
	List<ConstructionTask> radarTasks;
	List<ConstructionTask> constructionTasks;
	List<ReclaimTask> reclaimTasks;
	List<RepairTask> repairTasks;
	List<ConstructionTask> solarTasks;
	List<ConstructionTask> fusionTasks;
	List<ConstructionTask> pylonTasks;
	List<ConstructionTask> porcTasks;
	List<ConstructionTask> nanoTasks;
	List<ConstructionTask> AATasks;
	List<Unit> radars;
	List<Unit> porcs;
	List<Unit> nanos;
	List<Unit> fusions;
	List<Unit> mexes;
	List<Unit> solars;
	List<Unit> pylons;
	List<Unit> AAs;
	
	float effectiveIncomeMetal = 0;
	float effectiveIncomeEnergy = 0;
	
	public float effectiveIncome = 0;
	float effectiveExpenditure = 0;
	
	float metal = 0;
	float energy = 0;

	int numWorkers = 0;
	int numFighters = 0;
	int numErasers = 0;

	boolean enemyHasAir = false;
	
	int frame = 0;
	static int CMD_PRIORITY = 34220;
	static int CMD_MORPH = 31210;
	static int CMD_MISC_PRIORITY = 34221;
	
	Economy eco;
	Resource m;
	Resource e;
	
	private int myTeamID;
	private  int myAllyTeamID;
	private OOAICallback callback;
	private GraphManager graphManager;
	private MilitaryManager warManager;
	private ArrayList<String> attackers;
	
	public EconomyManager( ZKGraphBasedAI parent){
		this.parent = parent;
		this.callback = parent.getCallback();
		this.myTeamID = parent.teamID;
		this.myAllyTeamID = parent.allyTeamID;
		this.workers = new ArrayList<Worker>();
		this.commanders = new ArrayList<Worker>();
		this.factories = new ArrayList<Worker>();
		this.assigned = new ArrayList<Worker>();
		this.factoryTasks = new ArrayList<ConstructionTask>();
		this.radarTasks = new ArrayList<ConstructionTask>();
		this.constructionTasks = new ArrayList<ConstructionTask>();
		this.reclaimTasks = new ArrayList<ReclaimTask>();
		this.repairTasks = new ArrayList<RepairTask>();
		this.solarTasks = new ArrayList<ConstructionTask>();
		this.fusionTasks = new ArrayList<ConstructionTask>();
		this.pylonTasks = new ArrayList<ConstructionTask>();
		this.porcTasks = new ArrayList<ConstructionTask>();
		this.nanoTasks = new ArrayList<ConstructionTask>();
		this.AATasks = new ArrayList<ConstructionTask>();
		this.radars = new ArrayList<Unit>();
		this.porcs = new ArrayList<Unit>();
		this.nanos = new ArrayList<Unit>();
		this.fusions = new ArrayList<Unit>();
		this.mexes = new ArrayList<Unit>();
		this.solars = new ArrayList<Unit>();
		this.pylons = new ArrayList<Unit>();
		this.AAs = new ArrayList<Unit>();

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
			captureMexes();
			if (effectiveIncome > 22) {
				collectReclaimables();
			}
			if (effectiveIncome > 10){
				defendMexes();
			}
			cleanOrders();
			cleanWorkers();
			setPriorities();
		}

		if (frame % 300 == 0){
			assignNanos();
		}

		if (frame % 3 == 0) {
			//create new building tasks.
			for ( Worker w : workers) {
				if (!assigned.contains(w)) {
					createWorkerTask(w);
					break;
				}
			}

			assignWorkers(); // assign workers to tasks
		}
		return 0;
	}
	
	@Override
    public int init(int teamId, OOAICallback callback) {
        return 0;
    }
	
    @Override
    public int  unitFinished( Unit unit) {
    	
    	UnitDef def = unit.getDef(); 
    	String defName = def.getName();

    	if(defName.equals("corrad")){
    		radars.add(unit);
    	}

		if (defName.equals("spherecloaker")){
			numErasers++;
		}

    	if (unit.getMaxSpeed() > 0) {
			checkWorker(unit);
		}

		 ConstructionTask finished = null;
    	for (ConstructionTask ct:constructionTasks){
			if (ct.target != null){
				if(ct.target.getUnitId() == unit.getUnitId()){
					finished = ct;
					ct.stopWorkers();
				}
			}
		}
		constructionTasks.remove(finished);
		solarTasks.remove(finished);
		pylonTasks.remove(finished);
		fusionTasks.remove(finished);
		porcTasks.remove(finished);
		nanoTasks.remove(finished);
		factoryTasks.remove(finished);
		AATasks.remove(finished);

		return 0;
    }

	@Override
	public int unitDamaged( Unit h, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzed){
		// add repair tasks for damaged units
		RepairTask task = new RepairTask(h);
		if (!repairTasks.contains(task)){
			repairTasks.add(task);
		}


		//chicken workers
		for (Worker w: workers)
		if (w.id == h.getUnitId() && h.getHealth()/h.getMaxHealth() < 0.6){
			if (w.getTask() != null) {
				w.getTask().removeWorker(w);
				w.clearTask();
			}
			Worker fac = getNearestFac(h.getPos());
			AIFloat3 pos = getRadialPoint(fac.getPos(), 300f);
			h.moveTo(pos, (short) 0, frame+240);
			w.isChicken = true;
			w.chickenFrame = frame;
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
		AAs.remove(unit);

		// if the unit had a repair task targeting it, remove it
		RepairTask rt = new RepairTask(unit);
		repairTasks.remove(rt);

		if (unit.getMaxSpeed() > 0 && unit.getDef().isAbleToAttack()){
			numFighters--;
		}

		if (unit.getDef().getName().equals("spherecloaker")){
			numErasers--;
		}

		// If it was a building under construction, reset the builder's target
		if(unit.getMaxSpeed() == 0){
			for ( ConstructionTask ct:constructionTasks){
				if (ct.target != null) {
					if (ct.target.getUnitId() == unit.getUnitId()) {
						ct.target = null;
						ct.stopWorkers();
					}
				}
			}
		}

		// if we have a dead worker or factory, remove them and their tasks.
    	 Worker deadWorker = null;
	    for (Worker worker : workers) {
	    	if(worker.id == unit.getUnitId()){
	    		deadWorker = worker;
	    		WorkerTask wt = worker.getTask();
	    		if (wt != null) {
					wt.removeWorker(worker);
				}
				numWorkers--;
	    	}
	    }
		for ( Worker fac : factories) {
			if (fac.id == unit.getUnitId()) {
				deadWorker = fac;
			}
		}
	    if (deadWorker != null){
	    	workers.remove(deadWorker);
			factories.remove(deadWorker);
	    }
        return 0; // signaling: OK
    }

    @Override
    public int unitIdle(Unit unit) {
	    for (Worker worker : workers) {
	    	if(worker.id == unit.getUnitId()){
				if (worker.getTask() instanceof ReclaimTask){
					 ReclaimTask task = (ReclaimTask) worker.getTask();
					task.stopWorkers();
					reclaimTasks.remove(task);
	    		}else if (worker.id == unit.getUnitId() && worker.getTask() instanceof RepairTask){
					 RepairTask task = (RepairTask) worker.getTask();
					task.stopWorkers();
					repairTasks.remove(task);
				}else if (worker.isChicken){
					worker.isChicken = false;
				}
			}
	    }

		for (Worker f: factories){
			if (f.id == unit.getUnitId()){
				assignFactoryTask(f);
			}
		}
        return 0; // signaling: OK
    }
    
    @Override
    public int unitCreated(Unit unit,  Unit builder) {
    	if(builder != null && unit.isBeingBuilt()){
			if(unit.getMaxSpeed() == 0){
		    	for (Worker w:workers){
		    		if(w.id == builder.getUnitId()){
						if (w.getTask() != null && w.getTask() instanceof ConstructionTask){
							ConstructionTask ct = (ConstructionTask) w.getTask();
							ct.target = unit;
						}else{
							for (ConstructionTask ct:constructionTasks){
								float dist = distance(ct.getPos(), unit.getPos());
								if (dist < 25 && ct.buildType.getName().equals(unit.getDef().getName())){
									ct.target = unit;
								}
							}
						}
		    		}
		    	}
			}
    	} else if (builder != null && !unit.isBeingBuilt()){
			// instant factory plops only call unitcreated, not unitfinished.
			for ( Worker w : workers){
				if (w.id == builder.getUnitId()){
					WorkerTask task = w.getTask();
					constructionTasks.remove(task);
					factoryTasks.remove(task);
					w.clearTask();
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

		if (defName.equals("corrazor")){
			AAs.add(unit);
		}

		if (defName.equals("armnanotc")){
			nanos.add(unit);
			//AIFloat3 shiftedPosition = unit.getPos();
			//shiftedPosition.x+=30;
			//shiftedPosition.z+=30;
			//unit.patrolTo(shiftedPosition, (short) 0, frame + 900000);
			//unit.setRepeat(true, (short) 0, 900000);
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

	@Override
	public int enemyEnterLOS(Unit unit){
		if (unit.getDef().isAbleToFly()){
			enemyHasAir = true;
		}
		return 0;
	}

	private void assignNanos(){
		for (Unit n:nanos){
				Worker fac = getNearestFac(n.getPos());
				n.guard(fac.getUnit(), (short) 0, frame+9001);
				n.setRepeat(true, (short) 0, 900000);
		}
	}

	private void setPriorities(){
		if (effectiveIncome < 30){
			// set constructors at high prio at start, until 30 m/s income
			for (Worker w:workers){
				ArrayList<Float> params = new ArrayList<>();
				params.add((float) 2);
				w.getUnit().executeCustomCommand(CMD_PRIORITY, params, (short) 0, frame+30);
			}
		}else{
			// above 30m/s, set workers to normal prio.
			for (Worker w: workers){
				ArrayList<Float> params = new ArrayList<>();
				params.add((float) 1);
				w.getUnit().executeCustomCommand(CMD_PRIORITY, params, (short) 0, frame+30);
			}
		}
	}

	private Boolean needWorkers(){
		if (((float) numWorkers < Math.floor(((effectiveIncome)/3)) + fusions.size() && numFighters > numWorkers && effectiveIncome > 9 && effectiveIncome < 30)
				|| effectiveIncome > 30 && numFighters > numWorkers) {
			return true;
		}
		return false;
	}
    
    private String getCloaky() {
		if (needWorkers()) {
			return "armrectr";
		}

		if (effectiveIncome < 10) {
			return "armpw";
		}

		if (warManager.raiders.size() < 4 || Math.random() > 0.9) {
			if ((effectiveIncome > 20 && Math.random() > 0.75)
					|| (effectiveIncome > 30 && Math.random() > 0.5)
					|| (effectiveIncome > 40)) {
				return "spherepole";
			} else {
				return "armpw";
			}
		}

		if (effectiveIncome > 40 && energy > 400 && numErasers == 0){
			return "spherecloaker";
		}

		double rand = Math.random();
		if(rand > 0.4){
			return "armzeus";
		}else if (rand > 0.1){
			return "armwar";
		}else if (effectiveIncome > 30 && energy > 400){
			return "armsnipe";
		}else{
			return "armwar";
		}

    }

	private String getShields() {
		if (needWorkers()) {
			return "cornecro";
		}
		double rand = Math.random();
		if (rand > 0.1){
			return "corak";
		}else {
			return "corstorm";
		}
	}

	private String getGunship(){
		if(needWorkers()) {
			return "armca";
		}

		double rand = Math.random();
		if (rand > 0.35){
			return "gunshipsupport";
		}else if (rand > 0.01) {
			return "armbrawl";
		}else{
			return "blackdawn";
		}
	}

	private String getStrider(){
		double rand = Math.random();
		if(rand > 0.75){
			return  "scorpion";
		}else{
			return "dante";
		}
	}

    void checkWorker(Unit unit){
		UnitDef def = unit.getDef();
		if (def.isBuilder()){
			if(def.getName().contains("factory") || def.getName().contains("hub")){
				Worker fac = new Worker(unit);
				factories.add(fac);
				assignFactoryTask(fac);
				unit.setMoveState(2, (short) 0, frame+10);
			}
			else if (unit.getMaxSpeed() > 0){
				Worker w = new Worker(unit);
				workers.add(w);
				numWorkers++;
				if (def.getBuildSpeed() > 8) {
					commanders.add(w);
				}
			}
		}else if (unit.getMaxSpeed() > 0){
			numFighters++;
		}
    }

	void assignFactoryTask( Worker fac){
		UnitDef unit;
		if (fac.getUnit().getDef().getName().equals("factorycloak")){
			unit = callback.getUnitDefByName(getCloaky());
			fac.getUnit().build(unit, fac.getPos(), (short) 0, (short) 0, frame + 1000);
		}else if (fac.getUnit().getDef().getName().equals("factorygunship")){
			unit = callback.getUnitDefByName(getGunship());
			fac.getUnit().build(unit, fac.getPos(), (short) 0, (short) 0, frame + 1000);
		}else if (fac.getUnit().getDef().getName().equals("factoryshield")){
			unit = callback.getUnitDefByName(getShields());
			fac.getUnit().build(unit, fac.getPos(), (short) 0, (short) 0, frame + 1000);
		}else if (fac.getUnit().getDef().getName().equals("striderhub")){
			unit = callback.getUnitDefByName(getStrider());
			AIFloat3 pos = callback.getMap().findClosestBuildSite(unit, fac.getPos(), 250f, 3, 0);
			fac.getUnit().build(unit, pos, (short) 0, (short) 0, frame+1000);
		}

	}

	void assignWorkers() {
		 List<Worker> toAssign = new ArrayList<Worker>();
		int numAssigned = 0;

		// limit the number of workers assigned at one time to prevent super lag.
		for (Worker w : workers) {
			if (!assigned.contains(w) && numAssigned == 0){
				toAssign.add(w);
				assigned.add(w);
				numAssigned++;
			}
		}

		if (numAssigned == 0){
			assigned.clear();
		}

		for ( Worker w: toAssign){
			if (!w.isChicken) {
				 WorkerTask task = getCheapestJob(w);
				WorkerTask wtask = w.getTask();
				if (task != null && !task.equals(wtask)) {
					// remove it from its old assignment if it had one
					if (wtask != null){
						wtask.removeWorker(w);
					}
					if (task instanceof ConstructionTask) {
						 ConstructionTask ctask = (ConstructionTask) task;
						w.getUnit().build(ctask.buildType, ctask.getPos(), ctask.facing, (short) 0, frame + 500);
					} else if (task instanceof ReclaimTask) {
						 ReclaimTask rt = (ReclaimTask) task;
						w.getUnit().reclaimFeature(rt.target, (short) 0, frame + 500);
					} else if (task instanceof RepairTask) {
						 RepairTask rt = (RepairTask) task;
						w.getUnit().repair(rt.target, (short) 0, frame + 500);
					}
					w.setTask(task);
					task.addWorker(w);
				}
			}
		}
	}

	
	WorkerTask getCheapestJob( Worker worker){
		 WorkerTask task = null;
		float cost = Float.MAX_VALUE;

		if (worker.getTask() != null){
			task = worker.getTask();
			cost = costOfJob(worker, task);
		}

		for (WorkerTask t: constructionTasks){
			float tmpcost = costOfJob(worker, t);
			if (tmpcost < cost){
				cost = tmpcost;
				task = t;
			}
		}
		for (WorkerTask t: reclaimTasks){
			float tmpcost = costOfJob(worker, t);
			if (tmpcost < cost){
				cost = tmpcost;
				task = t;
			}
		}
		for (WorkerTask t: repairTasks){
			RepairTask rt = (RepairTask) t;
			// don't assign workers to repair themselves
			if (rt.target.getUnitId() != worker.id) {
				float tmpcost = costOfJob(worker, t);
				if (tmpcost < cost) {
					cost = tmpcost;
					task = t;
				}
			}
		}
		return task;
	}

	float costOfJob(Worker worker,  WorkerTask task){
		float costMod = 1;
		float dist = (distance(worker.getPos(),task.getPos()));
		boolean isExpensive = false;
		boolean isMex = false;
		boolean isPorc = false;

		for (Worker w: task.assignedWorkers){
			float idist = distance(w.getPos(),task.getPos());
			float rdist = Math.max(idist, 200);
			float deltadist = Math.abs(idist - dist);
			if (!w.equals(worker) && (rdist < dist || deltadist < 100)){
				costMod++;
			}
		}

		if (task instanceof ConstructionTask){
			 ConstructionTask ctask = (ConstructionTask) task;
			if (ctask.buildType.getName().contains("factory") && factories.size() == 0){
				return -1000; // factory plops and emergency facs get maximum priority
			}
			if (ctask.buildType.getCost(m) > 300){
				isExpensive = true;
			}else if (ctask.buildType.getName().equals("cormex")){
				isMex = true;
			}else if (ctask.buildType.isAbleToAttack()){
				isPorc = true;
			}
		}

		if (task instanceof ReclaimTask) {
			 ReclaimTask rtask = (ReclaimTask) task;
			FeatureDef fdef = rtask.target.getDef();
			if (fdef != null) {
				if (fdef.getContainedResource(m) > 300) {
					isExpensive = true;
				}
			}else{
				//for reclaim tasks that go out of LOS or the target no longer exists.
				return 100000;
			}
		}

		if (task instanceof RepairTask){
			RepairTask rptask = (RepairTask) task;
			UnitDef def = rptask.target.getDef();
			if (def != null) {
				if (def.getCost(m) > 700) {
					isExpensive = true;
				}
			}else{
				return 100000;
			}
		}

		if (costMod == 1){
		//for starting new jobs
			if (isExpensive && task instanceof RepairTask)
				return dist-500;
			if (task instanceof ReclaimTask && metal < 300) {
				return dist / 2;
			}else if (isMex){
				if (effectiveIncome > 30 && energy > 100) {
					return dist / (float) Math.log(dist);
				}
				return dist/2;
			}else if (isPorc){
				return dist-200;
			}else{
				return dist;
			}

		}else{
		// for assisting other workers
			if (isExpensive && task instanceof ReclaimTask && metal < 300){
				return dist + (600*(costMod-2));
			}else if (isExpensive) {
				return dist/(float)Math.log(dist) + (200 * (costMod-2));
			}else if (isPorc){
				return dist + (600*(costMod-2)) - 200;
			}else{
				return dist+(600*costMod);
			}
		}
	}

	boolean buildCheck( ConstructionTask task){
		float xsize = 0;
		float zsize = 0;

		//get the new building's area based on facing
		if (task.facing == 0 || task.facing == 2){
			xsize = task.buildType.getXSize()*4;
			zsize = task.buildType.getZSize()*4;
		}else{
			xsize = task.buildType.getZSize()*4;
			zsize = task.buildType.getXSize()*4;
		}

		//check for overlap with existing queued jobs
		for ( ConstructionTask c: constructionTasks){
			float cxsize = 0;
			float czsize = 0;

			//get the queued building's area based on facing
			if (c.facing == 0 || c.facing == 2){
				cxsize = c.buildType.getXSize()*4;
				czsize = c.buildType.getZSize()*4;
			}else{
				cxsize = c.buildType.getZSize()*4;
				czsize = c.buildType.getXSize()*4;
			}
			float minTolerance = xsize+cxsize;
			float axisDist = Math.abs(c.getPos().x - task.getPos().x);
			if (axisDist < minTolerance){
			// if it's too close in the x dimension
				minTolerance = zsize+czsize;
				axisDist = Math.abs(c.getPos().z - task.getPos().z);
				if (axisDist < minTolerance){
				//and it's too close in the z dimension
					return false;
				}
			}
		}
		return true;
	}

	void cleanOrders(){
		//remove invalid jobs from the queue
		 List<WorkerTask> invalidtasks = new ArrayList<WorkerTask>();

		for ( ConstructionTask t: constructionTasks){
			if (!callback.getMap().isPossibleToBuildAt(t.buildType, t.getPos(), t.facing) && t.target == null){
				//check to make sure it isn't our own nanoframe, since update is called before unitCreated
				List<Unit> stuff = callback.getFriendlyUnitsIn(t.getPos(), 50f);
				boolean isNano = false;
				for (Unit u:stuff){
					if (u.isBeingBuilt()){
						isNano = true;
					}
				}
				if (!isNano) {
					// if a construction job is blocked and it isn't our own nanoframe, remove it
					t.stopWorkers();
					invalidtasks.add(t);
				}
			}
		}
		constructionTasks.removeAll(invalidtasks);
		solarTasks.removeAll(invalidtasks);
		pylonTasks.removeAll(invalidtasks);
		fusionTasks.removeAll(invalidtasks);
		porcTasks.removeAll(invalidtasks);
		nanoTasks.removeAll(invalidtasks);
		factoryTasks.removeAll(invalidtasks);
		AATasks.removeAll(invalidtasks);

		invalidtasks.clear();

		for (ReclaimTask r: reclaimTasks){
			if (r.target.getDef() == null){
				// if a reclaimable feature falls out of LOS or is destroyed, remove its task
				r.stopWorkers();
				invalidtasks.add(r);
			}
		}
		reclaimTasks.removeAll(invalidtasks);
	}

	void cleanWorkers(){
		// fix workers that lost their orders due to interruption
		List<Worker> invalidworkers = new ArrayList<>();
		for (Worker w:workers){
			if (w.getUnit().getPos() == null){
				invalidworkers.add(w);
			}
		}
		workers.removeAll(invalidworkers);

		for (Worker w: workers){
			// unstick workers that were interrupted by random things
			if (w.getUnit().getCurrentCommands().size() == 0 && w.getTask() != null){
					w.getTask().removeWorker(w);
					w.clearTask();
			}

			// stop workers from chickening for too long.
			if (w.isChicken && frame - w.chickenFrame > 600){
				w.isChicken = false;
				w.getUnit().stop((short) 0, frame+30);
			}
		}
		// remove old com unit after morphs complete
		Worker invalidcom = null;
		for (Worker c:commanders){
			if (c.getUnit().getDef() == null){
				invalidcom = c;
			}
		}
		if (invalidcom != null) {
			if (invalidcom.getTask() != null) {
				invalidcom.getTask().removeWorker(invalidcom);
				invalidcom.clearTask();
			}
			commanders.remove(invalidcom);
		}
	}

    void createWorkerTask(Worker worker){
    	AIFloat3 position = worker.getPos();
		// do we need a factory?
		if ((factories.size() == 0 && factoryTasks.size() == 0)
				|| (effectiveIncome > 60 && factories.size() == 1 && factoryTasks.size() == 0)
				|| (effectiveIncome > 120 && factories.size() == 2 && factoryTasks.size() == 0)) {
			createFactoryTask(worker);
		}

		//Don't build crap right in front of the fac.
		boolean tooCloseToFac = false;
		for(Worker w:factories){
			Unit u = w.getUnit();
			float dist = distance(position,u.getPos());
			if (dist<450){
				tooCloseToFac = true;
			}
		}

		// do we need defense?
		if (effectiveIncome > 10 && !tooCloseToFac){
			createPorcTask(worker);
		}

    	// is there sufficient energy to cover metal income?
		if ((mexes.size() * 1.5) - 1.5 > solars.size()+solarTasks.size()
				|| (effectiveIncome > 15 && energy < 5 && solarTasks.size() < numWorkers)
				|| (effectiveIncome > 30)) {
			createEnergyTask(worker);
		}

    	
    	// do we need caretakers?
    	if((float)nanos.size()+nanoTasks.size() < Math.floor(effectiveIncome/5)-2 && factories.size() > 0){
			 Worker target = getCaretakerTarget();
			createNanoTurretTask(target.getUnit());
    	}

		// do we need radar?
		if (needRadar(position) && effectiveIncome > 10 && !tooCloseToFac){
    		createRadarTask(worker);
    	}

		// do we need pylons?
		/*if (effectiveIncomeEnergy > 150 && !tooCloseToFac){
			createGridTask(worker);
		}*/
    }
    
    void createRadarTask( Worker worker){
    	UnitDef radar = callback.getUnitDefByName("corrad");
    	AIFloat3 position = worker.getUnit().getPos();
    	
    	MetalSpot closest = graphManager.getClosestNeutralSpot(position);

		if (distance(closest.getPos(),position)<100){
    		AIFloat3 mexpos = closest.getPos();
			float distance = distance(mexpos, position);
			float extraDistance = 125;
			float vx = (position.x - mexpos.x)/distance; 
			float vz = (position.z - mexpos.z)/distance; 
			position.x = position.x+vx*extraDistance;
			position.z = position.z+vz*extraDistance;
    	}
    	
    	position = callback.getMap().findClosestBuildSite(radar,position,600f, 3, 0);

    	 ConstructionTask ct =  new ConstructionTask(radar, position, 0);
    	if (buildCheck(ct) && !radarTasks.contains(ct)){
			constructionTasks.add(ct);
			radarTasks.add(ct);
		}
    }
    
    void createPorcTask(Worker worker){
		AIFloat3 position = worker.getUnit().getPos();
		position = getRadialPoint(position, 150f);
		UnitDef porc = callback.getUnitDefByName("corrl");
		position = callback.getMap().findClosestBuildSite(porc,position,600f, 3, 0);

		if (!needDefender(position)){
			return;
		}

		ConstructionTask ct =  new ConstructionTask(porc, position, 0);
    	if (buildCheck(ct) && !porcTasks.contains(ct)){
			constructionTasks.add(ct);
			porcTasks.add(ct);
		}
    }

	void createAATask(Worker worker){
		AIFloat3 position = worker.getUnit().getPos();
		UnitDef aa = callback.getUnitDefByName("corrazor");

		position = callback.getMap().findClosestBuildSite(aa,position,600f, 3, 0);

		ConstructionTask ct =  new ConstructionTask(aa, position, 0);
		if (buildCheck(ct) && !AATasks.contains(ct)){
			constructionTasks.add(ct);
			AATasks.add(ct);
		}
	}
    
    void createFactoryTask( Worker worker){
		UnitDef cloak = callback.getUnitDefByName("factorycloak");
		UnitDef gunship = callback.getUnitDefByName("factorygunship");
		UnitDef strider = callback.getUnitDefByName("striderhub");
		UnitDef shields = callback.getUnitDefByName("factoryshield");
		UnitDef factory;
		AIFloat3 position = worker.getUnit().getPos();
		position.x = position.x + 120;
		position.z = position.z + 120;
		if (factories.size() > 0) {
			boolean good = false;
			AIFloat3 facpos = getNearestFac(position).getPos();
			while (!good) {
				position = getRadialPoint(facpos, 800f);
				position = callback.getMap().findClosestBuildSite(gunship,position,600f, 3, 0);
				if (distance(facpos, position) > 700){
					good = true;
				}
			}
		}

		if(factories.size() == 0){
			factory = cloak;
		}else if (factories.size() == 1){
			factory = strider;
		}else{
			factory = gunship;
		}
    	
    	MetalSpot closest = graphManager.getClosestNeutralSpot(position);
		if (distance(closest.getPos(),position)<100){
    		AIFloat3 mexpos = closest.getPos();
			float distance = distance(mexpos, position);
			float extraDistance = 100;
			float vx = (position.x - mexpos.x)/distance; 
			float vz = (position.z - mexpos.z)/distance; 
			position.x = position.x+vx*extraDistance;
			position.z = position.z+vz*extraDistance;
    	}
    	
    	position = callback.getMap().findClosestBuildSite(factory,position,600f, 3, 0);
    	
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

    	 ConstructionTask ct =  new ConstructionTask(factory, position, facing);
    	if (buildCheck(ct) && !factoryTasks.contains(ct)){
			constructionTasks.add(ct);
			factoryTasks.add(ct);
		}
    }

	
	void defendMexes(){
		List<MetalSpot> spots = graphManager.getNeutralSpots();
		UnitDef llt = callback.getUnitDefByName("corllt");

		for (MetalSpot ms:spots) {
			AIFloat3 position = ms.getPos();
			boolean needsllt = true;
			for(Unit u:porcs){
				float dist = distance(position,u.getPos());
				if (dist < 150){
					needsllt = false;
				}
			}

			for(ConstructionTask c:porcTasks){
				float dist = distance(position, c.getPos());
				if (dist < 150){
					needsllt = false;
				}
			}

			if (needsllt){
				position = getRadialPoint(ms.getPos(), 100f);
				position = callback.getMap().findClosestBuildSite(llt,position,600f, 3, 0);

				ConstructionTask ct =  new ConstructionTask(llt, position, 0);
				if (buildCheck(ct) && !porcTasks.contains(ct)){
					constructionTasks.add(ct);
					porcTasks.add(ct);
				}
			}
		}
	}

	boolean needAA(AIFloat3 position){
		if(!enemyHasAir){
			return false;
		}

		float aadist = Float.MAX_VALUE;

		for(Unit u:AAs){
			float dist = distance(position,u.getPos());
			if (dist<aadist){
				aadist = dist;
			}
		}

		for(ConstructionTask c:AATasks){
			float dist = distance(position, c.getPos());
			if (dist<aadist){
				aadist = dist;
			}
		}

		float minaadist = 800;

		if(aadist > minaadist && warManager.isFrontLine(position)){
			return true;
		}
		return false;
	}

	boolean needDefender(AIFloat3 position){
		float porcdist = Float.MAX_VALUE;

		for(Unit u:porcs){
			float dist = distance(position,u.getPos());
			if (dist<porcdist){
				porcdist = dist;
			}
		}

		for(ConstructionTask c:porcTasks){
			float dist = distance(position, c.getPos());
			if (dist<porcdist){
				porcdist = dist;
			}
		}

		float minporcdist = 500;

		if (effectiveIncome > 20 && warManager.isFrontLine(position)){
			minporcdist = 250;
		}

		if(porcdist > minporcdist){
			return true;
		}
		return false;
	}

	boolean needHLT(AIFloat3 position){
		float porcdist = Float.MAX_VALUE;

		for( Unit u:porcs){
			float dist = distance(position,u.getPos());
			if (dist<porcdist){
				porcdist = dist;
			}
		}

		for( ConstructionTask c:porcTasks){
			float dist = distance(position, c.getPos());
			if (dist<porcdist){
				porcdist = dist;
			}
		}

		float minporcdist = 500;

		if(porcdist > minporcdist && effectiveIncome > 30 && warManager.isFrontLine(position)){
			return true;
		}
		return false;
	}

	
	Boolean needRadar(AIFloat3 position){
		float closestRadarDistance = Float.MAX_VALUE;
		for( Unit r:radars){
			float distance = distance(r.getPos(),position);
			if(distance < closestRadarDistance){
				closestRadarDistance = distance;
			}
		}
		for( ConstructionTask r:radarTasks){
			float distance = distance(r.getPos(),position);
			if(distance < closestRadarDistance){
				closestRadarDistance = distance;
			}
		}

		if(closestRadarDistance > 1500){
			return true;
		}
		return false;
	}
	
	Boolean canBuildFusion( AIFloat3 position){
		 Worker nearestFac = getNearestFac(position);
		if ((effectiveIncome > 30 && nearestFac != null && fusions.size() < 6 && fusionTasks.size() == 0)){
			return true;
		}
		return false;
	}

    void captureMexes(){
    	UnitDef mex = callback.getUnitDefByName("cormex");
		List<MetalSpot> metalSpots = graphManager.getNeutralSpots();

		for ( MetalSpot ms: metalSpots){
			AIFloat3 position = ms.getPos();
			if (callback.getMap().isPossibleToBuildAt(mex, position, 0)){
				 ConstructionTask ct =  new ConstructionTask(mex, position, 0);
				if (!constructionTasks.contains(ct)){
					constructionTasks.add(ct);
				}
			}
		}
    }
    
    void createEnergyTask(Worker worker){
    	
    	// TODO: implement overdrive housekeeping in graphmanager, get buildpos from there 
    	
    	UnitDef solar = callback.getUnitDefByName("armsolar");
		UnitDef fusion = callback.getUnitDefByName("armfus");
		UnitDef singu = callback.getUnitDefByName("cafus");
    	AIFloat3 position = worker.getPos();

		/*Worker nearestFac = getNearestFac(worker.getPos());

		float fdist = 0f;
		if (nearestFac != null){
			fdist = distance(nearestFac.getPos(), position);
		}*/


		ConstructionTask ct;

		/*if (effectiveIncome > 50 && fdist > 1000 && !warManager.isFrontLine(position) && fusions.size() < 8 && fusionTasks.size() == 0){
			position = graphManager.getOverdriveSweetSpot(position);
			position = callback.getMap().findClosestBuildSite(fusion,position,600f, 3, 0);
			ct = new ConstructionTask(fusion, position, 0);
			if (buildCheck(ct) && !fusionTasks.contains(ct)){
				constructionTasks.add(ct);
				fusionTasks.add(ct);
			}
		}
		if (effectiveIncome > 100 && fdist > 1000 && !warManager.isFrontLine(position) && fusions.size() < 12 && fusionTasks.size() == 0){
			position = graphManager.getOverdriveSweetSpot(position);
			position = callback.getMap().findClosestBuildSite(singu,position,600f, 3, 0);
			ct = new ConstructionTask(singu, position, 0);
			if (buildCheck(ct) && !fusionTasks.contains(ct)){
				constructionTasks.add(ct);
				fusionTasks.add(ct);
			}
		}*/
		//else {

			position = graphManager.getNearestUnconnectedLink(position);
			if (position == null){
				return;
			}
			position = graphManager.getOverdriveSweetSpot(position);
			position = callback.getMap().findClosestBuildSite(solar,position,600f, 3, 0);

			float solarDist = 300;
		if (energy > 100 && effectiveIncome > 30){
			solarDist = 600;
		}

			// prevent ecomanager from spamming solars that graphmanager doesn't know about yet
			for (ConstructionTask s: solarTasks){
				float dist = distance(s.getPos(), position);
				if (dist < solarDist){
					return;
				}
			}

			// prevent a solar parking lot
			List<Unit> nearUnits = callback.getFriendlyUnitsIn(position, 300);
			int numSolars = 0;
			for ( Unit u : nearUnits){
				if (u.getDef().getName().equals("armsolar") && u.getTeam() == myTeamID){
					numSolars++;
				}
			}
			if (numSolars > 3){
				return;
			}
			ct = new ConstructionTask(solar, position, 0);
			if (buildCheck(ct) && !solarTasks.contains(ct)){
				constructionTasks.add(ct);
				solarTasks.add(ct);
			}
		//}
    }

	void createGridTask(Worker worker){
		ConstructionTask ct;
		UnitDef pylon = callback.getUnitDefByName("armestor");
		AIFloat3 position = graphManager.getNearestPylonSpot(worker.getPos());;
		position = callback.getMap().findClosestBuildSite(pylon,position,600f, 3, 0);

		// check the build site for existing pylons, since getOverdriveSweetSpot may cluster them.
		float gdist = Float.MAX_VALUE;
		for(Unit u:pylons){
			float dist = distance(position,u.getPos());
			if (dist<gdist){
				gdist = dist;
			}
		}

		for(ConstructionTask c:pylonTasks){
			float dist = distance(position,c.getPos());
			if (dist<gdist){
				gdist = dist;
			}
		}

		if(gdist > 600) {
			ct = new ConstructionTask(pylon, position, 0);
			if (buildCheck(ct) && !pylonTasks.contains(ct)){
				constructionTasks.add(ct);
				pylonTasks.add(ct);
			}
		}
	}
    
    void createNanoTurretTask( Unit target){
		UnitDef nano = callback.getUnitDefByName("armnanotc");
    	AIFloat3 position = target.getPos();
    	float buildDist = 400f;
		position = getRadialPoint(position, buildDist);
    	position = callback.getMap().findClosestBuildSite(nano,position,600f, 3, 0);
    	 ConstructionTask ct =  new ConstructionTask(nano, position, 0);
    	if (buildCheck(ct) && !nanoTasks.contains(ct)){
			constructionTasks.add(ct);
			nanoTasks.add(ct);
		}
    }

	
	Worker getCaretakerTarget(){
		 Worker target = null;
		int ctCount = 9001;
		for ( Worker f:factories){
			// Try to spread caretakers evenly between facs and to catch singus.
			List<Unit> nearUnits = callback.getFriendlyUnitsIn(f.getPos(), 800);
			int numCT = 0;
			for ( Unit u : nearUnits){
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

	void collectReclaimables(){
		List<Feature> feats = callback.getFeatures();
		for (Feature f : feats) {
			if (f.getDef().getContainedResource(m) > 0){
				 ReclaimTask rt = new ReclaimTask(f);
				if (!reclaimTasks.contains(rt)){
					reclaimTasks.add(rt);
				}
			}
		}
	}

	float distance( AIFloat3 pos1,  AIFloat3 pos2){
		float x1 = pos1.x;
		float z1 = pos1.z;
		float x2 = pos2.x;
		float z2 = pos2.z;
		return (float) Math.sqrt((x1-x2)*(x1-x2)+(z1-z2)*(z1-z2));
	}

	
	AIFloat3 getRadialPoint( AIFloat3 position, Float radius){
		// returns a random point lying on a circle around the given position.
		double angle = Math.random()*2*Math.PI;
		double vx = Math.cos(angle);
		double vz = Math.sin(angle);
		double x = position.x + radius*vx;
		double z = position.z + radius*vz;
		AIFloat3 pos = new AIFloat3();
		pos.x = (float) x;
		pos.z = (float) z;
		return pos;
	}

	
	Worker getNearestFac( AIFloat3 position){
		 Worker nearestFac = null;
		float dist = Float.MAX_VALUE;
		for ( Worker f:factories){
			float tdist = distance(position, f.getPos());
			if (tdist < dist){
				dist = tdist;
				nearestFac = f;
			}
		}
		return nearestFac;
	}
    
	public void setMilitaryManager(MilitaryManager militaryManager) {
		this.warManager = militaryManager;
	}
    
	public void setGraphManager(GraphManager graphManager) {
		this.graphManager = graphManager;
	}
}
