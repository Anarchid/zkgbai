package zkgbai.economy;

import java.util.*;
import java.util.Map;

import com.springrts.ai.oo.clb.*;
import zkgbai.Module;
import zkgbai.ZKGraphBasedAI;
import zkgbai.economy.tasks.*;
import zkgbai.economy.tasks.RepairTask;
import zkgbai.graph.GraphManager;
import zkgbai.graph.Link;
import zkgbai.graph.MetalSpot;
import zkgbai.kgbutil.HeightMap;
import zkgbai.kgbutil.TerrainAnalyzer;
import zkgbai.los.LosManager;
import zkgbai.military.Enemy;
import zkgbai.military.MilitaryManager;

import com.springrts.ai.oo.AIFloat3;
import zkgbai.military.UnitClasses;
import zkgbai.military.tasks.DefenseTarget;

import static zkgbai.kgbutil.KgbUtil.*;

public class EconomyManager extends Module {
	ZKGraphBasedAI ai;
	public Map<Integer, Worker> workers = new HashMap<>();
	public List<Worker> commanders = new ArrayList<>();
	public Queue<Worker> idlers = new LinkedList<>();
	public Queue<Worker> assigned = new LinkedList<>();
	public Queue<Worker> seeders = new LinkedList<>();
	List<ConstructionTask> factoryTasks = new ArrayList<>();
	List<ConstructionTask> superWepTasks = new ArrayList<>();
	List<ConstructionTask> radarTasks = new ArrayList<>();
	public List<ConstructionTask> constructionTasks = new ArrayList<>();
	public List<ReclaimTask> reclaimTasks = new ArrayList<>();
	public List<ReclaimTask> energyReclaimTasks = new ArrayList<>();
	public List<CombatReclaimTask> combatReclaimTasks = new ArrayList<>();
	List<RepairTask> repairTasks = new ArrayList<>();
	List<ConstructionTask> solarTasks = new ArrayList<>();
	List<ConstructionTask> windTasks = new ArrayList<>();
	List<ConstructionTask> fusionTasks = new ArrayList<>();
	List<ConstructionTask> pylonTasks = new ArrayList<>();
	public List<ConstructionTask> porcTasks = new ArrayList<>();
	List<ConstructionTask> nanoTasks = new ArrayList<>();
	List<ConstructionTask> storageTasks = new ArrayList<>();
	List<ConstructionTask> AATasks = new ArrayList<>();
	List<ConstructionTask> airpadTasks = new ArrayList<>();
	List<Unit> radars = new ArrayList<>();
	public List<Unit> porcs = new ArrayList<>();
	public Map<Integer, Nanotower> nanos = new HashMap<>();
	List<Unit> airpads = new ArrayList<>();
	public List<Unit> fusions = new ArrayList<>();
	List<Unit> mexes = new ArrayList<>();
	List<Unit> solars = new ArrayList<>();
	List<Unit> windgens = new ArrayList<>();
	List<Unit> pylons = new ArrayList<>();
	List<Unit> AAs = new ArrayList<>();
	List<Unit> screamers = new ArrayList<>();
	List<Unit> superWeps = new ArrayList<>();
	List<Unit> storages = new ArrayList<>();
	List<String> potentialFacList;
	Set<Integer> newWorkers = new HashSet<>();
	
	Unit sparrow = null;
	
	public float effectiveIncomeMetal = 0;
	float effectiveIncomeEnergy = 0;
	public float staticIncome = 0;
	public float reclaimIncome = 0;
	Queue<Float> reclaimRecord = new LinkedList<>();

	public float maxStorage = 0;

	float rawMexIncome = 0;
	public float baseIncome = 0;
	public float effectiveIncome = 0;
	float effectiveExpenditure = 0;
	
	float metal = 0;
	public float energy = 0;

	float reclaimValue = 0;
	boolean energyReclaim = false;

	public float adjustedIncome = 0;
	
	public int greed = 0;
	
	boolean bigMap;
	boolean waterDamage;
	boolean defendedFac = false;
	boolean addedFacs = false;
	boolean havePlanes = false;
	boolean antiNuke = false;
	boolean screamer = false;
	
	boolean hasSuperWep = false;

	boolean enemyHasAir = false;
	
	boolean morphedComs = false;
	
	boolean gotNuked = false;
	
	int frame = 0;
	int lastRCValueFrame = 0;

	static short OPTION_SHIFT_KEY = (1 << 5);
	static final int CMD_PRIORITY = 34220;
	static final int CMD_MORPH = 31210;
	static final int CMD_MISC_PRIORITY = 34221;
	static final int CMD_MORPH_UPGRADE_INTERNAL = 31207;
	
	int sWep = 0;
	boolean choseSupWep = false;
	
	Economy eco;
	Resource m;
	Resource e;
	
	private OOAICallback callback;
	private GraphManager graphManager;
	private MilitaryManager warManager;
	private LosManager losManager;
	private TerrainAnalyzer terrainManager;
	private FactoryManager facManager;
	
	private UnitClasses unitTypes;
	private BuildIDs buildIDs;
	private HeightMap heightMap;
	
	String userFac = null;
	
	int lastRepairedUnit = 0;
	Worker lastDamagedWorker = null;

	int nukeWepID;
	int wolvMineID;
	
	public EconomyManager(ZKGraphBasedAI ai){
		this.ai = ai;
		this.callback = ai.getCallback();
		this.eco = callback.getEconomy();
		
		this.m = callback.getResourceByName("Metal");
		this.e = callback.getResourceByName("Energy");

		this.waterDamage = (callback.getMap().getWaterDamage() > 0);

		this.bigMap = ai.mapDiag > 1270f;
		
		this.unitTypes = UnitClasses.getInstance();
		this.heightMap = new HeightMap(callback);
	}

	@Override
	public int init(int AIID, OOAICallback cb){
		this.warManager = ai.warManager;
		this.graphManager = ai.graphManager;
		this.losManager = ai.losManager;
		this.facManager = ai.facManager;

		this.terrainManager = new TerrainAnalyzer();
		this.potentialFacList = terrainManager.getInitialFacList();
		this.buildIDs = new BuildIDs(callback);

		nukeWepID = callback.getWeaponDefByName("staticnuke_crblmssl").getWeaponDefId();
		wolvMineID = callback.getUnitDefByName("wolverine_mine").getUnitDefId();

		captureMexes();
		
		return 0;
	}
	
	@Override
	public String getModuleName() {
		// TODO Auto-generated method stub
		return "EconomyManager";
	}
	
	@Override
	public int message(int player, String message) {
		if (ai.mergedAllies == 0 && message.startsWith("kgbfac")){
			if (message.endsWith("veh")){
				userFac = "factoryveh";
			}else if (message.endsWith("hover")){
				userFac = "factoryhover";
			}else if (message.endsWith("tank")){
				userFac = "factorytank";
			}else if (message.endsWith("cloak")){
				userFac = "factorycloak";
			}else if (message.endsWith("shield")){
				userFac = "factoryshield";
			}else if (message.endsWith("amph")){
				userFac = "factoryamph";
			}else if (message.endsWith("spider")){
				userFac = "factoryspider";
			}else{
				ai.say("Invalid factory name!");
				ai.say("Valid choices: veh, hover, tank, cloak, shield, amph, spider");
			}
			
			if (userFac != null){
				ai.say("Set first factory: " + userFac);
			}
		}else if (ai.mergedAllies > 0 && message.startsWith("kgbfac")){
			ai.say("Teams mode, cannot set first factory!");
		}
		return 0;
	}
	
	@Override
	public int update(int frame) {
		this.frame = frame;
		
		if (frame % 300 == ai.offset % 300) checkNanos();

		if (frame % 15 == ai.offset % 15) {
			//checkShields();
			rawMexIncome = 0;
			effectiveIncomeMetal = 2f * (1 + ai.mergedAllies);
			effectiveIncomeEnergy = 2f * (1 + ai.mergedAllies);

			maxStorage = Math.max(0.5f, (500f * (commanders.size() + storages.size())));

			for (Unit ms : mexes) {
				rawMexIncome += ms.getRulesParamFloat("mexIncome", 0.0f);
			}

			for (Unit ms : mexes) {
				effectiveIncomeMetal += ms.getRulesParamFloat("current_metalIncome", 0.0f);
			}
			
			for (Worker w : commanders) {
				effectiveIncomeMetal += w.getUnit().getRulesParamFloat("current_metalIncome", 0.0f);
				effectiveIncomeEnergy += w.getUnit().getRulesParamFloat("current_energyIncome", 0.0f);
			}

			for (Unit s: solars){
				effectiveIncomeEnergy += s.getRulesParamFloat("current_energyIncome", 0.0f);
			}

			for (Unit w:windgens){
				effectiveIncomeEnergy += w.getRulesParamFloat("current_energyIncome", 0.0f);
			}

			for (Unit f:fusions){
				effectiveIncomeEnergy += f.getRulesParamFloat("current_energyIncome", 0.0f);
			}

			baseIncome = Math.min(effectiveIncomeMetal, effectiveIncomeEnergy);

			metal = eco.getCurrent(m);
			energy = eco.getCurrent(e);

			float expendMetal = eco.getUsage(m);
			float expendEnergy = eco.getUsage(e);

			effectiveIncome = Math.min(effectiveIncomeMetal, effectiveIncomeEnergy);
			effectiveExpenditure = Math.min(expendMetal, expendEnergy);

			staticIncome = effectiveIncome;

			float currentReclaim = 0f;
			for (Worker w:workers.values()){
				currentReclaim += w.getUnit().getResourceMake(m);
			}
			reclaimRecord.add(currentReclaim);

			// Take a 5 second poll of reclaim income, add half to static income to turn reclaim into units.
			if (reclaimRecord.size() > 10){
				reclaimRecord.poll();

				reclaimIncome = 0;
				for (Float f: reclaimRecord){
					reclaimIncome += f;
				}
				reclaimIncome /= 10f;

				if (staticIncome > 20f || graphManager.eminentTerritory){
					staticIncome += reclaimIncome;
				}else {
					staticIncome += reclaimIncome/2f;
				}
			}

			adjustedIncome = (staticIncome/(1 + (ai.mergedAllies))) + ((1f + ai.mergedAllies) * (frame/1800f));
			
			if (fusions.size() > 3) {
				ArrayList<Float> params = new ArrayList<>();
				if (!fusionTasks.isEmpty()) {
					params.add(1f);
					for (Unit u : superWeps) {
						u.executeCustomCommand(CMD_PRIORITY, params, (short) 0, Integer.MAX_VALUE);
					}
				}else {
					if (energy > 100f) {
						params.add(2f);
					}else{
						params.add(1f);
					}
					for (Unit u : superWeps) {
						u.executeCustomCommand(CMD_PRIORITY, params, (short) 0, Integer.MAX_VALUE);
					}
				}
			}
			if (maxStorage > 1f && energy < 100f && metal/maxStorage > 0.5f) energyReclaim = true;
			if (maxStorage > 1f && energy/maxStorage > 0.75f) energyReclaim = false;
		}


		if (frame % 30 == ai.offset % 30) {
			captureMexes();
			if (defendedFac && effectiveIncome > 10f){
				defendLinks();
				defendMexes();
			}

			// remove finished or invalidated tasks
			cleanOrders();

			for (Unit u: screamers){
				if (u.getStockpileQueued() < 2) {
					u.stockpile((short) 0, Integer.MAX_VALUE);
				}
			}
			
			if (!warManager.enemyHasAntiNuke) {
				for (Unit u : superWeps){
					if (u.getDef().getName().equals("staticnuke") && !u.isBeingBuilt() && u.getStockpileQueued() < 2){
						u.stockpile((short) 0, Integer.MAX_VALUE);
					}
				}
			}
			
			// if greedy workers make up too much of the workforce, convert them.
			for (Worker w:workers.values()){
				if (workers.size() > greed && (greed < 3 || ((float) greed)/((float) workers.size()) <= Math.min(1f - graphManager.territoryFraction, 0.5f))) break;
				if (w.isGreedy) {
					w.isGreedy = false;
					greed--;
				}
			}
			
			/*if (!morphedComs && adjustedIncome > 20f){
				morphComs();
			}*/
		}
		
		assignWorkers();

		return 0;
	}
	
	@Override
	public int unitCreated(Unit unit,  Unit builder) {
		UnitDef def = unit.getDef();
		String defName = def.getName();
		
		if (def.getSpeed() > 0 && def.getBuildOptions().size() > 0 && def.getBuildSpeed() <= 8f){
			unit.setMoveState(0, (short) 0, Integer.MAX_VALUE);
			newWorkers.add(unit.getUnitId());
		}
		
		// Track info for construction tasks
		if(builder != null && unit.isBeingBuilt()){
			if(def.getSpeed() == 0){
				Worker w = workers.get(builder.getUnitId());
				if (w != null && w.getTask() != null && w.getTask() instanceof ConstructionTask && ((ConstructionTask) w.getTask()).buildType.getName().equals(defName)){
					ConstructionTask ct = (ConstructionTask) w.getTask();
					ct.target = unit;
					
					if (def.getUnitDefId() == buildIDs.mexID && !w.isCom && !defendedFac && mexes.size() > 1){
						defendMex(unit.getPos());
					}
				}else{
					for (ConstructionTask ct:constructionTasks){
						float dist = distance(ct.getPos(), unit.getPos());
						if (dist < 25 && ct.buildType.getName().equals(defName)){
							ct.target = unit;
						}
					}
				}
			}
		}else if (builder != null && !unit.isBeingBuilt()){
			// instant factory plops only call unitcreated, not unitfinished.
			for (Worker w : commanders){
				if (w.id == builder.getUnitId()){
					WorkerTask task = w.getTask();
					constructionTasks.remove(task);
					factoryTasks.remove(task);
					w.clearTask();
				}
			}
		}
		
		if ((defName.equals("factoryplane") || defName.equals("striderhub") || defName.equals("factorygunship")) && defendedFac){
			defendFusion(unit.getPos());
		}
		
		if (defName.equals("energypylon")){
			defendMex(unit.getPos());
			pylons.add(unit);
		}
		
		if(defName.equals("turretmissile") || defName.equals("turretlaser") || defName.equals("turretheavylaser") || defName.equals("turretgauss") || defName.equals("turretemp") || defName.equals("turretriot") || defName.equals("turretaalaser")){
			porcs.add(unit);
		}
		
		if (defName.equals("turretaalaser") || defName.equals("turretaaflak") || defName.equals("turretaafar")){
			AAs.add(unit);
		}
		
		if (defName.equals("staticrearm")){
			airpads.add(unit);
		}
		
		if (defName.equals("energyfusion") || defName.equals("energysingu") || defName.equals("staticheavyarty") || defName.equals("turretaaheavy") || defName.equals("turretaafar")){
			defendFusion(unit.getPos());
		}
		
		if (defName.equals("energyfusion") || defName.equals("energysingu")){
			fusions.add(unit);
			if (!screamer && fusions.size() > 4){
				screamer = true;
				UnitDef screamer = callback.getUnitDefByName("turretaaheavy");
				AIFloat3 pos = graphManager.getClosestFrontLineSpot(graphManager.getAllyCenter()).getPos();
				pos = getDirectionalPoint(graphManager.getAllyCenter(), graphManager.getEnemyCenter(), 0.5f * distance(graphManager.getAllyCenter(), pos));
				pos = callback.getMap().findClosestBuildSite(screamer, pos, 600, 3, 0);
				ConstructionTask ct = new ConstructionTask(screamer, pos, 0);
				if (buildCheck(ct) && !constructionTasks.contains(ct)) {
					constructionTasks.add(ct);
				}
			}
		}
		
		if (ai.mergedAllies > 1 && (defName.equals("striderdante") || defName.equals("striderscorpion")) && warManager.miscHandler.striders.isEmpty()){
			RepairTask rt = new RepairTask(unit);
			repairTasks.add(rt);
		}
		
		if (defName.equals("striderfunnelweb") && facManager.numFunnels == 0){
			RepairTask rt = new RepairTask(unit);
			repairTasks.add(rt);
		}
		
		if (defName.equals("factoryplane")){
			havePlanes = true;
		}
		
		if(defName.equals("turretaaheavy")){
			screamers.add(unit);
		}
		
		if (defName.equals("staticantinuke")){
			ArrayList<Float> params = new ArrayList<>();
			params.add(2f);
			unit.executeCustomCommand(CMD_PRIORITY, params, (short) 0, Integer.MAX_VALUE);
		}
		
		if (defName.equals("staticheavyarty") || defName.equals("staticnuke") || defName.equals("zenith") || defName.equals("raveparty")){
			superWeps.add(unit);
			if (defName.equals("staticnuke")){
				ArrayList<Float> params = new ArrayList<>();
				params.add(2f);
				unit.executeCustomCommand(CMD_MISC_PRIORITY, params, (short) 0, Integer.MAX_VALUE);
			}
		}
		
		return 0;
	}

    @Override
    public int unitFinished(Unit unit) {
    	
    	UnitDef def = unit.getDef();
    	String defName = def.getName();

		if(defName.equals("staticmex")){
			mexes.add(unit);
			/*if (defendedFac && ai.mergedAllies > 0 && mexes.size() > 1 + ai.mergedAllies){
				if (graphManager.isFrontLine(unit.getPos())) {
					fortifyMex(unit.getPos(), false);
				}else{
					defendMex(unit.getPos());
				}
			}*/
			
			if (!defendedFac && (mexes.size() > 1 + (ai.mergedAllies * 2) || graphManager.territoryFraction > 0.2f)){
				for (Worker f:facManager.factories.values()) {
					defendFac(f.getPos());
				}
				defendedFac = true;
			}
		}

		if(defName.equals("energysolar")){
			solars.add(unit);
		}

		if(defName.equals("energywind")){
			windgens.add(unit);
		}

    	if(defName.equals("staticradar")){
    		radars.add(unit);
    	}

		if(defName.equals("staticstorage")){
			storages.add(unit);
		}

		if (defName.equals("energysingu") || defName.equals("staticnuke") || defName.equals("zenith") || defName.equals("raveparty")){
			defendSingu(unit.getPos());
			for (Nanotower n : nanos.values()) {
				if (n.target.getUnitId() == unit.getUnitId()) {
					n.unit.selfDestruct((short) 0, Integer.MAX_VALUE);
				}
			}
			List<ConstructionTask> unneeded = new ArrayList<>();
			for (ConstructionTask c:nanoTasks){
				if (c.ctTarget.getUnitId() == unit.getUnitId()){
					List<Worker> idle = c.stopWorkers();
					for (Worker w:idle){
						idlers.add(w);
					}
					assigned.removeAll(idle);
					unneeded.add(c);
				}
			}
			constructionTasks.removeAll(unneeded);
			nanoTasks.removeAll(unneeded);
		}
		
		if (defName.equals("staticnuke") || defName.equals("zenith") || defName.equals("raveparty")){
			hasSuperWep = true;
		}

    	if (def.getSpeed() > 0 && def.getBuildOptions().size() > 0 && def.getBuildSpeed() > 8) {
		    Worker w = new Worker(unit);
		    w.isCom = true;
		    workers.put(w.id, w);
		    commanders.add(w);
		    generateTasks(w);
		    assignWorker(w);
		    assigned.add(w);
		    seeders.add(w);
			unit.setMoveState(0, (short) 0, Integer.MAX_VALUE);
		}

		ConstructionTask finished = null;
    	for (ConstructionTask ct:constructionTasks){
			if (ct.target != null && ct.target.getUnitId() == unit.getUnitId()){
				if (defName.equals("staticcon")){
					Nanotower nt = new Nanotower(unit, ct.ctTarget);
					nanos.put(unit.getUnitId(), nt);
					unit.setRepeat(true, (short) 0, Integer.MAX_VALUE);
					unit.guard(nt.target, (short) 0, Integer.MAX_VALUE);
				}

				finished = ct;
				List<Worker> idle = ct.stopWorkers();
				for (Worker w:idle){
					idlers.add(w);
				}
				assigned.removeAll(idle);
				break;
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
		superWepTasks.remove(finished);

		return 0;
    }

	@Override
	public int unitDamaged(Unit h, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzed){
		if (h.getDef().getUnitDefId() == wolvMineID) return 0;

		// add repair tasks for damaged units
		if (h.getUnitId() != lastRepairedUnit && h.getHealth() > 0){
			lastRepairedUnit = h.getUnitId();
			RepairTask task = new RepairTask(h);
			if (!repairTasks.contains(task)) {
				repairTasks.add(task);
			}
		}
		
		if (weaponDef != null && weaponDef.getWeaponDefId() == nukeWepID && (attacker == null || attacker.getAllyTeam() != ai.allyTeamID)){
			gotNuked = true;
		}

		/*if (h.getHealth() > 0 && h.getDef().getBuildSpeed() > 0 && h.getDef().getSpeed() > 0) {
			if (lastDamagedWorker == null || lastDamagedWorker.id != h.getUnitId()){
				lastDamagedWorker = workers.get(h.getUnitId());
			}
			Worker w = lastDamagedWorker;
			if (w != null && !w.isChicken && ((h.getHealth() / h.getMaxHealth() < 0.8 || warManager.getEffectiveThreat(h.getPos()) > 0) || (weaponDef != null && unitTypes.porcWeps.contains(weaponDef.getWeaponDefId())))) {
				// retreat if a worker gets attacked by enemy porc, is taking serious damage, or is in a hopeless situation.
				if (w.getTask() != null) {
					w.clearTask();
				}
				seeders.remove(w);
				AIFloat3 pos = graphManager.getClosestHaven(h.getPos());
				w.isChicken = true;
				w.chickenFrame = frame;
				w.retreatTo(pos, frame);
				warManager.defenseTargets.add(new DefenseTarget(h.getPos(), h.getMaxHealth(), frame));
			}
		}*/
		
		// If it was a building under construction, reset the builder's target
		ConstructionTask invalidtask = null;
		if(h.getDef().getSpeed() == 0 && (h.getHealth() <= 0f || !h.getDef().isAbleToAttack()) && weaponDef != null && (attacker == null || attacker.getAllyTeam() != ai.allyTeamID) && unitTypes.porcWeps.contains(weaponDef.getWeaponDefId())){
			for (ConstructionTask ct:constructionTasks){
				if (ct.target != null && ct.target.getUnitId() == h.getUnitId()) {
					// if a building was killed by enemy porc, cancel it.
					invalidtask = ct;
					List<Worker> idle = ct.stopWorkers();
					for (Worker w:idle){
					idlers.add(w);
				}
					assigned.removeAll(idle);
					break;
				}
			}
		}
		
		if (invalidtask != null) {
			constructionTasks.remove(invalidtask);
			solarTasks.remove(invalidtask);
			pylonTasks.remove(invalidtask);
			fusionTasks.remove(invalidtask);
			porcTasks.remove(invalidtask);
			nanoTasks.remove(invalidtask);
			factoryTasks.remove(invalidtask);
			AATasks.remove(invalidtask);
		}
		return 0;
	}
    
    @Override
    public int unitDestroyed(Unit unit, Unit attacker) {
		if (unit.getDef().getSpeed() == 0f) {
			radars.remove(unit);
			porcs.remove(unit);
			nanos.remove(unit.getUnitId());
			airpads.remove(unit);
			fusions.remove(unit);
			mexes.remove(unit);
			solars.remove(unit);
			windgens.remove(unit);
			pylons.remove(unit);
			AAs.remove(unit);
			superWeps.remove(unit);
			storages.remove(unit);
			screamers.remove(unit);
			
			for (ConstructionTask ct:constructionTasks){
				if (ct.target != null && ct.target.getUnitId() == unit.getUnitId()) {
					// if a building was killed while under construction, reset its target.
					ct.target = null;
					break;
				}
			}
		}

		newWorkers.remove(unit.getUnitId());
		
		String defName = unit.getDef().getName();
		if (defName.equals("staticnuke") || defName.equals("zenith") || defName.equals("raveparty")) {
			hasSuperWep = false;
		}

		// fortify mexes if they die
		if (defName.equals("staticmex") && (effectiveIncome > 25f || ai.mergedAllies > 1)){
			if (graphManager.getClosestSpot(unit.getPos()).owned) {
				// only fortify mexes that had already been completed
				fortifyMex(unit.getPos());
			}
		}

		// if the unit had a repair task targeting it, remove it
		RepairTask rt = new RepairTask(unit);
		repairTasks.remove(rt);
	
		if (unit.getDef().getSpeed() > 0 && unit.getDef().getBuildSpeed() > 0) {
			Worker deadWorker = workers.get(unit.getUnitId());
			if (deadWorker != null) {
				if (deadWorker.isGreedy) greed--;
				deadWorker.clearTask();
				workers.remove(deadWorker.id);
				commanders.remove(deadWorker);
				seeders.remove(deadWorker);
				idlers.remove(deadWorker);
				assigned.remove(deadWorker);
			}
		}
	    
        return 0; // signaling: OK
    }

	@Override
	public int unitCaptured(Unit unit, int oldTeamID, int newTeamID){
		if (oldTeamID == ai.teamID){
			return unitDestroyed(unit, null);
		}
		return 0;
	}

	@Override
	public int unitGiven(Unit unit, int oldTeamID, int newTeamID){
		if (newTeamID == ai.teamID){
			if (unit.getDef().getSpeed() > 0 && unit.getDef().getBuildOptions().size() > 0) {
				Worker w = new Worker(unit);
				workers.put(w.id, w);
				generateTasks(w);
				assignWorker(w);
				assigned.add(w);
				seeders.add(w);
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
			if(defName.equals("turretmissile") || defName.equals("turretlaser") || defName.equals("turretheavylaser") || defName.equals("turretemp") || defName.equals("turretgauss") || defName.equals("turretriot") || defName.equals("turretaalaser")){
				porcs.add(unit);
			}

			if (defName.equals("turretaalaser") || defName.equals("turretaafar") || defName.equals("turretaaflak")){
				AAs.add(unit);
			}

			if (defName.equals("staticcon")){
				unit.selfDestruct((short) 0, Integer.MAX_VALUE);
			}

			if(defName.equals("staticmex")){
				mexes.add(unit);
			}

			if(defName.equals("energysolar")){
				solars.add(unit);
			}

			if(defName.equals("energypylon")){
				pylons.add(unit);
			}

			if (defName.equals("energyfusion")){
				fusions.add(unit);
			}

			if (defName.equals("energysingu")){
				fusions.add(unit);
			}
		}

		return 0;
	}

	@Override
	public int unitIdle(Unit u) {
		if (newWorkers.contains(u.getUnitId()) && u.getCurrentCommands().isEmpty()){
			Worker w = new Worker(u);
			workers.put(w.id, w);
			if (workers.size() > 1 + ai.mergedAllies && (greed < 2 || ((((float) greed)/workers.size()) < Math.min(1f - graphManager.territoryFraction, 0.5f)))){
				w.isGreedy = true;
				greed++;
			}
			
			generateTasks(w);
			assignWorker(w);
			newWorkers.remove(u.getUnitId());
			
			assigned.add(w);
			seeders.add(w);
		}
		return 0;
	}

	@Override
	public int enemyEnterLOS(Unit e){
		// capture combat reclaim for enemy building nanoframes that enter los
		if (e.isBeingBuilt() && e.getDef().getSpeed() == 0 && warManager.getPorcThreat(e.getPos()) == 0){
			CombatReclaimTask crt = new CombatReclaimTask(e);
			if (!combatReclaimTasks.contains(crt)){
				combatReclaimTasks.add(crt);
			}
		}
		
		// porc over enemy mexes
		if (e.getDef().getName().equals("staticmex") && graphManager.isAllyTerritory(e.getPos())){
			fortifyMex(e.getPos());
		}

		// check for enemy air
		if (e.getDef().isAbleToFly() && !e.getDef().getName().equals("dronelight") && !e.getDef().getName().equals("droneheavyslow") && !e.getDef().getName().equals("dronecarry")){
			enemyHasAir = true;
		}
		return 0;
	}

	@Override
	public int enemyCreated(Unit e){
		// capture combat reclaim for undefended enemy buildings that are started within los
		if (e.getDef() != null && e.getDef().getSpeed() == 0 && warManager.getThreat(e.getPos()) == 0){
			CombatReclaimTask crt = new CombatReclaimTask(e);
			if (!combatReclaimTasks.contains(crt)){
				combatReclaimTasks.add(crt);
			}
		}
		
		// porc over enemy mexes
		if (e.getDef().getName().equals("staticmex") && graphManager.isAllyTerritory(e.getPos())){
			fortifyMex(e.getPos());
		}
		return 0;
	}

	private void checkNanos(){
		for (Nanotower n:nanos.values()){
			if (n.target == null || n.target.getHealth() <= 0) {
				n.unit.selfDestruct((short) 0, Integer.MAX_VALUE);
			}
		}
	}
	
	void assignWorkers(){
		if (frame < 8) return;
		
		// Generate tasks and assign workers to tasks on alternating frames.
		// Also refresh retreat paths for chickening units and process them when they're done chickening.
		if (frame % 2 == 0) {
			if (!idlers.isEmpty()) {
				Worker w = idlers.poll();
				if (w.getUnit().getHealth() <= 0 || w.getUnit().getTeam() != ai.teamID){
					w.clearTask();
					if (w.isGreedy) greed--;
					workers.remove(w.id);
					commanders.remove(w);
					seeders.remove(w);
				}else {
					if (!w.isChicken && warManager.getEffectiveThreat(w.getPos()) > warManager.getTotalFriendlyThreat(w.getPos())){
						w.isChicken = true;
						w.chickenFrame = frame;
						warManager.defenseTargets.add(new DefenseTarget(w.getPos(), w.getUnit().getMaxHealth(), frame));
					}
					if (w.isChicken) {
						if (frame - w.chickenFrame > 600) {
							w.isChicken = false;
							assignWorker(w);
							seeders.add(w);
						} else if (frame - w.lastRetreatFrame > 15) {
							AIFloat3 pos = graphManager.getClosestHaven(w.getPos());
							if (distance(w.getPos(), pos) > 25f) {
								w.retreatTo(pos, frame);
							} else {
								w.isChicken = false;
								assignWorker(w);
								seeders.add(w);
							}
						}
					} else {
						assignWorker(w);
					}
					assigned.add(w);
				}
			}else if (!assigned.isEmpty()){
				Worker w = assigned.poll();
				if (w.getUnit().getHealth() <= 0 || w.getUnit().getTeam() != ai.teamID){
					w.clearTask();
					if (w.isGreedy) greed--;
					workers.remove(w.id);
					commanders.remove(w);
					seeders.remove(w);
				}else {
					if (!w.isChicken && warManager.getEffectiveThreat(w.getPos()) > warManager.getTotalFriendlyThreat(w.getPos())){
						w.isChicken = true;
						w.chickenFrame = frame;
						warManager.defenseTargets.add(new DefenseTarget(w.getPos(), w.getUnit().getMaxHealth(), frame));
					}
					if (w.isChicken) {
						if (frame - w.chickenFrame > 600) {
							w.isChicken = false;
							assignWorker(w);
							seeders.add(w);
						} else if (frame - w.lastRetreatFrame > 15) {
							AIFloat3 pos = graphManager.getClosestHaven(w.getPos());
							if (distance(w.getPos(), pos) > 25f) {
								w.retreatTo(pos, frame);
							} else {
								w.isChicken = false;
								assignWorker(w);
								seeders.add(w);
							}
						}
					} else if (!w.unstick(frame, m, e)) {
						assignWorker(w);
					}
					assigned.add(w);
				}
			}
		}else if (!seeders.isEmpty()){
			Worker w = seeders.poll();
			if (w.getUnit().getHealth() <= 0 || w.getUnit().getTeam() != ai.teamID){
				w.clearTask();
				if (w.isGreedy) greed--;
				workers.remove(w.id);
				commanders.remove(w);
				idlers.remove(w);
				assigned.remove(w);
			}else {
				generateTasks(w);
				seeders.add(w);
			}
		}
	}

	void assignWorker(Worker w) {
		WorkerTask task = getCheapestJob(w);

		WorkerTask wtask = w.getTask();
		if (task != null && (wtask == null || task.taskID != wtask.taskID || distance(w.getPos(), task.getPos()) > w.getUnit().getDef().getBuildDistance() * 1.5f)) {
			
			// remove it from its old assignment if it had one
			if (wtask == null || task.taskID != wtask.taskID) {
				w.clearTask();
				w.setTask(task, frame);
			}
			
			boolean outOfRange = distance(w.getPos(), task.getPos()) > w.getUnit().getDef().getBuildDistance() * 1.5f;
			if (task instanceof ConstructionTask) {
				ConstructionTask ctask = (ConstructionTask) task;
				try {
					if (ctask.target != null && ctask.target.getHealth() <= 0) ctask.target = null;
					if (outOfRange) {
						w.moveTo(ctask.getPos());
						if (ctask.target == null) {
							w.getUnit().build(ctask.buildType, ctask.getPos(), ctask.facing, OPTION_SHIFT_KEY, Integer.MAX_VALUE);
						}else{
							w.getUnit().repair(ctask.target, OPTION_SHIFT_KEY, Integer.MAX_VALUE);
						}
					}else{
						if (ctask.target == null) {
							w.getUnit().build(ctask.buildType, ctask.getPos(), ctask.facing, (short) 0, Integer.MAX_VALUE);
						}else{
							w.getUnit().repair(ctask.target, (short) 0, Integer.MAX_VALUE);
						}
					}
				}catch (Throwable e){
					ai.debug(e);
					assigned.remove(w);
					return;
				}
			} else if (task instanceof ReclaimTask) {
				ReclaimTask rt = (ReclaimTask) task;
				if (outOfRange) {
					w.moveTo(rt.getPos());
					w.getUnit().reclaimFeature(rt.target, OPTION_SHIFT_KEY, Integer.MAX_VALUE);
				}else{
					w.getUnit().reclaimFeature(rt.target, (short) 0, Integer.MAX_VALUE);
				}
			}else if (task instanceof CombatReclaimTask){
				CombatReclaimTask crt = (CombatReclaimTask) task;
				// prevent workers from being unassigned to dangerous/invalid combat reclaim jobs
				if ((!crt.target.isBeingBuilt() && crt.target.getTeam() != ai.teamID) || warManager.getThreat(crt.getPos()) > 0 || crt.target.getHealth() <= 0){
					List<Worker> idle = task.stopWorkers();
					for (Worker wo:idle) {
						idlers.add(wo);
					}
					assigned.removeAll(idle);
					combatReclaimTasks.remove(crt);
					return;
				}
				// else assign
				if (outOfRange) {
					w.moveTo(crt.getPos());
					w.getUnit().reclaimUnit(crt.target, OPTION_SHIFT_KEY, Integer.MAX_VALUE);
				}else{
					w.getUnit().reclaimUnit(crt.target, (short) 0, Integer.MAX_VALUE);
				}
			} else if (task instanceof RepairTask) {
				RepairTask rt = (RepairTask) task;
				if (outOfRange){
					w.moveTo(rt.getPos());
					w.getUnit().repair(rt.target, OPTION_SHIFT_KEY, Integer.MAX_VALUE);
				}else {
					w.getUnit().repair(rt.target, (short) 0, Integer.MAX_VALUE);
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
			if (task instanceof ConstructionTask){
				ConstructionTask ctask = (ConstructionTask) task;
				if ((ctask.buildType.getName().equals("staticmex") && ctask.target != null && ctask.target.getHealth()/ctask.target.getMaxHealth() > 0.35f)
				|| (ctask.buildType.getName().equals("energywind") || ctask.buildType.getName().equals("energysolar") && ctask.target != null)
					&& warManager.getEffectiveThreat(ctask.getPos()) <= 0){
					cost -= 1000f;
				}
			}
			
			cost += 500f * Math.max(0, warManager.getEffectiveThreat(task.getPos()));
		}

		for (WorkerTask t: constructionTasks){
			float tmpcost = costOfJob(worker, t);
			tmpcost += 500f * Math.max(0, warManager.getEffectiveThreat(t.getPos()));
			if (tmpcost < cost){
				cost = tmpcost;
				task = t;
			}
		}
		
		if (maxStorage > 1f && metal/maxStorage < 0.9f && !energyReclaim) {
			for (WorkerTask t : reclaimTasks) {
				float tmpcost = costOfJob(worker, t);
				tmpcost += 500f * Math.max(0, warManager.getEffectiveThreat(t.getPos()));
				if (tmpcost < cost) {
					cost = tmpcost;
					task = t;
				}
			}
		}
		
		if (energyReclaim){
			for (WorkerTask t : energyReclaimTasks) {
				float tmpcost = costOfJob(worker, t);
				tmpcost += 500f * Math.max(0, warManager.getEffectiveThreat(t.getPos()));
				if (tmpcost < cost) {
					cost = tmpcost;
					task = t;
				}
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
				tmpcost += 500f * Math.max(0, warManager.getEffectiveThreat(rt.getPos()));
				if (tmpcost < cost) {
					cost = tmpcost;
					task = t;
				}
			}
		}
		return task;
	}

	float costOfJob(Worker worker,  WorkerTask task){
		float costMod = 1f;
		float dist = (distance(worker.getPos(),task.getPos()));

		for (Worker w: task.assignedWorkers){
			// increment cost mod for every other worker unassigned to the given task that isn't the worker we're assigning
			// as long as they're closer or equaldist to the target.
			if (w.isCom && worker.isGreedy) return dist + 1000f;
			float idist = distance(w.getPos(),task.getPos());
			float rdist = Math.max(idist, 200f);
			float deltadist = Math.abs(idist - dist);
			if (!w.equals(worker) && (rdist < dist || deltadist < 100f)){
				if (w.isCom && !worker.isCom){
					costMod += 2f;
				}else {
					costMod++;
				}
			}
		}

		if (task instanceof ConstructionTask){
			ConstructionTask ctask = (ConstructionTask) task;

			if (ctask.facPlop){
				// only assign facplop tasks to the commander who has the plop!
				if (worker.id != ctask.assignedPlop) {
					return Float.MAX_VALUE;
				}else{
					return -1000f;
				}
			}

			if (ctask.buildType.getUnitDefId() == buildIDs.solarID || ctask.buildType.getUnitDefId() == buildIDs.windID){
				// for small energy
				if ((energy < 50 && maxStorage > 0.5f) || effectiveIncomeMetal > effectiveIncomeEnergy - 2.5f) {
					if (worker.isGreedy) return dist + (10000f * costMod); // greedy workers shouldn't build energy at all.
					return ((dist / 3f) - 200f) + Math.max(0, (1000 * (costMod - 1)));
				}else {
					if (worker.isGreedy) return dist + (10000f * costMod); // greedy workers shouldn't build energy at all.
					return ((dist/4f) + Math.max(0, (1000 * (costMod - 1))));
				}
			}

			if (ctask.buildType.getUnitDefId() == buildIDs.mexID){
				// for mexes
				if (worker.isGreedy) return ((dist/4f) - 100f) + Math.max(0, (600f * (costMod - 1)));
				return ((dist/4f) - 100f) + Math.max(0, (600f * (costMod - 2)));
			}
			
			if (worker.isGreedy && ((ctask.facDef && effectiveIncome < 15f) || ctask.buildType.getCost(m) > 300f)){
				return dist + (10000f * costMod);
			}

			if (buildIDs.expensiveIDs.contains(ctask.buildType.getUnitDefId())){
				// for expensive porc.
				if (energy < 100f) return dist + (250f * costMod);
				if (costMod < 4){
					return (dist/2) - Math.min(3500f, ctask.buildType.getCost(m));
				}
				return ((dist/2) - Math.min(3500f, ctask.buildType.getCost(m))) + (500 * costMod);
			}

			if (ctask.buildType.isAbleToAttack()){
				// for porc
				if (warManager.slasherSpam * 140 > warManager.enemyPorcValue || (ai.mergedAllies > 2 && graphManager.territoryFraction < 0.2f)){
					return dist+300;
				}
				if (worker.isGreedy || worker.isCom) return ((dist/3f) - 200f) + Math.max(0, (1500 * (costMod - 1)));
				return ((dist/2f) - 200f) + Math.max(0, (1500 * (costMod - 2)));
			}

			if (ctask.buildType.getUnitDefId() == buildIDs.nanoID || ctask.buildType.getUnitDefId() == buildIDs.storageID || ctask.buildType.getUnitDefId() == buildIDs.airpadID) {
				// for nanotowers, airpads and storages
				return dist - 1000 + (500 * (costMod - 2));
			}

			if (ctask.buildType.getUnitDefId() == buildIDs.pylonID) {
				// for pylons
				if (worker.isGreedy) return dist + (10000f * costMod);
				return dist - 500 + (500 * (costMod - 1));
			}

			if (ctask.buildType.getUnitDefId() == buildIDs.radarID){
				// for radar
				if (worker.isGreedy) return dist + (10000f * costMod);
				return (dist/4f) - 300 + (1500 * (costMod - 1));
			}
			
			if (buildIDs.facIDs.contains(ctask.buildType.getUnitDefId())) {
				// factory plops and emergency facs get maximum priority
				if (facManager.factories.isEmpty()) {
					return -1000;
				}
				return ((dist / 2) - ctask.buildType.getCost(m)) + (500 * (costMod - 1));
			}
			
			if (ctask.buildType.getCost(m) > 300){
				// allow at least 3 builders for each expensive task, more if the cost is high.
				 return ((dist/2) - Math.min(3000f, ctask.buildType.getCost(m))) + Math.max(0, (500 * (costMod - 3)));
			}

			return (dist - 250) + (100 * (costMod - 1));
		}

		if (task instanceof ReclaimTask) {
			ReclaimTask rtask = (ReclaimTask) task;
			if (rtask.target.getDef() == null) return Float.MAX_VALUE;

			if (warManager.getPorcThreat(rtask.getPos()) > 0){
				// don't reclaim under enemy porc
				return (dist * dist) + (1000f * costMod);
			}
			
			float recMod = 100f;
			if (graphManager.isAllyTerritory(rtask.getPos())){
				recMod = 100f + (200f * graphManager.territoryFraction * graphManager.territoryFraction);
			}

			if (rtask.metalValue > 50){
				return ((dist/4f) - recMod) + Math.max(0, (250 * (costMod - 3)));
			}
			return ((dist/4f) - recMod) + (700 * (costMod - 1));
		}

		if (task instanceof CombatReclaimTask){
			// favor combat reclaim when it's possible
			return dist - 600 + Math.max(0, (600 * (costMod - 2)));
		}

		if (task instanceof RepairTask){
			RepairTask rptask = (RepairTask) task;
			if (rptask.target.getHealth() > 0) {
				if (!worker.isGreedy && rptask.target.getDef().getSpeed() > 0 && rptask.target.getDef().isAbleToAttack() && !rptask.target.getDef().isBuilder()) {
					// for mobile combat units
					if (warManager.getPorcThreat(rptask.getPos()) > 0) return Float.MAX_VALUE;
					
					if (energy < 100){
						return dist;
					}

					if (rptask.isShieldMob && worker.hasShields){
						return dist + (100 * (costMod - 1)) - 250 - rptask.target.getMaxHealth() / (5 * costMod);
					}

					if (!graphManager.isAllyTerritory(rptask.getPos())){
						return dist + 500;
					}

					if (rptask.isTank && ai.mergedAllies < 2){
						return dist + (100 * (costMod - 1)) - 250 - (rptask.target.getMaxHealth() - rptask.target.getHealth()) / (5 * costMod);
					}

					return dist + (100 * (costMod - 1)) - 250 - rptask.target.getMaxHealth() / (5 * costMod);
				}else if (!worker.isGreedy && rptask.target.getDef().isAbleToAttack() && !rptask.target.getDef().isBuilder() && !rptask.target.getDef().getName().equals("turretaalaser")){
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

	public boolean buildCheck(ConstructionTask task){
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
				List<Unit> stuff = callback.getFriendlyUnitsIn(t.getPos(), Math.min(t.buildType.getXSize(), t.buildType.getZSize()) * 4f);
				boolean isNano = false;
				for (Unit u:stuff){
					if (u.isBeingBuilt() && u.getDef().getUnitDefId() == t.buildType.getUnitDefId() && u.getTeam() == ai.teamID){
						isNano = true;
						t.target = u;
						break;
					}
				}
				if (!isNano) {
					// if a construction job is blocked and it isn't our own nanoframe, remove it
					List<Worker> idle = t.stopWorkers();
					for (Worker w:idle){
						idlers.add(w);
					}
					assigned.removeAll(idle);
					invalidtasks.add(t);
				}
			}
			
			if (warManager.getPorcThreat(t.getPos()) > 0 && (t.target == null || !t.buildType.isAbleToAttack())){
				List<Worker> idle = t.stopWorkers();
				for (Worker w:idle){
					idlers.add(w);
				}
				assigned.removeAll(idle);
				invalidtasks.add(t);
			}
		}
		
		for (ConstructionTask t: superWepTasks){
			if (frame-t.frameIssued > 900 && t.target == null){
				List<Worker> idle = t.stopWorkers();
				for (Worker w:idle){
					idlers.add(w);
				}
				assigned.removeAll(idle);
				invalidtasks.add(t);
			}
		}
		// remove old porc push tasks, so as not to waste metal on unneeded porc.
		for (ConstructionTask t: porcTasks){
			if (t.frameIssued > 0 && frame - t.frameIssued > 600 && t.assignedWorkers.isEmpty() && t.target == null){
				List<Worker> idle = t.stopWorkers();
				for (Worker w:idle){
					idlers.add(w);
				}
				assigned.removeAll(idle);
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
		superWepTasks.removeAll(invalidtasks);

		invalidtasks.clear();

		for (ReclaimTask rt:reclaimTasks){
			if (rt.target.getDef() == null){
				List<Worker> idle = rt.stopWorkers();
				for (Worker w:idle){
					idlers.add(w);
				}
				assigned.removeAll(idle);
				invalidtasks.add(rt);
			}
		}
		reclaimTasks.removeAll(invalidtasks);

		invalidtasks.clear();

		for (CombatReclaimTask crt:combatReclaimTasks){
			if ((!crt.target.isBeingBuilt() && crt.target.getTeam() != ai.teamID) || warManager.getPorcThreat(crt.getPos()) > 0 || crt.target.getHealth() <= 0){
				List<Worker> idle = crt.stopWorkers();
				for (Worker w:idle){
					idlers.add(w);
				}
				assigned.removeAll(idle);
				invalidtasks.add(crt);
			}
		}
		combatReclaimTasks.removeAll(invalidtasks);

		invalidtasks.clear();

		for (RepairTask rt:repairTasks){
			if (rt.target.getHealth() <= 0 || rt.target.getHealth() == rt.target.getMaxHealth()) {
				List<Worker> idle = rt.stopWorkers();
				for (Worker w:idle){
					idlers.add(w);
				}
				assigned.removeAll(idle);
				invalidtasks.add(rt);
			}
		}
		repairTasks.removeAll(invalidtasks);
		
		if (choseSupWep && sWep == 1 && warManager.enemyHasAntiNuke){
			sWep = 0;
		}
	}

	void checkShields(){
		for (Worker w:workers.values()){
			if (w.getUnit().getHealth() > 0 && w.getUnit().getTeam() == ai.teamID){
				// Check shield hp, because unitDamaged sucks.
				if (!w.isChicken && w.hasShields && w.getShields() < 0.3f){
					if (w.getTask() != null) {
						w.clearTask();
					}
					AIFloat3 pos = graphManager.getClosestHaven(w.getPos());
					w.retreatTo(pos, frame);
					w.isChicken = true;
					w.chickenFrame = frame;
					warManager.defenseTargets.add(new DefenseTarget(w.getPos(), w.getUnit().getMaxHealth() * 2f, frame));
					seeders.remove(w);
				}
			}
		}
	}

    void generateTasks(Worker worker){
    	AIFloat3 position = worker.getPos();
	    
		// do we need a factory?
		boolean hasStriders = false;
		boolean hasPlanes = false;
		boolean hasGunship = false;
		for (Worker f:facManager.factories.values()) {
			String defName = f.getUnit().getDef().getName();
			if (defName.equals("striderhub")) {
				hasStriders = true;
			}
			if (defName.equals("factoryplane")) {
				hasPlanes = true;
			}
			if (defName.equals("factorygunship")){
				hasGunship = true;
			}
		}
		
		boolean hasPlop = worker.hasPlop();
		if ((hasPlop && !worker.assignedPlop)
				|| (frame > 900 && !hasPlop && facManager.factories.size() < 1 + ai.mergedAllies && factoryTasks.isEmpty())
				|| (!worker.isGreedy && staticIncome > 50f && facManager.factories.size() == 1 && nanos.size() > 3 && factoryTasks.isEmpty())
				|| (!worker.isGreedy && staticIncome > 90f && facManager.factories.size() == 2 && nanos.size() > 6 && graphManager.eminentTerritory && factoryTasks.isEmpty())
				|| (ai.mergedAllies > 0 && staticIncome > 65f && !hasStriders && graphManager.eminentTerritory && factoryTasks.isEmpty())
				|| (ai.mergedAllies > 0 && staticIncome > 65f && graphManager.territoryFraction > 0.35f && (!hasPlanes && !hasGunship) && factoryTasks.isEmpty())
				|| (ai.mergedAllies > 0 && bigMap && graphManager.territoryFraction > 0.4f && (!warManager.miscHandler.striders.isEmpty() || !warManager.squadHandler.shieldSquads.isEmpty()) && (!hasPlanes || !hasGunship) && factoryTasks.isEmpty())) {
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
	
		if (!worker.isCom) collectReclaimables(worker);

		// do we need AA?
		if (enemyHasAir && warManager.maxEnemyAirValue > 1000f && graphManager.territoryFraction > 0.4f && AATasks.isEmpty()){
			createAATask(worker);
		}

		if (!worker.isGreedy) {
			// is there sufficient energy to cover metal income?
			Boolean eFac = false; // note: cloaky uniquely requires running energy rich.
			for (Factory f : facManager.factories.values()) {
				if (f.getUnit().getDef().getName().equals("factorycloak")) {
					eFac = true;
					break;
				}
			}
			
			float eRatio;
			if (eFac && ai.mergedAllies < 2) {
				eRatio = 1.5f;
			} else {
				eRatio = 1f;
			}
			
			float territorymod;
			if (graphManager.eminentTerritory) {
				territorymod = (graphManager.territoryFraction + (graphManager.territoryFraction * graphManager.territoryFraction)) / 2f;
			} else {
				territorymod = (graphManager.territoryFraction * graphManager.territoryFraction);
			}
			
			eRatio += territorymod + (fusions.size() / 3f);
			
			int energySize = (2 * (solars.size() + solarTasks.size())) + windgens.size() + windTasks.size();
			if ((Math.round(rawMexIncome * eRatio) > energySize && solarTasks.size() + windTasks.size() < facManager.numWorkers)
				      || (effectiveIncomeMetal > 12f && energy < 100 && solarTasks.size() + windTasks.size() < facManager.numWorkers)) {
				createEnergyTask(worker);
			}
			createFusionTask(worker);
		}

    	
    	// do we need caretakers?
    	if(!facManager.factories.isEmpty() &&
    		  (float)(nanos.size() + nanoTasks.size() + facManager.factories.size() + factoryTasks.size()) < Math.floor(staticIncome/(10f))){
			createNanoTurretTask(worker, getCaretakerTarget());
    	}

		//do we need storages?
		if (frame > 300 && !facManager.factories.isEmpty() &&
			  ((metal/maxStorage > 0.8f && staticIncome > 15f * (1f + ai.mergedAllies) && energy > 100f) || (commanders.size() + storages.size() < 1 + ai.mergedAllies)) && storageTasks.isEmpty() && facManager.factories.size() > 0){
			createStorageTask(worker, getCaretakerTarget());
		}

		// do we need airpads for planes
		if (!worker.isGreedy && !facManager.factories.isEmpty() &&
			  havePlanes && airpadTasks.isEmpty() && staticIncome > 15f
				&& ((float)airpads.size() < Math.round(warManager.bomberHandler.getBomberSize()/4f) && warManager.bomberHandler.getBomberSize() > 3)){
			createAirPadTask(getCaretakerTarget());
		}

		// do we need radar?
		if (!worker.isGreedy && adjustedIncome > 20f && energy > 100f && !tooCloseToFac){
    		createRadarTask(worker);
    	}
    	
    	// porc push against nearby enemy porc and defend the front line
	    if (defendedFac && porcTasks.size() < facManager.numWorkers * 1.5f) {
		    porcPush(worker.getPos());
		    if (graphManager.eminentTerritory) defendFront(worker.getPos());
	    }

		// do we need pylons?
		if (fusions.size() > 3 && !tooCloseToFac){
			createGridTask(worker.getPos());
		}
		
		if (bigMap && superWeps.size() > 3 && !hasSuperWep && superWepTasks.isEmpty()){
			choseSupWep = false;
		}
	
		// do we need superweapons?
		if (!choseSupWep && ((ai.allies.size() > 0 && hasStriders && (facManager.numStriders > 1)) || ai.allies.isEmpty()) && fusions.size() > 4){
			chooseSuperWeapon();
		}
		
		if (choseSupWep){
			if ((sWep == 0 || hasSuperWep) && superWepTasks.isEmpty()){
				createBerthaTask();
			}else if (sWep > 0 && !hasSuperWep && superWepTasks.isEmpty()){
				createSuperWepTask();
			}
		}
		
		// do we need a protector?
		if (!antiNuke && (warManager.enemyHasNuke || gotNuked)){
			antiNuke = true;
			UnitDef protector = callback.getUnitDefByName("staticantinuke");
			AIFloat3 pos = callback.getMap().findClosestBuildSite(protector, graphManager.getAllyCenter(), 600, 3, 0);
			ConstructionTask ct = new ConstructionTask(protector, pos, 0);
			if (buildCheck(ct) && !constructionTasks.contains(ct)) {
				constructionTasks.add(ct);
			}
		}
    }
	
	void createFactoryTask(Worker worker, boolean isPlop){
		UnitDef factory;
		
		if (potentialFacList.isEmpty() && facManager.factories.size() < 1 + ai.mergedAllies){
			potentialFacList = terrainManager.getInitialFacList();
			addedFacs = false;
		}
		
		if (facManager.factories.size() > 0 && !isPlop && !addedFacs){
			if (!potentialFacList.contains("factorygunship")) {
				potentialFacList.add("factorygunship");
			}
			// Planes are disabled because the meta is total shit.
			/*if(!potentialFacList.contains("factoryplane") && ai.mergedAllies > 2){
				potentialFacList.add("factoryplane");
			}*/
			potentialFacList.add("striderhub");
			
			if (facManager.factories.size() > 0 && (facManager.factories.size() < 3 || ai.mergedAllies > 0)) {
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
		for (Worker f:facManager.factories.values()){
			String defName = f.getUnit().getDef().getName();
			if (defName.equals("striderhub")){
				hasStriders = true;
			}
			
			// don't build the same fac twice.
			potentialFacList.remove(defName);
		}
		
		if (potentialFacList.isEmpty()){
			addedFacs = false;
			return;
		}
		
		// Uncomment this to set the intial fac for debugging purposes.
		/*if (facManager.factories.size() == 0){
			factory = callback.getUnitDefByName("factorytank");
			potentialFacList.remove("factorytank");
		}else*/ if (userFac != null && facManager.factories.size() == 0){
			factory = callback.getUnitDefByName(userFac);
			potentialFacList.remove(userFac);
		}else if (isPlop && potentialFacList.contains("factorygunship")){
			String facName = "factorygunship";
			factory = callback.getUnitDefByName(facName);
			potentialFacList.remove(facName);
		}else if (facManager.factories.size() < 1 + Math.max(ai.mergedAllies, 1) || hasStriders || isPlop) {
			int i = (int) Math.round(Math.random() * (potentialFacList.size() - 1.0));
			String facName = potentialFacList.get(i);
			factory = callback.getUnitDefByName(facName);
			potentialFacList.remove(i);
			
			// don't build two airfacs in small teams or on small maps.
			if (facName.equals("factoryplane") && ai.mergedAllies < 5 && !bigMap){
				potentialFacList.remove("factorygunship");
			}else if (facName.equals("factorygunship") && ai.mergedAllies < 5 && !bigMap){
				potentialFacList.remove("factoryplane");
			}
		}else{
			factory = callback.getUnitDefByName("striderhub");
			potentialFacList.remove("striderhub");
		}
		
		AIFloat3 position = worker.getPos();
		
		short facing;
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
		
		int i = 0;
		boolean good = false;
		float clearance = Math.max(factory.getXSize(), factory.getZSize()) * 4f;
		if (facManager.factories.isEmpty() || isPlop) {
			while (!good) {
				if (i++ > 10) return;
				position = worker.getPos();
				position = getDirectionalPoint(position, graphManager.getMapCenter(), 100f);
				position = callback.getMap().findClosestBuildSite(factory, position,600f, 3, facing);
				
				// don't let factories get blocked by random crap, nor build them on top of mexes
				int j = 0;
				while (!callback.getFriendlyUnitsIn(position, clearance).isEmpty() || !callback.getFeaturesIn(position, clearance).isEmpty() || distance(graphManager.getClosestSpot(position).getPos(), position) < 100 || !buildCheck(new ConstructionTask(factory, position, 0))) {
					if (j++ > 5) return;
					position = (i < 8) ? getAngularPoint(worker.getPos(), graphManager.getMapCenter(), 100f) : getRadialPoint(worker.getPos(), 125f);
					position = callback.getMap().findClosestBuildSite(factory,position,600f, 3, facing);
				}
				
				ConstructionTask ct =  new ConstructionTask(factory, position, facing); // only for preventing stupid placement
				if (buildCheck(ct)){
					good = true;
				}
			}
		}else{
			while (!good) {
				if (i > 5) return;
				// don't cram factories together
				position = getAngularPoint(graphManager.getAllyCenter(), graphManager.getAllyBase(), Math.max(450f, distance(graphManager.getAllyBase(), graphManager.getAllyCenter()) * (float) Math.random()));
				AIFloat3 facpos = getNearestFac(position).getPos();
				
				// don't let factories get blocked by random crap, nor build them on top of mexes
				int j = 0;
				while (distance(facpos, position) < 400f || callback.getFriendlyUnitsIn(position, 150f).size() > 0 || distance(graphManager.getClosestSpot(position).getPos(), position) < 100 || graphManager.isFrontLine(position)){
					if (j > 5) break;
					position = getRadialPoint(position, 175f);
					facpos = getNearestFac(position).getPos();
					j++;
				}
				
				position = callback.getMap().findClosestBuildSite(factory, position, 600f, 3, facing);
				
				ConstructionTask ct =  new ConstructionTask(factory, position, facing); // only for preventing stupid placement
				if (distance(facpos, position) > 400f && callback.getFriendlyUnitsIn(position, 150f).isEmpty() && distance(graphManager.getClosestSpot(position).getPos(), position) > 100 && !graphManager.isFrontLine(position) && buildCheck(ct)){
					good = true;
				}
				i++;
			}
		}
		
		ConstructionTask ct =  new ConstructionTask(factory, position, facing, isPlop);
		if (buildCheck(ct) && !factoryTasks.contains(ct) && worker.canReach(position)){
			if (isPlop){
				ct.assignedPlop = worker.id;
				worker.assignedPlop = true;
			}
			constructionTasks.add(ct);
			factoryTasks.add(ct);
		}
	}
    
    void createRadarTask(Worker worker){
    	UnitDef radar = callback.getUnitDefByName("staticradar");
    	AIFloat3 position = worker.getPos();
    	position = heightMap.getHighestPointInRadius(position, 800f);
    	position = callback.getMap().findClosestBuildSite(radar,position,600f, 3, 0);
    	if (!needRadar(position) || !worker.canReach(position)){
		    position = worker.getPos();
		    position = heightMap.getHighestPointInRadius(position, 500f);
		    position = callback.getMap().findClosestBuildSite(radar,position,600f, 3, 0);
		    if (!needRadar(position) || !worker.canReach(position)) return;
	    }

    	 ConstructionTask ct =  new ConstructionTask(radar, position, 0);
    	if (buildCheck(ct) && !radarTasks.contains(ct)){
			constructionTasks.add(ct);
			radarTasks.add(ct);
		}
    }

	void createAATask(Worker worker){
		AIFloat3 position = graphManager.getClosestFrontLineLink(worker.getPos());
		if (position == null) return;

		UnitDef aa;

		if (!graphManager.eminentTerritory || effectiveIncome < 30f || Math.random() > 0.5){
			aa = callback.getUnitDefByName("turretaalaser");
		}else{
			aa = callback.getUnitDefByName("turretaafar");
		}

		position = callback.getMap().findClosestBuildSite(aa,position,600f, 3, 0);

		// don't build AA too close together.
		for (Unit a:AAs){
			if (distance(a.getPos(), position) < 250f){
				return;
			}
		}

		ConstructionTask ct =  new ConstructionTask(aa, position, 0);
		if (buildCheck(ct) && !porcTasks.contains(ct)){
			constructionTasks.add(ct);
			AATasks.add(ct);
		}
	}
	
	void chooseSuperWeapon(){
		if ((superWeps.size() < 4 && Math.random() > 0.4) || !bigMap){
			sWep = 0;
		}else {
			double rand = Math.random();
			if (!warManager.enemyHasAntiNuke && rand > 0.66) {
				sWep = 1;
			} else if (rand > 0.33) {
				sWep = 2;
			} else {
				sWep = 3;
			}
		}
		choseSupWep = true;
	}

	void createBerthaTask(){
		UnitDef bertha = callback.getUnitDefByName("staticheavyarty");
		AIFloat3 position = null;
		
		boolean good = false;
		int i = 0;
		while (!good) {
			if (++i > 4) {
				return;
			}
			position = getAngularPoint(graphManager.getAllyBase(), graphManager.getMapCenter(), distance(graphManager.getAllyBase(), graphManager.getMapCenter()) * 0.65f);
			position = heightMap.getHighestPointInRadius(position, 800f);
			position = callback.getMap().findClosestBuildSite(bertha, position, 600f, 3, 0);
			int j = 0;
			while (!good) {
				if (++j > 3) {
					break;
				}
				good = true;
				for (Unit b : superWeps) {
					if (distance(b.getPos(), position) < 350) {
						good = false;
						break;
					}
				}
				
				if (graphManager.isEnemyTerritory(position) || graphManager.isFrontLine(position) || distance(position, getNearestFac(position).getPos()) < 300f) {
					good = false;
				}
				
				if (!good) {
					position = getRadialPoint(position, 300f);
					position = callback.getMap().findClosestBuildSite(bertha, position, 600f, 3, 0);
				}
			}
		}

		ConstructionTask ct =  new ConstructionTask(bertha, position, 0);
		if (buildCheck(ct) && !superWepTasks.contains(ct)){
			ct.frameIssued = frame;
			constructionTasks.add(ct);
			superWepTasks.add(ct);
		}
	}
	
	void createSuperWepTask(){
		UnitDef death;
		if (sWep == 1) {
			death = callback.getUnitDefByName("staticnuke");
		}else if (sWep == 2){
			death = callback.getUnitDefByName("zenith");
		}else{
			death = callback.getUnitDefByName("raveparty");
		}

		AIFloat3 position;
		try {
			position = getDirectionalPoint(graphManager.getAllyBase(), graphManager.getAllyCenter(), distance(graphManager.getAllyBase(), graphManager.getAllyCenter()) * 0.3f);
			position = callback.getMap().findClosestBuildSite(death, position, 600f, 3, 0);
		}catch (Throwable t){
			return;
		}
		
		boolean good = false;
		int i = 0;
		while (!good) {
			if (++i > 3) {
				return;
			}
			good = true;
			if (graphManager.isEnemyTerritory(position) || graphManager.isFrontLine(position) || distance(position, getNearestFac(position).getPos()) < 300f) {
				good = false;
			}
			
			if (!good) {
				position = getRadialPoint(position, 400f);
				position = callback.getMap().findClosestBuildSite(death, position, 600f, 3, 0);
			}
		}
		
		ConstructionTask ct =  new ConstructionTask(death, position, 0);
		if (buildCheck(ct) && !superWepTasks.contains(ct)){
			ct.frameIssued = frame;
			constructionTasks.add(ct);
			superWepTasks.add(ct);
		}
	}

	void defendSingu(AIFloat3 position){
		UnitDef llt = callback.getUnitDefByName("turretlaser");
		UnitDef defender = callback.getUnitDefByName("turretmissile");
		UnitDef hlt = callback.getUnitDefByName("turretheavylaser");
		UnitDef cobra = callback.getUnitDefByName("turretaaflak");
		UnitDef shield = callback.getUnitDefByName("staticshield");
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
	}

	void defendMex(AIFloat3 position){
		UnitDef llt = callback.getUnitDefByName("turretlaser");
		AIFloat3 pos;

		for (Unit u : porcs) {
			float dist = distance(position, u.getPos());
			if (dist < 350) {
				return;
			}
		}

		for (ConstructionTask c : porcTasks) {
			float dist = distance(position, c.getPos());
			if (dist < 350) {
				return;
			}
		}

		pos = getDirectionalPoint(graphManager.getAllyBase(), position, distance(graphManager.getAllyBase(), position) + 100f);
		pos = callback.getMap().findClosestBuildSite(llt, pos, 600f, 3, 0);

		ConstructionTask ct = new ConstructionTask(llt, pos, 0);
		if (buildCheck(ct) && !porcTasks.contains(ct)) {
			constructionTasks.add(ct);
			porcTasks.add(ct);
		}
	}

	void fortifyMex(AIFloat3 position){
		UnitDef llt = callback.getUnitDefByName("turretlaser");
		ConstructionTask ct;
		AIFloat3 pos;
		// build two llts
		pos = getAngularPoint(position, graphManager.getEnemyCenter(), 150f);
		pos = callback.getMap().findClosestBuildSite(llt, pos, 600f, 3, 0);

		ct = new ConstructionTask(llt, pos, 0);
		if (buildCheck(ct) && !porcTasks.contains(ct)) {
			constructionTasks.add(ct);
			porcTasks.add(ct);
		}
		
		/*pos = getAngularPoint(position, graphManager.getEnemyCenter(), 100f);
		pos = callback.getMap().findClosestBuildSite(llt, pos, 600f, 3, 0);
		
		ct = new ConstructionTask(llt, pos, 0);
		if (buildCheck(ct) && !porcTasks.contains(ct)) {
			constructionTasks.add(ct);
			porcTasks.add(ct);
		}*/
	}
	
	void defendFusion(AIFloat3 position){
		UnitDef hlt = callback.getUnitDefByName("turretheavylaser");
		UnitDef emp = callback.getUnitDefByName("turretemp");
		ConstructionTask ct;
		AIFloat3 pos;
		// build an hlt and a faraday
		pos = getAngularPoint(position, graphManager.getEnemyCenter(), 250f);
		pos = callback.getMap().findClosestBuildSite(hlt, pos, 600f, 3, 0);
		
		ct = new ConstructionTask(hlt, pos, 0);
		if (buildCheck(ct) && !porcTasks.contains(ct)) {
			constructionTasks.add(ct);
			porcTasks.add(ct);
		}
		
		pos = getDirectionalPoint(position, pos, distance(position, pos) + 75f);
		pos = callback.getMap().findClosestBuildSite(emp, pos, 600f, 3, 0);
		ct = new ConstructionTask(emp, pos, 0);
		if (buildCheck(ct) && !porcTasks.contains(ct)) {
			constructionTasks.add(ct);
			porcTasks.add(ct);
		}
	}

	void defendFac(AIFloat3 position){
		UnitDef llt = callback.getUnitDefByName("turretlaser");
		UnitDef defender = callback.getUnitDefByName("turretmissile");
		AIFloat3 pos = new AIFloat3(0,0,0);

		if (ai.mergedAllies == 0) {
			ConstructionTask ct;
			boolean good = false;
			int i = 0;
			while (!good) {
				if (i++ > 10){
					break;
				}
				pos = getAngularPoint(position, graphManager.getMapCenter(), 250f);
				pos = callback.getMap().findClosestBuildSite(llt, pos, 600f, 3, 0);
				ct = new ConstructionTask(llt, pos, 0);
				if (buildCheck(ct) && !porcTasks.contains(ct)) {
					good = true;
					ct.facDef = true;
					constructionTasks.add(ct);
					porcTasks.add(ct);
				}
			}

			/*AIFloat3 pos2;
			i = 0;
			good = false;
			while (!good) {
				if (i++ > 10) {
					break;
				}
				pos2 = getAngularPoint(position, graphManager.getMapCenter(), 250f);
				pos2 = callback.getMap().findClosestBuildSite(llt, pos2, 600f, 3, 0);
				ct = new ConstructionTask(llt, pos2, 0);
				ct.facDef = true;
				if (distance(pos, pos2) > 150f && buildCheck(ct) && !porcTasks.contains(ct)) {
					good = true;
					constructionTasks.add(ct);
					porcTasks.add(ct);
				}
			}*/
		}else{
			pos = getDirectionalPoint(graphManager.getAllyBase(), position, distance(position, graphManager.getAllyBase())+175f);
			pos = getDirectionalPoint(pos, graphManager.getMapCenter(), 75f);
			pos = callback.getMap().findClosestBuildSite(llt, pos, 600f, 3, 0);
			ConstructionTask ct =  new ConstructionTask(llt, pos, 0);
			if (buildCheck(ct) && !porcTasks.contains(ct)){
				constructionTasks.add(ct);
				porcTasks.add(ct);
			}
		}
	}
	
	void defendMexes(){
		for (MetalSpot ms:graphManager.getOwnedSpots()){
			defendMex(ms.getPos());
		}
	}

	void defendLinks(){
		if (ai.mergedAllies == 0){
			for (Link l : graphManager.getLinks()) {
				if (l.length <= 550 && (/*l.isAllyShadowed() ||*/ l.isOwned())) {
					defendLink(l);
				}
			}
		}else {
			for (Link l : graphManager.getLinks()) {
				if (l.length < 800 && l.isOwned() && graphManager.isFrontLine(l.getPos())) {
					defendLink(l);
				}
			}
		}
	}

	void defendLink(Link l){
		AIFloat3 position = l.getPos();
		UnitDef hlt = callback.getUnitDefByName("turretheavylaser");
		UnitDef razor = callback.getUnitDefByName("turretaalaser");
		UnitDef llt = callback.getUnitDefByName("turretlaser");
		UnitDef emp = callback.getUnitDefByName("turretemp");
		UnitDef defender = callback.getUnitDefByName("turretmissile");

		// don't spam redundant porc
		float porcdist = 300;
		for(Unit u:porcs){
			float dist = distance(position,u.getPos());
			if (dist < porcdist){
				return;
			}
		}

		for(ConstructionTask c:porcTasks){
			float dist = distance(position, c.getPos());
			if (dist < porcdist){
				return;
			}
		}

		AIFloat3 pos;
		ConstructionTask ct;
		// build an llt
		pos = callback.getMap().findClosestBuildSite(llt, position, 600f, 3, 0);

		ct = new ConstructionTask(llt, pos, 0);
		if (buildCheck(ct) && !porcTasks.contains(ct)) {
			constructionTasks.add(ct);
			porcTasks.add(ct);
		}
	}

	void porcPush(AIFloat3 position){
		UnitDef porc;
		double rand = Math.random();
		if (rand > 0.15 * Math.min(graphManager.territoryFraction * 2f, 1f)) {
			porc = callback.getUnitDefByName("turretmissile");
		}else {
			rand = Math.random();
			porc = rand > 0.35 ? callback.getUnitDefByName("turretheavylaser") : callback.getUnitDefByName("turretemp");
		}

		if (enemyHasAir && Math.random() > 0.95){
			porc = callback.getUnitDefByName("turretaalaser");
		}
		Enemy e = warManager.getClosestEnemyPorc(position);
		if (e == null){
			return;
		}
		
		AIFloat3 pos = getAngularPoint(e.position, position, e.ud.getMaxWeaponRange() + 100f);
		pos = callback.getMap().findClosestBuildSite(porc,pos,600f, 3, 0);

		float porcdist = 150f + (150f * (1f - Math.min(graphManager.territoryFraction * 2f, 1f)));

		for (Unit u : porcs) {
			float dist = distance(pos, u.getPos());
			if (dist < porcdist) {
				return;
			}
		}

		for (ConstructionTask c : porcTasks) {
			float dist = distance(pos, c.getPos());
			if (dist < porcdist) {
				return;
			}
		}

		ConstructionTask ct = new ConstructionTask(porc, pos, 0);
		ct.frameIssued = frame;
		if (buildCheck(ct) && !porcTasks.contains(ct)) {
			constructionTasks.add(ct);
			porcTasks.add(ct);
		}
	}

	void defendFront(AIFloat3 position){
		UnitDef porc;
		double rand = Math.random();
		if (rand > 0.6) {
			porc = callback.getUnitDefByName("turretmissile");
		}else if (rand > 0.25 * Math.min(graphManager.territoryFraction * 2f, 1f)){
			porc = callback.getUnitDefByName("turretlaser");
		}else {
			rand = Math.random();
			porc = rand > 0.75 ? callback.getUnitDefByName("turretheavylaser") : rand > 0.5 ? callback.getUnitDefByName("turretgauss") : rand > 0.25 ? callback.getUnitDefByName("turretriot") : callback.getUnitDefByName("turretemp");
		}

		MetalSpot ms = graphManager.getClosestFrontLineSpot(position);
		if (ms == null){
			return;
		}

		AIFloat3 pos = ms.getPos();
		AIFloat3 fpos = graphManager.getClosestBattleFront(ms.getPos());

		pos = getAngularPoint(pos, fpos, 150f + ((float) Math.random() * 350f));
		pos = callback.getMap().findClosestBuildSite(porc, pos,600f, 3, 0);

		float porcdist = 200f + (150f * (1f - Math.min(graphManager.territoryFraction * 2f, 1f)));

		for (Unit u : porcs) {
			float dist = distance(pos, u.getPos());
			if (dist < porcdist) {
				return;
			}
		}

		for (ConstructionTask c : porcTasks) {
			float dist = distance(pos, c.getPos());
			if (dist < porcdist) {
				return;
			}
		}

		ConstructionTask ct = new ConstructionTask(porc, pos, 0);
		ct.frameIssued = frame;
		if (buildCheck(ct) && !porcTasks.contains(ct)) {
			constructionTasks.add(ct);
			porcTasks.add(ct);
		}
	}
	
	Boolean needRadar(AIFloat3 position){
		float closestRadarDistance = Float.MAX_VALUE;
		for( Unit r: radars){
			float distance = distance(r.getPos(),position);
			if(distance < closestRadarDistance){
				closestRadarDistance = distance;
			}
		}
		for(ConstructionTask r: radarTasks){
			float distance = distance(r.getPos(),position);
			if(distance < closestRadarDistance){
				closestRadarDistance = distance;
			}
		}

		if(closestRadarDistance > 1250f){
			return true;
		}
		return false;
	}

    void captureMexes(){
    	UnitDef mex = callback.getUnitDefByName("staticmex");
		List<MetalSpot> metalSpots = graphManager.getNeutralSpots();

		for ( MetalSpot ms: metalSpots){
			AIFloat3 position = ms.getPos();
			if (callback.getMap().isPossibleToBuildAt(mex, position, 0) && warManager.getPorcThreat(position) == 0){
				ConstructionTask ct =  new ConstructionTask(mex, position, 0);
				if (!constructionTasks.contains(ct)){
					constructionTasks.add(ct);
				}
			}
		}
    }
    
    void morphComs(){
    	for (Worker w:commanders){
    		Unit u = w.getUnit();
    		float level = u.getRulesParamFloat("comm_level", 0f);
    		float chassis = u.getRulesParamFloat("comm_chassis", 0f);
    		float oldModCount = u.getRulesParamFloat("comm_module_count", 0f);
			float newModCount = 2f;
		    ArrayList<Float> params = new ArrayList<>();
		    params.add(level);
		    params.add(chassis);
		    params.add(oldModCount);
		    params.add(newModCount);
		    
		    for (int i = 1; i <= oldModCount; i++){
		    	float modID = u.getRulesParamFloat("comm_module_" + i, -1f);
		    	if (modID != -1f) {
				    params.add(modID);
			    }
		    }


		    params.add(13f); // shotgun

		    params.add(30f); // field radar
		    u.executeCustomCommand(CMD_MORPH_UPGRADE_INTERNAL, params, (short) 0, Integer.MAX_VALUE);
		    params.clear();
		    
		    params.add(3f);
		    u.executeCustomCommand(CMD_MISC_PRIORITY, params, (short) 0, Integer.MAX_VALUE);
	    }
	    morphedComs = true;
    }
    
    void createEnergyTask(Worker worker){
    	UnitDef solar = callback.getUnitDefByName("energysolar");
		UnitDef windgen = callback.getUnitDefByName("energywind");
    	AIFloat3 position = worker.getPos();

		ConstructionTask ct;

		// for solars
		if (((solars.size() + solarTasks.size()) < Math.round(rawMexIncome/2f) || solars.size() + solarTasks.size() * 2 < (windgens.size() + windTasks.size()))
				&& callback.getMap().getElevationAt(position.x, position.z) > 0){
		//if (callback.getMap().getElevationAt(position.x, position.z) > 0){
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
				if (dist < solarDist && s.target == null){
					return;
				}
			}

			// prevent ecomanager from spamming solars/windgens that graphmanager doesn't know about yet
			for (ConstructionTask s: windTasks){
				float dist = distance(s.getPos(), position);
				if (dist < solarDist && s.target == null){
					return;
				}
			}

			// prevent it from blocking the fac with crap
			if (!facManager.factories.isEmpty() && solars.size() + windgens.size() + solarTasks.size() + windTasks.size() > 2){
				AIFloat3 pos = getNearestFac(position).getPos();
				if (distance(pos, position) < 300f){
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
				if (dist < solarDist && s.target == null){
					return;
				}
			}

			// prevent ecomanager from spamming solars/windgens that graphmanager doesn't know about yet
			for (ConstructionTask s: windTasks){
				float dist = distance(s.getPos(), position);
				if (dist < solarDist && s.target == null){
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
		UnitDef fusion = callback.getUnitDefByName("energyfusion");
		UnitDef singu = callback.getUnitDefByName("energysingu");
		if (facManager.factories.isEmpty()) return;
		AIFloat3 position;
		ConstructionTask ct;
		int started = 0;
		for (ConstructionTask ft:fusionTasks){
			if (ft.target != null){
				started++;
			}
		}

		// for fusions
		if (effectiveIncome > 35f && energy > 100f
				&& Math.floor(staticIncome/27.5f) > fusions.size() && !facManager.factories.isEmpty()
			      && fusionTasks.size() + fusions.size() - started < (ai.mergedAllies == 0 ? 2 : 3) && fusionTasks.size() < 1 + ai.mergedAllies){
			boolean good = false;
			int i = 0;
			while (!good) {
				if (i++ > 4) return;
				position = getRadialPoint(graphManager.getAllyBase(), distance(graphManager.getAllyBase(), graphManager.getAllyCenter()) * (float) Math.random());
				position = graphManager.getClosestSpot(position).getPos();
				if (position == null) {
					continue;
				}
				position = callback.getMap().findClosestBuildSite(fusion, position, 600f, 3, 0);

				// don't build fusions too close to the fac
				float dist = distance(getNearestFac(position).getPos(), position);
				if (dist > 300f && !graphManager.isFrontLine(position) && !graphManager.isEnemyTerritory(position) && getFusionDist(position) > 250f) {
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
		else if (fusions.size() > 2 && fusions.size() < 5 + (ai.mergedAllies > 1 ? 1 : 0)
				&& graphManager.territoryFraction > 0.35
				&& !facManager.factories.isEmpty() && fusionTasks.isEmpty()){
			boolean good = false;
			int i = 0;
			while (!good) {
				if (i++ > 4) return;
				position = getRadialPoint(graphManager.getAllyBase(), distance(graphManager.getAllyBase(), graphManager.getAllyCenter()) * (float) Math.random());
				position = graphManager.getClosestSpot(position).getPos();
				if (position == null) {
					continue;
				}
				position = callback.getMap().findClosestBuildSite(singu, position, 600f, 3, 0);

				// don't build fusions too close to the fac or on the front line
				float dist = distance(getNearestFac(position).getPos(), position);
				if (dist > 300f && !graphManager.isFrontLine(position) && !graphManager.isEnemyTerritory(position) && getFusionDist(position) > 250f) {
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
		UnitDef pylon = callback.getUnitDefByName("energypylon");
		
		float fdist = Float.MAX_VALUE;
		AIFloat3 best = null;
		for (Unit f:fusions){
			float tmpdist = distance(f.getPos(), position);
			if (tmpdist < fdist){
				fdist = tmpdist;
				best = f.getPos();
			}
		}
		
		if (best != null){
			position = best;
		}

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
    
    void createNanoTurretTask(Worker w, Unit target){
		UnitDef nano = callback.getUnitDefByName("staticcon");
    	AIFloat3 position;
    	
		float buildDist;
	    float radius;
	    if (target.getDef().getName().equals("striderhub") || target.getDef().getName().contains("factory")){
		    radius = 400f;
		    buildDist = 350f;
	    }else{
		    radius = 350f; // for superweapons
		    buildDist = 300f;
	    }

		int i = 0;
		while (true) {
			position = getRadialPoint(target.getPos(), buildDist);
			position = callback.getMap().findClosestBuildSite(nano, position, 600f, 3, 0);
			
			if (distance(position, target.getPos()) < radius && distance(getNearestFac(position).getPos(), position) > 150f){
				ConstructionTask ct =  new ConstructionTask(nano, position, 0);
				ct.ctTarget = target;
				if (buildCheck(ct) && !nanoTasks.contains(ct) && w.canReach(position)){
					constructionTasks.add(ct);
					nanoTasks.add(ct);
					return;
				}
			}
			i++;
			if (i >= 8) buildDist -= 50f;
			if (i > 10) return;
		}

    }

	void createStorageTask(Worker w, Unit target){
		UnitDef stor = callback.getUnitDefByName("staticstorage");
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
		if (buildCheck(ct) && !nanoTasks.contains(ct) && w.canReach(position)){
			constructionTasks.add(ct);
			storageTasks.add(ct);
		}
	}

	void createAirPadTask(Unit target){
		if (target.getDef().getName().equals("striderhub")) return;
		UnitDef airpad = callback.getUnitDefByName("staticrearm");
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

	
	Unit getCaretakerTarget(){
		Unit target = null;
		float ctCount = 9001;
		for (Worker f:facManager.factories.values()){
			// Spread caretakers evenly between facs, with some priority.
			float numCT = 0;
			for (Nanotower n : nanos.values()){
				if (n.target != null && n.target.getHealth() > 0f && n.target.getUnitId() == f.id){
					numCT++;
				}
			}
			for (ConstructionTask c: nanoTasks){
				if (c.ctTarget != null && c.ctTarget.getHealth() > 0f && c.ctTarget.getUnitId() == f.id){
					numCT++;
				}
			}
			String defName = f.getUnit().getDef().getName();
			if (defName.equals("striderhub")){
				numCT += ai.mergedAllies > 0 ? -0.3f : 0.1f;
			}else if (defName.equals("factoryplane") || defName.equals("factorygunship")){
				numCT += ai.mergedAllies > 0 ? -0.2f : 0.1f;
			}else if (defName.equals("factoryshield")){
				numCT += ai.mergedAllies > 0 ? -0.1f : 0f;
			}
			if (numCT < ctCount){
				target = f.getUnit();
				ctCount = numCT;
			}
		}

		for (ConstructionTask c: factoryTasks){
			if (c.target != null && c.target.getHealth() > 0){
				float numCT = 0;
				for (Nanotower n : nanos.values()){
					if (n.target != null && n.target.getHealth() > 0f && n.target.getUnitId() == c.target.getUnitId()){
						numCT++;
					}
				}
				for (ConstructionTask nt: nanoTasks){
					if (nt.ctTarget != null && nt.ctTarget.getHealth() > 0f && nt.ctTarget.getUnitId() == c.target.getUnitId()){
						numCT++;
					}
				}
				String defName = c.target.getDef().getName();
				if (defName.equals("striderhub")){
					numCT -= 0.9f;
				}
				if (defName.equals("factoryplane") || defName.equals("factorygunship") || defName.equals("factoryshield")){
					numCT -= 0.5f;
				}
				if (numCT < ctCount){
					target = c.target;
					ctCount = numCT;
				}
			}
		}
		return target;
	}

	void collectReclaimables(Worker w){
		if (reclaimTasks.size() > Math.max(facManager.numWorkers, 20)) return;
		List<Feature> feats = callback.getFeaturesIn(w.getPos(), ai.mapDiag * 2.5f /* * graphManager.territoryFraction * 4f*/);
		
		// Sort by closest first.
		feats.sort(new Comparator<Feature>() {
			@Override
			public int compare(Feature o1, Feature o2) {
				float dist1 = distance(w.getPos(), o1.getPosition());
				float dist2 = distance(w.getPos(), o2.getPosition());
				return (int) Math.signum(dist1 - dist2);
			}
		});
		
		for (Feature f : feats) {
			if (!f.getDef().isReclaimable() || warManager.getPorcThreat(f.getPosition()) > 0) continue;
			if (f.getDef().getContainedResource(m) > f.getDef().getContainedResource(e)){
				if (reclaimTasks.size() > Math.max(facManager.numWorkers, 20)){
					break;
				}
				ReclaimTask rt = new ReclaimTask(f, f.getDef().getContainedResource(m));
				if (!reclaimTasks.contains(rt) && w.canReach(f.getPosition())){
					reclaimTasks.add(rt);
					break;
				}
			}else{
				if (energyReclaimTasks.size() > Math.max(facManager.numWorkers, 20)){
					break;
				}
				ReclaimTask rt = new ReclaimTask(f, f.getDef().getContainedResource(e));
				if (!energyReclaimTasks.contains(rt) && w.canReach(f.getPosition())){
					energyReclaimTasks.add(rt);
					break;
				}
			}
			
		}
	}

	public float getReclaimValue(){
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
		reclaimValue *= (1f + ai.mergedAllies)/(1f + ai.allies.size());
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
	
	public Worker getFarthestFac(AIFloat3 position){
		Worker farthestFac = null;
		float dist = 0f;
		for (Worker f:facManager.factories.values()){
			float tdist = distance(position, f.getPos());
			if (tdist > dist){
				dist = tdist;
				farthestFac = f;
			}
		}
		return farthestFac;
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
