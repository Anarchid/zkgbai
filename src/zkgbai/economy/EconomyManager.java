package zkgbai.economy;

import java.util.*;

import com.springrts.ai.oo.clb.*;
import zkgbai.Module;
import zkgbai.ZKGraphBasedAI;
import zkgbai.economy.tasks.*;
import zkgbai.economy.tasks.RepairTask;
import zkgbai.graph.GraphManager;
import zkgbai.graph.Link;
import zkgbai.graph.MetalSpot;
import zkgbai.los.LosManager;
import zkgbai.military.MilitaryManager;

import com.springrts.ai.oo.AIFloat3;
import zkgbai.military.UnitClasses;

import static zkgbai.kgbutil.KgbUtil.*;

public class EconomyManager extends Module {
	ZKGraphBasedAI ai;
	List<Worker> workers;
	List<Worker> assigned;
	List<Worker> populated;
	List<Worker> commanders;
	Deque<Worker> idlers;
	List<ConstructionTask> factoryTasks; // for constructors building factories
	List<ConstructionTask> berthaTasks;
	List<ConstructionTask> radarTasks;
	List<ConstructionTask> constructionTasks;
	public List<ReclaimTask> reclaimTasks;
	List<CombatReclaimTask> combatReclaimTasks;
	List<RepairTask> repairTasks;
	List<ConstructionTask> solarTasks;
	List<ConstructionTask> windTasks;
	List<ConstructionTask> fusionTasks;
	List<ConstructionTask> pylonTasks;
	public List<ConstructionTask> porcTasks;
	List<ConstructionTask> nanoTasks;
	List<ConstructionTask> storageTasks;
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
	List<Unit> screamers = new ArrayList<Unit>();
	List<Unit> berthas = new ArrayList<Unit>();
	List<Unit> storages;
	List<String> potentialFacList;
	
	public float effectiveIncomeMetal = 0;
	float effectiveIncomeEnergy = 0;
	public float staticIncome = 0;

	public float maxStorage = 0;

	float rawMexIncome = 0;
	public float baseIncome = 0;
	public float effectiveIncome = 0;
	float effectiveExpenditure = 0;

	public float teamIncome = 0;
	
	float metal = 0;
	public float energy = 0;

	float reclaimValue = 0;

	public float adjustedIncome = 0;

	boolean waterDamage = false;
	boolean defendedFac = false;
	boolean addedFacs = false;
	boolean leader = true;
	boolean havePlanes = false;
	boolean bigMap = false;
	boolean thirdFac = false;
	boolean antiNuke = false;
	boolean screamer = false;

	boolean enemyHasAir = false;
	
	int frame = 0;
	int lastPorcPushFrame = 0;
	int lastRCValueFrame = 0;

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
		this.berthaTasks = new ArrayList<ConstructionTask>();
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
		this.storageTasks = new ArrayList<ConstructionTask>();
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
		this.storages = new ArrayList<Unit>();

		this.eco = callback.getEconomy();
		
		this.m = callback.getResourceByName("Metal");
		this.e = callback.getResourceByName("Energy");

		if (callback.getMap().getWaterDamage() > 0){
			this.waterDamage = true;
		}

		this.bigMap = (callback.getMap().getHeight() + callback.getMap().getWidth() > 1536);
		this.thirdFac = (callback.getMap().getHeight() + callback.getMap().getWidth() > 1664 || ai.allies.size() > 0);

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

		if (frame % 15 == 0) {
			rawMexIncome = 0;
			effectiveIncomeEnergy = eco.getIncome(e);
			maxStorage = 500 + (500 * storages.size());

			for (Unit ms : mexes) {
				rawMexIncome += ms.getRulesParamFloat("mexIncome", 0.0f);
			}
			effectiveIncomeMetal = 0;

			for (Unit ms : mexes) {
				effectiveIncomeMetal += ms.getRulesParamFloat("current_metalIncome", 0.0f);
			}

			for (Worker w : workers) {
				effectiveIncomeMetal += w.getUnit().getRulesParamFloat("current_metalIncome", 0.0f);
			}

			baseIncome = Math.min(effectiveIncomeMetal, effectiveIncomeEnergy);

			metal = eco.getCurrent(m);
			energy = eco.getCurrent(e);

			float expendMetal = eco.getUsage(m);
			float expendEnergy = eco.getUsage(e);

			effectiveIncome = Math.min(effectiveIncomeMetal, effectiveIncomeEnergy);
			effectiveExpenditure = Math.min(expendMetal, expendEnergy);

			adjustedIncome = effectiveIncome/(1 + (ai.mergedAllies * 0.5f)) + (((float) frame)/1800f);

			staticIncome = effectiveIncome;

			if ((effectiveIncomeMetal > 20 && ai.mergedAllies == 0) || (effectiveIncomeMetal > 50)) {
				staticIncome += 0.5 * (eco.getIncome(m) - effectiveIncomeMetal);
				if (ai.mergedAllies > 0){
					staticIncome = eco.getIncome(m);
				}
			}
		}


		if (frame % 30 == 0) {
			captureMexes();
			collectReclaimables();

			// remove finished or invalidated tasks
			cleanOrders();

			if (defendedFac && adjustedIncome > 10f && mexes.size() > (1 + ai.mergedAllies) * 2){
				defendLinks();
			}

			for (Unit u: screamers){
				u.stockpile((short) 0, frame + 300);
			}
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

		if (frame % 2 == 0) {
			if (!idlers.isEmpty()) {
				assignWorkers(idlers.poll()); // assign workers to tasks
			}else if (frame % 10 == 0){
				boolean asn = false;
				for (Worker w : workers) {
					if (!assigned.contains(w) && !w.isChicken) {
						assignWorkers(w);
						asn = true;
						break;
					}
				}

				if (!asn) {
					assigned.clear();
				}
			}
		}

		return 0;
	}

    @Override
    public int unitFinished(Unit unit) {
    	
    	UnitDef def = unit.getDef();
    	String defName = def.getName();

		if(defName.equals("cormex")){
			mexes.add(unit);
			if (!defendedFac && mexes.size() > 1){
				defendFac();
			}
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

		if(defName.equals("armmstor")){
			storages.add(unit);
		}

		if (defName.equals("armfus") || defName.equals("cafus") || defName.equals("armbrtha")){
			fortifyMex(unit.getPos());
		}

		if (defName.equals("cafus")){
			defendSingu(unit.getPos());
		}

    	if (unit.getMaxSpeed() > 0 && def.getBuildOptions().size() > 0 && !defName.equals("armcsa")/*ignore athena*/) {
			Worker w = new Worker(unit);
			workers.add(w);
			assignWorkers(w);
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
					break;
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
		storageTasks.remove(finished);
		airpadTasks.remove(finished);
		factoryTasks.remove(finished);
		AATasks.remove(finished);
		berthaTasks.remove(finished);

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
		if (attacker != null && attacker.getMaxSpeed() == 0 && h.getMaxSpeed() > 0
				&& frame - lastPorcPushFrame > 15 && graphManager.isAllyTerritory(h.getPos())) {
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
		berthas.remove(unit);
		storages.remove(unit);
		screamers.remove(unit);

		// fortify mexes if they die
		if (unit.getDef().getName().equals("cormex") && (adjustedIncome > 20 || ai.mergedAllies > 1)){
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

		// if we have a dead worker, remove it.
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
		String defName = unit.getDef().getName();

    	// Track info for construction tasks
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
								if (dist < 50 && ct.buildType.getName().equals(defName)){
									ct.target = unit;
								}
							}
						}
		    		}
		    	}
			}
    	}else if (builder != null && !unit.isBeingBuilt() && !defName.equals("armcsa")){
			// instant factory plops only call unitcreated, not unitfinished.
			for (Worker w : workers){
				if (w.id == builder.getUnitId()){
					WorkerTask task = w.getTask();
					constructionTasks.remove(task);
					factoryTasks.remove(task);
					w.clearTask(frame);
				}
			}
		}

		if (defName.equals("cormex") && defendedFac && (mexes.size() > ai.mergedAllies * 2) && warManager.slasherSpam * 140 <= warManager.enemyPorcValue){
			defendMex(unit.getPos());
		}

		if(defName.equals("corrl") || defName.equals("corllt") || defName.equals("corhlt") || defName.equals("armpb") || defName.equals("armartic") || defName.equals("armdeva")){
			porcs.add(unit);
		}

		if (defName.equals("corrazor") || defName.equals("corflak") || defName.equals("armcir")){
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

		if (defName.equals("armfus") || defName.equals("cafus")){
			fusions.add(unit);
		}

		if ((defName.equals("dante") || defName.equals("scorpion")) && warManager.miscHandler.striders.isEmpty()){
			RepairTask rt = new RepairTask(unit);
			repairTasks.add(rt);
		}

		if (defName.equals("funnelweb") && facManager.numFunnels == 0){
			RepairTask rt = new RepairTask(unit);
			repairTasks.add(rt);
		}

		if (defName.equals("factoryplane")){
			havePlanes = true;
		}

		if(defName.equals("screamer")){
			screamers.add(unit);
		}

		if (defName.equals("armbrtha")){
			berthas.add(unit);
			if (!screamer){
				screamer = true;
				UnitDef screamer = callback.getUnitDefByName("screamer");
				AIFloat3 pos = callback.getMap().findClosestBuildSite(screamer, graphManager.getAllyCenter(), 600, 3, 0);
				ConstructionTask ct = new ConstructionTask(screamer, pos, 0);
				if (buildCheck(ct) && !constructionTasks.contains(ct)) {
					constructionTasks.add(ct);
				}
			}
			if (!antiNuke && berthas.size() > 2){
				antiNuke = true;
				UnitDef protector = callback.getUnitDefByName("armamd");
				AIFloat3 pos = callback.getMap().findClosestBuildSite(protector, graphManager.getAllyCenter(), 600, 3, 0);
				ConstructionTask ct = new ConstructionTask(protector, pos, 0);
				if (buildCheck(ct) && !constructionTasks.contains(ct)) {
					constructionTasks.add(ct);
				}
			}
		}

		if (defName.equals("armcsa")){
			createNanoTurretTask(unit);
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
		if (e.getDef().getName().equals("cormex") && graphManager.isAllyTerritory(e.getPos())){
			fortifyMex(e.getPos());
		}

		// check for enemy air
		if (e.getDef().isAbleToFly() && !e.getDef().getName().equals("attackdrone") && !e.getDef().getName().equals("battledrone") && !e.getDef().getName().equals("carrydrone")){
			enemyHasAir = true;
		}
		return 0;
	}

	@Override
	public int enemyCreated(Unit e){
		// capture combat reclaim for undefended enemy buildings that are started within los
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

	void assignWorkers(Worker toAssign) {
		if (toAssign.isChicken){
			return;
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
				}catch (Throwable e){
					ai.printException(e);
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

			if (ctask.facPlop && worker.getUnit().getRulesParamFloat("facplop", 0f) == 0f){
				// don't assign units to facplop tasks if they don't have a plop!
				return Float.MAX_VALUE;
			}
			if (ctask.buildType.getName().contains("factory") || ctask.buildType.getName().contains("hub") || ctask.buildType.getName().equals("armcsa")){
				// factory plops and emergency facs get maximum priority
				if (facManager.factories.size() == 0) {
					return -1000;
				}
				return ((dist/2) - ctask.buildType.getCost(m)) + (500 * (costMod - 1));
			}else if (ctask.buildType.getCost(m) > 300){
				// allow at least 3 builders for each expensive task, more if the cost is high.
				if (costMod < 4){
					return (dist/2) - (Math.min(3000, ctask.buildType.getCost(m))/costMod);
				}
				return ((dist/2) - Math.min(3000, ctask.buildType.getCost(m))) + (500 * costMod);
			}else if (ctask.buildType.getName().equals("cormex")){
				// for mexes
				return ((float) (dist/Math.log(dist)) - 100) + (600 * (costMod - 1));
			}else if (ctask.buildType.isAbleToAttack()){
				// for porc
				if (warManager.slasherSpam * 140 > warManager.enemyPorcValue){
					return dist+300;
				}
				return dist - 400 + Math.max(0, (1500 * (costMod - 2)));
			}else if (ctask.buildType.getName().equals("armnanotc") || ctask.buildType.getName().equals("armmstor") || ctask.buildType.getName().equals("armasp") || ctask.buildType.getName().equals("corrazor") || ctask.buildType.getName().equals("corflak")) {
				// for nanotowers, airpads, storages and AA
				return dist - 1000 + (500 * (costMod - 2));
			}else if (ctask.buildType.getName().equals("armestor")) {
				// for pylons
				return dist - 500 + (500 * (costMod - 1));
			}else if (ctask.buildType.getName().equals("corrad")){
				// for radar
				return dist - 300 + (1500 * (costMod - 1));
			}else if (ctask.buildType.getName().equals("armsolar") || ctask.buildType.getName().equals("armwin")){
				// for small energy
				if (energy < 50 || effectiveIncomeMetal > effectiveIncomeEnergy - 2.5f) {
					return ((dist / (float) Math.log(dist)) - 200) + (1000 * (costMod - 1));
				}else {
					return ((dist / (float) Math.log(dist))) + (1000 * (costMod - 1));
				}
			}else{
				return dist+(600 * (costMod - 1));
			}
		}

		if (task instanceof ReclaimTask) {
			ReclaimTask rtask = (ReclaimTask) task;

			if (metal/maxStorage > 0.9 || energy < 100){
				// don't favor reclaim if excessing or estalled
				return dist;
			}

			if (rtask.metalValue > 50){
				return ((float) (dist/Math.log(dist)) - 100) + Math.max(0, (250 * (costMod - 3)));
			}
			return ((float) (dist/Math.log(dist)) - 100) + (700 * (costMod - 1));
		}

		if (task instanceof CombatReclaimTask){
			// favor combat reclaim when it's possible
			return dist - 600 + Math.max(0, (600 * (costMod - 2)));
		}

		if (task instanceof RepairTask){
			RepairTask rptask = (RepairTask) task;
			if (rptask.target.getHealth() > 0) {
				if (rptask.target.getMaxSpeed() > 0 && rptask.target.getDef().isAbleToAttack() && !rptask.target.getDef().isBuilder()) {
					// for mobile combat units
					if (energy < 100){
						return dist;
					}

					if (rptask.isShieldMob && worker.getUnit().getDef().getName().equals("cornecro")){
						return dist + (100 * (costMod - 1)) - 250 - rptask.target.getMaxHealth() / (5 * costMod);
					}

					if (!graphManager.isAllyTerritory(rptask.getPos())){
						return dist + 500;
					}

					/*if (rptask.isTank){
						return dist + (100 * (costMod - 1)) - 250 - (rptask.target.getMaxHealth() - rptask.target.getHealth()) / (5 * costMod);
					}*/

					return dist + (100 * (costMod - 1)) - 250 - rptask.target.getMaxHealth() / (5 * costMod);
				}else if (rptask.target.getDef().isAbleToAttack() && !rptask.target.getDef().isBuilder()){
					// for static defenses
					return dist + (100 * (costMod - 1)) - (rptask.target.getMaxHealth() * 2) / costMod;
				}else{
					// for everything else
					return dist - 250 + (600 * (costMod - 1));
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
			if (t.target == null && !callback.getMap().isPossibleToBuildAt(t.buildType, t.getPos(), t.facing)){
				//check to make sure it isn't our own nanoframe, since update is called before unitCreated
				List<Unit> stuff = callback.getFriendlyUnitsIn(t.getPos(), 50f);
				boolean isNano = false;
				for (Unit u:stuff){
					if (u.isBeingBuilt() && u.getTeam() == myTeamID){
						isNano = true;
						break;
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
			if (frame-t.frameIssued > 900 && t.target == null){
				List<Worker> idle = t.stopWorkers(frame);
				idlers.addAll(idle);
				potentialFacList.add(t.buildType.getName());
				invalidtasks.add(t);
			}
		}
		for (ConstructionTask t: nanoTasks){
			if (frame-t.frameIssued > 600 && t.target == null){
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
		storageTasks.removeAll(invalidtasks);
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
			}else{
				List<Worker> idle = rt.stopWorkers(frame);
				idlers.addAll(idle);
				invalidtasks.add(rt);
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
		boolean hasStriders = false;
		boolean hasAir = false;
		for (Worker f:facManager.factories.values()) {
			String defName = f.getUnit().getDef().getName();
			if (defName.equals("striderhub")) {
				hasStriders = true;
			}
			if (defName.equals("factoryplane") || defName.equals("factorygunship")) {
				hasAir = true;
			}
		}

		boolean hasPlop = worker.getUnit().getRulesParamFloat("facplop", 0f) == 1f;
		if ((facManager.factories.size() == 0 && factoryTasks.isEmpty())
				|| (hasPlop && factoryTasks.size() < ai.mergedAllies + 1)
				|| (staticIncome > 40 && facManager.factories.size() == 1 && factoryTasks.isEmpty())
				|| (thirdFac && staticIncome > 65 && facManager.factories.size() == 2 && factoryTasks.isEmpty())
				|| (ai.mergedAllies > 0 && staticIncome > 65 && !hasStriders && factoryTasks.isEmpty())
				|| (ai.mergedAllies > 0 && staticIncome > 65 && !hasAir && factoryTasks.isEmpty())) {
			createFactoryTask(worker, hasPlop);
		}

		//Don't build crap right in front of the fac.
		boolean tooCloseToFac = false;
		for(Worker w:facManager.factories.values()){
			float dist = distance(position,w.getPos());
			if (dist<200){
				tooCloseToFac = true;
			}
		}

		// do we need AA?
		if (enemyHasAir && effectiveIncome > 30 && AATasks.isEmpty()){
			createAATask(worker);
		}

    	// is there sufficient energy to cover metal income?
		Boolean eFac = false; // note: cloaky uniquely requires running energy rich.
		for (Factory f:facManager.factories.values()){
			if (f.getUnit().getDef().getName().equals("factorycloak") /*|| f.getUnit().getDef().getName().equals("factoryshield")*/){
				eFac = true;
				break;
			}
		}

		float eRatio;
		if (effectiveIncomeMetal > 25f && eFac) {
			eRatio = 2.0f + (((float) fusions.size()) / 2f);
		}else if (bigMap) {
			eRatio = 1.5f;
		}else if (effectiveIncomeMetal > 20f || eFac) {
			eRatio = 1.5f + (((float)fusions.size())/2f);
		}else{
			eRatio = 1f;
		}

		if (ai.mergedAllies > 0){
			eRatio = 1.5f + (((float)fusions.size())/2f);
		}

		int energySize = (2 * (solars.size() + solarTasks.size())) + windgens.size() + windTasks.size();
		if ((Math.round(rawMexIncome * eRatio) > energySize && solarTasks.size() + windTasks.size() < facManager.numWorkers)
				|| (effectiveIncomeMetal > 12f && energy < 100 && solarTasks.size() + windTasks.size() < facManager.numWorkers)) {
			createEnergyTask(worker);
		}
		createFusionTask(worker);

    	
    	// do we need caretakers?
    	if(((float)(nanos.size() + nanoTasks.size()) < Math.floor(staticIncome/10f) - facManager.factories.size() || (metal/maxStorage > 0.5 && energy/maxStorage > 0.5))
				&& baseIncome > 20f * (1f + ai.mergedAllies/2f) && nanoTasks.size() < Math.max(facManager.factories.size() / 2, 1)){
			Worker target = getCaretakerTarget();
			createNanoTurretTask(target.getUnit());
    	}

		//do we need storages?
		if (metal/maxStorage > 0.8f && effectiveIncomeMetal > 15f * (1f + ai.mergedAllies) && storageTasks.isEmpty() && facManager.factories.size() > 0){
			Worker target = getNearestFac(position);
			createStorageTask(target.getUnit());
		}

		// do we need airpads for planes
		if (havePlanes && airpadTasks.isEmpty() && staticIncome > 15f
				&& ((float)airpads.size() < warManager.bomberHandler.getBomberSize()/5f && warManager.bomberHandler.getBomberSize() > 4)){
			Worker target = getCaretakerTarget();
			createAirPadTask(target.getUnit());
		}

		// do we need radar?
		if (needRadar(position) && effectiveIncome > 20 && energy > 100 && !tooCloseToFac){
    		createRadarTask(worker);
    	}

		// do we need pylons?
		if (fusions.size() > 2 && !tooCloseToFac){
			createGridTask(worker.getPos());
		}

		// do we need berthas?
		if (((ai.allies.size() > 0 && hasStriders) || ai.allies.isEmpty()) && bigMap && staticIncome > 85 && !fusions.isEmpty() && berthaTasks.isEmpty()){
			createBerthaTask();
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
		AIFloat3 position = worker.getPos();
		position = graphManager.getClosestHaven(position);
		UnitDef aa;

		if (Math.random() > 0.5 || adjustedIncome < 30f){
			aa = callback.getUnitDefByName("corrazor");
		}else{
			aa = callback.getUnitDefByName("armcir");
		}

		position = callback.getMap().findClosestBuildSite(aa,position,600f, 3, 0);

		if (graphManager.isFrontLine(position)){
			return;
		}

		// don't build AA too close together.
		for (Unit a:AAs){
			if (distance(a.getPos(), position) < 500){
				return;
			}
		}

		ConstructionTask ct =  new ConstructionTask(aa, position, 0);
		if (buildCheck(ct) && !porcTasks.contains(ct)){
			constructionTasks.add(ct);
			AATasks.add(ct);
		}
	}

	void createBerthaTask(){
		AIFloat3 position = null;

		UnitDef bertha = callback.getUnitDefByName("armbrtha");
		boolean good = false;
		int i = 0;

		while (!good) {
			position = getRadialPoint(graphManager.getAllyCenter(), 600f + ((float) Math.random() * 600f));
			position = callback.getMap().findClosestBuildSite(bertha, position, 600f, 3, 0);

			good = true;
			for (Unit b : berthas) {
				if (distance(b.getPos(), position) < 350) {
					good = false;
				}
			}
			i++;
			if (i > 25){
				return;
			}
		}

		ConstructionTask ct =  new ConstructionTask(bertha, position, 0);
		if (buildCheck(ct) && !berthaTasks.contains(ct)){
			constructionTasks.add(ct);
			berthaTasks.add(ct);
		}
	}
    
    void createFactoryTask(Worker worker, boolean isPlop){
		UnitDef factory;

		if (potentialFacList.isEmpty()){
			potentialFacList = terrainManager.getInitialFacList();
			addedFacs = false;
		}

		if (facManager.factories.size() > 0 && !isPlop && !addedFacs){
			potentialFacList.add("factorygunship");
			potentialFacList.add("striderhub");
			potentialFacList.add("factoryplane");

			if (facManager.factories.size() > 0 && facManager.factories.size() < 3) {
				// only build air or striders as second and third facs.
				potentialFacList.remove("factoryspider");
				potentialFacList.remove("factorycloak");
				potentialFacList.remove("factoryveh");
				potentialFacList.remove("factoryshield");
				potentialFacList.remove("factoryamph");
				potentialFacList.remove("factoryhover");
				potentialFacList.remove("factoryship");
				potentialFacList.remove("factoryjump");
				potentialFacList.remove("factorytank");
			}
			addedFacs = true;
		}

		boolean hasStriders = false;
		boolean hasShields = false;
		for (Worker f:facManager.factories.values()){
			String defName = f.getUnit().getDef().getName();
			if (defName.equals("striderhub")){
				hasStriders = true;
			}

			if (defName.equals("factoryshield")){
				hasShields = true;
			}

			// don't build the same fac twice.
			potentialFacList.remove(defName);
		}

		if (potentialFacList.isEmpty()){
			return;
		}

		/*if (facManager.factories.size() == 0){
			String facName = "factoryveh";
			factory = callback.getUnitDefByName(facName);
			potentialFacList.remove(facName);
		}else*/ if ((facManager.factories.size() < 2 && !hasShields) || hasStriders || isPlop) {
			int i = (int) Math.round(Math.random() * ((double) potentialFacList.size() - 1.0));
			String facName = potentialFacList.get(i);
			factory = callback.getUnitDefByName(facName);
			potentialFacList.remove(i);

			// don't build two airfacs
			if (facName.equals("factoryplane") && ai.mergedAllies < 5){
				potentialFacList.remove("factorygunship");
			}else if (facName.equals("factorygunship") && ai.mergedAllies < 5){
				potentialFacList.remove("factoryplane");
			}
		}else{
			factory = callback.getUnitDefByName("striderhub");
			potentialFacList.remove("striderhub");
		}

		AIFloat3 position = worker.getUnit().getPos();
		position.x = position.x + 150;
		position.z = position.z + 150;

		int i = 0;
		boolean good = false;
		if (facManager.factories.isEmpty() || isPlop) {
			while (!good) {
				if (i++ > 10) return;
				position = worker.getPos();
				position = getDirectionalPoint(position, graphManager.getMapCenter(), 200f);

				// don't let factories get blocked by random crap, nor build them on top of mexes
				while (!callback.getFriendlyUnitsIn(position, 150f).isEmpty() || distance(graphManager.getClosestSpot(position).getPos(), position) < 100 || !buildCheck(new ConstructionTask(factory, position, 0))) {
					if (i++ > 10) return;
					position = getRadialPoint(position, 200f);
					position = callback.getMap().findClosestBuildSite(factory,position,600f, 3, 0);
				}

				ConstructionTask ct =  new ConstructionTask(factory, position, 0); // only for preventing stupid placement
				if (buildCheck(ct)){
					good = true;
				}
			}
		}else{
			while (!good) {
				if (i++ > 10) return;
				AIFloat3 facpos = getNearestFac(position).getPos();

				// don't cram factories together
				position = getRadialPoint(facpos, 800f);
				position = graphManager.getClosestSpot(position).getPos();

				// don't let factories get blocked by random crap, nor build them on top of mexes
				while (callback.getFriendlyUnitsIn(position, 250f).size() > 0 || distance(graphManager.getClosestSpot(position).getPos(), position) < 100){
					if (i++ > 10) return;
					position = getRadialPoint(position, 200f);
				}

				position = callback.getMap().findClosestBuildSite(factory, position, 600f, 3, 0);

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

		ConstructionTask ct =  new ConstructionTask(factory, position, facing, isPlop);
		ct.frameIssued = frame;
		if (buildCheck(ct) && !factoryTasks.contains(ct)){
			constructionTasks.add(ct);
			factoryTasks.add(ct);
		}
    }

	void defendSingu(AIFloat3 position){
		UnitDef llt = callback.getUnitDefByName("corllt");
		UnitDef defender = callback.getUnitDefByName("corrl");
		UnitDef hlt = callback.getUnitDefByName("corhlt");
		UnitDef cobra = callback.getUnitDefByName("corflak");
		UnitDef shield = callback.getUnitDefByName("corjamt");
		UnitDef protector = callback.getUnitDefByName("armamd");
		AIFloat3 pos;
		Boolean good = false;
		ConstructionTask ct;
		int i = 0;

		// build an llt
		while (!good) {
			if (i++ > 100) break;
			pos = getRadialPoint(position, 150f);
			pos = callback.getMap().findClosestBuildSite(llt, pos, 600f, 3, 0);

			ct = new ConstructionTask(llt, pos, 0);
			if (buildCheck(ct) && !porcTasks.contains(ct)) {
				constructionTasks.add(ct);
				porcTasks.add(ct);
				good = true;
			}
		}

		good = false;
		i = 0;
		// build a defender
		while (!good) {
			if (i++ > 100) break;
			pos = getRadialPoint(position, 150f);
			pos = callback.getMap().findClosestBuildSite(defender, pos, 600f, 3, 0);

			ct = new ConstructionTask(defender, pos, 0);
			if (buildCheck(ct) && !porcTasks.contains(ct)) {
				constructionTasks.add(ct);
				porcTasks.add(ct);
				good = true;
			}
		}

		good = false;
		i = 0;
		// build an hlt
		while (!good) {
			if (i++ > 100) break;
			pos = getRadialPoint(position, 150f);
			pos = callback.getMap().findClosestBuildSite(hlt, pos, 600f, 3, 0);

			ct = new ConstructionTask(hlt, pos, 0);
			if (buildCheck(ct) && !porcTasks.contains(ct)) {
				constructionTasks.add(ct);
				porcTasks.add(ct);
				good = true;
			}
		}

		good = false;
		i = 0;
		// build a cobra
		while (!good) {
			if (i++ > 100) break;
			pos = getRadialPoint(position, 200f);
			pos = callback.getMap().findClosestBuildSite(cobra, pos, 600f, 3, 0);

			ct = new ConstructionTask(cobra, pos, 0);
			if (buildCheck(ct) && !porcTasks.contains(ct)) {
				constructionTasks.add(ct);
				AATasks.add(ct);
				good = true;
			}
		}

		good = false;
		i = 0;
		// build an area shield
		while (!good) {
			if (i++ > 100) break;
			pos = getRadialPoint(position, 75f);
			pos = callback.getMap().findClosestBuildSite(shield, pos, 600f, 3, 0);

			ct = new ConstructionTask(shield, pos, 0);
			if (buildCheck(ct) && !constructionTasks.contains(ct)) {
				constructionTasks.add(ct);
				good = true;
			}
		}

		good = false;
		i = 0;
		// build an antinuke
		while (!good) {
			if (i++ > 100) break;
			pos = getRadialPoint(position, 350f);
			pos = callback.getMap().findClosestBuildSite(protector, pos, 600f, 3, 0);

			ct = new ConstructionTask(protector, pos, 0);
			if (buildCheck(ct) && !constructionTasks.contains(ct)) {
				constructionTasks.add(ct);
				good = true;
			}
		}
	}

	void defendLink(AIFloat3 position){
		UnitDef hlt = callback.getUnitDefByName("corhlt");
		UnitDef stardust = callback.getUnitDefByName("armdeva");
		UnitDef llt = callback.getUnitDefByName("corllt");
		UnitDef defender = callback.getUnitDefByName("corrl");

		// don't spam redundant porc
		for(Unit u:porcs){
			float dist = distance(position,u.getPos());
			if (dist < 600){
				return;
			}
		}

		for(ConstructionTask c:porcTasks){
			float dist = distance(position, c.getPos());
			if (dist < 600){
				return;
			}
		}

		// build an llt and two defenders
		position = callback.getMap().findClosestBuildSite(llt,position,600f, 3, 0);

		ConstructionTask ct =  new ConstructionTask(llt, position, 0);
		if (buildCheck(ct) && !porcTasks.contains(ct)){
			constructionTasks.add(ct);
			porcTasks.add(ct);
		}

		if (adjustedIncome > 25) {
			for (int i = 0; i < 2; i++) {
				position = getRadialPoint(position, 100f);
				position = callback.getMap().findClosestBuildSite(defender, position, 600f, 3, 0);

				ct = new ConstructionTask(defender, position, 0);
				if (buildCheck(ct) && !porcTasks.contains(ct)) {
					constructionTasks.add(ct);
					porcTasks.add(ct);
				}
			}
		}
	}

	void fortifyMex(AIFloat3 position){
		UnitDef llt = callback.getUnitDefByName("corllt");
		UnitDef defender = callback.getUnitDefByName("corrl");
		ConstructionTask ct;
		MetalSpot ms = graphManager.getClosestSpot(position);

		// don't spam redundant porc
		for(ConstructionTask c:porcTasks){
			float dist = distance(position, c.getPos());
			if (dist < 250){
				return;
			}
		}

		if (effectiveIncome > 30) {
			// build an llt and two defenders
			UnitDef porc = Math.random() > 0.5 ? callback.getUnitDefByName("corhlt") : callback.getUnitDefByName("armpb");
			position = getRadialPoint(position, 150f);
			position = callback.getMap().findClosestBuildSite(llt, position, 600f, 3, 0);

			ct = new ConstructionTask(llt, position, 0);
			if (buildCheck(ct) && !porcTasks.contains(ct)) {
				constructionTasks.add(ct);
				porcTasks.add(ct);
			}

			for (int i = 0; i < 2; i++) {
				position = getRadialPoint(position, 100f);
				position = callback.getMap().findClosestBuildSite(defender, position, 600f, 3, 0);

				ct = new ConstructionTask(defender, position, 0);
				if (buildCheck(ct) && !porcTasks.contains(ct)) {
					constructionTasks.add(ct);
					porcTasks.add(ct);
				}
			}

			if (ms.enemyShadowed){
				position = getRadialPoint(position, 150f);
				position = callback.getMap().findClosestBuildSite(porc, position, 600f, 3, 0);

				ct = new ConstructionTask(porc, position, 0);
				if (buildCheck(ct) && !porcTasks.contains(ct)) {
					constructionTasks.add(ct);
					porcTasks.add(ct);
				}
			}
		}else{
			position = getRadialPoint(position, 150f);
			position = callback.getMap().findClosestBuildSite(llt, position, 600f, 3, 0);

			ct = new ConstructionTask(llt, position, 0);
			if (buildCheck(ct) && !porcTasks.contains(ct)) {
				constructionTasks.add(ct);
				porcTasks.add(ct);
			}

			position = getRadialPoint(position, 150f);
			position = callback.getMap().findClosestBuildSite(defender, position, 600f, 3, 0);

			ct = new ConstructionTask(defender, position, 0);
			if (buildCheck(ct) && !porcTasks.contains(ct)) {
				constructionTasks.add(ct);
				porcTasks.add(ct);
			}
		}
	}

	void defendMex(AIFloat3 position){
		UnitDef llt = callback.getUnitDefByName("corllt");
		UnitDef defender = callback.getUnitDefByName("corrl");


		for (Unit u : porcs) {
			float dist = distance(position, u.getPos());
			if (dist < 300) {
				return;
			}
		}

		for (ConstructionTask c : porcTasks) {
			float dist = distance(position, c.getPos());
			if (dist < 300) {
				return;
			}
		}

		if (adjustedIncome < 30) {
			// build an llt
			position = getRadialPoint(position, 100f);
			position = callback.getMap().findClosestBuildSite(llt, position, 600f, 3, 0);

			ConstructionTask ct = new ConstructionTask(llt, position, 0);
			if (buildCheck(ct) && !porcTasks.contains(ct)) {
				constructionTasks.add(ct);
				porcTasks.add(ct);
			}
		}else{
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

	void defendFac(){
		UnitDef llt = callback.getUnitDefByName("corllt");
		AIFloat3 pos = new AIFloat3(0,0,0);
		int count = 0;

		// get the avg position of the fac and extant mexes.
		for (Factory f: facManager.factories.values()){
			AIFloat3 fpos = f.getPos();
			pos.x += fpos.x;
			pos.z += fpos.z;
			count++;
		}

		for (Unit u: mexes){
			AIFloat3 uPos = u.getPos();
			pos.x += uPos.x;
			pos.z += uPos.z;
			count++;
		}

		pos.x /= count;
		pos.z /= count;

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
		if (rand > 0.3) {
			porc = callback.getUnitDefByName("corrl");
		}else if (rand > 0.15){
			porc = callback.getUnitDefByName("corllt");
		}else {
			porc = Math.random() > 0.5 ? callback.getUnitDefByName("corhlt") : callback.getUnitDefByName("armpb");
		}
		AIFloat3 pos = getDirectionalPoint(attacker.getPos(), unit.getPos(), attacker.getMaxRange()+100);
		pos = callback.getMap().findClosestBuildSite(porc,pos,600f, 3, 0);

		for (Unit u : porcs) {
			float dist = distance(pos, u.getPos());
			if (dist < 225) {
				return;
			}
		}

		for (ConstructionTask c : porcTasks) {
			float dist = distance(pos, c.getPos());
			if (dist < 225) {
				return;
			}
		}

		ConstructionTask ct = new ConstructionTask(porc, pos, 0);
		if (buildCheck(ct) && !porcTasks.contains(ct)) {
			constructionTasks.add(ct);
			porcTasks.add(ct);
		}
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

	void defendLinks(){
		for (Link l:graphManager.getLinks()){
			if (l.length < 600 && graphManager.isAllyTerritory(l.getPos())){
				defendLink(l.getPos());
			}
		}
	}
    
    void createEnergyTask(Worker worker){
    	UnitDef solar = callback.getUnitDefByName("armsolar");
		UnitDef windgen = callback.getUnitDefByName("armwin");
    	AIFloat3 position = worker.getPos();

		ConstructionTask ct;

		// for solars
		if (((solars.size() + solarTasks.size()) < Math.round(rawMexIncome/2f) || solars.size() + solarTasks.size() * 2 < (windgens.size() + windTasks.size()))
				&& callback.getMap().getElevationAt(position.x, position.z) > 0){
			if (position == null){
				return;
			}
			position = graphManager.getOverdriveSweetSpot(position, solar);
			// in case all metal spots are connected
			if (position == null){
				return;
			}
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

			// prevent it from blocking the fac with crap
			if (!facManager.factories.isEmpty() && solars.size() + windgens.size() > 2){
				AIFloat3 pos = getNearestFac(position).getPos();
				if (distance(pos, position) < 200){
					return;
				}
			}

			ct = new ConstructionTask(solar, position, 0);
			if (buildCheck(ct) && !solarTasks.contains(ct)){
				constructionTasks.add(ct);
				solarTasks.add(ct);
			}
		}else {
			// for windgens
			if (position == null){
				return;
			}
			position = graphManager.getOverdriveSweetSpot(position, windgen);
			// in case all metal spots are connected
			if (position == null){
				return;
			}
			position = callback.getMap().findClosestBuildSite(windgen, position, 600f, 3, 0);

			// prevent it from blocking the fac with crap
			if (!facManager.factories.isEmpty() && solars.size() + windgens.size() > 2){
				AIFloat3 pos = getNearestFac(position).getPos();
				if (distance(pos, position) < 200){
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

	void createFusionTask(Worker worker){
		UnitDef fusion = callback.getUnitDefByName("armfus");
		UnitDef singu = callback.getUnitDefByName("cafus");
		Worker fac = getNearestFac(worker.getPos());
		if (fac == null) return;
		AIFloat3 position = fac.getPos();
		ConstructionTask ct;

		// for fusions
		if (adjustedIncome > 35 && energy > 100 && ((float) graphManager.getNeutralSpots().size()/(float) graphManager.getMetalSpots().size() < 0.5) && Math.floor(effectiveIncome/25) > fusions.size() && !facManager.factories.isEmpty() && fusions.size() < 3 && fusionTasks.isEmpty()){
			boolean good = false;
			int i = 0;
			while (!good) {
				if (i++ > 10) return;
				position = getRadialPoint(position, 1600f);
				position = graphManager.getClosestSpot(position).getPos();
				if (position == null) {
					continue;
				}
				position = callback.getMap().findClosestBuildSite(fusion, position, 600f, 3, 0);

				// don't build fusions too close to the fac
				AIFloat3 pos = getNearestFac(position).getPos();
				if (distance(pos, position) > 300 && !graphManager.isFrontLine(position) && !graphManager.isEnemyTerritory(position) && getFusionDist(position) > 400) {
					ct = new ConstructionTask(fusion, position, 0);
					if (buildCheck(ct)) {
						constructionTasks.add(ct);
						fusionTasks.add(ct);
						good = true;
					}
				}
			}
		}
		// for singus
		else if (fusions.size() > 2 && fusions.size() < 5 && ((float) graphManager.getNeutralSpots().size()/(float) graphManager.getMetalSpots().size() < 0.33) && !facManager.factories.isEmpty() && fusionTasks.isEmpty()){
			boolean good = false;
			int i = 0;
			while (!good) {
				if (i++ > 10) return;
				position = getRadialPoint(position, 1600f);
				position = graphManager.getClosestSpot(position).getPos();
				if (position == null) {
					continue;
				}
				position = callback.getMap().findClosestBuildSite(singu, position, 600f, 3, 0);

				// don't build fusions too close to the fac or on the front line
				AIFloat3 pos = getNearestFac(position).getPos();
				if (distance(pos, position) > 300 && !graphManager.isFrontLine(position) && !graphManager.isEnemyTerritory(position) && getFusionDist(position) > 300) {
					ct = new ConstructionTask(singu, position, 0);
					if (buildCheck(ct)) {
						constructionTasks.add(ct);
						fusionTasks.add(ct);
						good = true;
					}
				}
			}
		}
	}

	void createGridTask(AIFloat3 position){
		ConstructionTask ct;
		UnitDef pylon = callback.getUnitDefByName("armestor");

		position = graphManager.getOverdriveSweetSpot(position, pylon);
		if (position == null){
			return;
		}
		position = callback.getMap().findClosestBuildSite(pylon,position,600f, 3, 0);

		// don't build pylons in the middle of nowhere
		if (getFusionDist(position) > 1500){
			return;
		}

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

		if(gdist > 750) {
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

		int i = 0;
		while (true) {
			position = getRadialPoint(target.getPos(), buildDist);
			position = callback.getMap().findClosestBuildSite(nano, position, 600f, 3, 0);
			if (distance(position, target.getPos()) < 500){
				ConstructionTask ct =  new ConstructionTask(nano, position, 0);
				ct.frameIssued = frame;
				if (buildCheck(ct) && !nanoTasks.contains(ct)){
					constructionTasks.add(ct);
					nanoTasks.add(ct);
					return;
				}
			}
			i++;
			if (i > 10) return;
		}

    }

	void createStorageTask(Unit target){
		UnitDef stor = callback.getUnitDefByName("armmstor");
		AIFloat3 position = null;
		final float buildDist = 400f;

		boolean good = false;
		while (!good) {
			position = getRadialPoint(target.getPos(), buildDist);
			position = callback.getMap().findClosestBuildSite(stor, position, 600f, 3, 0);
			if (distance(position, target.getPos()) < 500){
				good = true;
			}
		}
		ConstructionTask ct =  new ConstructionTask(stor, position, 0);
		ct.frameIssued = frame;
		if (buildCheck(ct) && !nanoTasks.contains(ct)){
			constructionTasks.add(ct);
			storageTasks.add(ct);
		}
	}

	void createAirPadTask(Unit target){
		if (target.getDef().getName().equals("striderhub")) return;
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
		for (Worker f:facManager.factories.values()){
			// Try to spread caretakers evenly between facs and to catch singus.
			int numCT = 0;
			for (Unit u : nanos){
				if (distance(u.getPos(), f.getPos()) < 500f && !u.isBeingBuilt()){
					numCT++;
				}
			}
			for (ConstructionTask c: nanoTasks){
				if (distance(c.getPos(), f.getPos()) < 500f){
					numCT++;
				}
			}
			if (f.getUnit().getDef().getName().equals("striderhub")){
				numCT /= 2;
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
			if (reclaimTasks.size() > facManager.numWorkers){
				break;
			}
			if (f.getDef().getContainedResource(m) > 0 && f.getDef().isReclaimable()){
				ReclaimTask rt = new ReclaimTask(f, f.getDef().getContainedResource(m));
				if (!reclaimTasks.contains(rt)){
					reclaimTasks.add(rt);
				}
			}
		}
	}

	float getReclaimValue(){
		if (effectiveIncome < 20f){
			return 0f;
		}
		if (lastRCValueFrame == 0 || frame - lastRCValueFrame > 300){
			reclaimValue = 0;
			lastRCValueFrame = frame;
			List<Feature> feats = callback.getFeatures();
			for (Feature f : feats) {
				reclaimValue += f.getDef().getContainedResource(m);
			}
		}
		return reclaimValue;
	}
	
	public Worker getNearestFac(AIFloat3 position){
		 Worker nearestFac = null;
		float dist = Float.MAX_VALUE;
		for (Worker f:facManager.factories.values()){
			float tdist = distance(position, f.getPos());
			if (tdist < dist){
				dist = tdist;
				nearestFac = f;
			}
		}
		return nearestFac;
	}

	public Float getFusionDist(AIFloat3 position){
		float dist = Float.MAX_VALUE;
		for (Unit f:fusions){
			float tdist = distance(position, f.getPos());
			if (tdist < dist){
				dist = tdist;
			}
		}
		return dist;
	}
}
