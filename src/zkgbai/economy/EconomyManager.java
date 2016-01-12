package zkgbai.economy;

import java.util.*;

import com.springrts.ai.oo.clb.*;
import zkgbai.Module;
import zkgbai.ZKGraphBasedAI;
import zkgbai.economy.tasks.*;
import zkgbai.economy.tasks.RepairTask;
import zkgbai.graph.GraphManager;
import zkgbai.graph.MetalSpot;
import zkgbai.los.LosManager;
import zkgbai.military.MilitaryManager;

import com.springrts.ai.oo.AIFloat3;

import static zkgbai.kgbutil.KgbUtil.*;

public class EconomyManager extends Module {
	ZKGraphBasedAI ai;
	List<Worker> workers;
	List<Worker> assigned;
	List<Worker> populated;
	List<Worker> commanders;
	Deque<Worker> idlers;
	List<ConstructionTask> factoryTasks; // for constructors building factories
	List<ConstructionTask> radarTasks;
	List<ConstructionTask> constructionTasks;
	public List<ReclaimTask> reclaimTasks;
	List<CombatReclaimTask> combatReclaimTasks;
	List<RepairTask> repairTasks;
	List<ConstructionTask> solarTasks;
	List<ConstructionTask> windTasks;
	List<ConstructionTask> fusionTasks;
	List<ConstructionTask> pylonTasks;
	List<ConstructionTask> porcTasks;
	List<ConstructionTask> nanoTasks;
	List<ConstructionTask> AATasks;
	List<ConstructionTask> airpadTasks;
	List<Unit> radars;
	public List<Unit> porcs;
	List<Unit> nanos;
	List<Unit> airpads;
	public List<Unit> fusions;
	List<Unit> mexes;
	List<Unit> solars;
	List<Unit> windgens;
	List<Unit> pylons;
	List<Unit> AAs;
	List<String> potentialFacList;
	
	public float effectiveIncomeMetal = 0;
	float effectiveIncomeEnergy = 0;
	
	public float effectiveIncome = 0;
	float effectiveExpenditure = 0;

	public float teamIncome = 0;
	
	float metal = 0;
	public float energy = 0;

	public float adjustedIncome = 0;

	boolean waterDamage = false;
	boolean defendedFac = false;
	boolean addedFacs = false;
	boolean leader = true;
	boolean havePlanes = false;

	
	int frame = 0;
	int lastPorcPushFrame = 0;

	static short OPTION_SHIFT_KEY = (1 << 5);
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
	private LosManager losManager;
	private TerrainAnalyzer terrainManager;
	private FactoryManager facManager;
	
	public EconomyManager( ZKGraphBasedAI ai){
		this.ai = ai;
		this.callback = ai.getCallback();
		this.myTeamID = ai.teamID;
		this.myAllyTeamID = ai.allyTeamID;
		this.workers = new ArrayList<Worker>();
		this.commanders = new ArrayList<Worker>();
		this.assigned = new ArrayList<Worker>();
		this.populated = new ArrayList<Worker>();
		this.idlers = new ArrayDeque<Worker>();
		this.factoryTasks = new ArrayList<ConstructionTask>();
		this.radarTasks = new ArrayList<ConstructionTask>();
		this.constructionTasks = new ArrayList<ConstructionTask>();
		this.reclaimTasks = new ArrayList<ReclaimTask>();
		this.combatReclaimTasks = new ArrayList<CombatReclaimTask>();
		this.repairTasks = new ArrayList<RepairTask>();
		this.solarTasks = new ArrayList<ConstructionTask>();
		this.windTasks = new ArrayList<ConstructionTask>();
		this.fusionTasks = new ArrayList<ConstructionTask>();
		this.pylonTasks = new ArrayList<ConstructionTask>();
		this.porcTasks = new ArrayList<ConstructionTask>();
		this.nanoTasks = new ArrayList<ConstructionTask>();
		this.AATasks = new ArrayList<ConstructionTask>();
		this.airpadTasks = new ArrayList<ConstructionTask>();
		this.radars = new ArrayList<Unit>();
		this.porcs = new ArrayList<Unit>();
		this.nanos = new ArrayList<Unit>();
		this.airpads = new ArrayList<Unit>();
		this.fusions = new ArrayList<Unit>();
		this.mexes = new ArrayList<Unit>();
		this.solars = new ArrayList<Unit>();
		this.windgens = new ArrayList<Unit>();
		this.pylons = new ArrayList<Unit>();
		this.AAs = new ArrayList<Unit>();

		this.eco = callback.getEconomy();
		
		this.m = callback.getResourceByName("Metal");
		this.e = callback.getResourceByName("Energy");

		if (callback.getMap().getWaterDamage() > 0){
			this.waterDamage = true;
		}

		// find out how many allies we have to weight resource income
		/*if (callback.getTeams().getSize() > 2){
			this.teamcount++;
			ai.debug("Number of teams detected: " + callback.getTeams().getSize());
		}*/
	}

	@Override
	public int init(int AIID, OOAICallback cb){
		this.warManager = ai.warManager;
		this.graphManager = ai.graphManager;
		this.losManager = ai.losManager;
		this.facManager = ai.facManager;

		this.terrainManager = new TerrainAnalyzer();
		this.potentialFacList = terrainManager.getInitialFacList();

		return 0;
	}
	
	@Override
	public String getModuleName() {
		// TODO Auto-generated method stub
		return "EconomyManager";
	}
	
	@Override
	public int update(int frame) {
		this.frame = frame;
		cleanWorkers();

		effectiveIncomeMetal = eco.getIncome(m);
		effectiveIncomeEnergy = eco.getIncome(e);

		metal = eco.getCurrent(m);
		energy = eco.getCurrent(e);

		float expendMetal = eco.getUsage(m);
		float expendEnergy = eco.getUsage(e);

		effectiveIncome = Math.min(effectiveIncomeMetal, effectiveIncomeEnergy);
		effectiveExpenditure = Math.min(expendMetal, expendEnergy);

		adjustedIncome = effectiveIncome + ai.allies.size() + (((float) frame)/1800f);

		leader = true;
		teamIncome = effectiveIncome;
		for (Integer teamID: ai.allies){
			float allyIncome = Math.min(callback.getGame().getTeamResourceIncome(teamID, m.getResourceId()), callback.getGame().getTeamResourceIncome(teamID, e.getResourceId()));
			teamIncome += allyIncome;
			if (allyIncome > effectiveIncome){
				leader = false;
			}
		}

		if (frame % 30 == 0) {
			captureMexes();
			if (effectiveIncome > 22) {
				collectReclaimables();
			}
			setPriorities();

			// remove finished or invalidated tasks
			cleanOrders();
		}

		if (frame % 300 == 0){
			assignNanos();
		}


		if (frame % 10 == 0) {
			//create new building tasks.
			boolean pop = false;
			for (Worker w : workers) {
				if (!populated.contains(w)) {
					generateTasks(w);
					populated.add(w);
					pop = true;
					break;
				}
			}
			if (!pop){
				populated.clear();
			}
		}

		if (frame % 3 == 0) {
			assignWorkers(); // assign workers to tasks
		}

		return 0;
	}

    @Override
    public int unitFinished(Unit unit) {
    	
    	UnitDef def = unit.getDef(); 
    	String defName = def.getName();

		if (!defendedFac && defName.equals("cormex")){
			defendFac();
		}

		if(defName.equals("cormex")){
			mexes.add(unit);
		}

		if(defName.equals("armsolar")){
			solars.add(unit);
		}

		if(defName.equals("armwin")){
			windgens.add(unit);
		}

    	if(defName.equals("corrad")){
    		radars.add(unit);
    	}

    	if (unit.getMaxSpeed() > 0 && def.getBuildOptions().size() > 0) {
			Worker w = new Worker(unit);
			workers.add(w);
			idlers.add(w);
			unit.setMoveState(0, (short) 0, frame+300);
			if (def.getBuildSpeed() > 8) {
				commanders.add(w);
			}
		}

		ConstructionTask finished = null;
    	for (ConstructionTask ct:constructionTasks){
			if (ct.target != null){
				if(ct.target.getUnitId() == unit.getUnitId()){
					finished = ct;
					List<Worker> idle = ct.stopWorkers(frame);
					for (Worker i:idle){
						idlers.add(i);
					}
				}
			}
		}

		constructionTasks.remove(finished);
		solarTasks.remove(finished);
		windTasks.remove(finished);
		pylonTasks.remove(finished);
		fusionTasks.remove(finished);
		porcTasks.remove(finished);
		nanoTasks.remove(finished);
		airpadTasks.remove(finished);
		factoryTasks.remove(finished);
		AATasks.remove(finished);

		return 0;
    }

	@Override
	public int unitDamaged(Unit h, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzed){
		// add repair tasks for damaged units
		RepairTask task = new RepairTask(h);
		if (!repairTasks.contains(task)){
			repairTasks.add(task);
		}

		// defender push against enemy porc
		if (attacker != null && attacker.getMaxSpeed() == 0 && frame - lastPorcPushFrame > 15) {
			lastPorcPushFrame = frame;
			porcPush(h, attacker);
		}

		for (Worker w: workers) {
			if (w.id == h.getUnitId()) {
				// retreat if a worker gets attacked by enemy porc
				if (attacker != null && attacker.getMaxSpeed() == 0) {
					if (w.getTask() != null) {
						w.getTask().removeWorker(w);
						w.clearTask(frame);
					}
					// move away from the enemy porc
					float x = -200 * dir.x;
					float z = -200 * dir.z;
					AIFloat3 pos = h.getPos();
					AIFloat3 target = new AIFloat3();
					target.x = pos.x + x;
					target.z = pos.z + z;
					h.moveTo(target, (short) 0, frame);
				}

				// chicken workers if damaged too much
				if (h.getHealth() / h.getMaxHealth() < 0.8 && !w.isChicken) {
					if (w.getTask() != null) {
						w.getTask().removeWorker(w);
						w.clearTask(frame);
					}
					AIFloat3 pos = graphManager.getClosestHaven(h.getPos());
					h.moveTo(pos, (short) 0, frame + 240);
					w.isChicken = true;
					w.chickenFrame = frame;
				}
				break;
			}
		}
		return 0;
	}
    
    @Override
    public int unitDestroyed(Unit unit, Unit attacker) {
		radars.remove(unit);
    	porcs.remove(unit);
		nanos.remove(unit);
		airpads.remove(unit);
		fusions.remove(unit);
		mexes.remove(unit);
		solars.remove(unit);
		windgens.remove(unit);
		pylons.remove(unit);
		AAs.remove(unit);

		// fortify mexes if they die
		if (unit.getDef().getName().equals("cormex")){
			fortifyMex(unit.getPos());
		}

		// if the unit had a repair task targeting it, remove it
		RepairTask rt = new RepairTask(unit);
		repairTasks.remove(rt);

		ConstructionTask invalidtask = null;

		// If it was a building under construction, reset the builder's target
		if(unit.getMaxSpeed() == 0){
			for (ConstructionTask ct:constructionTasks){
				if (ct.target != null) {
					if (ct.target.getUnitId() == unit.getUnitId()) {
						// if a building was killed by enemy porc, cancel it.
						if (attacker != null && attacker.getMaxSpeed() == 0){
							invalidtask = ct;
							List<Worker> idle = ct.stopWorkers(frame);
							idlers.addAll(idle);
						}else {
							ct.target = null;
						}
					}
				}
			}
		}

		constructionTasks.remove(invalidtask);
		solarTasks.remove(invalidtask);
		pylonTasks.remove(invalidtask);
		fusionTasks.remove(invalidtask);
		porcTasks.remove(invalidtask);
		nanoTasks.remove(invalidtask);
		factoryTasks.remove(invalidtask);
		AATasks.remove(invalidtask);

		// if we have a dead worker or factory, remove them and their tasks.
    	 Worker deadWorker = null;
	    for (Worker worker : workers) {
	    	if(worker.id == unit.getUnitId()){
	    		deadWorker = worker;
	    		WorkerTask wt = worker.getTask();
	    		if (wt != null) {
					wt.removeWorker(worker);
				}
	    	}
	    }
	    if (deadWorker != null){
	    	workers.remove(deadWorker);
			idlers.remove(deadWorker);
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
    	}else if (builder != null && !unit.isBeingBuilt()){
			// instant factory plops only call unitcreated, not unitfinished.
			for ( Worker w : workers){
				if (w.id == builder.getUnitId()){
					WorkerTask task = w.getTask();
					constructionTasks.remove(task);
					factoryTasks.remove(task);
					w.clearTask(frame);
				}
			}
		}

		String defName = unit.getDef().getName();

		if (defName.equals("cormex") && defendedFac){
			if (effectiveIncome < 30) {
				defendMex(unit.getPos());
			}else{
				fortifyMex(unit.getPos());
			}
		}

		if(defName.equals("corrl") || defName.equals("corllt") || defName.equals("corhlt") || defName.equals("armartic")){
			porcs.add(unit);
		}

		if (defName.equals("corrazor") || defName.equals("corflak")){
			AAs.add(unit);
		}

		if (defName.equals("armasp")){
			airpads.add(unit);
		}

		if (defName.equals("armnanotc")){
			nanos.add(unit);
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

		// rush striders
		if ((defName.equals("dante") || defName.equals("scorpion") || defName.equals("funnelweb")) && warManager.miscHandler.striders.size() == 0){
			RepairTask rt = new RepairTask(unit);
			repairTasks.add(rt);
		}

		if (defName.equals("armbanth") || defName.equals("armorco")){
			RepairTask rt = new RepairTask(unit);
			repairTasks.add(rt);
		}

		if (defName.equals("factoryplane")){
			havePlanes = true;
		}

        return 0;
    }

	@Override
	public int unitCaptured(Unit unit, int oldTeamID, int newTeamID){
		if (oldTeamID == myTeamID){
			return unitDestroyed(unit, null);
		}
		return 0;
	}

	@Override
	public int unitGiven(Unit unit, int oldTeamID, int newTeamID){
		if (newTeamID == myTeamID){
			if (unit.getMaxSpeed() > 0 && unit.getDef().getBuildOptions().size() > 0) {
				Worker w = new Worker(unit);
				workers.add(w);
				idlers.add(w);
				if (unit.getDef().getBuildSpeed() > 8) {
					commanders.add(w);
				}
			}

			if (unit.isBeingBuilt()){
				RepairTask rp = new RepairTask(unit);
				if (!repairTasks.contains(rp)) {
					repairTasks.add(rp);
				}
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
		}

		String defName = unit.getDef().getName();
		if(defName.equals("corrl") || defName.equals("corllt") || defName.equals("corhlt") || defName.equals("armartic")){
			porcs.add(unit);
		}

		return 0;
	}

	@Override
	public int enemyEnterLOS(Unit e){
		// capture combat reclaim for enemy building nanoframes that enter los
		if (e.isBeingBuilt() && e.getMaxSpeed() == 0 && warManager.getThreat(e.getPos()) == 0){
			CombatReclaimTask crt = new CombatReclaimTask(e);
			if (!combatReclaimTasks.contains(crt)){
				combatReclaimTasks.add(crt);
			}
		}

		// porc over enemy mexes
		if (e.getDef().getName().equals("cormex")){
			fortifyMex(e.getPos());
		}
		return 0;
	}

	@Override
	public int enemyCreated(Unit e){
		// capture combat reclaim for enemy buildings that are started within los
		if (e.getMaxSpeed() == 0 && warManager.getThreat(e.getPos()) == 0){
			CombatReclaimTask crt = new CombatReclaimTask(e);
			if (!combatReclaimTasks.contains(crt)){
				combatReclaimTasks.add(crt);
			}
		}
		return 0;
	}

	private void assignNanos(){
		for (Unit n:nanos){
				Worker fac = getNearestFac(n.getPos());
				if (fac != null) {
					n.guard(fac.getUnit(), (short) 0, frame + 3000);
					n.setRepeat(true, (short) 0, frame + 3000);
				}
		}
	}

	private void setPriorities() {
		// set the first fusion to high prio so it gets built quickly.
		ArrayList<Float> params = new ArrayList<>();
		params.add((float) 2);

		if (fusions.size() == 1){
			// set fusions to high prio if energy isn't full
			for (Unit f: fusions){
				f.executeCustomCommand(CMD_PRIORITY, params, (short) 0, frame + 300);
			}
		}

		// set facs+nanos to normal prio if resources are available, low prio early game or when estalled.
		params.clear();
		if (energy > 100) {
			params.add(1f);
		}else{
			params.add(3f);
		}

		for (Unit n: nanos){
			n.executeCustomCommand(CMD_PRIORITY, params, (short) 0, frame + 300);
		}

		for (Worker f : facManager.factories) {
			f.getUnit().executeCustomCommand(CMD_PRIORITY, params, (short) 0, frame + 300);
		}
	}

	void assignWorkers() {
		// assign idle workers first
		Worker toAssign = idlers.poll();

		if (toAssign != null && toAssign.isChicken){
			toAssign = null;
		}

		// limit the number of workers assigned at one time to prevent super lag.
		if (toAssign == null) {
			for (Worker w : workers) {
				if (!assigned.contains(w) && !w.isChicken) {
					toAssign = w;
					break;
				}
			}

			if (toAssign == null) {
				assigned.clear();
				return;
			}
		}

		Worker w = toAssign;
		assigned.add(w);

		WorkerTask task = getCheapestJob(w);
		WorkerTask wtask = w.getTask();
		if (task != null && !task.equals(wtask)) {
			// remove it from its old assignment if it had one
			if (wtask != null) {
				wtask.removeWorker(w);
			}
			if (task instanceof ConstructionTask) {
				ConstructionTask ctask = (ConstructionTask) task;
				try {
					w.getUnit().build(ctask.buildType, ctask.getPos(), ctask.facing, (short) 0, frame + 5000);
				}catch (Exception e){
					List<Worker> idle = task.stopWorkers(frame);
					idlers.addAll(idle);
					constructionTasks.remove(task);
					assigned.remove(w);
					return;
				}
			} else if (task instanceof ReclaimTask) {
				ReclaimTask rt = (ReclaimTask) task;
				w.getUnit().moveTo(getDirectionalPoint(rt.getPos(), w.getPos(), 100f), (short) 0, frame + 300);
				w.getUnit().reclaimInArea(rt.getPos(), 75f, OPTION_SHIFT_KEY, frame + 5000);
			}else if (task instanceof CombatReclaimTask){
				CombatReclaimTask crt = (CombatReclaimTask) task;
				// prevent workers from being assigned to dangerous/invalid combat reclaim jobs
				if (!crt.target.isBeingBuilt() || warManager.getThreat(crt.getPos()) > 0 || crt.target.getHealth() <= 0){
					List<Worker> idle = task.stopWorkers(frame);
					idlers.addAll(idle);
					combatReclaimTasks.remove(crt);
					assigned.remove(w);
					return;
				}
				// else assign
				w.getUnit().moveTo(getDirectionalPoint(crt.getPos(), w.getPos(), 100f), (short) 0, frame + 300);
				w.getUnit().reclaimUnit(crt.target, OPTION_SHIFT_KEY, frame + 5000);
			} else if (task instanceof RepairTask) {
				RepairTask rt = (RepairTask) task;
				w.getUnit().repair(rt.target, (short) 0, frame + 5000);
			}
			w.setTask(task, frame);
			task.addWorker(w);
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
		for (WorkerTask t: combatReclaimTasks){
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

		for (Worker w: task.assignedWorkers){
			// increment cost mod for every other worker assigned to the given task that isn't the worker we're assigning
			// as long as they're closer or equaldist to the target.
			float idist = distance(w.getPos(),task.getPos());
			float rdist = Math.max(idist, 200);
			float deltadist = Math.abs(idist - dist);
			if (!w.equals(worker) && (rdist < dist || deltadist < 100)){
				costMod++;
			}
		}

		if (task instanceof ConstructionTask){
			 ConstructionTask ctask = (ConstructionTask) task;
			if (ctask.buildType.getName().contains("factory") && facManager.factories.size() == 0){
				// factory plops and emergency facs get maximum priority
				return -1000;
			}
			
			if (ctask.buildType.getCost(m) > 500){
				// give expensive stuff high prio
				return dist - ctask.buildType.getCost(m) + (500 * (costMod - 2));
			}else if (ctask.buildType.getName().equals("cormex")){
				// for mexes
				// favor expansion highly when not stalled
				if (energy > 100) {
					return dist / (float) Math.log(dist) + (600 * (costMod - 1));
				}
				// otherwise favor expansion moderately
				return dist/2;
			}else if (ctask.buildType.isAbleToAttack()){
				// for porc
				return dist-300 + Math.max(0, (600 * (costMod - 2)));
			}else if (ctask.buildType.getName().equals("armnanotc") || ctask.buildType.getName().equals("armasp") || ctask.buildType.getName().equals("corrazor") || ctask.buildType.getName().equals("corflak")) {
				// for nanotowers and airpads and AA
				return dist - 1000 + (500 * (costMod - 2));
			}else if (ctask.buildType.getName().equals("armestor")){
				// for pylons
				return dist - 500 + (500 * (costMod - 1));
			}else if (ctask.buildType.getName().equals("armsolar") || ctask.buildType.getName().equals("armwin") && energy < 400){
				// favor solars highly when estalled
				return (dist/(float) Math.log(dist)) + (600 * (costMod-1));
			}else{
				return dist+(600 * (costMod - 1));
			}
		}

		if (task instanceof ReclaimTask) {
			ReclaimTask rtask = (ReclaimTask) task;
			if (metal > 400){
				// don't favor reclaim if excessing
				return dist;
			}

			if (rtask.def.getContainedResource(m) > 300) {
				// for big reclaimables
				return (dist/(float) Math.log(dist)) - rtask.target.getReclaimLeft() + (600 * (costMod - 2));
			}
			// for other reclaimables
			return (dist / (float) Math.log(dist)) + (600 * (costMod - 1));
		}

		if (task instanceof CombatReclaimTask){
			// favor combat reclaim when it's possible
			return dist - 600 + Math.max(0, (600 * (costMod - 2)));
		}

		if (task instanceof RepairTask){
			RepairTask rptask = (RepairTask) task;
			if (rptask.target.getHealth() > 0) {
				if (rptask.target.getMaxSpeed() > 0 && rptask.target.getDef().isAbleToAttack()) {
					// for mobile combat units
					return dist + (100 * (costMod - 1)) - Math.min(10000,(rptask.target.getMaxHealth() - rptask.target.getHealth())) / (5 * costMod);
				}else if (rptask.target.getDef().isAbleToAttack()){
					// for static defenses
					return dist + (100 * (costMod - 1)) - ((rptask.target.getMaxHealth() * 4) - rptask.target.getHealth()) / costMod;
				}else{
					// for everything else
					return dist + (600 * (costMod - 1));
				}
			}else{
				return Float.MAX_VALUE;
			}
		}

		// this will never be reached, but java doesn't know that.
		return Float.MAX_VALUE;
	}

	boolean buildCheck(ConstructionTask task){
		// stop things from being built underwater if the map water does damage or if water pathing is bad on the map
		if ((waterDamage || !graphManager.isWaterMap) && callback.getMap().getElevationAt(task.getPos().x, task.getPos().z) < 10f){
			return false;
		}

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

		for (ConstructionTask t: constructionTasks){
			if (!callback.getMap().isPossibleToBuildAt(t.buildType, t.getPos(), t.facing) && t.target == null){
				//check to make sure it isn't our own nanoframe, since update is called before unitCreated
				List<Unit> stuff = callback.getFriendlyUnitsIn(t.getPos(), 50f);
				boolean isNano = false;
				for (Unit u:stuff){
					if (u.isBeingBuilt() && u.getTeam() == myTeamID){
						isNano = true;
					}
				}
				if (!isNano) {
					// if a construction job is blocked and it isn't our own nanoframe, remove it
					List<Worker> idle = t.stopWorkers(frame);
					idlers.addAll(idle);
					invalidtasks.add(t);
				}
			}
		}

		// clear fac and nano tasks that workers can't reach.
		for (ConstructionTask t: factoryTasks){
			if (frame-t.frameIssued > 1800 && t.target == null){
				List<Worker> idle = t.stopWorkers(frame);
				idlers.addAll(idle);
				potentialFacList.add(t.buildType.getName());
				invalidtasks.add(t);
			}
		}
		for (ConstructionTask t: nanoTasks){
			if (frame-t.frameIssued > 1800 && t.target == null){
				List<Worker> idle = t.stopWorkers(frame);
				idlers.addAll(idle);
				invalidtasks.add(t);
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

		for (ReclaimTask rt:reclaimTasks){
			if (losManager.isInLos(rt.getPos())){
				if (rt.target.getReclaimLeft() <= 0){
					List<Worker> idle = rt.stopWorkers(frame);
					idlers.addAll(idle);
					invalidtasks.add(rt);
				}
			}
		}
		reclaimTasks.removeAll(invalidtasks);

		invalidtasks.clear();

		for (CombatReclaimTask crt:combatReclaimTasks){
			if (!crt.target.isBeingBuilt() || warManager.getThreat(crt.getPos()) > 0 || crt.target.getHealth() <= 0){
				invalidtasks.add(crt);
			}
		}
		combatReclaimTasks.removeAll(invalidtasks);

		invalidtasks.clear();

		for (RepairTask rt:repairTasks){
			if (rt.target.getHealth() <= 0 || rt.target.getHealth() == rt.target.getMaxHealth()) {
				List<Worker> idle = rt.stopWorkers(frame);
				idlers.addAll(idle);
				invalidtasks.add(rt);
			}
		}
		repairTasks.removeAll(invalidtasks);
	}

	void cleanWorkers(){
		// Remove dead workers not yet detected by unitDestroyed.
		List<Worker> invalidworkers = new ArrayList<>();
		for (Worker w:workers){
			if (w.getUnit().getHealth() <= 0 || w.getUnit().getTeam() != myTeamID){
				invalidworkers.add(w);
			}
		}

		for (Worker w:invalidworkers){
			if (w.getTask() != null){
				w.getTask().removeWorker(w);
			}
		}

		workers.removeAll(invalidworkers);
		idlers.removeAll(invalidworkers);


		if (frame % 150 == 0) {
			for (Worker w : workers) {
				// unstick workers that were interrupted by random things and lost their orders.
				if (w.getUnit().getCurrentCommands().size() == 0 && w.getTask() != null) {
					w.getTask().removeWorker(w);
					w.clearTask(frame);
					idlers.add(w);
				}

				if (w.getUnit().getCurrentCommands().size() == 0 && !idlers.contains(w)) {
					idlers.add(w);
				}

				// detect and unstick workers that get stuck on pathing obstacles.
				if (w.unstick(frame)){
					idlers.add(w);
				}

				// stop workers from chickening for too long.
				if (w.isChicken && frame - w.chickenFrame > 600 || w.getUnit().getCurrentCommands().isEmpty()) {
					w.isChicken = false;
					idlers.add(w);
					w.getUnit().stop((short) 0, frame + 3000);
				}
			}
		}
		// remove old com unit after morphs complete
		Worker invalidcom = null;
		for (Worker c:commanders){
			if (c.getUnit().getHealth() <= 0){
				invalidcom = c;
			}
		}
		if (invalidcom != null) {
			if (invalidcom.getTask() != null) {
				invalidcom.getTask().removeWorker(invalidcom);
				invalidcom.clearTask(frame);
			}
			commanders.remove(invalidcom);
		}
	}

    void generateTasks(Worker worker){
    	AIFloat3 position = worker.getPos();
		// do we need a factory?
		if ((facManager.factories.size() == 0 && factoryTasks.size() == 0)
				|| (effectiveIncome > 40 && facManager.factories.size() == 1 && factoryTasks.size() == 0)
				|| (effectiveIncome > 65 && facManager.factories.size() == 2 && factoryTasks.size() == 0)
				|| (effectiveIncome > 120 && facManager.factories.size() == 3 && factoryTasks.size() == 0)) {
			createFactoryTask(worker);
		}

		//Don't build crap right in front of the fac.
		boolean tooCloseToFac = false;
		for(Worker w:facManager.factories){
			Unit u = w.getUnit();
			float dist = distance(position,u.getPos());
			if (dist<450){
				tooCloseToFac = true;
			}
		}

		// do we need AA?
		if (facManager.enemyHasAir && effectiveIncome > 30 && AATasks.isEmpty()){
			createAATask(worker);
		}

    	// is there sufficient energy to cover metal income?
		int energySize = solars.size() + solarTasks.size() + (windgens.size() + windTasks.size())/2;
		if ((effectiveIncome < 15 && mexes.size() > energySize)
				|| (effectiveIncome > 15 && energy < 100 && solarTasks.size() + windTasks.size() < facManager.numWorkers)
				|| (effectiveIncome > 30 && solarTasks.size() + windTasks.size() < facManager.numWorkers)) {
			createEnergyTask(worker);
		}

    	
    	// do we need caretakers?
    	if(((float)nanos.size() < (effectiveIncome/10)-2 || (metal > 400 && energy > 400))
				&& facManager.factories.size() > 0 && nanoTasks.isEmpty() && effectiveIncome > 20){
			Worker target = getCaretakerTarget();
			createNanoTurretTask(target.getUnit());
    	}

		// do we need airpads for planes
		if (havePlanes && airpadTasks.isEmpty() && effectiveIncome > 15
				&& ((float)airpads.size() < Math.floor(effectiveIncome/30) || airpads.isEmpty())){
			Worker target = getCaretakerTarget();
			createAirPadTask(target.getUnit());
		}

		// do we need radar?
		if (needRadar(position) && effectiveIncome > 10 && !tooCloseToFac){
    		createRadarTask(worker);
    	}

		// do we need pylons?
		if (fusions.size() > 2 && !tooCloseToFac){
			createGridTask(worker);
		}
    }
    
    void createRadarTask(Worker worker){
    	UnitDef radar = callback.getUnitDefByName("corrad");
    	AIFloat3 position = worker.getUnit().getPos();
    	
    	MetalSpot closest = graphManager.getClosestNeutralSpot(position);

		if (closest != null && distance(closest.getPos(),position)<100){
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

	void createAATask(Worker worker){
		AIFloat3 position = worker.getUnit().getPos();
		position = getRadialPoint(graphManager.getAllyCenter(), 800f);
		UnitDef aa;

		if (Math.random() > 0.5){
			aa = callback.getUnitDefByName("corrazor");
		}else{
			aa = callback.getUnitDefByName("corflak");
		}

		position = callback.getMap().findClosestBuildSite(aa,position,600f, 3, 0);

		ConstructionTask ct =  new ConstructionTask(aa, position, 0);
		if (buildCheck(ct) && !porcTasks.contains(ct)){
			constructionTasks.add(ct);
			AATasks.add(ct);
		}
	}
    
    void createFactoryTask(Worker worker){
		UnitDef factory;

		if (potentialFacList.isEmpty()){
			potentialFacList = terrainManager.getInitialFacList();
			addedFacs = false;
		}

		if (effectiveIncome > 30 && !addedFacs){
			potentialFacList.add("factorygunship");
			potentialFacList.add("striderhub");
			potentialFacList.add("factoryplane");
			addedFacs = true;
		}

		boolean hasStriders = false;
		for (Worker f:facManager.factories){
			if (f.getUnit().getDef().getName().equals("striderhub")){
				hasStriders = true;
			}

			if (potentialFacList.contains(f.getUnit().getDef().getName())){
				potentialFacList.remove(f.getUnit().getDef().getName()); // don't build the same fac twice.
			}
		}

		if (facManager.factories.size() < 2 || hasStriders) {
			int i = (int) Math.round(Math.random() * (potentialFacList.size() - 1));
			String facName = potentialFacList.get(i);
			factory = callback.getUnitDefByName(facName);
			potentialFacList.remove(i);
		}else{
			factory = callback.getUnitDefByName("striderhub");
			potentialFacList.remove("striderhub");
		}

		AIFloat3 position = worker.getUnit().getPos();
		position.x = position.x + 150;
		position.z = position.z + 150;

		if (facManager.factories.size() > 0) {
			boolean good = false;

			while (!good) {
				AIFloat3 facpos = getNearestFac(position).getPos();

				// don't cram factories together
				position = getRadialPoint(facpos, 800f);
				position = graphManager.getClosestSpot(position).getPos();

				// don't let factories get blocked by random crap
				if (callback.getFriendlyUnitsIn(position, 250f).size() > 0){
					position = getDirectionalPoint(position, graphManager.getAllyCenter(), 150f);
				}
				position = callback.getMap().findClosestBuildSite(factory,position,600f, 3, 0);

				ConstructionTask ct =  new ConstructionTask(factory, position, 0); // only for preventing stupid placement
				if (distance(getNearestFac(position).getPos(), position) > 700 && buildCheck(ct)){
					good = true;
				}
			}
		}

		// don't block mexes with factories
    	MetalSpot closest = graphManager.getClosestNeutralSpot(position);
		if (distance(closest.getPos(),position)<100){
    		AIFloat3 mexpos = closest.getPos();
			position = getDirectionalPoint(mexpos, graphManager.getMapCenter(), 150f);
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
		ct.frameIssued = frame;
		if (buildCheck(ct) && !factoryTasks.contains(ct)){
			constructionTasks.add(ct);
			factoryTasks.add(ct);
		}
    }

	void fortifyMex(AIFloat3 position){
		UnitDef llt = callback.getUnitDefByName("corllt");
		UnitDef defender = callback.getUnitDefByName("corrl");
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
			// build an llt and a defender
			position = getRadialPoint(position, 100f);
			position = callback.getMap().findClosestBuildSite(llt,position,600f, 3, 0);

			ConstructionTask ct =  new ConstructionTask(llt, position, 0);
			if (buildCheck(ct) && !porcTasks.contains(ct)){
				constructionTasks.add(ct);
				porcTasks.add(ct);
			}

			position = getRadialPoint(position, 100f);
			position = callback.getMap().findClosestBuildSite(defender,position,600f, 3, 0);

			ct =  new ConstructionTask(defender, position, 0);
			if (buildCheck(ct) && !porcTasks.contains(ct)){
				constructionTasks.add(ct);
				porcTasks.add(ct);
			}
		}

	}

	void defendMex(AIFloat3 position){
		UnitDef llt = callback.getUnitDefByName("corllt");
		boolean needsdefense = true;
		for(Unit u:porcs){
			float dist = distance(position,u.getPos());
			if (dist < 150){
				needsdefense = false;
			}
		}

		for(ConstructionTask c:porcTasks){
			float dist = distance(position, c.getPos());
			if (dist < 150){
				needsdefense = false;
			}
		}

		if (needsdefense){
			// build an llt and a defender
			position = getRadialPoint(position, 100f);
			position = callback.getMap().findClosestBuildSite(llt,position,600f, 3, 0);

			ConstructionTask ct =  new ConstructionTask(llt, position, 0);
			if (buildCheck(ct) && !porcTasks.contains(ct)){
				constructionTasks.add(ct);
				porcTasks.add(ct);
			}
		}

	}

	void defendFac(){
		UnitDef llt = callback.getUnitDefByName("corllt");
		AIFloat3 pos = facManager.factories.get(0).getUnit().getPos();
		pos = getDirectionalPoint(pos, graphManager.getEnemyCenter(), 150f);
		pos = callback.getMap().findClosestBuildSite(llt,pos,600f, 3, 0);

		ConstructionTask ct =  new ConstructionTask(llt, pos, 0);
		if (buildCheck(ct) && !porcTasks.contains(ct)){
			constructionTasks.add(ct);
			porcTasks.add(ct);
		}
		defendedFac = true;
	}

	void porcPush(Unit unit, Unit attacker){
		UnitDef porc;
		double rand = Math.random();
		if (rand > 0.2) {
			porc = callback.getUnitDefByName("corrl");
		}else if (rand > 0.1){
			porc = callback.getUnitDefByName("corllt");
		}else{
			porc = callback.getUnitDefByName("corhlt");
		}
		AIFloat3 pos = getDirectionalPoint(attacker.getPos(), unit.getPos(), attacker.getMaxRange()+100);
		pos = callback.getMap().findClosestBuildSite(porc,pos,600f, 3, 0);

		float porcdist = Float.MAX_VALUE;

		for(Unit u:porcs){
			float dist = distance(pos,u.getPos());
			if (dist<porcdist){
				porcdist = dist;
			}
		}

		for(ConstructionTask c:porcTasks){
			float dist = distance(pos, c.getPos());
			if (dist<porcdist){
				porcdist = dist;
			}
		}

		float minporcdist = 100;

		if(porcdist > minporcdist) {
			ConstructionTask ct = new ConstructionTask(porc, pos, 0);
			if (buildCheck(ct) && !porcTasks.contains(ct)) {
				constructionTasks.add(ct);
				porcTasks.add(ct);
			}
		}
	}

	boolean needDefender(AIFloat3 position){
		if (distance(position, graphManager.getClosestSpot(position).getPos()) > 600){
			return false;
		}

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
			minporcdist = 400;
			List<Unit> enemies = callback.getEnemyUnitsIn(position, 1200f);
			if (enemies.size() > 3){
				minporcdist = 300;
			}
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
		UnitDef windgen = callback.getUnitDefByName("armwin");
		UnitDef fusion = callback.getUnitDefByName("armfus");
		UnitDef singu = callback.getUnitDefByName("cafus");
    	AIFloat3 position = worker.getPos();

		ConstructionTask ct;

		// for fusions
		if (adjustedIncome > 1000 && !facManager.factories.isEmpty() && fusions.size() < 3 && fusionTasks.isEmpty()){
			position = getNearestFac(position).getPos();
			position = getRadialPoint(position, 1200f);
			position = graphManager.getOverdriveSweetSpot(position, fusion);
			position = callback.getMap().findClosestBuildSite(fusion,position,600f, 3, 0);

			// don't build fusions too close to the fac
			AIFloat3 pos = getNearestFac(position).getPos();
			if (distance(pos, position) < 600 || warManager.isFrontLine(position)){
				return;
			}

			ct = new ConstructionTask(fusion, position, 0);
			if (buildCheck(ct)){
				constructionTasks.add(ct);
				fusionTasks.add(ct);
			}
		}
		// for singus
		if (adjustedIncome > 1500 && !facManager.factories.isEmpty() && factoryTasks.isEmpty() && fusions.size() < 5 && fusionTasks.isEmpty()){
			position = getNearestFac(position).getPos();
			position = getRadialPoint(position, 1200f);
			position = graphManager.getOverdriveSweetSpot(position, singu);
			position = callback.getMap().findClosestBuildSite(singu,position,600f, 3, 0);

			// don't build fusions too close to the fac or on the front line
			AIFloat3 pos = getNearestFac(position).getPos();
			if (distance(pos, position) < 600 || warManager.isFrontLine(position)){
				return;
			}

			ct = new ConstructionTask(singu, position, 0);
			if (buildCheck(ct)){
				constructionTasks.add(ct);
				fusionTasks.add(ct);
			}
		}
		// for solars
		else if (Math.random() > 0.66 && callback.getMap().getElevationAt(position.x, position.z) > 0){
			if (solars.size() + (windgens.size())/2 > 3) {
				position = graphManager.getNearestUnconnectedLink(position);
			}
			if (position == null){
				return;
			}
			position = graphManager.getOverdriveSweetSpot(position, solar);
			position = callback.getMap().findClosestBuildSite(solar, position, 600f, 3, 0);

			float solarDist = 400;

			// prevent ecomanager from spamming solars/windgens that graphmanager doesn't know about yet
			for (ConstructionTask s: solarTasks){
				float dist = distance(s.getPos(), position);
				if (dist < solarDist){
					return;
				}
			}

			// prevent ecomanager from spamming solars/windgens that graphmanager doesn't know about yet
			for (ConstructionTask s: windTasks){
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

			// prevent it from blocking the fac with crap
			if (!facManager.factories.isEmpty()){
				AIFloat3 pos = getNearestFac(position).getPos();
				if (distance(pos, position) < 300){
					return;
				}
			}

			ct = new ConstructionTask(solar, position, 0);
			if (buildCheck(ct) && !solarTasks.contains(ct)){
				constructionTasks.add(ct);
				solarTasks.add(ct);
			}
			// windgens
		}else {
			if (solars.size() + (windgens.size())/2 > 3) {
				position = graphManager.getNearestUnconnectedLink(position);
			}
			if (position == null){
				return;
			}
			position = graphManager.getOverdriveSweetSpot(position, windgen);
			position = callback.getMap().findClosestBuildSite(windgen, position, 600f, 3, 0);

			// prevent it from blocking the fac with crap
			if (!facManager.factories.isEmpty()){
				AIFloat3 pos = getNearestFac(position).getPos();
				if (distance(pos, position) < 300){
					return;
				}
			}

			float solarDist = 400;

			// prevent ecomanager from spamming solars/windgens that graphmanager doesn't know about yet
			for (ConstructionTask s: solarTasks){
				float dist = distance(s.getPos(), position);
				if (dist < solarDist){
					return;
				}
			}

			// prevent ecomanager from spamming solars/windgens that graphmanager doesn't know about yet
			for (ConstructionTask s: windTasks){
				float dist = distance(s.getPos(), position);
				if (dist < solarDist){
					return;
				}
			}

			ct = new ConstructionTask(windgen, position, 0);
			if (buildCheck(ct) && !windTasks.contains(ct)){
				constructionTasks.add(ct);
				windTasks.add(ct);
			}
		}
    }

	void createGridTask(Worker worker){
		ConstructionTask ct;
		UnitDef pylon = callback.getUnitDefByName("armestor");

		AIFloat3 position = graphManager.getNearestPylonSpot(worker.getPos());
		position = graphManager.getOverdriveSweetSpot(position, pylon);
		if (position == null){
			return;
		}
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

		if(gdist > 500) {
			ct = new ConstructionTask(pylon, position, 0);
			if (buildCheck(ct) && !pylonTasks.contains(ct)){
				constructionTasks.add(ct);
				pylonTasks.add(ct);
			}
		}
	}
    
    void createNanoTurretTask(Unit target){
		UnitDef nano = callback.getUnitDefByName("armnanotc");
    	AIFloat3 position = null;
    	final float buildDist = 400f;

		boolean good = false;
		while (!good) {
			position = getRadialPoint(target.getPos(), buildDist);
			position = callback.getMap().findClosestBuildSite(nano, position, 600f, 3, 0);
			if (distance(position, target.getPos()) < 500){
				good = true;
			}
		}
		ConstructionTask ct =  new ConstructionTask(nano, position, 0);
		ct.frameIssued = frame;
    	if (buildCheck(ct) && !nanoTasks.contains(ct)){
			constructionTasks.add(ct);
			nanoTasks.add(ct);
		}
    }

	void createAirPadTask(Unit target){
		UnitDef airpad = callback.getUnitDefByName("armasp");
		AIFloat3 position = null;
		final float buildDist = 400f;

		position = getRadialPoint(target.getPos(), buildDist);
		position = callback.getMap().findClosestBuildSite(airpad, position, 600f, 3, 0);

		ConstructionTask ct =  new ConstructionTask(airpad, position, 0);
		if (buildCheck(ct) && !airpadTasks.contains(ct)){
			constructionTasks.add(ct);
			airpadTasks.add(ct);
		}
	}

	
	Worker getCaretakerTarget(){
		 Worker target = null;
		int ctCount = 9001;
		for ( Worker f:facManager.factories){
			// Try to spread caretakers evenly between facs and to catch singus.
			List<Unit> nearUnits = callback.getFriendlyUnitsIn(f.getPos(), 450);
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
	
	public Worker getNearestFac(AIFloat3 position){
		 Worker nearestFac = null;
		float dist = Float.MAX_VALUE;
		for (Worker f:facManager.factories){
			float tdist = distance(position, f.getPos());
			if (tdist < dist){
				dist = tdist;
				nearestFac = f;
			}
		}
		return nearestFac;
	}
}
