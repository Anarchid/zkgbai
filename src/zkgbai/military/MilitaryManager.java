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
	
	java.util.Map<Integer,Enemy> targets = new HashMap<Integer,Enemy>();
	java.util.Map<Integer,Enemy> enemyPorcs = new HashMap<Integer,Enemy>();
	public List<DefenseTarget> defenseTargets;
	List<DefenseTarget> airDefenseTargets;

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

	public float enemyPorcValue = 0f;
	public float maxEnemyAirValue = 0f;
	public int slasherSpam = 0;
	public int enemyHeavyFactor = 0;
	public boolean enemyHasTrollCom = false;
	
	public boolean enemyHasAntiNuke = false;
	public boolean enemyHasNuke = false;
	boolean hasNuke = false;

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
		this.defenseTargets = new ArrayList<DefenseTarget>();
		this.airDefenseTargets = new ArrayList<DefenseTarget>();

		this.AAs = new HashMap<Integer, Raider>();

		this.nano = callback.getUnitDefByName("armnanotc").getUnitDefId();
		this.unitTypes = UnitClasses.getInstance();
		this.m = callback.getResourceByName("Metal");
		
		targetMarkers = new ArrayList<TargetMarker>();
		width = ai.getCallback().getMap().getWidth()/4;
		height = ai.getCallback().getMap().getHeight()/4;

		this.threatGraphics = new ArrayGraphics(width, height);
		this.enemyPorcGraphics = new ArrayGraphics(width, height);
		this.allyThreatGraphics = new ArrayGraphics(width, height);
		this.allyPorcGraphics = new ArrayGraphics(width, height);
		this.aaThreatGraphics = new ArrayGraphics(width, height);
		this.aaPorcGraphics = new ArrayGraphics(width, height);
		this.valueGraphics = new ArrayGraphics(width, height);
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
	
	private void paintValueMap(String wepName){
		valueGraphics.clear();
		int r, rr;
		if (wepName.equals("corsilo")) {
			r = 28; // general superwep kill radius
			rr = 40;
		}else{
			r = 20;
			rr = 25;
		}
		for(Enemy t:targets.values()){
			AIFloat3 position = t.position;
			if (position != null && t.identified && t.ud != null && !t.isNanoSpam && !t.ud.isAbleToFly() && !t.ud.getName().equals("corrazor") && !t.ud.getName().equals("armpb")){
				int x = (int) (position.x / 32);
				int y = (int) (position.z / 32);
				int value = (int) Math.min(t.ud.getCost(m)/10f, 750f);
				
				valueGraphics.paintCircle(x, y, r, value);
			}
		}
		
		for (Squad s:squadHandler.squads){
			if (!s.isDead()){
				AIFloat3 position = s.getPos();
				int x = (int) (position.x / 32);
				int y = (int) (position.z / 32);
				int value = (int) (s.metalValue/10f);
				
				valueGraphics.unpaintCircle(x, y, rr, value);
			}
		}
		
		for (Strider s:miscHandler.striders.values()){
			AIFloat3 position = s.getPos();
			int x = (int) (position.x / 32);
			int y = (int) (position.z / 32);
			int value = (int) Math.min(s.metalValue/10f, 750f);
			
			valueGraphics.unpaintCircle(x, y, rr, value);
		}
		
		if (squadHandler.nextShieldSquad != null && !squadHandler.nextShieldSquad.isDead()){
			AIFloat3 position = squadHandler.nextShieldSquad.getPos();
			int x = (int) (position.x / 32);
			int y = (int) (position.z / 32);
			int value = (int) ((squadHandler.nextShieldSquad.metalValue + squadHandler.nextShieldSquad.funnelValue)/10f);
			valueGraphics.unpaintCircle(x, y, r, value);
		}
	}
	
	private void paintThreatMap(){
		threatGraphics.clear();
		allyThreatGraphics.clear();
		aaThreatGraphics.clear();
		
		boolean updatePorc = false;
		if (frame % 300 == 0){
			enemyPorcGraphics.clear();
			updatePorc = true;
		}

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
					int power = (int) ((s.getUnit().getPower() + s.getUnit().getMaxHealth()) / 6);
					AIFloat3 pos = s.getPos();
					int x = (int) pos.x / 32;
					int y = (int) pos.z / 32;
					int rad = 40;

					allyThreatGraphics.paintCircle(x, y, rad, power);
				}
				
				// paint allythreat for krows
				for (Strider s : miscHandler.krows.values()) {
					int power = (int) ((s.getUnit().getPower() + s.getUnit().getMaxHealth()) / 6);
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
					&& (!unitTypes.planes.contains(t.ud.getName()))
					&& !t.unit.isBeingBuilt()
					&& !t.unit.isParalyzed()
					&& t.unit.getRulesParamFloat("disarmed", 0f) == 0) {
				int x = (int) (position.x / 32);
				int y = (int) (position.z / 32);
				int r = (int) ((t.threatRadius) / 32);

				if (t.ud.getName().equals("gushipsupport")){
					r *= 5;
				}

				if (!t.isStatic) {
					if (!t.ud.isAbleToFly()) {
						effectivePower = (int) ((float) effectivePower * Math.max(0, (1f - (((float) (frame - t.lastSeen)) / 900f))));
					}
					// paint enemy threat for mobiles
					if (t.isAA || t.isFlexAA){
						aaThreatGraphics.paintCircle(x, y, (int) (r*1.25f), effectivePower);
					}
					if (!t.isAA){
						threatGraphics.paintCircle(x, y, (int) (r*1.25f), effectivePower);
					}
				}else if (!t.isAA){
					// for statics
					threatGraphics.paintCircle(x, y, (int) (r*1.25f), effectivePower);
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
				ai.printException(e);
				System.exit(-1);
			}
		}
	}
	
	public float getThreat(AIFloat3 position){
		int x = (int) (position.x/32);
		int y = (int) (position.z/32);

		return (threatGraphics.getValue(x, y))/500f;
	}
	
	public float getPorcThreat(AIFloat3 position){
		int x = (int) (position.x/32);
		int y = (int) (position.z/32);
		
		return (enemyPorcGraphics.getValue(x, y))/500f;
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
	
	public float getTotalFriendlyThreat(AIFloat3 position){
		int x = (int) (position.x/32);
		int y = (int) (position.z/32);
		
		return (allyThreatGraphics.getValue(x, y) + allyPorcGraphics.getValue(x, y))/500f;
	}

	public float getAAThreat(AIFloat3 position){
		int x = (int) (position.x/32);
		int y = (int) (position.z/32);

		return (aaThreatGraphics.getValue(x, y) + aaPorcGraphics.getValue(x, y))/500f;
	}

	public float getEffectiveAAThreat(AIFloat3 position){
		int x = (int) (position.x/32);
		int y = (int) (position.z/32);

		return Math.max(0, ((aaThreatGraphics.getValue(x, y) + aaPorcGraphics.getValue(x, y) + threatGraphics.getValue(x, y)) - (allyThreatGraphics.getValue(x, y) + allyPorcGraphics.getValue(x, y)))/500f);
	}
	
	public float getValue(AIFloat3 position){
		int x = (int) (position.x/32);
		int y = (int) (position.z/32);
		
		return (valueGraphics.getValue(x, y));
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
			float tmpcost = distance(origin, e.position);
			if (!e.isAA){
				tmpcost -= e.getDanger();
			}

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
				tmpcost += 2000f * getAAThreat(m.getPos());
				
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
				if (e.position != null && e.identified && getEffectiveAAThreat(e.position) < fthreat) {
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
		if(targets.size() > 0){
			Iterator<Enemy> enemies = targets.values().iterator();
			while(enemies.hasNext()){
				Enemy e = enemies.next();
				if (e.position != null && e.identified && !e.ud.isAbleToFly()) {
					float tmpcost = distance(origin, e.position) - (!e.isAA ? 10f * Math.min(e.ud.getCost(m), 1500f) : e.ud.getCost(m));
					tmpcost += 10000f * getAAThreat(e.position);
					
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
			if (!e.identified || e.ud == null || e.ud.isAbleToFly() || e.ud.getCost(m) < 700f || e.ud.getHealth() < 1000f || !graphManager.isAllyTerritory(e.position) || getPorcThreat(e.position) > 0){
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
					|| (!graphManager.isAllyTerritory(e.position) && getEffectiveThreat(getDirectionalPoint(e.position, origin, range)) > 0)
				|| getTotalFriendlyThreat(e.position) == 0
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
				target = graphManager.getClosestAirHaven(graphManager.getClosestFrontLineSpot(graphManager.getAllyCenter()).getPos());
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

	public AIFloat3 getBerthaTarget(AIFloat3 origin){
		AIFloat3 target = null;
		float cost = Float.MIN_VALUE;

		// for berthas the more expensive the target the better
		for (Enemy e : targets.values()) {
			if (!e.identified || e.position == null || e.position.equals(nullpos) || e.isNanoSpam || distance(e.position, origin) > 6200 || e.ud.isAbleToFly() || (e.ud.getName().equals("armamd") && !hasNuke)
				|| (e.ud.getSpeed() > 0 && e.ud.getBuildSpeed() > 8f)){
				// berthas can't target things outside their range, and are not good vs air.
				// we also ignore antinukes because kgb doesn't build nukes anyway.
				continue;
			}

			float tmpcost = e.ud.getCost(m) - ((frame - e.lastSeen)/2f);
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
			callback.getGame().sendTextMessage("/say <ZKGBAI> Nice knowing you.", 0);
		}else if (rand > 0.6){
			callback.getGame().sendTextMessage("/say <ZKGBAI> It's cooking time!", 0);
		}else if (rand > 0.4){
			callback.getGame().sendTextMessage("/say <ZKGBAI> SHINY!", 0);
		}else if (rand > 0.2){
			callback.getGame().sendTextMessage("/say <ZKGBAI> R.I.P.", 0);
		}else{
			callback.getGame().sendTextMessage("/say <ZKGBAI> Get rekt.", 0);
		}
	}
	
	public Enemy getClosestEnemyPorc(AIFloat3 position){
		Enemy best = null;
		float dist = Float.MAX_VALUE;
		for (Enemy e:enemyPorcs.values()){
			if (!e.identified || e.ud == null || !e.isStatic || e.getDanger() == 0 || e.isAA || e.isNanoSpam){
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

		// check for defense targets first
		for (DefenseTarget d : defenseTargets) {
			if (getThreat(d.position) > getFriendlyThreat(pos) || getPorcThreat(d.position) > 0){
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
					|| (!graphManager.isAllyTerritory(e.position) || getPorcThreat(e.position) > 0)){
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
			if (getAAThreat(e.position) > getFriendlyThreat(pos)
					    || !graphManager.isAllyTerritory(e.position)
				    || getPorcThreat(e.position) > 0){
				continue;
			}
			float tmpcost = distance(pos, e.position) - e.getDanger();
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
		AIFloat3 position = null;
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
			if (getThreat(d.position) > getFriendlyThreat(pos) || getPorcThreat(d.position) > 0){
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
					|| (!graphManager.isAllyTerritory(e.position) || getPorcThreat(e.position) > 0)){
				continue;
			}
			float tmpcost = distance(pos, e.position) - e.getDanger();

			if (e.isMajorCancer){
				tmpcost /= 4;
				tmpcost -= 1000;
			}else if (e.isMinorCancer || (e.isAA && !e.ud.getName().equals("corrazor"))){
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

		enemyPorcValue = 0f;
		slasherSpam = 0;
		// add up the value of heavy porc that the enemy has
		// and also check for slasher spam
		enemyHasAntiNuke = false;
		enemyHasNuke = false;
		float enemyAirValue = 0f;
		enemyHeavyFactor = 0;
		for (Enemy t: targets.values()){
			if (t.identified){
				String defName = t.ud.getName();
				if (defName.equals("corhlt") || defName.equals("armpb") || defName.equals("armdeva") || defName.equals("armcrabe")
				|| defName.equals("corrl") || defName.equals("corllt") || defName.equals("amphassault")) {
					enemyPorcValue += t.ud.getCost(m);
				}else if (defName.equals("armanni") || defName.equals("cordoom") || defName.equals("corjamt") || defName.equals("core_spectre")) {
					enemyPorcValue += 1.5f * t.ud.getCost(m);
				}else if (defName.equals("shieldfelon")){
					enemyPorcValue += 2f * t.ud.getCost(m);
				}else if (defName.equals("cormist")) {
					slasherSpam++;
				}else if (t.isRaider){
					slasherSpam--;
				}else if (defName.equals("armamd")){
					enemyHasAntiNuke = true;
				}else if (defName.equals("corsilo")){
					enemyHasNuke = true;
				}else if (defName.equals("armbanth")){
					enemyHeavyFactor++;
				}else if (defName.equals("armorco")){
					enemyHeavyFactor += 2;
				}else if (t.ud.isAbleToFly() && !t.isNanoSpam){
					if (defName.equals("attackdrone") || defName.equals("battledrone") || defName.equals("carrydrone")){
						enemyAirValue += 25f;
					}else {
						enemyAirValue += t.ud.getBuildTime();
					}
				}else if (t.unit.getRulesParamFloat("comm_level", 0f) > 3f){
					enemyHasTrollCom = true;
				}
			}
		}
		if (enemyAirValue > maxEnemyAirValue){
			maxEnemyAirValue = enemyAirValue;
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

		if (frame % 4 == 0 && !AAs.isEmpty()){
			boolean assigned = false;
			for (Raider r:AAs.values()){
				if (r.assigned || frame - r.lastTaskFrame < 15 || retreatHandler.isRetreating(r.getUnit())){
					continue;
				}
				r.raid(getAATarget(r), frame);
				r.assigned = true;
				assigned = true;
				break;
			}
			if (!assigned){
				for (Raider r:AAs.values()){
					r.assigned = false;
				}
			}
		}

        return 0; // signaling: OK
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
				int x = (int) (position.x / 32);
				int y = (int) (position.z / 32);
				int r = (int) ((e.threatRadius) / 32);

				aaPorcGraphics.paintCircle(x, y, (int) (r*1.2f), effectivePower);
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
				int x = (int) (position.x / 32);
				int y = (int) (position.z / 32);
				int r = (int) ((e.threatRadius) / 32);

				aaThreatGraphics.paintCircle(x, y, (int) (r*1.2f), effectivePower);
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
			if (e.identified && e.ud != null && (e.ud.getName().equals("cafus") || e.ud.getName().equals("amgeo")) && !e.isNanoSpam){
				callback.getGame().sendTextMessage("/say <ZKGBAI> SHINY!", 0);
			}

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
        	if (e.isPorc && !e.isNanoSpam){
        		enemyPorcs.remove(unit.getUnitId());
	        }
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
	
	    if (defName.equals("corsilo")){
		    hasNuke = true;
		    miscHandler.addNuke(unit);
	    }
	
	    if (defName.equals("zenith")){
		    miscHandler.addZenith(unit);
	    }
	
	    if (defName.equals("raveparty")){
		    miscHandler.addDRP(unit);
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

		if (defName.equals("fighter")){
			unit.setMoveState(0, (short) 0, frame + 10);
			miscHandler.addSwift(unit);
		}

		if (defName.equals("corvamp")){
			unit.setMoveState(1, (short) 0, frame + 10);
			Raider f = new Raider(unit, unit.getDef().getCost(m));
			hawks.put(f.id, f);
			if (hawks.size() > 4){
				AAs.putAll(hawks);
				hawks.clear();
			}
		}
		
		if (defName.equals("armcomdgun")){
			unit.setMoveState(0, (short) 0, frame + 10);
			unit.setFireState(1, (short) 0, frame + 10);
			Raider f = new Raider(unit, unit.getDef().getCost(m));
			miscHandler.addUlti(f);
		}
	
	    if (defName.equals("corcrw")){
		    unit.setMoveState(1, (short) 0, frame + 10);
		    Krow f = new Krow(unit, unit.getDef().getCost(m));
		    miscHandler.addKrow(f);
	    }

		if (unitTypes.striders.contains(defName)){
			Strider st = new Strider(unit, unit.getDef().getCost(m));
			unit.setMoveState(1, (short) 0, frame + 10);
			miscHandler.addStrider(st);
		}else if (unitTypes.smallRaiders.contains(defName)) {
			unit.setMoveState(1, (short) 0, frame + 10);
			Raider r = new Raider(unit, unit.getDef().getCost(m));
			raiderHandler.addSmallRaider(r);
			AIFloat3 pos = graphManager.getAllyCenter();
			unit.fight(pos, (short) 0, frame + 300);
		}else if (unitTypes.mediumRaiders.contains(defName)) {
			unit.setMoveState(1, (short) 0, frame + 10);
			Raider r = new Raider(unit, unit.getDef().getCost(m));
			raiderHandler.addMediumRaider(r);
			AIFloat3 pos = graphManager.getAllyCenter();
			unit.fight(pos, (short) 0, frame + 300);
		}else if (unitTypes.soloRaiders.contains(defName)) {
			unit.setMoveState(1, (short) 0, frame + 10);
			Raider r = new Raider(unit, unit.getDef().getCost(m));
			raiderHandler.addSoloRaider(r);
		}else if (unitTypes.assaults.contains(defName)) {
			unit.setMoveState(1, (short) 0, frame + 10);
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
			if (defName.equals("cormist") || defName.equals("armcrabe")) {
				unit.setMoveState(0, (short) 0, frame + 10);
				ArrayList<Float> params = new ArrayList<>();
				params.add((float) 1);
				unit.executeCustomCommand(CMD_UNIT_AI, params, (short) 0, frame + 30);
			} else {
				unit.setMoveState(1, (short) 0, frame + 10);
			}
			Fighter f = new Fighter(unit, unit.getDef().getCost(m));
			miscHandler.addLoner(f);
		}else if (unitTypes.arties.contains(defName)){
			if (defName.equals("armmerl") || defName.equals("cormart") || defName.equals("armraven") || defName.equals("trem")){
				unit.setMoveState(0, (short) 0, frame + 10);
				ArrayList<Float> params = new ArrayList<>();
				params.add((float) 1);
				unit.executeCustomCommand(CMD_UNIT_AI, params, (short) 0, frame + 30);
			}else{
				unit.setMoveState(1, (short) 0, frame + 10);
			}
			Fighter f = new Fighter(unit, unit.getDef().getCost(m));
			miscHandler.addArty(f);
		}else if (unitTypes.AAs.contains(defName)){
			unit.setMoveState(2, (short) 0, frame + 10);
			Raider f = new Raider(unit, unit.getDef().getCost(m));
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
		}else if (defName.equals("armbrtha")) {
			miscHandler.addBertha(unit);
		}
    	
    	if (unit.getDef().getSpeed() > 0 && unit.getDef().getBuildOptions().size() == 0
				&& !unitTypes.shieldMobs.contains(defName) && !unitTypes.noRetreat.contains(defName)){
    		retreatHandler.addCoward(unit);
    	}

        return 0; // signaling: OK
    }

	@Override
	public int unitCreated(Unit unit, Unit builder){
		String defName = unit.getDef().getName();

		// Paint ally threat for porc
		if (unit.getDef().getSpeed() == 0 && unit.getDef().isAbleToAttack() && !unit.getDef().getName().equals("armbrtha")){
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
	    if (unit.getDef().getName().equals("wolverine_mine")){
		    return 0;
	    }
        retreatHandler.removeUnit(unit);
		raiderHandler.removeUnit(unit);
		squadHandler.removeUnit(unit);
		bomberHandler.removeUnit(unit);
		miscHandler.removeUnit(unit);
	
		if (unit.getDef().getName().equals("corsilo")){
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

		// create a defense task, if appropriate.
		if ((!unit.getDef().isAbleToAttack() || unit.getDef().getSpeed() == 0 || unit.getDef().isBuilder() || graphManager.isAllyTerritory(unit.getPos()))
				&& frame - lastDefenseFrame > 150){
			lastDefenseFrame = frame;
			DefenseTarget dt = null;
			
			float udmg = (unit.getDef().getName().equals("cormex") && graphManager.isFrontLine(unit.getPos())) ? 1500f : unit.getMaxHealth();
			if (attacker != null){
				if (attacker.getPos() != null && attacker.getDef() != null && !attacker.getDef().isAbleToFly()) {
					dt = new DefenseTarget(attacker.getPos(), udmg + attacker.getMaxHealth(), frame);
				}
			}else {
				dt = new DefenseTarget(unit.getPos(), udmg, frame);
			}
			if (dt != null) {
				defenseTargets.add(dt);
			}

			if (attacker != null) {
				if (attacker.getDef() != null) {
					if (attacker.getDef().isAbleToFly()) {
						DefenseTarget adt = new DefenseTarget(unit.getPos(), udmg + attacker.getMaxHealth(), frame);
						airDefenseTargets.add(adt);
					}
				}
			}
		}

		// Unpaint ally threat for porc
		if (unit.getDef().getSpeed() == 0 && unit.getDef().isAbleToAttack() && !unit.getDef().getName().equals("armbrtha")){
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
		if (h.getDef().getName().equals("wolverine_mine")){
			return 0;
		}
		// check if the damaged unit is on fire.
		boolean on_fire = false;
	
		if (h.getRulesParamFloat("on_fire", 0.0f) > 0) {
			on_fire = true;
		}
	
		retreatHandler.checkUnit(h);
		// retreat scouting raiders so that they don't suicide into enemy raiders
		if (!retreatHandler.isRetreating(h) && !on_fire) {
			raiderHandler.avoidEnemies(h, attacker, dir);
		}
	
		// create a defense task, if appropriate.
		if ((attacker == null || attacker.getPos() == null || attacker.getPos().equals(nullpos) || attacker.getDef() == null)
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
			} else if (!attacker.getDef().getName().equals("wolverine_mine")) {
				dt = new DefenseTarget(h.getPos(), (attacker.getMaxHealth() + h.getMaxHealth()) / 2f, frame);
			}
		
			// don't create defense targets vs air units.
			if (dt != null) {
				if (weaponDef != null && weaponDef.getName().startsWith("armbrawl")) {
					airDefenseTargets.add(dt);
				} else {
					defenseTargets.add(dt);
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
		}
		return 0;
	}
}