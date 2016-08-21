package zkgbai.military;

import java.util.*;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.*;

import zkgbai.Module;
import zkgbai.ZKGraphBasedAI;
import zkgbai.graph.GraphManager;
import zkgbai.graph.MetalSpot;
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
	
	java.util.Map<Integer,Enemy> targets;
	List<DefenseTarget> defenseTargets;

	public java.util.Map<Integer, Fighter> AAs;

	RadarIdentifier radarID;

	ArrayGraphics threatGraphics;
	ArrayGraphics allyThreatGraphics;
	ArrayGraphics allyPorcGraphics;
	ArrayGraphics aaThreatGraphics;
	ArrayGraphics aaPorcGraphics;
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
	int lastAirDefenseFrame = 0;

	public float enemyPorcValue = 0f;
	public int slasherSpam = 0;

	static int CMD_DONT_FIRE_AT_RADAR = 38372;
	static int CMD_AIR_STRAFE = 39381;
	static int CMD_AP_FLY_STATE = 34569;
	static int CMD_UNIT_AI = 36214;
	
	@Override
	public String getModuleName() {
		return "MilitaryManager";
	}
	
	public MilitaryManager(ZKGraphBasedAI ai){
		this.ai = ai;
		this.callback = ai.getCallback();
		this.targets = new HashMap<Integer,Enemy>();
		this.defenseTargets = new ArrayList<DefenseTarget>();

		this.AAs = new HashMap<Integer, Fighter>();

		this.nano = callback.getUnitDefByName("armnanotc").getUnitDefId();
		this.unitTypes = UnitClasses.getInstance();
		this.m = callback.getResourceByName("Metal");
		
		targetMarkers = new ArrayList<TargetMarker>();
		width = ai.getCallback().getMap().getWidth()/4;
		height = ai.getCallback().getMap().getHeight()/4;

		this.threatGraphics = new ArrayGraphics(width, height);
		this.allyThreatGraphics = new ArrayGraphics(width, height);
		this.allyPorcGraphics = new ArrayGraphics(width, height);
		this.aaThreatGraphics = new ArrayGraphics(width, height);
		this.aaPorcGraphics = new ArrayGraphics(width, height);
		allyPorcGraphics.clear();
		aaPorcGraphics.clear();
		
		try{
			radarID = new RadarIdentifier(ai.getCallback());
		}catch(Exception e){
			ai.printException(e);
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

		return 0;
	}
	
	private void paintThreatMap(){
		threatGraphics.clear();
		allyThreatGraphics.clear();
		aaThreatGraphics.clear();

		// paint allythreat in a separate thread, since it's independent of enemy threat.
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				// paint allythreat for raiders
				for (Raider r : raiderHandler.soloRaiders) {
					int power = (int) ((r.getUnit().getPower() + r.getUnit().getMaxHealth()) / 20);
					AIFloat3 pos = r.getPos();
					int x = (int) pos.x / 32;
					int y = (int) pos.z / 32;
					int rad = 13;
					allyThreatGraphics.paintCircle(x, y, rad, power);
				}

				for (Raider r : raiderHandler.smallRaiders.values()) {
					int power = (int) ((r.getUnit().getPower() + r.getUnit().getMaxHealth()) / 20);
					AIFloat3 pos = r.getPos();
					int x = (int) pos.x / 32;
					int y = (int) pos.z / 32;
					int rad = 13;
					allyThreatGraphics.paintCircle(x, y, rad, power);
				}

				for (Raider r : raiderHandler.mediumRaiders.values()) {
					int power = (int) ((r.getUnit().getPower() + r.getUnit().getMaxHealth()) / 20);
					AIFloat3 pos = r.getPos();
					int x = (int) pos.x / 32;
					int y = (int) pos.z / 32;
					int rad = 13;
					allyThreatGraphics.paintCircle(x, y, rad, power);
				}

				// paint allythreat for fighters
				for (Fighter f : squadHandler.fighters.values()) {
					int power = (int) ((f.getUnit().getPower() + f.getUnit().getMaxHealth()) / 8);
					AIFloat3 pos = f.getPos();
					int x = (int) pos.x / 32;
					int y = (int) pos.z / 32;
					int rad = 40;

					allyThreatGraphics.paintCircle(x, y, rad, power);
				}

				// paint allythreat for striders
				for (Strider s : miscHandler.striders.values()) {
					int power = (int) ((s.getUnit().getPower() + s.getUnit().getMaxHealth()) / 5);
					AIFloat3 pos = s.getPos();
					int x = (int) pos.x / 32;
					int y = (int) pos.z / 32;
					int rad = 40;

					allyThreatGraphics.paintCircle(x, y, rad, power);
				}
			}
		});
		thread.start();

		// Note: allythreat for porc is painted separately.

		for(Enemy t:targets.values()){
			int effectivePower = (int) t.getDanger();

			AIFloat3 position = t.position;

			if (position != null && t.ud != null
					&& effectivePower > 0
					&& !unitTypes.planes.contains(t.ud.getName())
					&& !t.unit.isBeingBuilt()) {
				int x = (int) (position.x / 32);
				int y = (int) (position.z / 32);
				int r = (int) ((t.threatRadius) / 32);

				if (!t.isStatic) {
					// paint enemy threat for mobiles
					if (t.isAA){
						aaThreatGraphics.paintCircle(x, y, (int) (r*1.5f), effectivePower);
					}else{
						threatGraphics.paintCircle(x, y, (int) (r*1.5f), effectivePower);
					}
				}else if (!t.isAA){
					// for statics
					threatGraphics.paintCircle(x, y, (int) (r*1.25f), effectivePower);
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

		try {
			thread.join();
		}catch (Exception e){
			ai.printException(e);
			System.exit(-1);
		}
	}
	
	public float getThreat(AIFloat3 position){
		int x = (int) (position.x/32);
		int y = (int) (position.z/32);

		return (threatGraphics.getValue(x, y))/500f;
	}

	public float getEffectiveThreat(AIFloat3 position){
		int x = (int) (position.x/32);
		int y = (int) (position.z/32);

		return ((threatGraphics.getValue(x, y)) - (allyThreatGraphics.getValue(x, y) + allyPorcGraphics.getValue(x, y)))/500f;
	}

	public float getFriendlyThreat(AIFloat3 position){
		int x = (int) (position.x/32);
		int y = (int) (position.z/32);

		return allyThreatGraphics.getValue(x, y)/500f;
	}

	public float getAAThreat(AIFloat3 position){
		int x = (int) (position.x/32);
		int y = (int) (position.z/32);

		return (aaThreatGraphics.getValue(x, y) + aaPorcGraphics.getValue(x, y))/500f;
	}

	public float getEffectiveAAThreat(AIFloat3 position){
		int x = (int) (position.x/32);
		int y = (int) (position.z/32);

		return ((aaThreatGraphics.getValue(x, y) + aaPorcGraphics.getValue(x, y)) - (allyThreatGraphics.getValue(x, y) + allyPorcGraphics.getValue(x, y)))/500f;
	}
    
    
	public AIFloat3 getTarget(AIFloat3 origin, boolean defend){
    	AIFloat3 target = null;
		float fthreat = getFriendlyThreat(origin);
    	float cost = Float.MAX_VALUE;

		// first check defense targets
		if (defend) {
			// check for defense targets first
			for (DefenseTarget d : defenseTargets) {
				if (getThreat(d.position) > getFriendlyThreat(origin)){
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
		List<MetalSpot> ms = graphManager.getEnemySpots();
		for (MetalSpot m:ms){
			if (getThreat(m.getPos()) > getFriendlyThreat(origin)){
				continue;
			}
			float tmpcost = (distance(m.getPos(), origin)/4) - 500;
    			
			if (tmpcost < cost){
				target = m.getPos();
				cost = tmpcost;
			}
		}


		// then look for enemy units to kill, and see if any are better targets
		// then check for nearby enemies that aren't raiders.
		for (Enemy e : targets.values()) {
			if (getThreat(e.position) > getFriendlyThreat(origin)
					|| (e.identified && e.ud.isAbleToFly())
					|| (e.isRaider)){
				continue;
			}
			float tmpcost = distance(origin, e.position) - e.getDanger();

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

	public AIFloat3 getAirTarget(AIFloat3 origin, boolean defend){
		AIFloat3 target = null;
		float fthreat = getFriendlyThreat(origin);
		float cost = Float.MAX_VALUE;

		// first check defense targets
		if (defend) {
			for (DefenseTarget d : defenseTargets) {
				float tmpcost = (distance(origin, d.position) - d.damage)/1+(2 * getThreat(d.position));

				if (tmpcost < cost && getEffectiveAAThreat(d.position) < fthreat) {
					cost = tmpcost;
					target = d.position;
				}

			}
		}

		// then look for enemy mexes to kill, and see which is cheaper
		List<MetalSpot> ms = graphManager.getEnemySpots();
		if(ms.size() > 0){
			for (MetalSpot m:ms){
				float tmpcost = distance(m.getPos(), origin)/2;

				if (tmpcost < cost && getEffectiveAAThreat(m.getPos()) < fthreat){
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

					if (graphManager.isAllyTerritory(e.position)){
						tmpcost /= 4;
						tmpcost -= 500;
					}

					if (tmpcost < cost && getEffectiveAAThreat(e.position) < fthreat) {
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

		// check for defense targets first
		for (DefenseTarget d : defenseTargets) {
			if (getThreat(d.position) > getFriendlyThreat(pos)){
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
			if (getThreat(e.position) > getFriendlyThreat(pos)
					|| (e.identified && e.ud.isAbleToFly())
					|| (!graphManager.isAllyTerritory(e.position) || graphManager.isEnemyTerritory(e.position))){
				continue;
			}
			float tmpcost = distance(pos, e.position) - e.getDanger();

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
		AIFloat3 position = null;
		try {
			position = graphManager.getClosestFrontLineSpot(pos).getPos();
		}catch (Exception e){}
		if (position != null) {
			return position;
		}else{
			//otherwise rally near ally center.
			position = graphManager.getClosestHaven(graphManager.getAllyCenter());
		}

		return position;
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
			if (getThreat(d.position) > getFriendlyThreat(pos)){
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
			if (getThreat(e.position) > getFriendlyThreat(pos)
					|| e.isRiot
					|| (e.identified && e.ud.isAbleToFly())
					|| (!graphManager.isAllyTerritory(e.position) || graphManager.isEnemyTerritory(e.position))){
				continue;
			}
			float tmpcost = distance(pos, e.position) - e.getDanger();

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
		AIFloat3 position = null;
		try {
			position = graphManager.getClosestHaven(graphManager.getClosestFrontLineSpot(pos).getPos());
		}catch (Exception e){}
		if (position != null) {
			return position;
		}else{
			//otherwise rally near ally center.
			position = graphManager.getClosestHaven(graphManager.getAllyCenter());
		}

		return position;
	}

	public Collection<Enemy> getTargets(){
		return targets.values();
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
					int x = (int) (position.x / 32);
					int y = (int) (position.z / 32);
					int r = (int) ((t.threatRadius) / 32);

					aaPorcGraphics.unpaintCircle(x, y, (int) (r * 1.2f), effectivePower);
				}
			}
		}

		for (Enemy t: outdated){
			t.position = null; // needed for BomberTasks
			targets.remove(t.unitID);
		}

		List<DefenseTarget> expired = new ArrayList<DefenseTarget>();
		for (DefenseTarget d:defenseTargets){
			if (frame - d.frameIssued > 300){
				expired.add(d);
			}
		}
		defenseTargets.removeAll(expired);

		enemyPorcValue = 0f;
		slasherSpam = 0;
		// add up the value of heavy porc that the enemy has
		// and also check for slasher spam
		for (Enemy t: targets.values()){
			if (t.identified){
				if (t.ud.getName().equals("corhlt") || t.ud.getName().equals("armcrabe")) {
					enemyPorcValue += t.ud.getCost(m);
				}else if (t.ud.getName().equals("armanni") || t.ud.getName().equals("cordoom") || t.ud.getName().equals("corjamt")){
					enemyPorcValue += 1.5 * t.ud.getCost(m);
				}else if (t.ud.getName().equals("corrl") || t.ud.getName().equals("corllt")){
					enemyPorcValue += t.ud.getCost(m) * 2f;
				}else if (t.ud.getName().equals("cormist")){
					slasherSpam++;
				}
			}
		}
	}
    
    @Override
    public int update(int frame) {
    	this.frame = frame;

		if(frame%15 == 0) {
			updateTargets();
			paintThreatMap();
		}

		retreatHandler.update(frame);
		raiderHandler.update(frame);
		squadHandler.update(frame);
		miscHandler.update(frame);
		bomberHandler.update(frame);


        return 0; // signaling: OK
    }
	
    @Override
    public int enemyEnterLOS(Unit enemy) {
    	Resource metal = ai.getCallback().getResourceByName("Metal");

		if(targets.containsKey(enemy.getUnitId())){
    		Enemy e = targets.get(enemy.getUnitId()); 
    		e.visible = true;
			e.lastSeen = frame;
			if (!e.identified) {
				e.setIdentified();
				e.updateFromUnitDef(enemy.getDef(), enemy.getDef().getCost(metal));
			}

			// paint enemy threat for aa
			if (e.isStatic && e.isAA && !e.isPainted && !e.unit.isBeingBuilt()) {
				int effectivePower = (int) e.getDanger();
				AIFloat3 position = e.position;
				int x = (int) (position.x / 32);
				int y = (int) (position.z / 32);
				int r = (int) ((e.threatRadius) / 32);

				aaPorcGraphics.paintCircle(x, y, (int) (r*1.2f), effectivePower);
				e.isPainted = true;
			}
    	}
        return 0; // signaling: OK
    }

	@Override
	public int enemyFinished(Unit enemy) {
		if(targets.containsKey(enemy.getUnitId())){
			Enemy e = targets.get(enemy.getUnitId());
			// paint enemy threat for aa
			if (e.isStatic && e.isAA && !e.isPainted) {
				int effectivePower = (int) e.getDanger();
				AIFloat3 position = e.position;
				int x = (int) (position.x / 32);
				int y = (int) (position.z / 32);
				int r = (int) ((e.threatRadius) / 32);

				aaThreatGraphics.paintCircle(x, y, (int) (r*1.2f), effectivePower);
				e.isPainted = true;
			}
		}else{
			Resource metal = ai.getCallback().getResourceByName("Metal");
			Enemy e = new Enemy(enemy, enemy.getDef().getCost(metal));
			targets.put(enemy.getUnitId(),e);
			e.visible = true;
			e.setIdentified();
			e.lastSeen = frame;
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
    		if(enemy.getDef() != null){
	    		Enemy e = new Enemy(enemy, enemy.getDef().getCost(ai.getCallback().getResourceByName("Metal")));
	    		targets.put(enemy.getUnitId(),e);
	    		e.visible = true;
    			e.isRadarVisible = true;
				e.lastSeen = frame;
    		}else{
    			Enemy e = new Enemy(enemy, 50);
        		targets.put(enemy.getUnitId(),e);
    			e.isRadarVisible = true;
				e.lastSeen = frame;
    		}
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

			// Unpaint enemy threat for statics
			if (e.isStatic && e.isAA && e.isPainted) {
				int effectivePower = (int) e.getDanger();
				AIFloat3 position = e.position;
				int x = (int) (position.x / 32);
				int y = (int) (position.z / 32);
				int r = (int) ((e.threatRadius) / 32);

				aaPorcGraphics.unpaintCircle(x, y, (int) (r * 1.2f), effectivePower);
			}
			e.position = null;

        	targets.remove(unit.getUnitId());
        }	    
        return 0; // signaling: OK
    }
    
    @Override
    public int unitFinished(Unit unit) {
		String defName = unit.getDef().getName();

		// enable snipers and penetrators to shoot radar dots
		if (defName.equals("armsnipe") || defName.equals("armmanni")){
			ArrayList<Float> params = new ArrayList<>();
			params.add((float) 0);
			unit.executeCustomCommand(CMD_DONT_FIRE_AT_RADAR, params, (short) 0, frame+60);
			unit.setMoveState(1, (short) 0, frame + 10);
		}

		// disable air strafe for brawlers
		if (defName.equals("armbrawl")){
			ArrayList<Float> params = new ArrayList<>();
			params.add((float) 0);
			unit.executeCustomCommand(CMD_AIR_STRAFE, params, (short) 0, frame+30);
		}

		// set blastwings to land when idle
		if (defName.equals("blastwing")){
			unit.setIdleMode(1, (short) 0, frame+30);
		}

		if (unitTypes.planes.contains(defName)){
			unit.setIdleMode(0, (short) 0, frame+30);
		}

		// Activate outlaws
		if (defName.equals("cormak")){
			unit.setOn(true, (short) 0, frame+300);
		}

		if (unitTypes.striders.contains(defName)){
			Strider st = new Strider(unit, unit.getDef().getCost(m));
			unit.setMoveState(1, (short) 0, frame + 10);
			miscHandler.addStrider(st);
		}else if (unitTypes.smallRaiders.contains(defName)) {
			Raider r = new Raider(unit, unit.getDef().getCost(m));
			raiderHandler.addSmallRaider(r);
			AIFloat3 pos = graphManager.getAllyCenter();
			unit.fight(pos, (short) 0, frame + 300);
		}else if (unitTypes.mediumRaiders.contains(defName)) {
			Raider r = new Raider(unit, unit.getDef().getCost(m));
			raiderHandler.addMediumRaider(r);
			AIFloat3 pos = graphManager.getAllyCenter();
			unit.fight(pos, (short) 0, frame + 300);
		}else if (unitTypes.soloRaiders.contains(defName)) {
			Raider r = new Raider(unit, unit.getDef().getCost(m));
			raiderHandler.addSoloRaider(r);
		}else if (unitTypes.assaults.contains(defName)) {
			Fighter f = new Fighter(unit, unit.getDef().getCost(m));
			squadHandler.addAssault(f);
		}else if (unitTypes.shieldMobs.contains(defName)){
			Fighter f = new Fighter(unit, unit.getDef().getCost(m));
			squadHandler.addShieldMob(f);
		}else if (unitTypes.airMobs.contains(defName)){
			Fighter f = new Fighter(unit, unit.getDef().getCost(m));
			squadHandler.addAirMob(f);
		}else if(unitTypes.mobSupports.contains(defName)){
			Fighter f = new Fighter(unit, unit.getDef().getCost(m));
			miscHandler.addSupport(f);
    	}else if(unitTypes.loners.contains(defName)) {
			if (defName.equals("cormist") || defName.equals("armcrabe") || defName.equals("armmerl") || defName.equals("cormart") || defName.equals("armraven") || defName.equals("trem")){
				unit.setMoveState(0, (short) 0, frame + 10);
				ArrayList<Float> params = new ArrayList<>();
				params.add((float) 1);
				unit.executeCustomCommand(CMD_UNIT_AI, params, (short) 0, frame+30);
			}else{
				unit.setMoveState(2, (short) 0, frame + 10);
			}
			Fighter f = new Fighter(unit, unit.getDef().getCost(m));
			miscHandler.addLoner(f);
		}else if (unitTypes.AAs.contains(defName)){
			unit.setMoveState(2, (short) 0, frame + 10);
			Fighter f = new Fighter(unit, unit.getDef().getCost(m));
			AAs.put(f.id, f);
			AIFloat3 pos = graphManager.getAllyCenter();
			if (pos != null){
				unit.fight(pos, (short) 0, frame + 300);
			}
		}else if (unitTypes.sappers.contains(defName)){
			unit.setMoveState(2, (short) 0, frame + 10);
			unit.setFireState(2, (short) 0, frame + 10);
			ArrayList<Float> params = new ArrayList<>();
			params.add((float) 0);
			unit.executeCustomCommand(CMD_UNIT_AI, params, (short) 0, frame+30);
			unit.fight(getRadialPoint(graphManager.getAllyCenter(), 800f), (short) 0, frame+300);
			miscHandler.addSapper(unit);
		}else if (unitTypes.bombers.contains(defName)){
			unit.setMoveState(2, (short) 0, frame + 10);
			unit.setFireState(2, (short) 0, frame + 10);
			Fighter f = new Fighter(unit, unit.getDef().getCost(m));
			bomberHandler.addBomber(f);
		}
    	
    	if (unit.getMaxSpeed() > 0 && unit.getDef().getBuildOptions().size() == 0
				&& !unitTypes.shieldMobs.contains(defName) && !unitTypes.noRetreat.contains(defName)){
    		retreatHandler.addCoward(unit);
    	}

        return 0; // signaling: OK
    }

	@Override
	public int unitCreated(Unit unit, Unit builder){
		if (unit.getDef().getName().equals("cormex")){
			DefenseTarget dt = new DefenseTarget(unit.getPos(), -1000, frame);
			defenseTargets.add(dt);
		}

		// Paint ally threat for porc
		if (unit.getMaxSpeed() == 0 && unit.getDef().isAbleToAttack()){
			int power = (int) ((unit.getPower() + unit.getMaxHealth())/10);
			float radius = unit.getMaxRange();
			AIFloat3 pos = unit.getPos();
			int x = (int) pos.x/32;
			int y = (int) pos.z/32;
			int rad = (int) radius/32;

			allyPorcGraphics.paintCircle(x, y, rad, power);
		}
		return 0;
	}
    
    @Override
    public int unitDestroyed(Unit unit, Unit attacker) {
        retreatHandler.removeUnit(unit);
		raiderHandler.removeUnit(unit);
		squadHandler.removeUnit(unit);
		bomberHandler.removeUnit(unit);
		miscHandler.removeUnit(unit);

		if (AAs.containsKey(unit.getUnitId())){
			AAs.remove(unit.getUnitId());
		}

		if (unitTypes.sappers.contains(unit.getDef().getName())){
			// rally units to fight if a tick/roach etc dies.
			DefenseTarget dt = new DefenseTarget(unit.getPos(), 1000f, frame);
			defenseTargets.add(dt);
		}

		// create a defense task, if appropriate.
		if ((!unit.getDef().isAbleToAttack() || unit.getMaxSpeed() == 0 || distance(unit.getPos(), graphManager.getAllyCenter()) < distance(unit.getPos(), graphManager.getEnemyCenter()))
				&& frame - lastDefenseFrame > 150){
			lastDefenseFrame = frame;
			DefenseTarget dt = null;
			if (attacker != null){
				if (attacker.getPos() != null && attacker.getDef() != null && !attacker.getDef().isAbleToFly()) {
					dt = new DefenseTarget(attacker.getPos(), 1000, frame);
				}
			}
			if (dt == null){
				dt = new DefenseTarget(unit.getPos(), 1000, frame);
			}
			defenseTargets.add(dt);

			if (attacker != null) {
				if (attacker.getDef() != null) {
					if (attacker.getDef().isAbleToFly()) {
						for (Fighter f : AAs.values()) {
							if (f.getUnit().getHealth() > 0 && f.getUnit().getTeam() == ai.teamID) {
								f.fightTo(unit.getPos(), frame);
							}
						}
					}
				}
			}
		}

		// Unpaint ally threat for porc
		if (unit.getMaxSpeed() == 0 && unit.getDef().isAbleToAttack()){
			int power = (int) ((unit.getPower() + unit.getMaxHealth())/10);
			float radius = unit.getMaxRange();
			AIFloat3 pos = unit.getPos();
			int x = (int) pos.x/32;
			int y = (int) pos.z/32;
			int rad = (int) radius/32;

			allyPorcGraphics.unpaintCircle(x, y, rad, power);
		}

        return 0; // signaling: OK
    }
    
    @Override
    public int unitDamaged(Unit h, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzed) {
		// check if the damaged unit is on fire.
		boolean on_fire = false;

		if (h.getRulesParamFloat("on_fire", 0.0f) > 0){
			on_fire = true;
		}


		// retreat damaged mob units
		retreatHandler.checkUnit(h);

		// retreat scouting raiders so that they don't suicide into enemy raiders
		if (!retreatHandler.isRetreating(h) && !on_fire) {
			raiderHandler.avoidEnemies(h, attacker, dir);
		}


			// create a defense task, if appropriate.
			if ((h.getMaxSpeed() == 0 || !h.getDef().isAbleToAttack() || distance(h.getPos(), graphManager.getAllyCenter()) < distance(h.getPos(), graphManager.getEnemyCenter()) || (attacker == null || attacker.getPos() == null || attacker.getPos().equals(nullpos)))
					&& frame - lastDefenseFrame > 30 && !on_fire) {
				lastDefenseFrame = frame;
				DefenseTarget dt = null;

				// only create defense targets on unitDamaged if the attacker is invisible or out of los.
				if (attacker == null || attacker.getDef() == null) {
					float x = 800 * dir.x;
					float z = 800 * dir.z;
					AIFloat3 pos = h.getPos();
					AIFloat3 target = new AIFloat3();
					target.x = pos.x + x;
					target.z = pos.z + z;
					dt = new DefenseTarget(target, 2000f, frame);
				}else if (h.getDef().isBuilder()){
					dt = new DefenseTarget(h.getPos(), 2000f, frame);
				}

				// don't create defense targets vs air units.
				boolean isAirEnemy = false;
				if (attacker != null && attacker.getDef() != null && attacker.getDef().isAbleToFly()){
					isAirEnemy = true;
				}

				if (!isAirEnemy && dt != null) {
					defenseTargets.add(dt);
				}
			}


		// Mobilize anti-air units vs enemy air
		if (attacker != null && attacker.getDef() != null && attacker.getDef().isAbleToFly() && frame - lastAirDefenseFrame > 300){
			lastAirDefenseFrame = frame;
			for (Fighter f : AAs.values()) {
				if (f.getUnit().getHealth() > 0 && f.getUnit().getTeam() == ai.teamID) {
					f.fightTo(h.getPos(), frame);
				}
			}
		}
		return 0;
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
			unitFinished(unit);
			retreatHandler.checkUnit(unit);
		}
		return 0;
	}
}