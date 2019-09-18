package zkgbai.economy;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.OOAICallback;
import com.springrts.ai.oo.clb.Resource;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;
import zkgbai.Module;
import zkgbai.ZKGraphBasedAI;
import zkgbai.graph.GraphManager;
import zkgbai.military.MilitaryManager;
import zkgbai.military.UnitClasses;
import zkgbai.military.unitwrappers.Raider;

import java.util.*;

/**
 * Created by aeonios on 12/8/2015.
 */
public class FactoryManager extends Module {
	ZKGraphBasedAI ai;
	EconomyManager economyManager;
	MilitaryManager warManager;
	GraphManager graphManager;
	private OOAICallback callback;
	UnitClasses unitTypes;
	
	public Map<Integer, Factory> factories = new HashMap<Integer, Factory>();;
	Set<Integer> newUnits = new HashSet<>();
	
	int frame = 0;
	
	boolean smallMap = false;
	boolean bigMap = false;
	
	public boolean earlyWorker = false;
	
	boolean hasFusion = false;
	
	int scoutsNeeded = 0;
	int scoutBudget = 0;
	
	boolean enemyHasAir = false;
	boolean enemyHasDrones = false;
	boolean enemyHasAmphs = false;
	boolean enemyHasHeavyPorc = false;
	
	public int numWorkers = 0;
	float mobileBP = 0;
	public float workerValue = 0;
	public float fighterValue = 0;
	public float armyValue = 0;
	public float lightArtyValue = 0;
	public float heavyArtyValue = 0;
	public float artyValue = 0;
	float assaultValue = 0;
	float AAvalue = 0;
	
	int numAAs = 0;
	
	int numErasers = 0;
	int numAspis = 0;
	
	int numWarriors = 0;
	int numZeus = 0;
	int numRockos = 0;
	int numGlaives = 0;
	int numScythes = 0;
	
	int numMaces = 0;
	int numScalpels = 0;
	
	int numReapers = 0;
	int numBanishers = 0;
	
	int numLevelers = 0;
	int numRavagers = 0;
	
	int numScouts = 0;
	int numKodachis = 0;
	
	int numInfis = 0;
	int numUltis = 0;
	
	int numFelons = 0;
	int numRogues = 0;
	int numThugs = 0;
	int numLaws = 0;
	int numRackets = 0;
	int numBandits = 0;
	public int numFunnels = 0;
	int numDomis = 0;
	
	int numVenoms = 0;
	int numRedbacks = 0;
	int numRecluses = 0;
	int numHermits = 0;
	
	int numRavens = 0;
	
	int numStriders = 0;
	int numHeavyStriders = 0;
	
	public Unit striderTarget = null;
	Factory striderHub = null;
	
	Resource m;
	
	static int CMD_PRIORITY = 34220;
	
	public FactoryManager(ZKGraphBasedAI ai){
		this.ai = ai;
		this.callback = ai.getCallback();
		
		this.smallMap = (callback.getMap().getWidth() + callback.getMap().getHeight() < 1280);
		this.bigMap = Math.max(callback.getMap().getHeight(), callback.getMap().getWidth()) > 640 && !smallMap;
		
		this.earlyWorker = bigMap;
		this.m = callback.getResourceByName("Metal");
	}
	
	@Override
	public int init(int AIID, OOAICallback cb){
		this.warManager = ai.warManager;
		this.economyManager = ai.ecoManager;
		this.graphManager = ai.graphManager;
		this.unitTypes = UnitClasses.getInstance();
		
		return 0;
	}
	
	@Override
	public String getModuleName() {
		// TODO Auto-generated method stub
		return "FactoryManager";
	}
	
	@Override
	public int update(int frame) {
		this.frame = frame;
		
		if (frame % 30 == 0){
			List<Float> params = new ArrayList<>();
			if (earlyWorker && economyManager.workers.size() > 1 && numWorkers < 3 && economyManager.adjustedIncome < 10f){
				params.add(3f);
			}else{
				params.add(1f);
			}
			for (Factory f:factories.values()) f.getUnit().executeCustomCommand(CMD_PRIORITY, params, (short) 0, Integer.MAX_VALUE);
		}
		
		if (frame % 60 == 0){
			if (striderHub != null){
				if (striderTarget == null){
					assignFactoryTask(striderHub);
				}else{
					striderHub.getUnit().repair(striderTarget, (short) 0, Integer.MAX_VALUE);
				}
			}
		}
		
		return 0;
	}
	
	@Override
	public int unitCreated(Unit unit,  Unit builder) {
		UnitDef def = unit.getDef();
		String defName = def.getName();
		
		if (defName.contains("hub")){
			unit.setMoveState(0, (short) 0, Integer.MAX_VALUE);
		}
		
		if(defName.contains("factory")){
			// Set default rally point.
			byte facing = (byte) unit.getBuildingFacing();
			AIFloat3 target = unit.getPos();
			float rallydist = 175f;
			switch (facing) {
				case 0:
					target.z += rallydist;
					break;
				case 1:
					target.x += rallydist;
					break;
				case 2:
					target.z -= rallydist;
					break;
				case 3:
					target.x -= rallydist;
					break;
			}
			unit.moveTo(target, (short) 0, Integer.MAX_VALUE);
		}
		
		if (def.getBuildSpeed() > 0 && def.getSpeed() > 0){
			numWorkers++;
			mobileBP += def.getBuildSpeed();
			if (def.getBuildSpeed() < 8f) {
				workerValue += unit.getDef().getCost(m);
			}
		}else if (def.getSpeed() > 0 && !def.isAbleToFly() && !unitTypes.AAs.contains(defName)){
			newUnits.add(unit.getUnitId());
			armyValue += unit.getDef().getCost(m)/2f;
		}else if (unitTypes.AAs.contains(defName)){
			newUnits.add(unit.getUnitId());
			numAAs++;
			AAvalue += unit.getDef().getCost(m);
			armyValue += unit.getDef().getCost(m)/2f;
		}
		
		if (defName.equals("cloakraid") || defName.equals("shieldraid") || defName.equals("amphraid") || defName.equals("hoverraid") || defName.equals("tankraid")){
			Factory scoutFac;
			try {
				scoutFac = factories.get(builder.getUnitId());
			}catch (Exception e){
				scoutFac = (Factory) economyManager.getNearestFac(unit.getPos());
			}
			if (scoutFac.creatingScout) {
				unit.setMoveState(1, (short) 0, Integer.MAX_VALUE);
				warManager.raiderHandler.addSoloRaider(new Raider(unit, unit.getDef().getCost(m)));
			}
		}
		
		if (defName.equals("energyfusion") || defName.equals("energysingu")){
			fighterValue += 250f;
		}
		
		if(defName.equals("tankheavyarty") || defName.equals("hoverarty") || defName.equals("spidercrabe") || defName.equals("amphassault") || defName.equals("striderarty")){
			artyValue += def.getCost(m);
		}
		
		if (defName.equals("shieldarty")){
			artyValue += def.getCost(m)/2f;
		}
		
		if (defName.equals("tankarty")){
			artyValue += def.getCost(m)/2f;
		}
		
		if (defName.equals("vehheavyarty") || defName.equals("cloaksnipe")){
			heavyArtyValue += def.getCost(m);
			artyValue += def.getCost(m);
		}
		
		if(defName.equals("veharty") || defName.equals("cloakarty")){
			artyValue += def.getCost(m)/2f;
			lightArtyValue += def.getCost(m)/2f;
		}
		
		if(defName.equals("hoverassault") || defName.equals("cloakassault") || defName.equals("vehassault")
			     || defName.equals("amphfloater") || defName.equals("shieldassault") || defName.equals("tankassault")){
			assaultValue += def.getCost(m);
		}
		
		if(defName.equals("cloakriot")){
			numWarriors++;
		}
		
		if(defName.equals("cloakassault")){
			numZeus++;
		}
		
		if(defName.equals("cloakskirm")){
			numRockos++;
		}
		
		if(defName.equals("cloakraid")){
			numGlaives++;
		}
		
		if (defName.equals("cloakheavyraid")){
			numScythes++;
		}
		
		if(defName.equals("shieldraid")){
			numBandits++;
		}
		
		if(defName.equals("shieldfelon")){
			numFelons++;
		}
		
		if (defName.equals("shieldskirm")){
			numRogues++;
		}
		
		if (defName.equals("shieldassault")){
			numThugs++;
		}
		
		if (defName.equals("shieldriot")){
			numLaws++;
		}
		
		if (defName.equals("shieldarty")){
			numRackets++;
		}
		
		if (defName.equals("striderfunnelweb")){
			numFunnels++;
		}
		
		if (defName.equals("vehcapture")){
			numDomis++;
		}
		
		if (defName.equals("spideremp")){
			numVenoms++;
		}
		
		if (defName.equals("spiderriot")){
			numRedbacks++;
		}
		
		if (defName.equals("spiderskirm")){
			numRecluses++;
		}
		
		if (defName.equals("spiderassault")){
			numHermits++;
		}
		
		if (defName.equals("hoverriot")){
			numMaces++;
		}
		
		if (defName.equals("hoverskirm")){
			numScalpels++;
		}
		
		if (defName.equals("tankassault")){
			numReapers++;
		}
		
		if (defName.equals("tankriot")){
			numBanishers++;
		}
		
		if (defName.equals("vehscout") || defName.equals("hoverraid")){
			numScouts++;
		}
		
		if (defName.equals("tankraid")){
			numKodachis++;
		}
		
		if (defName.equals("vehassault")){
			numRavagers++;
		}
		
		if (defName.equals("vehriot")){
			numLevelers++;
		}
		
		if (defName.equals("bomberprec") || defName.equals("bomberriot")){
			numRavens++;
		}
		
		if (defName.equals("spiderantiheavy")){
			numInfis++;
		}
		
		if (defName.equals("cloakjammer")){
			numErasers++;
		}
		
		if (defName.equals("shieldshield")){
			numAspis++;
		}
		
		if (defName.equals("striderdante") || defName.equals("striderscorpion") || defName.equals("striderarty") || defName.equals("striderfunnelweb")){
			striderTarget = unit;
			numStriders++;
		}
		
		if (defName.equals("striderantiheavy")){
			striderTarget = unit;
			numUltis++;
		}
		
		if (defName.equals("striderbantha") || defName.equals("striderdetriment")){
			striderTarget = unit;
			numHeavyStriders++;
		}
		
		return 0;
	}
	
	@Override
	public int unitFinished(Unit unit) {
		if (striderTarget != null && striderTarget.getUnitId() == unit.getUnitId()){
			striderTarget = null;
		}
		
		if (newUnits.contains(unit.getUnitId())){
			newUnits.remove(unit.getUnitId());
			String defName = unit.getDef().getName();
			if (!unitTypes.AAs.contains(defName)){
				fighterValue += Math.min(unit.getDef().getCost(m), unitTypes.heavies.contains(defName) ? 500f: 350f)/(unitTypes.arties.contains(defName) ? 2f : 1f);
			}
			armyValue += unit.getDef().getCost(m)/2f;
		}
		
		String defName = unit.getDef().getName();
		if(defName.contains("factory") || defName.contains("hub")){
			Factory fac;
			if (factories.size() < 1 + ai.mergedAllies) {
				fac = new Factory(unit, true);
			}else{
				fac = new Factory(unit, false);
			}
			if (!defName.contains("hub")) {
				for (UnitDef ud : unit.getDef().getBuildOptions()) {
					if (!ud.getBuildOptions().isEmpty()) {
						fac.costPerBP = ud.getCost(m)/ud.getBuildSpeed();
						fac.workerBP = ud.getBuildSpeed();
						break;
					}
				}
			}
			factories.put(fac.id, fac);
			
			assignFactoryTask(fac);
			unit.setMoveState(2, (short) 0, Integer.MAX_VALUE);
			
			if (defName.equals("striderhub")){
				striderHub = fac;
			}
		}
		
		return 0;
	}
	
	@Override
	public int unitDestroyed(Unit unit, Unit attacker) {
		if (striderTarget != null && striderTarget.getUnitId() == unit.getUnitId()){
			striderTarget = null;
		}
		
		UnitDef def = unit.getDef();
		String defName = def.getName();
		
		if (defName.equals("striderhub")){
			striderHub = null;
		}
		
		if (def.isBuilder() && def.getSpeed() > 0) {
			numWorkers--;
			mobileBP -= def.getBuildSpeed();
			if (def.getBuildSpeed() < 8f) {
				workerValue -= unit.getDef().getCost(m);
			}
		}else if (newUnits.contains(unit.getUnitId())){
			newUnits.remove(unit.getUnitId());
			armyValue -= unit.getDef().getCost(m)/2f;
		}else if (def.getSpeed() > 0 && !def.isAbleToFly() && !unitTypes.AAs.contains(defName)){
			fighterValue -= Math.min(unit.getDef().getCost(m), unitTypes.heavies.contains(defName) ? 500f: 350f)/(unitTypes.arties.contains(defName) ? 2f : 1f);
			armyValue -= unit.getDef().getCost(m);
		}else if (unitTypes.AAs.contains(defName)){
			numAAs--;
			AAvalue -= unit.getDef().getCost(m);
			armyValue -= unit.getDef().getCost(m);
		}
		
		if (defName.equals("energyfusion") || defName.equals("energysingu")){
			fighterValue -= 250f;
		}
		
		if(defName.equals("tankheavyarty") || defName.equals("hoverarty") || defName.equals("spidercrabe") || defName.equals("amphassault") || defName.equals("striderarty")){
			artyValue -= def.getCost(m);
		}
		
		if (defName.equals("tankarty")){
			artyValue -= def.getCost(m)/2f;
		}
		
		if (defName.equals("vehheavyarty") || defName.equals("cloaksnipe")){
			heavyArtyValue -= def.getCost(m);
			artyValue -= def.getCost(m);
		}
		
		if(defName.equals("cloakarty") || defName.equals("veharty")){
			artyValue -= def.getCost(m)/2f;
			lightArtyValue -= def.getCost(m)/2f;
		}
		
		if(defName.equals("hoverassault") || defName.equals("cloakassault") || defName.equals("vehassault")
			     || defName.equals("amphfloater") || defName.equals("shieldassault") || defName.equals("tankassault")){
			assaultValue -= def.getCost(m);
		}
		
		if (defName.equals("cloakjammer")){
			numErasers--;
		}
		
		if (defName.equals("shieldshield")){
			numAspis--;
		}
		
		if(defName.equals("cloakriot")){
			numWarriors--;
		}
		
		if(defName.equals("cloakassault")){
			numZeus--;
		}
		
		if(defName.equals("cloakskirm")){
			numRockos--;
		}
		
		if(defName.equals("shieldraid")){
			numBandits--;
		}
		
		if (defName.equals("shieldfelon")){
			numFelons--;
		}
		
		if (defName.equals("shieldskirm")){
			numRogues--;
		}
		if (defName.equals("shieldassault")){
			numThugs--;
		}
		
		if (defName.equals("shieldriot")){
			numLaws--;
		}
		
		if (defName.equals("shieldarty")){
			numRackets--;
		}
		
		if (defName.equals("striderfunnelweb")){
			numFunnels--;
		}
		
		if (defName.equals("vehcapture")){
			numDomis--;
		}
		
		if (defName.equals("spideremp")){
			numVenoms--;
		}
		
		if (defName.equals("spiderriot")){
			numRedbacks--;
		}
		
		if (defName.equals("spiderskirm")){
			numRecluses--;
		}
		
		if (defName.equals("spiderassault")){
			numHermits--;
		}
		
		if (defName.equals("hoverriot")){
			numMaces--;
		}
		
		if (defName.equals("hoverskirm")){
			numScalpels--;
		}
		
		if (defName.equals("vehscout") || defName.equals("hoverraid")){
			numScouts--;
		}
		
		if (defName.equals("tankraid")){
			numKodachis--;
		}
		
		if (defName.equals("tankassault")){
			numReapers--;
		}
		
		if (defName.equals("tankriot")){
			numBanishers--;
		}
		
		if (defName.equals("vehassault")){
			numRavagers--;
		}
		
		if (defName.equals("vehriot")){
			numLevelers--;
		}
		
		if (defName.equals("bomberprec") || defName.equals("bomberriot")){
			numRavens--;
		}
		
		if (defName.equals("spiderantiheavy")){
			numInfis--;
		}
		
		if (defName.equals("striderantiheavy")){
			numUltis--;
		}
		
		if (defName.equals("striderdante") || defName.equals("striderscorpion") || defName.equals("striderarty") || defName.equals("striderfunnelweb")){
			numStriders--;
		}
		
		if (defName.equals("striderbantha") || defName.equals("striderdetriment")){
			numHeavyStriders--;
		}
		
		if (factories.containsKey(unit.getUnitId())){
			factories.remove(unit.getUnitId());
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
			if (unit.getDef().getName().equals("striderhub")){
				unit.selfDestruct((short) 0, Integer.MAX_VALUE);
			}else {
				return unitCreated(unit, null);
			}
		}
		return 0;
	}
	
	@Override
	public int unitIdle(Unit unit) {
		if (factories.containsKey(unit.getUnitId()) && !unit.getDef().getName().equals("striderhub") && !unit.getDef().getName().equals("athena")){
			Factory fac = factories.get(unit.getUnitId());
			assignFactoryTask(fac);
		}
		
		return 0; // signaling: OK
	}
	
	@Override
	public int enemyEnterLOS(Unit unit){
		if (unit.getDef().isAbleToFly() && !unit.getDef().getName().equals("dronelight") && !unit.getDef().getName().equals("droneheavyslow") && !unit.getDef().getName().equals("dronecarry")){
			enemyHasAir = true;
		}else if (unit.getDef().getName().equals("dronelight") || unit.getDef().getName().equals("droneheavyslow") || unit.getDef().getName().equals("dronecarry")){
			enemyHasDrones = true;
		}
		
		if (unit.getDef().getTooltip().contains("Amph")){
			enemyHasAmphs = true;
		}
		
		if (unit.getDef().getName().equals("turretheavylaser") || unit.getDef().getName().equals("turretriot") || unit.getDef().getName().equals("turretgauss") || unit.getDef().getName().equals("turretheavy") || unit.getDef().getName().equals("turretantiheavy") || unit.getDef().getName().equals("spidercrabe")){
			enemyHasHeavyPorc = true;
		}
		return 0;
	}
	
	void assignFactoryTask(Factory fac){
		String defName = fac.getUnit().getDef().getName();
		UnitDef unit;
		
		if (fac.creatingScout) {
			fac.creatingScout = false;
			fac.scoutAllowance--;
		}
		
		scoutsNeeded = 0;
		for (Factory f:factories.values()) scoutsNeeded += f.maxScoutAllowance;
		scoutsNeeded -= warManager.raiderHandler.soloRaiders.size();
		scoutBudget = Math.max(0, Math.min(scoutsNeeded, fac.scoutAllowance));
		
		hasFusion = (!economyManager.fusions.isEmpty() &&
			               (!economyManager.fusions.get(0).isBeingBuilt() || economyManager.fusions.get(0).getHealth() > economyManager.fusions.get(0).getDef().getHealth() / 2f));
		
		if (defName.equals("factorycloak")){
			unit = callback.getUnitDefByName(getCloaky(fac));
			fac.getUnit().build(unit, fac.getPos(), (short) 0, (short) 0, Integer.MAX_VALUE);
		}else if (defName.equals("factoryshield")){
			unit = callback.getUnitDefByName(getShields(fac));
			fac.getUnit().build(unit, fac.getPos(), (short) 0, (short) 0, Integer.MAX_VALUE);
		}else if (defName.equals("factorygunship")){
			unit = callback.getUnitDefByName(getGunship(fac));
			fac.getUnit().build(unit, fac.getPos(), (short) 0, (short) 0, Integer.MAX_VALUE);
		}else if (defName.equals("factoryplane")){
			unit = callback.getUnitDefByName(getPlanes(fac));
			fac.getUnit().build(unit, fac.getPos(), (short) 0, (short) 0, Integer.MAX_VALUE);
		}else if (defName.equals("factoryamph")){
			unit = callback.getUnitDefByName(getAmphs(fac));
			fac.getUnit().build(unit, fac.getPos(), (short) 0, (short) 0, Integer.MAX_VALUE);
		}else if (defName.equals("factoryveh")){
			unit = callback.getUnitDefByName(getLV(fac));
			fac.getUnit().build(unit, fac.getPos(), (short) 0, (short) 0, Integer.MAX_VALUE);
		}else if (defName.equals("factoryhover")){
			unit = callback.getUnitDefByName(getHovers(fac));
			fac.getUnit().build(unit, fac.getPos(), (short) 0, (short) 0, Integer.MAX_VALUE);
		}else if (defName.equals("factorytank")){
			unit = callback.getUnitDefByName(getTanks(fac));
			fac.getUnit().build(unit, fac.getPos(), (short) 0, (short) 0, Integer.MAX_VALUE);
		}else if (defName.equals("factoryspider")){
			unit = callback.getUnitDefByName(getSpiders(fac));
			fac.getUnit().build(unit, fac.getPos(), (short) 0, (short) 0, Integer.MAX_VALUE);
		}else if (defName.equals("striderhub")){
			fac.getUnit().stop((short) 0, Integer.MAX_VALUE);
			unit = callback.getUnitDefByName(getStrider());
			AIFloat3 pos = callback.getMap().findClosestBuildSite(unit, fac.getPos(), 250f, 3, 0);
			fac.getUnit().build(unit, pos, (short) 0, (short) 0, Integer.MAX_VALUE);
		}
	}
	
	private Boolean needWorkers(Factory fac){
		String facType = fac.getUnit().getDef().getName();
		
		if ((facType.equals("factoryplane") || facType.equals("factorygunship"))
			      && numWorkers >= 1 + ai.mergedAllies){
			return false;
		}
		
		if (ai.mergedAllies > 0 && (fac.raiderSpam < 0 || fac.expensiveRaiderSpam < 0)){
			return false;
		}
		
		float reclaimValue = economyManager.getReclaimValue();
		
		float territoryMod = Math.min(0.26f, graphManager.territoryFraction/1.8f);
		float fv = (fighterValue * Math.min(0.5f, graphManager.territoryFraction)) + Math.min(fighterValue * territoryMod, reclaimValue/2f);
		float income = economyManager.effectiveIncomeMetal + (reclaimValue/(2f * fac.costPerBP));
		int workerDeficit = Math.round((income - mobileBP)/fac.workerBP);
		
		if (workerDeficit > 0 &&
			      (fv > workerValue || numWorkers == 0
					     || (economyManager.adjustedIncome > 15f && economyManager.metal/economyManager.maxStorage > 0.8f && economyManager.energy/economyManager.maxStorage > 0.5f && economyManager.maxStorage > 0.5f && workerDeficit > -3))
			      || (earlyWorker && numWorkers < 2)
		) {
			return true;
		}else if ((fac.raiderSpam >= 0 && numWorkers < ((ai.mergedAllies == 0 || bigMap) ? 3 : 2) * (1 + ai.mergedAllies))){
			return true;
		}
		return false;
	}
	
	private String getCloaky(Factory fac) {
		if (needWorkers(fac)) {
			return "cloakcon";
		}
		
		if (warManager.slasherSpam * 280 > assaultValue){
			return "cloakassault";
		}
		
		if (fac.raiderSpam < 0 || scoutBudget > 0) {
			if (scoutBudget == 0) {
				fac.raiderSpam++;
			}else{
				fac.creatingScout = true;
			}
			return "cloakraid";
		}
		
		if (fac.expensiveRaiderSpam < 0){
			fac.expensiveRaiderSpam++;
			return "cloakheavyraid";
		}
		
		if ((warManager.maxEnemyAirValue > AAvalue && fighterValue > AAvalue && (numAAs < 4 || Math.random() > 0.5))){
			return "cloakaa";
		}
		
		if (hasFusion && numErasers == 0 && warManager.squadHandler.squads.size() > 0 && Math.random() > 0.75){
			return "cloakjammer";
		}
		
		if (economyManager.adjustedIncome > 15 && Math.random() > 0.95 - (graphManager.territoryFraction/20f)){
			return "cloakbomb";
		}
		
		if (hasFusion && warManager.enemyHeavyPorcValue > heavyArtyValue && Math.random() > 0.85) {
			fac.scoutAllowance = Math.min(fac.maxScoutAllowance, fac.scoutAllowance + 2);
			return "cloaksnipe";
		}
		
		if (graphManager.eminentTerritory && warManager.enemyLightPorcValue > lightArtyValue && Math.random() > 0.85){
			fac.scoutAllowance = Math.min(fac.maxScoutAllowance, fac.scoutAllowance + 1);
			return "cloakarty";
		}
		
		if (Math.random() > (warManager.sniperSightings.isEmpty() ? 0.95 + Math.min(0.025f, graphManager.territoryFraction/20f) : 0.85)){
			fac.raiderSpam -= Math.min(12, Math.max(6, (int) Math.floor(economyManager.adjustedIncome/5f))) - 1;
			return "cloakraid";
		}
		if (economyManager.adjustedIncome > 15f && Math.random() > 0.925 + Math.min(0.05f, graphManager.territoryFraction/10f)) {
			fac.expensiveRaiderSpam -= (int) Math.min(4, Math.max(2, Math.floor(economyManager.adjustedIncome / 10))) - 1;
			return "cloakheavyraid";
		}
		
		fac.raiderSpam--;
		double rand = Math.random();
		if (!hasFusion){
			if (numRockos < (2 * numWarriors) + (3 * numZeus)) {
				return "cloakskirm";
			} else if (rand > 0.6) {
				fac.scoutAllowance = Math.min(fac.maxScoutAllowance, fac.scoutAllowance + 1);
				return "cloakassault";
			} else {
				fac.scoutAllowance = Math.min(fac.maxScoutAllowance, fac.scoutAllowance + 1);
				return "cloakriot";
			}
		} else {
			if (numRockos < (2 * numWarriors) + (3 * numZeus)) {
				return "cloakskirm";
			} else if (rand > 0.6) {
				fac.scoutAllowance = Math.min(fac.maxScoutAllowance, fac.scoutAllowance + 1);
				return "cloakassault";
			} else if (rand > 0.15) {
				fac.scoutAllowance = Math.min(fac.maxScoutAllowance, fac.scoutAllowance + 1);
				return "cloakriot";
			} else {
				fac.scoutAllowance = Math.min(fac.maxScoutAllowance, fac.scoutAllowance + 2);
				return "cloaksnipe";
			}
		}
	}
	
	private String getShields(Factory fac) {
		if (needWorkers(fac)) {
			return "shieldcon";
		}
		
		if (warManager.slasherSpam * 140 > assaultValue && warManager.slasherSpam * 140 > warManager.enemyPorcValue){
			return "shieldassault";
		}
		
		if (fac.raiderSpam < 0 || scoutBudget > 0) {
			if (scoutBudget == 0) {
				fac.raiderSpam++;
			}else{
				fac.creatingScout = true;
			}
			if (fac.smallRaiderSpam && scoutBudget == 0) return "shieldassault";
			return "shieldraid";
		}
		
		if (fac.expensiveRaiderSpam < 0) {
			fac.expensiveRaiderSpam++;
			if (fac.smallRaiderSpam) return "shieldriot";
			return "shieldskirm";
		}
		
		if (fac.smallRaiderSpam) fac.smallRaiderSpam = false;
		
		if ((warManager.maxEnemyAirValue > AAvalue && fighterValue > AAvalue && (numAAs < 6 || Math.random() > 0.5))){
			return "shieldaa";
		}
		
		fac.raiderSpam -= 2;
		fac.expensiveRaiderSpam--;
		fac.scoutAllowance = Math.min(fac.maxScoutAllowance, fac.scoutAllowance + 1);
		
		if (hasFusion && numFelons > 0 && ((numAspis < numFelons && Math.random() > 0.5) || (numAspis >= numFelons && numAspis < 2 * numFelons && Math.random() > 0.8))){
			fac.smallRaiderSpam = true;
			return "shieldshield";
		}
		
		if (economyManager.adjustedIncome > 20 && numFelons < Math.min(Math.floor(economyManager.adjustedIncome/15), 4) && Math.random() > 0.5){
			fac.smallRaiderSpam = true;
			return "shieldfelon";
		}
		
		if (economyManager.adjustedIncome > 20 && Math.random() > 0.95 - Math.min(0.05f, graphManager.territoryFraction/10f)){
			return "shieldbomb";
		}
		
		if (Math.random() > (warManager.sniperSightings.isEmpty() ? 0.95 + Math.min(0.025f, graphManager.territoryFraction/20f) : 0.85)){
			fac.raiderSpam -= Math.min(12, Math.max(6, (int) Math.floor(economyManager.adjustedIncome/5f)));
		}
		
		if (economyManager.adjustedIncome > 20 && numRackets * 3 < numThugs + numLaws){
			return "shieldarty";
		}else if (numLaws * 2 > numThugs){
			return "shieldassault";
		}else {
			return "shieldriot";
		}
	}
	
	private String getAmphs(Factory fac) {
		if (needWorkers(fac)) {
			return "amphcon";
		}
		
		if (warManager.slasherSpam * 140 > assaultValue){
			return "amphfloater";
		}
		
		if (fac.raiderSpam < 0 || scoutBudget > 0) {
			if (scoutBudget == 0) {
				fac.raiderSpam++;
			}else{
				fac.creatingScout = true;
			}
			return "amphraid";
		}
		
		if ((warManager.maxEnemyAirValue > AAvalue && fighterValue > AAvalue && (numAAs < 3 || Math.random() > 0.5))){
			return "amphaa";
		}
		
		if (economyManager.adjustedIncome > 30f
			      && warManager.enemyPorcValue > artyValue && warManager.sniperSightings.isEmpty() && fighterValue - artyValue > 2000f && Math.random() > 0.85){
			fac.scoutAllowance = Math.min(fac.maxScoutAllowance, fac.scoutAllowance + 2);
			return "amphassault";
		}
		
		if (economyManager.adjustedIncome > 20 && Math.random() > 0.9){
			return "amphbomb";
		}
		
		fac.raiderSpam--;
		fac.scoutAllowance = Math.min(fac.maxScoutAllowance, fac.scoutAllowance + 1);
		
		if (Math.random() > (warManager.sniperSightings.isEmpty() ? 0.9 + Math.min(0.05f, graphManager.territoryFraction/10f): 0.85)){
			fac.raiderSpam -= Math.min(12, Math.max(6, (int) Math.floor(economyManager.adjustedIncome/5f)));
		}
		
		if (economyManager.adjustedIncome > 35f && Math.random() > 0.9 && fighterValue - artyValue > 2000f){
			fac.scoutAllowance = Math.min(fac.maxScoutAllowance, fac.scoutAllowance + 1);
			return "amphassault";
		}
		double rand = Math.random();
		if (rand > 0.20) {
			return "amphfloater";
		} else {
			if (Math.random() > 0.5) return "amphriot";
			return "amphimpulse";
		}
	}
	
	private String getLV(Factory fac) {
		if (needWorkers(fac)) {
			return "vehcon";
		}
		
		if (warManager.slasherSpam * 140 > assaultValue && warManager.slasherSpam * 140 > warManager.enemyPorcValue){
			return "vehassault";
		}
		
		if (graphManager.eminentTerritory
			      && warManager.enemyHeavyPorcValue > heavyArtyValue && Math.random() > 0.95){
			return "vehheavyarty";
		}
		
		if (economyManager.adjustedIncome > 20
			      && warManager.enemyLightPorcValue > lightArtyValue && Math.random() > 0.65){
			return "veharty";
		}
		
		if ((numAspis > 0 || !warManager.squadHandler.shieldSquads.isEmpty()) && numDomis < 8 && Math.random() > 0.5){
			return "vehcapture";
		}
		
		if (fac.raiderSpam < 0) {
			if ((economyManager.adjustedIncome > 15 && Math.random() > 0.7) || numScouts == 0 || (bigMap && numScouts < 2)){
				return "vehscout";
			}
			fac.raiderSpam++;
			return "vehraid";
		}
		
		if ((warManager.maxEnemyAirValue > AAvalue && fighterValue > AAvalue && (numAAs < 4 || Math.random() > 0.5))){
			return "vehaa";
		}
		
		if (Math.random() > (warManager.sniperSightings.isEmpty() ? 0.95 + Math.min(0.025f, graphManager.territoryFraction/20f) : 0.85)){
			fac.raiderSpam -= Math.min(8f, Math.max(4f, Math.floor(economyManager.adjustedIncome / 4f))) + 1;
		}
		
		double rand = Math.random();
		if (economyManager.adjustedIncome < 35) {
			if (rand > 0.1) {
				fac.raiderSpam--;
				if (numRavagers > numLevelers * 3 || numLevelers == 0) {
					return "vehriot";
				}
				return "vehassault";
			} else {
				return "vehsupport";
			}
		}else{
			fac.raiderSpam--;
			if (rand > 0.35) {
				if (numRavagers > numLevelers * 3 || numLevelers == 0) {
					return "vehriot";
				}
				return "vehassault";
			} else {
				return "veharty";
			}
		}
		
	}
	
	private String getHovers(Factory fac) {
		if (needWorkers(fac)) {
			return "hovercon";
		}
		
		if (warManager.slasherSpam * 140 > assaultValue && warManager.slasherSpam * 140 > warManager.enemyPorcValue){
			return "hoverassault";
		}
		
		if (fac.raiderSpam < 0 || scoutBudget > 0) {
			if (scoutBudget == 0){
				fac.raiderSpam++;
			}else{
				fac.creatingScout = true;
			}
			return "hoverraid";
		}
		
		if (fac.expensiveRaiderSpam < 0){
			fac.expensiveRaiderSpam++;
			return "hoverassault";
		}
		
		if (economyManager.adjustedIncome > 30f
			      && warManager.enemyPorcValue > artyValue && armyValue - artyValue > 2000f && Math.random() > 0.65){
			fac.scoutAllowance = Math.min(fac.maxScoutAllowance, fac.scoutAllowance + 2);
			return "hoverarty";
		}
		
		if ((warManager.maxEnemyAirValue > AAvalue && fighterValue > AAvalue && (numAAs < 3 || Math.random() > 0.5))) {
			return "hoveraa";
		}
		
		if (Math.random() > (warManager.sniperSightings.isEmpty() ? 0.95 + Math.min(0.025f, graphManager.territoryFraction/20f) : 0.85)){
			fac.raiderSpam -= Math.min(12, Math.max(6, (int) Math.floor(economyManager.adjustedIncome/5f)));
		}
		if (Math.random() > 0.9) {
			fac.expensiveRaiderSpam -= (int) Math.min(6, Math.max(2, Math.floor(economyManager.adjustedIncome / 10f))) + 1;
		}
		
		if (Math.random() < graphManager.territoryFraction){
			fac.expensiveRaiderSpam--;
		}else {
			fac.raiderSpam--;
		}
		if (numScalpels > numMaces * (economyManager.adjustedIncome > 30f ? 6 : 4) && numScalpels > 2){
			fac.scoutAllowance = Math.min(fac.maxScoutAllowance, fac.scoutAllowance + 1);
			return "hoverriot";
		}
		
		if (graphManager.eminentTerritory && Math.random() > 0.9){
			fac.scoutAllowance = Math.min(fac.maxScoutAllowance, fac.scoutAllowance + 2);
			return "hoverarty";
		}
		if (Math.random() > 0.5) fac.scoutAllowance = Math.min(fac.maxScoutAllowance, fac.scoutAllowance + 1);
		return "hoverskirm";
	}
	
	private String getTanks(Factory fac) {
		fac.raiderSpam = -scoutBudget;
		if (needWorkers(fac)) {
			return "tankcon";
		}
		
		if (warManager.slasherSpam * 140f > warManager.enemyPorcValue || warManager.slasherSpam * 560f > assaultValue){
			return "tankassault";
		}
		
		if (scoutBudget > 0) {
			fac.creatingScout = true;
			return "tankraid";
		}
		
		if (fac.expensiveRaiderSpam < 0){
			fac.expensiveRaiderSpam++;
			if (Math.random() > 0.5) fac.scoutAllowance = Math.min(fac.maxScoutAllowance, fac.scoutAllowance + 1);
			return "tankheavyraid";
		}
		
		fac.scoutAllowance = Math.min(fac.maxScoutAllowance, fac.scoutAllowance + 1);
		
		if ((warManager.maxEnemyAirValue > AAvalue && fighterValue > AAvalue && (numAAs < 2 || Math.random() > 0.5))){
			return "tankaa";
		}
		
		if ((economyManager.adjustedIncome < 35f || earlyWorker) && !graphManager.eminentTerritory) {
			fac.expensiveRaiderSpam -= 2;
		}else{
			fac.expensiveRaiderSpam--;
		}
		
		if (economyManager.adjustedIncome > 30f && warManager.enemyPorcValue > artyValue && !warManager.squadHandler.squads.isEmpty() && Math.random() > 0.5){
			return "tankarty";
		}
		
		if (Math.random() > (warManager.sniperSightings.isEmpty() ? 0.9 : 0.75)) {
			fac.expensiveRaiderSpam -= (int) Math.min(8, Math.max(2, Math.floor(economyManager.adjustedIncome / (bigMap ? 2.5f : 5f)))) + 1;
		}
		
		double rand = Math.random();
		if (economyManager.adjustedIncome < 35) {
			if (numBanishers * 3 > numReapers){
				fac.expensiveRaiderSpam--;
				return "tankassault";
			}else {
				return "tankriot";
			}
		}else{
			if (rand > 0.1) {
				if (numBanishers * 4 > numReapers){
					return "tankassault";
				}else {
					return "tankriot";
				}
			}else {
				fac.expensiveRaiderSpam -= 4;
				fac.scoutAllowance = Math.min(fac.maxScoutAllowance, fac.scoutAllowance + 1);
				return "tankheavyassault";
			}
		}
	}
	
	private String getSpiders(Factory fac) {
		if (needWorkers(fac)) {
			return "spidercon";
		}
		
		if (warManager.slasherSpam * 140 > assaultValue){
			if (Math.random() > 0.5) {
				return "spiderassault";
			}else{
				return "spiderskirm";
			}
		}
		
		if (fac.raiderSpam < 0) {
			if (Math.random() > 0.5) {
				fac.raiderSpam++;
			}
			return "spiderscout";
		}
		
		if ((warManager.maxEnemyAirValue > AAvalue && fighterValue > AAvalue && (numAAs < 4 || Math.random() > 0.5))){
			return "spideraa";
		}
		
		if (((!bigMap && economyManager.adjustedIncome > 30) || economyManager.adjustedIncome > 60)
			      && warManager.enemyPorcValue > artyValue && Math.random() > 0.75){
			fac.raiderSpam -= 4;
			return "spidercrabe";
		}
		
		fac.raiderSpam -= 2;
		if (numRedbacks < numVenoms){
			return "spiderriot";
		}
		
		if (numRecluses < (numVenoms + numRedbacks)){
			return "spiderskirm";
		}
		
		if (numHermits < (numVenoms + numRedbacks)){
			return "spiderassault";
		}
		
		double rand = Math.random();
		if (rand > 0.9 || numVenoms == 0) {
			return "spideremp";
		} else{
			return "spiderskirm";
		}
	}
	
	private String getGunship(Factory fac){
		if (fac.raiderSpam < 0){
			if (Math.random() > 0.5){
				fac.raiderSpam++;
			}
			return "gunshipbomb";
		}
		
		if (fac.expensiveRaiderSpam < 0){
			fac.expensiveRaiderSpam++;
			return "gunshipheavyskirm";
		}
		
		if(needWorkers(fac)) {
			return "gunshipcon";
		}
		
		if (graphManager.eminentTerritory && (warManager.maxEnemyAirValue > AAvalue && fighterValue > AAvalue)){
			return "gunshipaa";
		}
		
		if (Math.random() > 0.75) {
			fac.raiderSpam -= 2;
		}
		
		if (graphManager.eminentTerritory && Math.random() > 0.95 - Math.max(0.1, 0.2 * graphManager.territoryFraction)){
			fac.raiderSpam -= 3;
			fac.expensiveRaiderSpam -= 6;
			return "gunshipkrow";
		}
		
		if (!graphManager.eminentTerritory){
			return "gunshipskirm";
		}
		
		if (Math.random() > 0.90){
			return "gunshipassault";
		}
		return "gunshipskirm";
	}
	
	private String getPlanes(Factory fac){
		// note: planes do not have raiders and lichos are best AA.
		if(needWorkers(fac)) {
			return "planecon";
		}
		
		if (fac.raiderSpam < 0){
			fac.raiderSpam++;
			return "planefighter";
		}
        
        /*if (((enemyHasAir || enemyHasDrones) && warManager.AAs.size() < 3 + Math.ceil(ai.mergedAllies/2f)) ||
                (enemyHasAir && fighterValue > AAvalue * (3f + (3f * graphManager.territoryFraction * graphManager.territoryFraction)) && Math.random() > 0.5)){
            return "planeheavyfighter";
        }*/
		
		if (graphManager.eminentTerritory) {
			fac.raiderSpam--;
		}
		
		if (numRavens <= (economyManager.baseIncome/(12 * (1 + (0.5 * ai.mergedAllies)))) + 1 || economyManager.adjustedIncome < 35) {
			if (Math.random() > 0.2) {
				return "bomberprec";
			}
			return "bomberriot";
		}
		fac.raiderSpam -= 3;
		return "bomberheavy";
	}
	
	private String getStrider(){
		if (economyManager.adjustedIncome > 60 && warManager.miscHandler.striders.size() > 1 + numHeavyStriders && Math.random() > 0.5){
			if (numHeavyStriders == 0 || Math.random() > 0.35) {
				return "striderbantha";
			}else{
				return "striderdetriment";
			}
		}
		
		if (numUltis < 1 + warManager.enemyHeavyFactor && hasFusion && ((numStriders > 0 && Math.random() > 0.5) || warManager.enemyHasTrollCom)){
			return "striderantiheavy";
		}
		
		double rand = Math.random();
		if(rand > 0.75){
			return  "striderscorpion";
		}else{
			return "striderdante";
		}
	}
}
