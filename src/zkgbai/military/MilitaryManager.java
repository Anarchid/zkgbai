package zkgbai.military;

import java.util.*;

import com.springrts.ai.AI;
import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.*;

import zkgbai.Module;
import zkgbai.ZKGraphBasedAI;
import zkgbai.economy.Worker;
import zkgbai.graph.GraphManager;
import zkgbai.graph.MetalSpot;
import zkgbai.kgbutil.ArrayGraphics;
import zkgbai.kgbutil.ByteArrayGraphics;
import zkgbai.kgbutil.Pathfinder;
import zkgbai.los.LosManager;
import zkgbai.military.fighterhandlers.*;
import zkgbai.military.unitwrappers.*;
import zkgbai.military.tasks.DefenseTarget;

import static zkgbai.kgbutil.KgbUtil.*;

public class MilitaryManager extends Module {
	
	ZKGraphBasedAI ai;
	GraphManager graphManager;
	public Pathfinder pathfinder;
	
	java.util.Map<Integer,Enemy> targets = new HashMap<Integer,Enemy>();
	public java.util.Map<Integer,Enemy> enemyPorcs = new HashMap<Integer,Enemy>();
	public List<DefenseTarget> defenseTargets = new ArrayList<DefenseTarget>();
	List<DefenseTarget> airDefenseTargets = new ArrayList<DefenseTarget>();
	public List<DefenseTarget> sniperSightings = new ArrayList<DefenseTarget>();

	public java.util.Map<Integer, Raider> AAs;
	public java.util.Map<Integer, Raider> hawks = new HashMap<>();

	RadarIdentifier radarID;

	ArrayGraphics threatGraphics;
	ArrayGraphics enemyPorcGraphics;
	ArrayGraphics allyThreatGraphics;
	ArrayGraphics allyPorcGraphics;
	ArrayGraphics aaThreatGraphics;
	ArrayGraphics aaPorcGraphics;
	ArrayGraphics valueGraphics;
	ByteArrayGraphics bpGraphics;
	ByteArrayGraphics riotGraphics;
	ByteArrayGraphics scytheGraphics;
	public ArrayList<TargetMarker> targetMarkers;
	final int width;
	final int height;
	
    public static final short OPTION_SHIFT_KEY = (1 << 5); //  32
	static AIFloat3 nullpos = new AIFloat3(0,0,0);
	
	int nano;

	private LosManager losManager;
	private OOAICallback callback;
	private UnitClasses unitTypes;

	public RetreatHandler retreatHandler;
	public RaiderHandler raiderHandler;
	public SquadHandler squadHandler;
	public MiscHandler miscHandler;
	public BomberHandler bomberHandler;

	private Resource m;

	int frame = 0;
	int lastDefenseFrame = 0;
	int lastValueRedrawFrame = 0;
	
	public int availableMobileThreat;

	public float enemyPorcValue = 0f;
	public float enemyLightPorcValue = 0f;
	public float enemyHeavyPorcValue = 0f;
	public float maxEnemyAirValue = 0f;
	public int slasherSpam = 0;
	public int enemyHeavyFactor = 0;
	public boolean enemyHasTrollCom = false;

	public float enemyFighterValue = 0f;
	
	public boolean enemyHasAntiNuke = false;
	public boolean enemyHasNuke = false;
	boolean hasNuke = false;

	Set<Integer> newUnits = new HashSet<>();
	Set<Integer> unbuiltPorcs = new HashSet<>();

	static int CMD_DONT_FIRE_AT_RADAR = 38372;
	static int CMD_AIR_STRAFE = 39381;
	static int CMD_AP_FLY_STATE = 34569;
	static int CMD_UNIT_AI = 36214;

	// unit and weapon def IDs for unitDamaged.
	int wolvMineID = 0;
	int wolvMineBomblet = 0;
	int sniperBulletID = 0;
	int brawlerBulletID = 0;
	
	@Override
	public String getModuleName() {
		return "MilitaryManager";
	}
	
	public MilitaryManager(ZKGraphBasedAI ai){
		this.ai = ai;
		this.callback = ai.getCallback();

		this.AAs = new HashMap<Integer, Raider>();

		this.nano = callback.getUnitDefByName("staticcon").getUnitDefId();
		this.unitTypes = UnitClasses.getInstance();
		this.m = callback.getResourceByName("Metal");
		
		targetMarkers = new ArrayList<TargetMarker>();
		width = ai.getCallback().getMap().getWidth()/8;
		height = ai.getCallback().getMap().getHeight()/8;

		this.threatGraphics = new ArrayGraphics(width, height);
		this.enemyPorcGraphics = new ArrayGraphics(width, height);
		this.allyThreatGraphics = new ArrayGraphics(width, height);
		this.allyPorcGraphics = new ArrayGraphics(width, height);
		this.aaThreatGraphics = new ArrayGraphics(width, height);
		this.aaPorcGraphics = new ArrayGraphics(width, height);
		this.valueGraphics = new ArrayGraphics(width, height);
		this.bpGraphics = new ByteArrayGraphics(width, height);
		this.riotGraphics = new ByteArrayGraphics(width, height);
		this.scytheGraphics = new ByteArrayGraphics(width, height);
		allyPorcGraphics.clear();
		aaPorcGraphics.clear();
		
		try{
			radarID = new RadarIdentifier(ai.getCallback());
		}catch(Exception e){
			ai.debug(e);
		}
	}

	@Override
	public int init(int AIID, OOAICallback cb){
		this.losManager = ai.losManager;
		this.graphManager = ai.graphManager;

		this.pathfinder = Pathfinder.getInstance();

		this.retreatHandler = new RetreatHandler();
		this.raiderHandler = new RaiderHandler();
		this.squadHandler = new SquadHandler();
		this.bomberHandler = new BomberHandler();
		this.miscHandler = new MiscHandler();

		retreatHandler.init();

		wolvMineID = callback.getUnitDefByName("wolverine_mine").getUnitDefId();
		wolvMineBomblet = callback.getWeaponDefByName("wolverine_mine_bomblet").getWeaponDefId();
		sniperBulletID = callback.getWeaponDefByName("cloaksnipe_shockrifle").getWeaponDefId();
		brawlerBulletID = callback.getWeaponDefByName("gunshipheavyskirm_emg").getWeaponDefId();

		return 0;
	}
	
	@Override
	public int update(int frame) {
		this.frame = frame;
		
		if(frame % 15 == (ai.offset + 2) % 15) {
			updateTargets();
			paintThreatMap();
		}
		
		retreatHandler.update(frame);
		raiderHandler.update(frame);
		squadHandler.update(frame);
		miscHandler.update(frame);
		bomberHandler.update(frame);
		
		if (frame % 22 == ai.offset % 22 && !AAs.isEmpty()){
			List<Integer> dead = new ArrayList<>();
			for (Raider r:AAs.values()){
				if (r.getUnit().getHealth() <= 0 || r.getUnit().getTeam() != ai.teamID){
					dead.add(r.id);
					continue;
				}
				if (retreatHandler.isRetreating(r.getUnit())){
					continue;
				}
				if (getPorcThreat(r.getPos()) > 0 || getEffectiveThreat(r.getPos()) > 0){
					if (r.getUnit().getDef().isAbleToFly()){
						r.sneak(graphManager.getClosestAirHaven(r.getPos()), frame);
					}else {
						r.sneak(graphManager.getClosestHaven(r.getPos()), frame);
					}
				}else {
					r.raid(getAngularPoint(getAATarget(r), r.getPos(), 350f), frame);
				}
			}
			for (Integer id:dead){
				AAs.remove(id);
			}
		}
		
		return 0; // signaling: OK
	}
	
	@Override
	public int unitCreated(Unit unit, Unit builder){
		UnitDef ud = unit.getDef();
		
		if (ud.getSpeed() > 0 && ud.getBuildOptions().isEmpty()){
			newUnits.add(unit.getUnitId());
		}
		
		// Paint ally threat for porc
		if (unit.getDef().getSpeed() == 0 && unit.getDef().isAbleToAttack() && unit.getDef().getCost(m) < 1500f){
			unbuiltPorcs.add(unit.getUnitId());
		}
		return 0;
	}
	
	@Override
	public int unitFinished(Unit unit){
		String defName = unit.getDef().getName();
		if (defName.equals("staticnuke")){
			hasNuke = true;
			miscHandler.addNuke(unit);
		}
		
		if (defName.equals("zenith")){
			miscHandler.addZenith(unit);
		}
		
		if (defName.equals("raveparty")){
			miscHandler.addDRP(unit);
		}
		
		if (defName.equals("staticheavyarty")) {
			miscHandler.addBertha(unit);
		}
		
		if (defName.equals("striderantiheavy")){
			unit.setMoveState(0, (short) 0, Integer.MAX_VALUE);
			unit.setFireState(1, (short) 0, Integer.MAX_VALUE);
			Raider f = new Raider(unit, unit.getDef().getCost(m));
			miscHandler.addUlti(f);
		}
		
		if (unitTypes.striders.contains(defName)){
			Strider st = new Strider(unit, unit.getDef().getCost(m));
			unit.setMoveState(1, (short) 0, Integer.MAX_VALUE);
			miscHandler.addStrider(st);
		}
		
		if (unit.getDef().getSpeed() > 0 && unit.getDef().getBuildOptions().isEmpty()
			      && !defName.equals("shieldfelon") && !unitTypes.noRetreat.contains(defName)){
			retreatHandler.addCoward(unit);
		}
		
		if (unbuiltPorcs.contains(unit.getUnitId())){
			unbuiltPorcs.remove(unit.getUnitId());
			int power = (int) ((unit.getPower() + unit.getMaxHealth())/10);
			float radius = unit.getMaxRange();
			AIFloat3 pos = unit.getPos();
			int x = Math.round(pos.x/64f);
			int y = Math.round(pos.z/64f);
			int rad = Math.round(radius/64f);
			
			allyPorcGraphics.paintCircle(x, y, rad, power);
		}
		return 0;
	}
	
	@Override
	public int unitIdle(Unit unit) {
		if (newUnits.contains(unit.getUnitId())){
			newUnits.remove(unit.getUnitId());
		}else{
			return 0;
		}
		
		String defName = unit.getDef().getName();
		
		// enable snipers and penetrators to shoot radar dots
		if (defName.equals("cloaksnipe") || defName.equals("hoverarty")){
			ArrayList<Float> params = new ArrayList<>();
			params.add((float) 0);
			unit.executeCustomCommand(CMD_DONT_FIRE_AT_RADAR, params, (short) 0, Integer.MAX_VALUE);
			unit.setMoveState(1, (short) 0, Integer.MAX_VALUE);
		}
		
		// disable air strafe for brawlers
		if (defName.equals("gunshipheavyskirm")){
			ArrayList<Float> params = new ArrayList<>();
			params.add((float) 0);
			unit.executeCustomCommand(CMD_AIR_STRAFE, params, (short) 0, Integer.MAX_VALUE);
		}
		
		// set blastwings to land when idle
		if (defName.equals("gunshipbomb")){
			unit.setIdleMode(1, (short) 0, Integer.MAX_VALUE);
		}
		
		if (unitTypes.planes.contains(defName)){
			unit.setIdleMode(0, (short) 0, Integer.MAX_VALUE);
		}
		
		// Activate outlaws
		if (defName.equals("shieldriot")){
			unit.setOn(true, (short) 0, Integer.MAX_VALUE);
		}
		
		if (defName.equals("planefighter")){
			unit.setMoveState(0, (short) 0, Integer.MAX_VALUE);
			miscHandler.addSwift(unit);
		}
		
		if (defName.equals("planeheavyfighter")){
			unit.setMoveState(1, (short) 0, Integer.MAX_VALUE);
			Raider f = new Raider(unit, unit.getDef().getCost(m));
			hawks.put(f.id, f);
			if (hawks.size() > 4){
				AAs.putAll(hawks);
				hawks.clear();
			}
		}
		
		if (defName.equals("gunshipkrow")){
			unit.setMoveState(1, (short) 0, Integer.MAX_VALUE);
			Krow f = new Krow(unit, unit.getDef().getCost(m));
			miscHandler.addKrow(f);
		}
		
		if (unitTypes.smallRaiders.contains(defName)) {
			unit.setMoveState(1, (short) 0, Integer.MAX_VALUE);
			Raider r = new Raider(unit, unit.getDef().getCost(m));
			raiderHandler.addSmallRaider(r);
		}else if (unitTypes.mediumRaiders.contains(defName)) {
			unit.setMoveState(1, (short) 0, Integer.MAX_VALUE);
			Raider r = new Raider(unit, unit.getDef().getCost(m));
			raiderHandler.addMediumRaider(r);
		}else if (unitTypes.soloRaiders.contains(defName)) {
			unit.setMoveState(1, (short) 0, Integer.MAX_VALUE);
			Raider r = new Raider(unit, unit.getDef().getCost(m));
			raiderHandler.addSoloRaider(r);
		}else if (unitTypes.assaults.contains(defName)) {
			unit.setMoveState(1, (short) 0, Integer.MAX_VALUE);
			Fighter f = new Fighter(unit, unit.getDef().getCost(m));
			squadHandler.addAssault(f);
		}else if (unitTypes.shieldMobs.contains(defName)){
			Fighter f = new Fighter(unit, unit.getDef().getCost(m));
			f.shieldMob = true;
			squadHandler.addShieldMob(f);
		}else if(unitTypes.mobSupports.contains(defName)){
			Fighter f = new Fighter(unit, unit.getDef().getCost(m));
			miscHandler.addSupport(f);
		}else if(unitTypes.loners.contains(defName)) {
			if (defName.equals("vehsupport") || defName.equals("spidercrabe")) {
				unit.setMoveState(0, (short) 0, Integer.MAX_VALUE);
				ArrayList<Float> params = new ArrayList<>();
				params.add((float) 1);
				unit.executeCustomCommand(CMD_UNIT_AI, params, (short) 0, Integer.MAX_VALUE);
			} else {
				unit.setMoveState(1, (short) 0, Integer.MAX_VALUE);
			}
			Fighter f = new Fighter(unit, unit.getDef().getCost(m));
			miscHandler.addLoner(f);
		}else if (unitTypes.arties.contains(defName)){
			if (defName.equals("vehheavyarty") || defName.equals("tankarty") || defName.equals("striderarty") || defName.equals("tankheavyarty")){
				unit.setMoveState(0, (short) 0, Integer.MAX_VALUE);
				ArrayList<Float> params = new ArrayList<>();
				params.add((float) 1);
				unit.executeCustomCommand(CMD_UNIT_AI, params, (short) 0, Integer.MAX_VALUE);
			}else{
				unit.setMoveState(1, (short) 0, Integer.MAX_VALUE);
			}
			Fighter f = new Fighter(unit, unit.getDef().getCost(m));
			miscHandler.addArty(f);
		}else if (unitTypes.AAs.contains(defName)){
			unit.setMoveState(2, (short) 0, Integer.MAX_VALUE);
			Raider f = new Raider(unit, unit.getDef().getCost(m));
			AAs.put(f.id, f);
			AIFloat3 pos = graphManager.getAllyCenter();
			if (pos != null){
				unit.fight(pos, (short) 0, Integer.MAX_VALUE);
			}
		}else if (unitTypes.sappers.contains(defName)){
			unit.setMoveState(2, (short) 0, Integer.MAX_VALUE);
			unit.setFireState(2, (short) 0, Integer.MAX_VALUE);
			ArrayList<Float> params = new ArrayList<>();
			params.add((float) 0);
			unit.executeCustomCommand(CMD_UNIT_AI, params, (short) 0, Integer.MAX_VALUE);
			unit.fight(getRadialPoint(graphManager.getAllyCenter(), 800f), (short) 0, Integer.MAX_VALUE);
			miscHandler.addSapper(unit);
		}else if (unitTypes.bombers.contains(defName)) {
			unit.setMoveState(2, (short) 0, Integer.MAX_VALUE);
			unit.setFireState(2, (short) 0, Integer.MAX_VALUE);
			Fighter f = new Fighter(unit, unit.getDef().getCost(m));
			bomberHandler.addBomber(f);
		}
		
		return 0; // signaling: OK
	}
	
	@Override
	public int unitDamaged(Unit h, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzed) {
		if (h.getDef().getUnitDefId() == wolvMineID || (weaponDef != null && weaponDef.getWeaponDefId() == wolvMineBomblet)){
			return 0;
		}
		// check if the damaged unit is on fire.
		boolean on_fire = h.getRulesParamFloat("on_fire", 0.0f) > 0;
		
		// retreat scouting raiders so that they don't suicide into enemy raiders
		if (h.getMaxSpeed() > 0) {
			retreatHandler.checkUnit(h, on_fire);
			if (!on_fire) raiderHandler.avoidEnemies(h, attacker, dir);
		}
		
		// create a defense task, if appropriate.
		if ((attacker == null || attacker.getPos() == null || attacker.getPos().equals(nullpos) || attacker.getDef() == null)
			      && (frame - lastDefenseFrame > 30 || (weaponDef != null && weaponDef.getWeaponDefId() == sniperBulletID)) && !on_fire) {
			lastDefenseFrame = frame;
			DefenseTarget dt = null;
			
			// only create defense targets on unitDamaged if the attacker is invisible or out of los.
			if ((attacker == null || attacker.getDef() == null) && (weaponDef == null || !unitTypes.porcWeps.contains(weaponDef.getWeaponDefId()))) {
				float x = 800 * dir.x;
				float z = 800 * dir.z;
				AIFloat3 pos = h.getPos();
				AIFloat3 target = new AIFloat3();
				target.x = pos.x + x;
				target.z = pos.z + z;
				dt = new DefenseTarget(target, 2000f, frame);
			} else if (attacker != null && attacker.getDef() != null) {
				dt = new DefenseTarget(attacker.getPos(), (attacker.getMaxHealth() + h.getMaxHealth()) / 2f, frame);
			}
			
			// don't create defense targets vs air units.
			if (dt != null) {
				if (weaponDef != null && weaponDef.getWeaponDefId() == sniperBulletID){
					sniperSightings.add(dt);
					defenseTargets.add(dt);
				}else if (weaponDef != null && weaponDef.getWeaponDefId() == brawlerBulletID) {
					airDefenseTargets.add(dt);
				} else {
					defenseTargets.add(dt);
				}
			}
		}
		return 0;
	}
	
	@Override
	public int unitDestroyed(Unit unit, Unit attacker) {
		if (unit.getDef().getName().equals("wolverine_mine")){
			return 0;
		}
		
		if (newUnits.contains(unit.getUnitId())){
			newUnits.remove(unit.getUnitId());
		}
		
		retreatHandler.removeUnit(unit);
		raiderHandler.removeUnit(unit);
		squadHandler.removeUnit(unit);
		bomberHandler.removeUnit(unit);
		miscHandler.removeUnit(unit);
		
		if (unit.getDef().getName().equals("staticnuke")){
			hasNuke = false;
		}
		
		if (AAs.containsKey(unit.getUnitId())){
			AAs.remove(unit.getUnitId());
		}
		
		if (hawks.containsKey(unit.getUnitId())){
			hawks.remove(unit.getUnitId());
		}
		
		if (unitTypes.sappers.contains(unit.getDef().getName())){
			// rally units to fight if a tick/roach etc dies.
			DefenseTarget dt = new DefenseTarget(unit.getPos(), 1500f, frame);
			defenseTargets.add(dt);
		}
		
		// Unpaint ally threat for porc
		if (unit.getDef().getSpeed() == 0 && unit.getDef().isAbleToAttack() && !unit.getDef().getName().equals("staticheavyarty")){
			if (unbuiltPorcs.contains(unit.getUnitId())){
				unbuiltPorcs.remove(unit.getUnitId());
			}else{
				int power = (int) ((unit.getPower() + unit.getMaxHealth()) / 10);
				float radius = unit.getMaxRange();
				AIFloat3 pos = unit.getPos();
				int x =  Math.round(pos.x / 64f);
				int y = Math.round(pos.z / 64f);
				int rad = Math.round(radius / 64f);
				
				allyPorcGraphics.unpaintCircle(x, y, rad, power);
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
		// remove units that allies captured from enemy targets
		if (targets.containsKey(unit.getUnitId())){
			Enemy e = targets.get(unit.getUnitId());
			e.position = null; // needed for bomberTasks
			targets.remove(unit.getUnitId());
		}
		if (newTeamID == ai.teamID){
			unitCreated(unit, null);
			unitFinished(unit);
			unitIdle(unit);
		}
		return 0;
	}
	
	@Override
	public int enemyEnterLOS(Unit enemy) {
		Resource metal = ai.getCallback().getResourceByName("Metal");
		
		if(targets.containsKey(enemy.getUnitId())){
			if (enemy.getDef().getName().equals("wolverine_mine")){
				targets.remove(enemy.getUnitId());
				return 0;
			}
			Enemy e = targets.get(enemy.getUnitId());
			e.visible = true;
			e.lastSeen = frame;
			e.checkNano();
			if (!e.identified) {
				e.setIdentified();
				e.updateFromUnitDef(enemy.getDef(), enemy.getDef().getCost(metal));
				if (e.isPorc && !e.isNanoSpam){
					enemyPorcs.put(enemy.getUnitId(),e);
				}
			}
			
			// paint enemy threat for aa
			if (e.isStatic && e.isAA && !e.isPainted && !e.unit.isBeingBuilt()) {
				int effectivePower = (int) e.getDanger();
				AIFloat3 position = e.position;
				int x = Math.round(position.x / 64f);
				int y = Math.round(position.z / 64f);
				int r = Math.round((e.threatRadius) / 64f);
				
				aaPorcGraphics.paintCircle(x, y, r + 1, effectivePower);
				e.isPainted = true;
			}
		}else if (!enemy.getDef().getName().equals("wolverine_mine")){
			Enemy e = new Enemy(enemy, enemy.getDef().getCost(metal));
			targets.put(enemy.getUnitId(),e);
			e.visible = true;
			e.setIdentified();
			e.lastSeen = frame;
			e.checkNano();
			
			if (e.isPorc && !e.isNanoSpam){
				enemyPorcs.put(enemy.getUnitId(),e);
			}
			
			if (e.isStatic && e.isAA && !e.isPainted && !e.unit.isBeingBuilt()) {
				int effectivePower = (int) e.getDanger();
				AIFloat3 position = e.position;
				int x = Math.round(position.x / 64f);
				int y = Math.round(position.z / 64f);
				int r = Math.round((e.threatRadius) / 64f);
				
				aaPorcGraphics.paintCircle(x, y, r + 1, effectivePower);
				e.isPainted = true;
			}
		}
		return 0; // signaling: OK
	}
	
	@Override
	public int enemyFinished(Unit enemy) {
		if (enemy.getDef() == null){
			return 0;
		}
		
		Resource metal = ai.getCallback().getResourceByName("Metal");
		
		if(targets.containsKey(enemy.getUnitId())){
			Enemy e = targets.get(enemy.getUnitId());
			e.checkNano();
			if (!e.identified) {
				e.setIdentified();
				e.updateFromUnitDef(enemy.getDef(), enemy.getDef().getCost(metal));
				if (e.isPorc && !e.isNanoSpam){
					enemyPorcs.put(enemy.getUnitId(),e);
				}
			}
			// paint enemy threat for aa
			if (e.isStatic && e.isAA && !e.isPainted) {
				int effectivePower = (int) e.getDanger();
				AIFloat3 position = e.position;
				int x = Math.round(position.x / 64f);
				int y = Math.round(position.z / 64f);
				int r = Math.round((e.threatRadius) / 64f);
				
				aaThreatGraphics.paintCircle(x, y, r + 1, effectivePower);
				e.isPainted = true;
			}
		}else if (!enemy.getDef().getName().equals("wolverine_mine")){
			Enemy e = new Enemy(enemy, enemy.getDef().getCost(metal));
			targets.put(enemy.getUnitId(),e);
			e.visible = true;
			e.setIdentified();
			e.lastSeen = frame;
			
			if (e.isPorc && !e.isNanoSpam){
				enemyPorcs.put(enemy.getUnitId(),e);
			}
		}
		return 0;
	}
	
	@Override
	public int enemyLeaveLOS(Unit enemy) {
		if(targets.containsKey(enemy.getUnitId())){
			targets.get(enemy.getUnitId()).visible = false;
			targets.get(enemy.getUnitId()).lastSeen = frame;
		}
		return 0; // signaling: OK
	}
	
	@Override
	public int enemyEnterRadar(Unit enemy) {
		if(targets.containsKey(enemy.getUnitId())){
			targets.get(enemy.getUnitId()).isRadarVisible = true;
			targets.get(enemy.getUnitId()).lastSeen = frame;
		}else{
			Enemy e = new Enemy(enemy, 50);
			targets.put(enemy.getUnitId(),e);
			e.isRadarVisible = true;
			e.lastSeen = frame;
		}
		
		return 0; // signaling: OK
	}
	
	@Override
	public int enemyLeaveRadar(Unit enemy) {
		if(targets.containsKey(enemy.getUnitId())){
			targets.get(enemy.getUnitId()).isRadarVisible = false;
			targets.get(enemy.getUnitId()).lastSeen = frame;
		}
		return 0; // signaling: OK
	}
	
	@Override
	public int enemyDestroyed(Unit unit, Unit attacker) {
		if(targets.containsKey(unit.getUnitId())){
			Enemy e = targets.get(unit.getUnitId());
			if (e.identified && e.ud != null && (e.ud.getName().equals("energysingu") || e.ud.getName().equals("energyheavygeo")) && !e.isNanoSpam){
				ai.say("SHINY!");
			}
			
			// Unpaint enemy threat for statics
			if (e.isStatic && e.isAA && e.isPainted) {
				int effectivePower = (int) e.getDanger();
				AIFloat3 position = e.position;
				int x = Math.round(position.x / 64f);
				int y = Math.round(position.z / 64f);
				int r = Math.round((e.threatRadius) / 64f);
				
				aaPorcGraphics.unpaintCircle(x, y, r + 1, effectivePower);
			}
			e.position = null;
			
			targets.remove(unit.getUnitId());
			if (e.isPorc && !e.isNanoSpam){
				enemyPorcs.remove(unit.getUnitId());
			}
		}
		return 0; // signaling: OK
	}
	
	private void paintValueMap(String wepName){
		valueGraphics.clear();
		int r, rr;
		if (wepName.equals("staticnuke")) {
			r = 14; // general superwep kill radius
			rr = 20;
		}else{
			r = 10;
			rr = 13;
		}
		for(Enemy t:targets.values()){
			AIFloat3 position = t.position;
			if (position != null && t.identified && t.ud != null && !t.isNanoSpam && !t.ud.isAbleToFly() && !t.ud.getName().equals("turretaalaser") && !t.ud.getName().equals("turretgauss")){
				int x = Math.round(position.x / 64f);
				int y = Math.round(position.z / 64f);
				int value = (int) Math.min(t.ud.getCost(m)/10f, 750f);
				
				valueGraphics.paintCircle(x, y, r, value);
			}
		}
		
		for (Squad s:squadHandler.squads){
			if (!s.isDead()){
				AIFloat3 position = s.getPos();
				int x = Math.round(position.x / 64f);
				int y = Math.round(position.z / 64f);
				int value = (int) (s.metalValue/10f);
				
				valueGraphics.unpaintCircle(x, y, rr, value);
			}
		}
		
		for (Strider s:miscHandler.striders.values()){
			AIFloat3 position = s.getPos();
			int x = Math.round(position.x / 64f);
			int y = Math.round(position.z / 64f);
			int value = (int) Math.min(s.metalValue/10f, 750f);
			
			valueGraphics.unpaintCircle(x, y, rr, value);
		}
		
		for (ShieldSquad s: squadHandler.shieldSquads){
			if (!s.isDead()) {
				AIFloat3 position = s.getPos();
				int x = Math.round(position.x / 64f);
				int y = Math.round(position.z / 64f);
				int value = (int) (s.metalValue / 10f);
				valueGraphics.unpaintCircle(x, y, r, value);
			}
		}
	}
	
	private void paintThreatMap(){
		threatGraphics.clear();
		aaThreatGraphics.clear();
		riotGraphics.clear();
		scytheGraphics.clear();
		
		boolean updatePorc = false;
		if (frame % 300 == (ai.offset + 2) % 300){
			enemyPorcGraphics.clear();
			updatePorc = true;
		}

		// paint allythreat in a separate thread, since it's independent of enemy threat.
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				allyThreatGraphics.clear();
				availableMobileThreat = 0;
				// paint allythreat for raiders
				for (Raider r : raiderHandler.soloRaiders) {
					if (r.getUnit().getHealth() <= 0 || r.getUnit().getTeam() != ai.teamID) continue;
					int power = (int) ((r.getUnit().getPower() + r.getUnit().getMaxHealth()) / 14f);
					AIFloat3 pos = r.getPos();
					int x = Math.round(pos.x / 64f);
					int y = Math.round(pos.z / 64f);
					int rad = 7;
					allyThreatGraphics.paintCircle(x, y, rad, power);
				}

				for (RaiderSquad rs: raiderHandler.raiderSquads) {
					if (rs.isDead()) continue;
					int power = rs.getThreat();
					if (rs.status == 'f') availableMobileThreat += power;
					AIFloat3 pos = rs.getPos();
					int x = Math.round(pos.x / 64f);
					int y = Math.round(pos.z / 64f);
					int rad = 8;
					allyThreatGraphics.paintCircle(x, y, rad, power);
				}

				// paint allythreat for fighters
				for (Squad s: squadHandler.squads) {
					if (s.isDead()) continue;
					int power = s.getThreat();
					availableMobileThreat += power;
					AIFloat3 pos = s.getPos();
					int x = Math.round(pos.x / 64f);
					int y = Math.round(pos.z / 64f);
					int rad = 13;

					allyThreatGraphics.paintCircle(x, y, rad, power);
				}
				
				for (Squad s: squadHandler.airSquads) {
					if (s.isDead()) continue;
					int power = s.getThreat();
					availableMobileThreat += power;
					AIFloat3 pos = s.getPos();
					int x = Math.round(pos.x / 64f);
					int y = Math.round(pos.z / 64f);
					int rad = 13;
					
					allyThreatGraphics.paintCircle(x, y, rad, power);
				}
				
				for (ShieldSquad s: squadHandler.shieldSquads) {
					if (s.isDead()) continue;
					int power = s.getThreat();
					availableMobileThreat += power;
					AIFloat3 pos = s.getPos();
					int x = Math.round(pos.x / 64f);
					int y = Math.round(pos.z / 64f);
					int rad = 13;
					
					allyThreatGraphics.paintCircle(x, y, rad, power);
				}

				// paint allythreat for striders
				for (Strider s : miscHandler.striders.values()) {
					if (s.getUnit().getHealth() <= 0 || s.getUnit().getTeam() != ai.teamID) continue;
					int power = (int) ((s.getUnit().getPower() + s.getUnit().getMaxHealth()) / 6f);
					if (!retreatHandler.isRetreating(s.getUnit())) availableMobileThreat += power;
					AIFloat3 pos = s.getPos();
					int x = Math.round(pos.x / 64f);
					int y = Math.round(pos.z / 64f);
					int rad = 15;

					allyThreatGraphics.paintCircle(x, y, rad, power);
				}
				
				// paint allythreat for krows
				for (Strider s : miscHandler.krows.values()) {
					if (s.getUnit().getHealth() <= 0 || s.getUnit().getTeam() != ai.teamID) continue;
					int power = (int) ((s.getUnit().getPower() + s.getUnit().getMaxHealth()) / 6f);
					if (!retreatHandler.isRetreating(s.getUnit())) availableMobileThreat += power;
					AIFloat3 pos = s.getPos();
					int x = Math.round(pos.x / 64f);
					int y = Math.round(pos.z / 64f);
					int rad = 15;
					
					allyThreatGraphics.paintCircle(x, y, rad, power);
				}
				
				availableMobileThreat /= 500f;
				availableMobileThreat *= 1.5f;

				// paint allythreat for commanders
				for (Worker w: ai.ecoManager.commanders){
					if (w.getUnit().getHealth() <= 0 || w.getUnit().getTeam() != ai.teamID) continue;
					int power = (int) ((w.getUnit().getPower() + w.getUnit().getMaxHealth()) / 10f);
					AIFloat3 pos = w.getPos();
					int x = Math.round(pos.x / 64f);
					int y = Math.round(pos.z / 64f);
					int rad = 15;

					allyThreatGraphics.paintCircle(x, y, rad, power);
				}

				//paint buildpower
				for (Worker w: ai.ecoManager.workers.values()){
					if (w.getUnit().getHealth() <= 0 || w.getUnit().getTeam() != ai.teamID) continue;
					AIFloat3 pos = w.getPos();
					int x = Math.round(pos.x / 64f);
					int y = Math.round(pos.z / 64f);

					bpGraphics.paintCircle(x, y, w.buildRange, Math.round(w.bp));
				}
			}
		});
		thread.start();

		// Note: allythreat for porc is painted separately.

		for(Enemy t:targets.values()){
			int effectivePower = (int) t.getDanger();

			AIFloat3 position = t.position;
			
			int x = Math.round(position.x / 64f);
			int y = Math.round(position.z / 64f);
			if (!t.identified || t.ud == null || !t.ud.isAbleToFly()) {
				scytheGraphics.paintCircle(x, y, 4, 1);
			}

			if (position != null && t.ud != null
					&& effectivePower > 0
					&& (!unitTypes.planes.contains(t.ud.getName()))
					&& !t.unit.isBeingBuilt()
					&& !t.unit.isParalyzed()
					&& t.unit.getRulesParamFloat("disarmed", 0f) == 0) {
				int r = Math.round(t.threatRadius/64f);

				if (t.ud.getName().equals("gunshipskirm")){
					r *= 5;
				}
				
				if (!t.isStatic) {
					if (!t.ud.isAbleToFly()) {
						effectivePower = (int) (effectivePower * Math.max(0, (1f - (((frame - t.lastSeen)) / 900f))));
					}
					// paint enemy threat for mobiles
					if (t.isAA || t.isFlexAA){
						aaThreatGraphics.paintCircle(x, y, Math.round(r * 1.25f), effectivePower);
					}else{
						if (t.isRiot) riotGraphics.paintCircle(x, y, r + 1, 1);
						threatGraphics.paintCircle(x, y, r + 1, effectivePower);
					}
				}else if (!t.isAA){
					// for statics
					if (t.isRiot) riotGraphics.paintCircle(x, y, r + 1, 1);
					threatGraphics.paintCircle(x, y, r + 1, effectivePower);
					if (updatePorc){
						enemyPorcGraphics.paintCircle(x, y, r - 1, effectivePower);
					}
				}
			}
		}

		ArrayList<TargetMarker> deadMarkers = new ArrayList<TargetMarker>();
		for(TargetMarker tm:targetMarkers){
			int age = ai.currentFrame - tm.frame;
			if(age < 255){

			}else{
				deadMarkers.add(tm);
			}
		}
		
		for(TargetMarker tm:deadMarkers){
			targetMarkers.remove(tm);
		}
		
		boolean ok = false;
		while (!ok) {
			try {
				thread.join();
				ok = true;
			} catch (InterruptedException e) {
				// ignore, JVM is just being a dolt
			} catch (Exception e) {
				// something more brutal happened
				ai.debug(e);
				System.exit(-1);
			}
		}
	}
	
	private void updateTargets() {
		List<Enemy> outdated = new ArrayList<Enemy>();
		for (Enemy t : targets.values()) {
			AIFloat3 tpos = t.unit.getPos();
			if (tpos != null && !tpos.equals(nullpos)) {
				t.lastSeen = frame;
				t.position = tpos;
			}else if (frame - t.lastSeen > 1800 && !t.isStatic) {
				// remove mobiles that haven't been seen for over 60 seconds.
				outdated.add(t);
			}else if (t.position != null && losManager.isInLos(t.position)) {
				// remove targets that aren't where we last saw them.
				outdated.add(t);
				
				// Unpaint enemy threat for aa
				if (t.isAA && t.isPainted) {
					int effectivePower = (int) t.getDanger();
					AIFloat3 position = t.position;
					int x = Math.round(position.x / 64f);
					int y = Math.round(position.z / 64f);
					int r = Math.round((t.threatRadius) / 64f);
					
					aaPorcGraphics.unpaintCircle(x, y, r + 1, effectivePower);
				}
			}
		}
		
		for (Enemy t: outdated){
			t.position = null; // needed for BomberTasks
			targets.remove(t.unitID);
			if (t.isPorc && !t.isNanoSpam){
				enemyPorcs.remove(t.unitID);
			}
		}
		
		List<DefenseTarget> expired = new ArrayList<DefenseTarget>();
		for (DefenseTarget d:defenseTargets){
			if (frame - d.frameIssued > 300){
				expired.add(d);
			}
		}
		defenseTargets.removeAll(expired);
		expired.clear();
		
		for (DefenseTarget d:airDefenseTargets){
			if (frame - d.frameIssued > 900){
				expired.add(d);
			}
		}
		airDefenseTargets.removeAll(expired);
		
		for (DefenseTarget d:sniperSightings){
			if (frame - d.frameIssued > 900){
				expired.add(d);
			}
		}
		sniperSightings.removeAll(expired);
		
		enemyPorcValue = 0f;
		enemyLightPorcValue = 0f;
		enemyHeavyPorcValue = 0f;
		slasherSpam = 0;
		// add up the value of heavy porc that the enemy has
		// and also check for slasher spam
		enemyHasAntiNuke = false;
		enemyHasNuke = false;
		float enemyAirValue = 0f;
		enemyHeavyFactor = 0;
		enemyFighterValue = 0;
		for (Enemy t: targets.values()){
			if (t.identified){
				String defName = t.ud.getName();
				if (defName.equals("turretheavylaser") || defName.equals("turretgauss") || defName.equals("turretriot")
					      || defName.equals("turretemp") || defName.equals("turretimpulse") || defName.equals("spidercrabe")){
					enemyPorcValue += t.ud.getCost(m);
					enemyHeavyPorcValue += t.ud.getCost(m);
				}else if (defName.equals("turretmissile") || defName.equals("turretlaser")) {
					enemyPorcValue += t.ud.getCost(m);
					enemyLightPorcValue += t.ud.getCost(m);
				}else if (defName.equals("turretantiheavy") || defName.equals("turretheavy") || defName.equals("staticshield") || defName.equals("shieldshield")) {
					enemyPorcValue += 1.5f * t.ud.getCost(m);
					enemyHeavyPorcValue += 1.5f * t.ud.getCost(m);
				}else if (defName.equals("shieldfelon") || defName.equals("shieldassault") || defName.equals("amphassault")){
					enemyPorcValue += 2f * t.ud.getCost(m);
					enemyLightPorcValue += t.ud.getCost(m);
					enemyHeavyPorcValue += t.ud.getCost(m);
				}else if (defName.equals("vehsupport")) {
					slasherSpam++;
				}else if (t.isRaider){
					slasherSpam--;
				}else if (defName.equals("staticantinuke")){
					enemyHasAntiNuke = true;
				}else if (defName.equals("staticnuke")){
					enemyHasNuke = true;
				}else if (defName.equals("striderbantha")){
					enemyHeavyFactor++;
				}else if (defName.equals("striderdetriment")){
					enemyHeavyFactor += 2;
				}else if (t.ud.isAbleToFly() && !t.isNanoSpam){
					if (defName.equals("dronelight") || defName.equals("droneheavyslow") || defName.equals("dronecarry")){
						enemyAirValue += 25f;
					}else {
						enemyAirValue += t.ud.getBuildTime();
					}
				}else if (t.unit.getRulesParamFloat("comm_level", 0f) > 3f){
					enemyHasTrollCom = true;
				}
				
				if (t.identified && t.ud.getSpeed() > 0 && t.ud.getMaxWeaponRange() > 0 && t.ud.getBuildOptions().isEmpty()){
					enemyFighterValue += t.ud.getCost(m);
				}
			}
		}
		if (enemyAirValue > maxEnemyAirValue){
			maxEnemyAirValue = enemyAirValue;
		}
	}
	
	public float getThreat(AIFloat3 position){
		int x = Math.round(position.x / 64f);
		int y = Math.round(position.z / 64f);

		return (threatGraphics.getValue(x, y))/500f;
	}
	
	public float getPorcThreat(AIFloat3 position){
		int x = Math.round(position.x / 64f);
		int y = Math.round(position.z / 64f);
		
		return (enemyPorcGraphics.getValue(x, y))/500f;
	}

	public float getRiotThreat(AIFloat3 position){
		int x = Math.round(position.x / 64f);
		int y = Math.round(position.z / 64f);

		return riotGraphics.getValue(x, y);
	}

	public float getScytheThreat(AIFloat3 position){
		int x = Math.round(position.x / 64f);
		int y = Math.round(position.z / 64f);

		return scytheGraphics.getValue(x, y);
	}

	public float getEffectiveThreat(AIFloat3 position){
		int x = Math.round(position.x / 64f);
		int y = Math.round(position.z / 64f);

		return ((threatGraphics.getValue(x, y)) - (allyThreatGraphics.getValue(x, y) + allyPorcGraphics.getValue(x, y)))/500f;
	}
	
	public float getTacticalThreat(AIFloat3 position){
		int x = Math.round(position.x / 64f);
		int y = Math.round(position.z / 64f);
		
		return ((threatGraphics.getValue(x, y)) - allyPorcGraphics.getValue(x, y))/500f;
	}

	public float getFriendlyThreat(AIFloat3 position){
		int x = Math.round(position.x / 64f);
		int y = Math.round(position.z / 64f);

		return allyThreatGraphics.getValue(x, y)/500f;
	}
	
	public float getFriendlyPorcThreat(AIFloat3 position){
		int x = Math.round(position.x / 64f);
		int y = Math.round(position.z / 64f);
		
		return allyPorcGraphics.getValue(x, y);
	}
	
	public float getTotalFriendlyThreat(AIFloat3 position){
		int x = Math.round(position.x / 64f);
		int y = Math.round(position.z / 64f);
		
		return (allyThreatGraphics.getValue(x, y) + allyPorcGraphics.getValue(x, y))/500f;
	}

	public float getAAThreat(AIFloat3 position){
		int x = Math.round(position.x / 64f);
		int y = Math.round(position.z / 64f);

		return (aaThreatGraphics.getValue(x, y) + aaPorcGraphics.getValue(x, y))/500f;
	}

	public float getEffectiveAAThreat(AIFloat3 position){
		int x = Math.round(position.x / 64f);
		int y = Math.round(position.z / 64f);

		return Math.max(0, ((aaThreatGraphics.getValue(x, y) + aaPorcGraphics.getValue(x, y) + threatGraphics.getValue(x, y)) - (allyThreatGraphics.getValue(x, y) + allyPorcGraphics.getValue(x, y)))/500f);
	}
	
	public float getValue(AIFloat3 position){
		int x = Math.round(position.x / 64f);
		int y = Math.round(position.z / 64f);
		
		return (valueGraphics.getValue(x, y));
	}

	public float getBP(AIFloat3 position){
		int x = Math.round(position.x / 64f);
		int y = Math.round(position.z / 64f);

		return (bpGraphics.getValue(x, y));
	}
 
	public AIFloat3 getTarget(Unit u, boolean defend){
		AIFloat3 origin = u.getPos();
    	AIFloat3 target = null;
		float fthreat = getFriendlyThreat(origin);
    	float cost = Float.MAX_VALUE;

		// first check defense targets
		if (defend) {
			// check for defense targets first
			for (DefenseTarget d : defenseTargets) {
				if (getTacticalThreat(d.position) > availableMobileThreat) continue;
				float tmpcost = distance(origin, d.position) - (d.damage * (1f + getEffectiveThreat(d.position)));

				if (tmpcost < cost) {
					cost = tmpcost;
					target = d.position;
				}
			}
		}

		// then look for enemy mexes to kill, and see which is cheaper
		for (MetalSpot m:graphManager.getEnemySpots()){
			if (getThreat(m.getPos()) > fthreat || !pathfinder.isAssaultReachable(u, m.getPos(), fthreat)){
				continue;
			}
			float tmpcost = (distance(m.getPos(), origin)/4f) - 500f;
			tmpcost += 500f * getThreat(m.getPos());
   
			if (tmpcost < cost){
				target = m.getPos();
				cost = tmpcost;
			}
		}


		// then look for enemy units to kill, and see if any are better targets
		// then check for nearby enemies that aren't raiders.
		for (Enemy e : targets.values()) {
			boolean allyTerritory = graphManager.isAllyTerritory(e.position);
			float ethreat = getEffectiveThreat(e.position);
			if ((ethreat > fthreat && (!allyTerritory || getPorcThreat(e.position) > 0))
					  || getTacticalThreat(e.position) > availableMobileThreat
					|| (e.identified && e.ud.isAbleToFly())
				      || ((!e.identified || e.isRaider) && distance(origin, e.position) > 1250f)){
				continue;
			}
			float tmpcost = distance(origin, e.position);
			if (!e.isAA){
				if (e.identified && !e.isImportant && !e.isRaider && allyTerritory){
					tmpcost /= 2;
					tmpcost -= e.getDanger() * (1f + getThreat(e.position));
				}else if (e.isImportant && pathfinder.isAssaultReachable(u, e.position, fthreat)){
					tmpcost /= 4f;
					tmpcost -= 750f;
				}else {
					tmpcost -= e.getDanger();
				}
			}

			if (tmpcost < cost) {
				cost = tmpcost;
				target = e.position;
			}
		}
    	
    	if (target != null){
			TargetMarker tm = new TargetMarker(target, frame);
			targetMarkers.add(tm);
			return target;
		}

		// if no targets are available attack enemyshadowed metal spots until we find one
    	for (MetalSpot m:graphManager.getUnownedSpots()){
			if (m.enemyShadowed){
				float tmpcost = distance(m.getPos(), origin);
				tmpcost /= 1+getThreat(m.getPos());
				tmpcost /= (frame-m.getLastSeen())/600;

				if (tmpcost < cost) {
					target = m.getPos();
					cost = tmpcost;
				}
			}
		}
		if (target != null) {
			TargetMarker tm = new TargetMarker(target, frame);
			targetMarkers.add(tm);
			return target;
		}
		return nullpos;
    }

	public AIFloat3 getArtyTarget(AIFloat3 origin, boolean defend){
		AIFloat3 target = null;
		float cost = Float.MAX_VALUE;

		// first check defense targets
		if (defend) {
			// check for defense targets first
			for (DefenseTarget d : defenseTargets) {
				float tmpcost = distance(origin, d.position) - d.damage;

				if (tmpcost < cost) {
					cost = tmpcost;
					target = d.position;
				}
			}
		}

		// then look for enemy mexes to kill, and see which is cheaper
		List<MetalSpot> ms = graphManager.getEnemySpots();
		for (MetalSpot m:ms){
			float tmpcost = (distance(m.getPos(), origin)/4) - (500 * getThreat(m.getPos()));

			if (tmpcost < cost){
				target = m.getPos();
				cost = tmpcost;
			}
		}


		// then look for enemy units to kill, and see if any are better targets
		// then check for nearby enemies that aren't raiders.
		for (Enemy e : targets.values()) {
			if ((e.identified && e.ud.isAbleToFly())
					|| (e.isRaider)){
				continue;
			}
			float tmpcost = distance(origin, e.position) - (500 * getThreat(e.position));

			if (e.isMajorCancer){
				tmpcost /= 4;
				tmpcost -= 1000;
			}else if (e.isMinorCancer){
				tmpcost /= 2;
				tmpcost -= 500;
			}

			if (graphManager.isAllyTerritory(e.position)){
				tmpcost /= 2;
				tmpcost -= 250;
			}

			if (tmpcost < cost) {
				cost = tmpcost;
				target = e.position;
			}
		}

		if (target != null){
			TargetMarker tm = new TargetMarker(target, frame);
			targetMarkers.add(tm);
			return target;
		}

		// if no targets are available attack enemyshadowed metal spots until we find one
		ms = graphManager.getUnownedSpots();
		for (MetalSpot m:ms){
			if (m.enemyShadowed){
				float tmpcost = distance(m.getPos(), origin);
				tmpcost /= 1+getThreat(m.getPos());
				tmpcost /= (frame-m.getLastSeen())/600;

				if (tmpcost < cost) {
					target = m.getPos();
					cost = tmpcost;
				}
			}
		}
		if (target != null) {
			TargetMarker tm = new TargetMarker(target, frame);
			targetMarkers.add(tm);
			return target;
		}
		return nullpos;
	}

	public AIFloat3 getAirTarget(Unit u, boolean defend){
		AIFloat3 origin = u.getPos();
		AIFloat3 target = null;
		float fthreat = getFriendlyThreat(origin);
		float cost = Float.MAX_VALUE;
		
		// first check defense targets
		if (defend) {
			for (DefenseTarget d : defenseTargets) {
				if (getEffectiveAAThreat(d.position) > fthreat){
					continue;
				}
				float tmpcost = distance(origin, d.position) - d.damage;
				
				if (tmpcost < cost) {
					cost = tmpcost;
					target = d.position;
				}
			}
		}
		
		// then look for enemy mexes to kill, and see which is cheaper
		for (MetalSpot m:graphManager.getEnemySpots()){
			if (getEffectiveAAThreat(m.getPos()) > fthreat || !pathfinder.isAssaultReachable(u, m.getPos(), fthreat)){
				continue;
			}
			float tmpcost = (distance(m.getPos(), origin)/4f) - 500f;
			
			if (tmpcost < cost){
				target = m.getPos();
				cost = tmpcost;
			}
		}
		
		
		// then look for enemy units to kill, and see if any are better targets
		for (Enemy e: targets.values()) {
			boolean allyTerritory = graphManager.isAllyTerritory(e.position);
			if (e.position != null && e.identified && getEffectiveAAThreat(e.position) < fthreat) {
				float tmpcost = distance(origin, e.position);
				if (!e.isAA){
					if (!e.isRaider && allyTerritory){
						tmpcost /= (1f + Math.max(0, getEffectiveThreat(e.position)));
					}
					tmpcost -= e.getDanger();
				}
				
				if (e.isMajorCancer) {
					tmpcost /= 4;
				} else if (e.isMinorCancer) {
					tmpcost /= 2;
				}
				
				tmpcost += 2000f * getAAThreat(e.position);
				
				if (tmpcost < cost) {
					cost = tmpcost;
					target = e.position;
				}
			}
		}
		
		
		if (target != null){
			TargetMarker tm = new TargetMarker(target, frame);
			targetMarkers.add(tm);
			return target;
		}
		
		// if no targets are available attack enemyshadowed metal spots until we find one
		for (MetalSpot m:graphManager.getUnownedSpots()){
			if (m.enemyShadowed){
				float tmpcost = distance(m.getPos(), origin);
				tmpcost /= 1f + getThreat(m.getPos());
				tmpcost /= (frame - m.getLastSeen())/600;
				
				if (tmpcost < cost) {
					target = m.getPos();
					cost = tmpcost;
				}
			}
		}
		if (target != null) {
			TargetMarker tm = new TargetMarker(target, frame);
			targetMarkers.add(tm);
			return target;
		}
		return nullpos;
	}
	
	public AIFloat3 getKrowTarget(AIFloat3 origin){
		AIFloat3 target = null;
		float fthreat = getFriendlyThreat(origin);
		float cost = Float.MAX_VALUE;
		
		// then look for enemy mexes to kill, and see which is cheaper
		List<MetalSpot> ms = graphManager.getEnemySpots();
		if(ms.size() > 0){
			for (MetalSpot m:ms){
				float tmpcost = distance(m.getPos(), origin);
				tmpcost += 2000f * getAAThreat(m.getPos());
				
				if (tmpcost < cost && getEffectiveAAThreat(m.getPos()) < fthreat){
					target = m.getPos();
					cost = tmpcost;
				}
			}
		}
		
		// then look for enemy units to kill, and see if any are better targets
		for (Enemy e:targets.values()) {
			if (e.position != null && e.identified && !e.ud.isAbleToFly()) {
				float tmpcost = distance(origin, e.position) - (!e.isAA ? 10f * Math.min(e.ud.getCost(m), 1500f) : e.ud.getCost(m));
				tmpcost += 10000f * getAAThreat(e.position);
				
				if (tmpcost < cost) {
					cost = tmpcost;
					target = e.position;
				}
			}
		}
		
		
		
		if (target != null){
			TargetMarker tm = new TargetMarker(target, frame);
			targetMarkers.add(tm);
			return target;
		}
		
		// if no targets are available attack enemyshadowed metal spots until we find one
		ms = graphManager.getUnownedSpots();
		for (MetalSpot m:ms){
			if (m.enemyShadowed){
				float tmpcost = distance(m.getPos(), origin);
				tmpcost /= 1+getThreat(m.getPos());
				tmpcost /= (frame-m.getLastSeen())/600;
				
				if (tmpcost < cost) {
					target = m.getPos();
					cost = tmpcost;
				}
			}
		}
		if (target != null) {
			TargetMarker tm = new TargetMarker(target, frame);
			targetMarkers.add(tm);
			return target;
		}
		return nullpos;
	}
	
	public AIFloat3 getBomberTarget(AIFloat3 origin, boolean defend){
		AIFloat3 target = null;
		float cost = Float.MAX_VALUE;
		
		// first check defense targets
		if (defend) {
			for (DefenseTarget d : defenseTargets) {
				float tmpcost = (distance(origin, d.position) - d.damage)/1+(2 * Math.max(0f, getThreat(d.position) - getAAThreat(d.position)));
				
				if (tmpcost < cost) {
					cost = tmpcost;
					target = d.position;
				}
				
			}
		}
		
		// then look for enemy mexes to kill, and see which is cheaper
		List<MetalSpot> ms = graphManager.getEnemySpots();
		if(ms.size() > 0){
			for (MetalSpot m:ms){
				float tmpcost = distance(m.getPos(), origin)/(2 - Math.min(getAAThreat(m.getPos()), 1f));
				
				if (tmpcost < cost){
					target = m.getPos();
					cost = tmpcost;
				}
			}
		}
		
		// then look for enemy units to kill, and see if any are better targets
		if(targets.size() > 0){
			Iterator<Enemy> enemies = targets.values().iterator();
			while(enemies.hasNext()){
				Enemy e = enemies.next();
				if (e.position != null && e.identified) {
					float tmpcost = distance(origin, e.position) - e.getDanger();
					
					if (e.isMajorCancer){
						tmpcost /= 4;
						tmpcost -= 1000;
					}else if (e.isMinorCancer){
						tmpcost /= 2;
						tmpcost -= 500;
					}
					
					tmpcost += 2000f * getAAThreat(e.position);
					
					if (tmpcost < cost) {
						cost = tmpcost;
						target = e.position;
					}
				}
			}
		}
		
		if (target != null){
			TargetMarker tm = new TargetMarker(target, frame);
			targetMarkers.add(tm);
			return target;
		}
		
		// if no targets are available attack enemyshadowed metal spots until we find one
		ms = graphManager.getUnownedSpots();
		for (MetalSpot m:ms){
			if (m.enemyShadowed){
				float tmpcost = distance(m.getPos(), origin);
				tmpcost /= 1+getThreat(m.getPos());
				tmpcost /= (frame-m.getLastSeen())/600;
				
				if (tmpcost < cost) {
					target = m.getPos();
					cost = tmpcost;
				}
			}
		}
		if (target != null) {
			TargetMarker tm = new TargetMarker(target, frame);
			targetMarkers.add(tm);
			return target;
		}
		return nullpos;
	}
	
	public Enemy getUltiTarget(AIFloat3 origin){
		Enemy target = null;
		float cost = Float.MAX_VALUE;
		
		for (Enemy e:targets.values()){
			if (!e.identified || e.ud == null || e.ud.isAbleToFly() || e.ud.getCost(m) < 600f || e.ud.getHealth() < 1000f || !graphManager.isAllyTerritory(e.position) || getPorcThreat(e.position) > 0){
				continue;
			}
			
			float tmpcost = distance(origin, e.position) - e.getDanger();
			if (tmpcost < cost){
				target = e;
				cost = tmpcost;
			}
		}
		return target;
	}

	public AIFloat3 getAATarget(Raider r){
		AIFloat3 origin = r.getPos();
		AIFloat3 target = null;
		float range = r.getUnit().getMaxRange();
		float cost = Float.MAX_VALUE;

		// first check defense targets

		// check for defense targets first
		for (DefenseTarget d : airDefenseTargets) {
			if (getTotalFriendlyThreat(d.position) == 0 || getPorcThreat(getDirectionalPoint(d.position, origin, range)) > 0){
				continue;
			}
			float tmpcost = distance(origin, d.position) - d.damage;

			if (tmpcost < cost) {
				cost = tmpcost;
				target = d.position;
			}
		}

		// then look for enemy air units to kill, and see if any are better targets
		for (Enemy e : targets.values()) {
			if (!e.identified
					|| !e.ud.isAbleToFly()
					|| getEffectiveThreat(getDirectionalPoint(e.position, origin, range)) > 0
				|| getPorcThreat(e.position) > 0){
				continue;
			}
			float tmpcost = distance(origin, e.position) - e.getDanger();

			if (tmpcost < cost) {
				cost = tmpcost;
				target = e.position;
			}
		}

		if (target != null){
			TargetMarker tm = new TargetMarker(target, frame);
			targetMarkers.add(tm);
			return target;
		}
		
		if (target == null){
			try {
				target = graphManager.getClosestAirHaven(graphManager.getClosestFrontLineSpot(graphManager.getEnemyCenter()).getPos());
			}catch (Exception e){
				// null pointer guard
			}
		}

		if (target != null) {
			TargetMarker tm = new TargetMarker(target, frame);
			targetMarkers.add(tm);
			return target;
		}
		return nullpos;
	}
	
	public AIFloat3 getMerlinTarget(Unit u){
		float maxRange = u.getMaxRange();
		float cost = Float.MAX_VALUE;
		AIFloat3 target = nullpos;
		for (Enemy e:targets.values()){
			if (!e.identified || e.ud == null || e.ud.isAbleToFly() || e.isRaider
			|| !graphManager.isAllyTerritory(getDirectionalPoint(e.position, u.getPos(), maxRange))) continue;
			float tmpcost = distance(e.position, u.getPos());
			tmpcost += 250f * e.ud.getSpeed();
			tmpcost -= e.ud.getCost(m) * getScytheThreat(e.position);
			
			if (tmpcost < cost){
				cost = tmpcost;
				target = e.position;
			}
		}
		return target;
	}

	public AIFloat3 getBerthaTarget(AIFloat3 origin){
		AIFloat3 target = null;
		float cost = Float.MIN_VALUE;
		float maxrange = callback.getUnitDefByName("staticheavyarty").getMaxWeaponRange();

		// for berthas the more expensive the target the better
		for (Enemy e : targets.values()) {
			if (!e.identified || e.position == null || e.position.equals(nullpos) || e.isNanoSpam || distance(e.position, origin) > maxrange || e.ud.isAbleToFly() || (e.ud.getName().equals("staticantinuke") && !hasNuke)
				|| (e.ud.getSpeed() > 0 && e.ud.getBuildSpeed() > 8f)){
				// berthas can't target things outside their range, and are not good vs air.
				// we also ignore antinuke unless we actually have nukes.
				continue;
			}

			float tmpcost = (e.ud.getCost(m) * getScytheThreat(e.position)) - ((frame - e.lastSeen)/2f);
			if (e.ud.getName().contains("factory") || e.ud.getName().contains("hub") || e.ud.getName().contains("felon")){
				tmpcost += 800;
				if (e.ud.getName().contains("hub")){
					tmpcost *= 2;
				}
			}


			if (tmpcost > cost) {
				cost = tmpcost;
				target = e.position;
			}
		}

		if (target != null){
			TargetMarker tm = new TargetMarker(target, frame);
			targetMarkers.add(tm);
			return target;
		}

		return null;
	}
	
	public AIFloat3 getSuperWepTarget(Unit u, boolean cook){
		if (frame - lastValueRedrawFrame > 350) {
			paintValueMap(u.getDef().getName());
			lastValueRedrawFrame = frame;
		}
		AIFloat3 origin = u.getPos();
		float range = u.getMaxRange();
		AIFloat3 target = null;
		float cost = Float.MIN_VALUE;
		
		// for berthas the more expensive the target the better
		for (Enemy e : targets.values()) {
			if (!e.identified || e.position == null || e.position.equals(nullpos) || distance(e.position, origin) > range || e.ud.isAbleToFly()
					|| graphManager.isAllyTerritory(e.position)){
				// superweapons should avoid teamkilling.
				continue;
			}
			float tmpcost = 0;
			
			tmpcost = getValue(e.position);
			
			if (tmpcost > cost) {
				cost = tmpcost;
				target = e.position;
			}
		}
		
		if (target != null){
			TargetMarker tm = new TargetMarker(target, frame);
			targetMarkers.add(tm);
			if (cook) {
				cook();
			}
			return target;
		}
		
		return null;
	}
	
	void cook(){
		double rand = Math.random();
		if (rand > 0.8){
			ai.say("Nice knowing you.");
		}else if (rand > 0.6){
			ai.say("It's cooking time!");
		}else if (rand > 0.4){
			ai.say("SHINY!");
		}else if (rand > 0.2){
			ai.say("R.I.P.");
		}else{
			ai.say("Get rekt.");
		}
	}
	
	public Enemy getClosestEnemyPorc(AIFloat3 position){
		Enemy best = null;
		float dist = Float.MAX_VALUE;
		for (Enemy e:enemyPorcs.values()){
			if (!e.identified || e.ud == null || !e.isStatic || e.getDanger() == 0 || e.isAA || e.isNanoSpam || e.position == null){
				continue;
			}
			float tmpdist = distance(position, e.position) + e.ud.getMaxWeaponRange();
			if (tmpdist < dist){
				best = e;
				dist = tmpdist;
			}
		}
		return best;
	}

	public AIFloat3 getRallyPoint(AIFloat3 pos){
		AIFloat3 target = null;
		if (pos == null){
			pos = graphManager.getAllyCenter();
		}
		if (pos == null){
			List<Unit> units = callback.getFriendlyUnits();
			for (Unit u: units){
				pos = u.getPos();
				break;
			}
		}

		float cost = Float.MAX_VALUE;
		float fthreat = getFriendlyThreat(pos);

		// check for defense targets first
		for (DefenseTarget d : defenseTargets) {
			if (getPorcThreat(d.position) > 0 || getTacticalThreat(d.position) > availableMobileThreat){
				continue;
			}
			float tmpcost = distance(pos, d.position) - d.damage;

			if (tmpcost < cost) {
				cost = tmpcost;
				target = d.position;
			}
		}

		// then check for nearby enemies
		for (Enemy e : targets.values()) {
			boolean allyTerritory = graphManager.isAllyTerritory(e.position);
			float ethreat = getEffectiveThreat(e.position);
			float pthreat = getPorcThreat(e.position);
			if ((ethreat > fthreat && (!allyTerritory || pthreat > 0))
					  || getTacticalThreat(e.position) > availableMobileThreat
					|| (e.identified && e.ud.isAbleToFly())
					  || ((!e.identified || e.isRaider) && distance(pos, e.position) > 800f)
					|| (!allyTerritory || pthreat > 0)){
				continue;
			}
			float tmpcost = distance(pos, e.position);
			if (!e.isAA){
				if (e.identified && !e.isRaider){
					tmpcost /= 2;
					tmpcost -= e.getDanger() * (1f + getThreat(e.position));
				}else {
					tmpcost -= e.getDanger();
				}
			}

			if (tmpcost < cost) {
				cost = tmpcost;
				target = e.position;
			}
		}

		if (target != null){
			return target;
		}

		//if there aren't any, then rally near the front line.
		AIFloat3 position;
		position = getWorkerEscort(pos);
		if (position != null) return position;

		try {
			position = graphManager.getClosestFrontLineSpot(pos).getPos();
		}catch (Exception e){}
		if (position != null) {
			return position;
		}
		
		position = graphManager.getClosestLeadingLink(pos);
		if (position != null) {
			return position;
		}
		
		//otherwise rally near ally center.
		position = graphManager.getClosestHaven(graphManager.getAllyCenter());

		if (position != null) {
			return position;
		}
		
		if (graphManager.getAllyCenter() != null) {
			return graphManager.getAllyCenter();
		}
		
		return graphManager.getClosestSpot(pos).getPos();
	}
	
	public AIFloat3 getAirRallyPoint(AIFloat3 pos){
		AIFloat3 target = null;
		if (pos == null){
			pos = graphManager.getAllyCenter();
		}
		if (pos == null){
			List<Unit> units = callback.getFriendlyUnits();
			for (Unit u: units){
				pos = u.getPos();
				break;
			}
		}
		
		float cost = Float.MAX_VALUE;
		
		// check for defense targets first
		for (DefenseTarget d : defenseTargets) {
			if (getEffectiveAAThreat(d.position) > getFriendlyThreat(pos) || getPorcThreat(d.position) > 0){
				continue;
			}
			float tmpcost = distance(pos, d.position) - d.damage;
			tmpcost += 2000f * getAAThreat(d.position);
			
			if (tmpcost < cost) {
				cost = tmpcost;
				target = d.position;
			}
		}
		
		// then check for nearby enemies
		for (Enemy e : targets.values()) {
			if (getEffectiveAAThreat(e.position) > getFriendlyThreat(pos)
					    || !graphManager.isAllyTerritory(e.position)
				    || getPorcThreat(e.position) > 0){
				continue;
			}
			float tmpcost = distance(pos, e.position);
			if (!e.isAA){
				tmpcost -= e.getDanger() * (1f + Math.max(0, getEffectiveThreat(e.position)));
			}
			tmpcost += 2000f * getAAThreat(e.position);
			
			if (e.isMajorCancer){
				tmpcost /= 4;
				tmpcost -= 1000;
			}else if (e.isMinorCancer){
				tmpcost /= 2;
				tmpcost -= 500;
			}
			
			if (tmpcost < cost) {
				cost = tmpcost;
				target = e.position;
			}
		}
		
		if (target != null){
			return target;
		}
		
		//if there aren't any, then rally near the front line.
		AIFloat3 position = getWorkerEscort(pos);
		if (position != null) return position;
		
		try {
			position = graphManager.getClosestFrontLineSpot(pos).getPos();
		}catch (Exception e){}
		if (position != null) {
			position = graphManager.getClosestAirHaven(position);
			return position;
		}
		
		if (position != null) {
			return position;
		}
		
		position = graphManager.getClosestAirLeadingLink(pos);
		if (position != null) {
			return position;
		}
		
		if (graphManager.getAllyCenter() != null) {
			return graphManager.getAllyCenter();
		}
		
		return graphManager.getClosestSpot(pos).getPos();
	}

	public AIFloat3 getRaiderRally(AIFloat3 pos){
		AIFloat3 target = null;
		if (pos == null){
			pos = graphManager.getAllyCenter();
		}
		if (pos == null){
			List<Unit> units = callback.getFriendlyUnits();
			for (Unit u: units){
				pos = u.getPos();
				break;
			}
		}

		float cost = Float.MAX_VALUE;

		// check for defense targets first
		for (DefenseTarget d : defenseTargets) {
			if ((getEffectiveThreat(d.position) > getFriendlyThreat(pos) || getPorcThreat(d.position) > 0 || getRiotThreat(d.position) > 0) && !squadHandler.fighters.isEmpty()){
				continue;
			}
			float tmpcost = distance(pos, d.position) - d.damage;

			if (tmpcost < cost) {
				cost = tmpcost;
				target = d.position;
			}
		}

		// then check for nearby enemies
		for (Enemy e : targets.values()) {
			if (((getEffectiveThreat(e.position) > getFriendlyThreat(pos)
					|| (e.isRiot || getRiotThreat(e.position) > 0)
					|| (e.identified && e.ud.isAbleToFly())) && !squadHandler.fighters.isEmpty())
					|| (!graphManager.isAllyTerritory(e.position) || getPorcThreat(e.position) > 0)){
				continue;
			}
			float tmpcost = distance(pos, e.position) - e.getDanger();

			if (e.isMajorCancer){
				tmpcost /= 4;
				tmpcost -= 1000;
			}else if (e.isMinorCancer || (e.isAA && !e.ud.getName().equals("turretaalaser"))){
				tmpcost /= 2;
				tmpcost -= 500;
			}

			if (tmpcost < cost) {
				cost = tmpcost;
				target = e.position;
			}
		}

		if (target != null){
			return target;
		}
		
		AIFloat3 position = null;
		position = getWorkerEscort(pos);
		if (position != null) return position;

		try {
			position = graphManager.getClosestFrontLineSpot(pos).getPos();
		}catch (Exception e){}
		if (position != null) {
			return position;
		}
		
		//if there aren't any, then rally near the front line.
		position = graphManager.getClosestLeadingLink(pos);
		if (position != null) {
			return position;
		}
		
		//otherwise rally near ally center.
		position = graphManager.getClosestHaven(graphManager.getAllyCenter());
		
		if (position != null) {
			return position;
		}
		
		if (graphManager.getAllyCenter() != null) {
			return graphManager.getAllyCenter();
		}
		
		return graphManager.getClosestSpot(pos).getPos();
	}

	private AIFloat3 getWorkerEscort(AIFloat3 pos){
		AIFloat3 target = null;
		float mindist = Float.MAX_VALUE;
		Worker nearestFac = ai.ecoManager.getNearestFac(pos);
		AIFloat3 facpos = (nearestFac != null) ? nearestFac.getPos() : pos;
		AIFloat3 enemyCenter = !graphManager.getEnemyCenter().equals(nullpos) ? graphManager.getEnemyCenter() : pos;
		for (Worker w: ai.ecoManager.workers.values()){
			if (getEffectiveThreat(w.getPos()) > getFriendlyThreat(pos)) continue;
			float dist = distance(pos, w.getPos()) - distance(w.getPos(), facpos) + distance(w.getPos(), enemyCenter) + (1000f * (getTotalFriendlyThreat(w.getPos()) - getEffectiveThreat(w.getPos())));
			if (dist < mindist){
				target = w.getPos();
				mindist = dist;
			}
		}
		return callback.getMap().findClosestBuildSite(callback.getUnitDefByName("mahlazer"), getAngularPoint(target, graphManager.getMapCenter(), 250f), 600f, 3, 0);
	}

	public Collection<Enemy> getTargets(){
		return targets.values();
	}
}