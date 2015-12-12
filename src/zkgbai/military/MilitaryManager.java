package zkgbai.military;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.*;

import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.ImageBuffer;
import org.newdawn.slick.opengl.pbuffer.GraphicsFactory;

import zkgbai.Module;
import zkgbai.ZKGraphBasedAI;
import zkgbai.economy.EconomyManager;
import zkgbai.economy.FactoryManager;
import zkgbai.graph.GraphManager;
import zkgbai.graph.MetalSpot;
import zkgbai.los.LosManager;
import zkgbai.military.tasks.DefenseTarget;
import zkgbai.military.tasks.ScoutTask;

public class MilitaryManager extends Module {
	
	ZKGraphBasedAI parent;
	GraphManager graphManager;
	public Pathfinder pathfinder;
	
	java.util.Map<Integer,Enemy> targets;
	List<DefenseTarget> defenseTargets;
	java.util.Map<Integer, Fighter> fighters;
	java.util.Map<Integer, Fighter> supports;
	java.util.Map<Integer, Fighter> loners;
	java.util.Map<Integer, Unit> sappers;
	public java.util.Map<Integer, Fighter> AAs;
	List<Unit> cowardUnits;
	List<Unit> retreatingUnits;
	List<Unit> retreatedUnits;
	List<Unit> havens;
	
	Squad nextSquad;
	Squad nextAirSquad;
	public ShieldSquad nextShieldSquad;
	public List<Squad> squads;
	List<Raider> raidQueue;
	List<Raider> mediumRaidQueue;
	public List<Raider> raiders;
	List<Strider> striders;

	List<ScoutTask> scoutTasks;

	RadarIdentifier radarID;
	
	int maxUnitPower = 0;
	ImageBuffer threatmap;
	Graphics threatGraphics;
	ArrayList<TargetMarker> targetMarkers;
	
    public static final short OPTION_SHIFT_KEY = (1 << 5); //  32
	static AIFloat3 nullpos = new AIFloat3(0,0,0);
	
	int nano;

	private LosManager losManager;
	private EconomyManager ecoManager;
	private FactoryManager factoryManager;
	private OOAICallback callback;
	private UnitClasses unitTypes;

	private Resource m;

	int frame = 0;
	int lastDefenseFrame = 0;
	int lastAirDefenseFrame = 0;

	static int CMD_DONT_FIRE_AT_RADAR = 38372;
	static int CMD_AIR_STRAFE = 39381;
	static int CMD_AP_FLY_STATE = 34569;
	static int CMD_UNIT_AI = 36214;
	
	@Override
	public String getModuleName() {
		return "MilitaryManager";
	}
	
	public void setGraphManager(GraphManager gm){
		this.graphManager = gm;
	}
	
	public void setLosManager(LosManager losManager) {
		this.losManager = losManager;
	}

	public void setEcoManager(EconomyManager ecoManager){this.ecoManager = ecoManager;}

	public void setFactoryManager(FactoryManager facManager) {
		this.factoryManager = facManager;
	}
	
	public MilitaryManager(ZKGraphBasedAI parent){
		this.parent = parent;
		this.callback = parent.getCallback();
		this.targets = new HashMap<Integer,Enemy>();
		this.defenseTargets = new ArrayList<DefenseTarget>();
		this.fighters = new HashMap<Integer, Fighter>();
		this.supports = new HashMap<Integer, Fighter>();
		this.loners = new HashMap<Integer, Fighter>();
		this.sappers = new HashMap<Integer, Unit>();
		this.AAs = new HashMap<Integer, Fighter>();
		this.raiders = new ArrayList<Raider>();
		this.raidQueue = new ArrayList<Raider>();
		this.mediumRaidQueue = new ArrayList<Raider>();
		this.striders = new ArrayList<Strider>();
		this.squads = new ArrayList<Squad>();
		this.nextSquad = null;
		this.nextShieldSquad = null;
		this.nextAirSquad = null;
		this.cowardUnits = new ArrayList<Unit>();
		this.retreatingUnits = new ArrayList<Unit>();
		this.retreatedUnits = new ArrayList<Unit>();
		this.havens = new ArrayList<Unit>();
		this.scoutTasks = new ArrayList<ScoutTask>();
		this.nano = callback.getUnitDefByName("armnanotc").getUnitDefId();
		this.unitTypes = new UnitClasses();
		this.m = callback.getResourceByName("Metal");
		this.pathfinder = new Pathfinder(this);
		
		targetMarkers = new ArrayList<TargetMarker>();
		int width = parent.getCallback().getMap().getWidth();
		int height = parent.getCallback().getMap().getHeight();
		
		this.threatmap = new ImageBuffer(width, height);
		try {
			this.threatGraphics = threatmap.getImage().getGraphics();
		}catch (Exception e){
			parent.printException(e);
			System.exit(0);
		}
		
		try{
			radarID = new RadarIdentifier(parent.getCallback());
		}catch(Exception e){
			parent.printException(e);
		}
	}
	
	private void paintThreatMap(){
		
		int w = threatmap.getWidth();
		int h = threatmap.getHeight();
		
		threatGraphics.setBackground(new Color(0, 0, 0, 0));
        threatGraphics.clear();
		threatGraphics.setDrawMode(Graphics.MODE_ADD);

		// paint allythreat for raiders
		for (Raider r:raiders){
			float power = Math.min(1.0f, (r.getUnit().getPower() + r.getUnit().getMaxHealth())/5000);
			float radius = r.getUnit().getMaxRange();
			AIFloat3 pos = r.getPos();
			int x = (int) pos.x/8;
			int y = (int) pos.z/8;
			int rad = 50;

			threatGraphics.setColor(new Color(0f, power, 0f));
			paintCircle(x, y, rad);
		}

		// paint allythreat for fighters
		for (Fighter f:fighters.values()){
			float power = Math.min(1.0f, (f.getUnit().getPower() + f.getUnit().getMaxHealth())/5000);
			AIFloat3 pos = f.getPos();
			int x = (int) pos.x/8;
			int y = (int) pos.z/8;
			int rad = 60;

			threatGraphics.setColor(new Color(0f, power, 0f));
			paintCircle(x, y, rad);
		}

		// paint allythreat for porc
		for (Unit p:ecoManager.porcs){
			float power = Math.min(1.0f, ((p.getPower() + p.getMaxHealth())/5000) * 2);
			float radius = p.getMaxRange();
			AIFloat3 pos = p.getPos();
			int x = (int) pos.x/8;
			int y = (int) pos.z/8;
			int rad = (int) radius/8;

			threatGraphics.setColor(new Color(0f, power, 0f));
			paintCircle(x, y, rad);
		}

		for(Enemy t:targets.values()){
			float effectivePower = Math.min(1.0f , t.getDanger()/5000);

			AIFloat3 position = t.position;

			if (position != null && t.ud != null
					&& !t.ud.getTooltip().contains("Anti-Air") && !unitTypes.planes.contains(t.ud.getName())
					&& t.unit != null && !t.unit.isBeingBuilt()) {
				int x = (int) (position.x / 8);
				int y = (int) (position.z / 8);
				int r = (int) ((t.threatRadius) / 8);

				if (t.speed > 0) {
					// for enemy mobiles
					effectivePower *= 1-((frame - t.lastSeen)/1800); // reduce threat over time when enemies leave los
					if (!t.isRiot) {
						threatGraphics.setColor(new Color(effectivePower, 0f, 0f)); //Direct Threat Color, red
						paintCircle(x, y, r); // draw direct threat circle
					}else{
						// use a threat gradient for riots to improve pathing intelligence
						threatGraphics.setColor(new Color(effectivePower/3, 0f, 0f)); //Direct Threat Color, red
						paintCircle(x, y, r/2);
						paintCircle(x, y, r);
						paintCircle(x, y, (int) (r*1.5f));
					}
				} else {
					// for enemy statics
					threatGraphics.setColor(new Color(effectivePower, 0f, 0f)); //Direct Threat Color, red
					paintCircle(x, y, (int) (r*1.2f)); // draw direct threat circle.
				}


			}
		}

		// mark front line territory
		List<MetalSpot> frontLine = graphManager.getFrontLineSpots();
		for (MetalSpot ms: frontLine){
			AIFloat3 pos = ms.getPos();
			int x = (int) pos.x/8;
			int y = (int) pos.z/8;

			threatGraphics.setColor(new Color(0f, 0f, 0.5f)); // front line territory color, blue
			paintCircle(x, y, 75); // 800 elmo radius around each frontline mex
		}

		ArrayList<TargetMarker> deadMarkers = new ArrayList<TargetMarker>();
		for(TargetMarker tm:targetMarkers){
			int age = parent.currentFrame - tm.frame;
			if(age < 255){

			}else{
				deadMarkers.add(tm);
			}
		}
		
		for(TargetMarker tm:deadMarkers){
			targetMarkers.remove(tm);
		}
		
	}
	
	private void paintCircle(int x, int y, int r){
		threatGraphics.fillOval(x-r, y-r, 2*r, 2*r);
	}
	
	private void paintHollowCircle(int x, int y, int r){
		threatGraphics.drawOval(x-r, y-r, 2*r, 2*r);
	}
	
	public Image getThreatMap(){
		return this.threatmap.getImage();
	}
	
	public float getThreat(AIFloat3 position){
		int x = (int) (position.x/8);
		int y = (int) (position.z/8);
		Color c;
		try{
			c = threatmap.getImage().getColor(x,y);
		}catch (Exception e) {
			return 0;
		}
		float threat = (float) c.getRed();
		return threat/255;
	}

	public float getEffectiveThreat(AIFloat3 position){
		int x = (int) (position.x/8);
		int y = (int) (position.z/8);
		Color c;
		try{
			c = threatmap.getImage().getColor(x,y);
		}catch (Exception e) {
			return 0;
		}
		float threat = (float) c.getRed();
		float pthreat = (float) c.getGreen();
		return (threat-(2*pthreat))/255;
	}

	public float getFriendlyThreat(AIFloat3 position){
		int x = (int) (position.x/8);
		int y = (int) (position.z/8);
		try {
			Color c = threatmap.getImage().getColor(x,y);
			float pthreat = (float) c.getGreen();
			return pthreat / 255;
		}catch (Exception e){
			return 0;
		}
	}

	public boolean isFrontLine(AIFloat3 position){
		int x = (int) (position.x/8);
		int y = (int) (position.z/8);
		Color c = threatmap.getImage().getColor(x,y);
		float value = (float) c.getBlue();
		if (value/255 > 0){
			return true;
		}
		return false;
	}

	private void createScoutTasks(){
		List<MetalSpot> unscouted = graphManager.getEnemyTerritory();
		if (unscouted.isEmpty() && scoutTasks.isEmpty()){
			unscouted = graphManager.getUnownedSpots(); // if enemy territory is not known, get all spots not in our own territory.
		}

		for (MetalSpot ms: unscouted){
			ScoutTask st = new ScoutTask(ms.getPos(), ms);
			if (!scoutTasks.contains(st)){
				scoutTasks.add(st);
			}
		}
	}

	private void checkScoutTasks(){
		List<ScoutTask> finished = new ArrayList<ScoutTask>();
		if (graphManager.getEnemyTerritory().isEmpty()){
			for (ScoutTask st: scoutTasks){
				if (st.spot.visible){
					st.endTask(parent.currentFrame);
					finished.add(st);
				}
			}
		}else {
			for (ScoutTask st : scoutTasks) {
				if (!st.spot.hostile && !st.spot.enemyShadowed) {
					st.endTask(parent.currentFrame);
					finished.add(st);
				}
			}
		}
		scoutTasks.removeAll(finished);
	}

	private void assignRaiders(){
		boolean needUnstick = false;
		if (frame % 30 == 0){
			needUnstick = true;
		}
		for (Raider r: raiders){
			if (retreatingUnits.contains(r.getUnit()) || r.getUnit().getHealth() <= 0){
				continue;
			}

			if (needUnstick){
				r.unstick(frame);
			}

			ScoutTask bestTask = null;
			float cost = Float.MAX_VALUE;

			if (r.getTask() != null){
				bestTask = r.getTask();
				cost = getScoutCost(bestTask, r) - 200;

			}

			for (ScoutTask s:scoutTasks){
				float tmpcost = getScoutCost(s, r);
				if (tmpcost < cost){
					cost = tmpcost;
					bestTask = s;
				}
			}

			boolean overThreat = (getEffectiveThreat(r.getPos()) > 0);
			if (bestTask != null && (overThreat || bestTask != r.getTask() || r.getUnit().getCurrentCommands().isEmpty())){
				if (!bestTask.spot.hostile){
					if (overThreat){
						Deque path = pathfinder.findPath(r.getUnit(), getRadialPoint(bestTask.target, 100f), pathfinder.RAIDER_PATH);
						r.sneak(path, frame);
					}else {
						r.fightTo(bestTask.target, frame);
					}
				}else{ // for raiding
					Deque path = pathfinder.findPath(r.getUnit(), getRadialPoint(bestTask.target, 50f), pathfinder.RAIDER_PATH);
					if (overThreat){
						r.sneak(path, frame);
					}else {
						r.raid(path, frame);
					}
				}
				bestTask.addRaider(r);
				r.setTask(bestTask);
			}
		}
	}

	private float getScoutCost(ScoutTask task,  Raider raider){
		float cost = graphManager.groundDistance(task.target, raider.getPos());
		if (task.spot.hostile){
			cost /= 4;
		}else {
			if ((frame - task.spot.getLastSeen()) < 450) {
				cost += 3000;
			}else {
				// reduce cost relative to every 30 seconds since last seen for enemy shadowed spots
				cost /= ((frame - task.spot.getLastSeen()) / 900);
			}
		}

		if (getThreat(task.target) > getFriendlyThreat(raider.getPos())){
			cost += 9001;
		}
		return cost;
	}

	private void addToSquad(Fighter f){
		// create a new squad if there isn't one
		if (nextSquad == null){
			nextSquad = new Squad();
			nextSquad.setTarget(getRallyPoint(f.getPos()), frame);
			nextSquad.income = ecoManager.effectiveIncome;
		}

		nextSquad.addUnit(f, frame);

		if (nextSquad.metalValue > nextSquad.income * 60){
			nextSquad.status = 'r';
			squads.add(nextSquad);
			List<Fighter> tooFar = nextSquad.cutoff();
			if (!tooFar.isEmpty()) {
				nextSquad = new Squad();
				nextSquad.income = ecoManager.effectiveIncome;
				nextSquad.setTarget(getRallyPoint(f.getPos()), frame);
				for (Fighter fi : tooFar) {
					nextSquad.addUnit(fi, frame);
				}
			}else{
				nextSquad = null;
			}
		}
	}

	private void addToShieldSquad(Fighter f){
		// create a new squad if there isn't one
		if (nextShieldSquad == null){
			nextShieldSquad = new ShieldSquad();
			nextShieldSquad.setTarget(getRallyPoint(f.getPos()), frame);
			nextShieldSquad.status = 'a';
		}
		nextShieldSquad.addUnit(f, frame);
	}

	private void addToAirSquad( Fighter f){
		// create a new squad if there isn't one
		if (nextAirSquad == null){
			nextAirSquad = new Squad();
			nextAirSquad.setTarget(getRallyPoint(f.getPos()), frame);
			nextAirSquad.income = ecoManager.effectiveIncome;
		}

		nextAirSquad.addUnit(f, frame);

		if (nextAirSquad.metalValue > nextAirSquad.income * 60){
			nextAirSquad.status = 'r';
			squads.add(nextAirSquad);
			nextAirSquad.setTarget(graphManager.getAllyCenter(), frame);
			nextAirSquad = null;
		}
	}

	private void updateSquads(){
		// set the rally point for the next forming squad for defense
		if (nextSquad != null && frame % 270 == 0) {
			if (getEffectiveThreat(nextSquad.getPos()) > 0 || getEffectiveThreat(nextSquad.target) > 0) {
				nextSquad.retreatTo(graphManager.getAllyCenter(), frame);
			}else {
				nextSquad.setTarget(getRallyPoint(nextSquad.getPos()), frame);
			}
		}
		if (nextAirSquad != null && frame % 450 == 0) {
			nextAirSquad.setTarget(getRallyPoint(nextAirSquad.getPos()), frame);
		}
		if (nextShieldSquad != null && frame % 180 == 0) {
			// shields only get one squad into which it dumps all of its mobs.
			if (nextShieldSquad.getHealth() < 0.95){
				nextShieldSquad.retreatTo(graphManager.getAllyCenter(), frame);
			}else if (nextShieldSquad.metalValue > ecoManager.effectiveIncome * 30 && nextShieldSquad.metalValue > 1000){
				AIFloat3 target = getTarget(nextShieldSquad.getPos(), true);
				// reduce redundant order spam.
				if (!target.equals(nextShieldSquad.target)) {
					nextShieldSquad.setTarget(target, frame);
				}
			}else {
				nextShieldSquad.setTarget(getRallyPoint(nextShieldSquad.getPos()), frame);
			}
		}

		List<Squad> deadSquads = new ArrayList<Squad>();
		boolean assigned = false;

		for (Squad s: squads){
			if (s.isDead()){
				deadSquads.add(s);
				continue;
			}

			// set rallying for squads that are finished forming and gathering to attack
			if (s.status == 'r' && !s.assigned){
				assigned = true;
				s.assigned = true;
				if (s.isRallied(frame)){
					s.status = 'a';
				}
				break;
			}else if (s.status == 'a' && !s.assigned){
				if (getEffectiveThreat(s.getPos()) > 0){
					s.retreatTo(graphManager.getAllyCenter(), frame);
					assigned = true;
					s.assigned = true;
					break;
				}
				AIFloat3 target = getTarget(s.getPos(), true);
				// reduce redundant order spam.
				if (!target.equals(s.target)) {
					assigned = true;
					s.assigned = true;
					s.setTarget(target, frame);
					break;
				}
			}
		}
		squads.removeAll(deadSquads);
		if (!assigned){
			for (Squad s: squads){
				s.assigned = false;
			}
		}
	}

	private void updateSupports(){
		Iterator<Fighter> iter = supports.values().iterator();
		while (iter.hasNext()) {
			Fighter s = iter.next();
			if (!retreatingUnits.contains(s.getUnit())) {
				if (s.squad != null) {
					if (!s.squad.isDead()) {
						s.moveTo(s.squad.getPos(), frame);
					} else {
						s.squad = null;
					}
				}

				if (s.squad == null && (nextSquad != null || nextShieldSquad != null)) {

					if (s.getUnit().getDef().getName().equals("spherecloaker") && nextSquad != null) {
						s.squad = nextSquad;
					}
					if (s.squad == null && s.getUnit().getDef().getName().equals("core_spectre") && nextShieldSquad != null && nextShieldSquad.getPos() != null){
						s.squad = nextShieldSquad;
					}

					if (s.squad != null) {
						if (s.squad.status == 'f') {
							s.moveTo(s.squad.target, frame);
						} else if (s.squad.getPos() != null){
							s.moveTo(s.squad.getPos(), frame);
						}
					}
				} else if (s.squad == null) {
					s.moveTo(getRallyPoint(s.getPos()), frame);
				}
			}
		}
	}
    
    
	private AIFloat3 getTarget(AIFloat3 origin, boolean defend){
    	AIFloat3 target = null;
    	float cost = Float.MAX_VALUE;

		// first check defense targets
		if (defend) {
			for (DefenseTarget d : defenseTargets) {
				float tmpcost = (graphManager.groundDistance(origin, d.position) - d.damage)/1+(2 * getThreat(d.position));

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
    			float tmpcost = GraphManager.groundDistance(m.getPos(), origin)/2;
    			
    			if (tmpcost < cost){
    				target = m.getPos();
    				cost = tmpcost;
    			}
    		}
    	}

		// if there aren't any defense targets or enemy mexes to attack, find a unit to kill
    	if(targets.size() > 0){
    		Iterator<Enemy> enemies = targets.values().iterator();
    		while(enemies.hasNext()){
    			Enemy e = enemies.next();
				if (e.position != null && e.identified) {
					float tmpcost = graphManager.groundDistance(origin, e.position) / (1 + (e.value/1000));

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
				float tmpcost = GraphManager.groundDistance(m.getPos(), origin);
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

	private void dgunStriders(){
		for (Strider s:striders){
			String defName = s.getUnit().getDef().getName();
			if (!defName.equals("dante") && !defName.equals("scorpion") && !defName.equals("armbanth")){
				continue;
			}
			AIFloat3 target = getDgunTarget(s.getPos());
			if (target != null && frame > s.lastDgunFrame + s.dgunReload){
				s.getUnit().dGunPosition(target, (short) 0, frame + 3000);
				s.lastDgunFrame = frame;
			}
		}
	}

	private AIFloat3 getDgunTarget(AIFloat3 pos){
		List<Unit> enemies = callback.getEnemyUnitsIn(pos, 450f);
		AIFloat3 target = null;
		float bestScore = 0;
		for (Unit e:enemies){
			float cost = e.getDef().getCost(m);
			if (e.getMaxSpeed() > 0 && !e.getDef().isAbleToFly() && cost > 200){
				if (cost > bestScore){
					bestScore = cost;
					target = e.getPos();
				}
			}else if (e.getMaxSpeed() == 0){
				if (cost > bestScore){
					bestScore = cost;
					target = e.getPos();
				}
			}
		}
		return target;
	}

	private AIFloat3 getRallyPoint(AIFloat3 pos){
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
			float tmpcost = graphManager.groundDistance(pos, d.position) - d.damage;

			if (tmpcost < cost) {
				cost = tmpcost;
				target = d.position;
			}
		}

		if (target != null){
			return target;
		}

		//if there aren't any, then get the closest friendly front line spot.
		AIFloat3 position = null;
		try {
			position = graphManager.getClosestFrontLineSpot(pos).getPos();
		}catch (Exception e){}
		if (position != null) {
			return position;
		}else{
			//otherwise rally to ally center.
			position = graphManager.getAllyCenter();
		}

		return position;
	}

	private void retreatCowards(){
		AIFloat3 position = graphManager.getAllyCenter();
		boolean retreated = false;
		List<Unit> healedUnits = new ArrayList<Unit>();
		for (Unit u: retreatingUnits){
			if (u.getHealth() == u.getMaxHealth()){
				healedUnits.add(u);
				continue;
			}
			if(u.getHealth() > 0 && !retreatedUnits.contains(u)){
				UnitDef building = callback.getUnitDefByName("factorygunship");
				position = callback.getMap().findClosestBuildSite(building, position, 600f, 3, 0);
				
				Deque<AIFloat3> path = pathfinder.findPath(u, position, pathfinder.AVOID_ENEMIES);
				u.moveTo(path.poll(), (short) 0, frame + 300); // skip first waypoint if target actually found to prevent stuttering, otherwise use it.

				if (path.isEmpty()){
					// pathing failed
				}else{
					u.moveTo(path.poll(), (short) 0, frame + 300); // immediately move to first non-redundant waypoint

					int pathSize = Math.min(5, path.size());
					for(int i=0;i<pathSize;i++){ // queue up to the next 5 waypoints with shift
						u.moveTo(path.poll(), OPTION_SHIFT_KEY, frame+300);
						if(path.isEmpty()) break;
					}
					// let the rest of the waypoints get handled the next time around.
				}
				retreatedUnits.add(u);
				retreated = true;
				break;
			}
		}
		if (!retreated){
			retreatedUnits.clear();
		}

		retreatingUnits.removeAll(healedUnits);
		retreatedUnits.removeAll(healedUnits);
	}

	private void updateTargets() {
		List<Enemy> outdated = new ArrayList<Enemy>();
		for (Enemy t : targets.values()) {
			AIFloat3 tpos = t.unit.getPos();
			if (tpos != null && !tpos.equals(nullpos)) {
				t.lastSeen = frame;
				if (!t.getIdentified() && t.position != null) {

					/*float speed = GraphManager.groundDistance(t.position, tpos) / 15;

					if (speed > t.maxObservedSpeed) {
						RadarDef rd = radarID.getDefBySpeed(speed);
						t.maxObservedSpeed = speed;
						if (rd != null) {
							t.updateFromRadarDef(rd);
						}
					}*/
				}
				t.position = tpos;
			}else if (frame - t.lastSeen > 1800 && !t.isStatic) {
				// remove mobiles that haven't been seen for over 60 seconds.
				outdated.add(t);
			}else if (t.position != null && t.isStatic && losManager.isInLos(t.position)) {
				// remove targets that aren't where we last saw them.
				outdated.add(t);
			}
		}

		for (Enemy t: outdated){
			targets.remove(t.unitID);
		}

		List<DefenseTarget> expired = new ArrayList<DefenseTarget>();
		for (DefenseTarget d:defenseTargets){
			if (frame - d.frameIssued > 600){
				expired.add(d);
			}
		}
		defenseTargets.removeAll(expired);
	}

	void updateSappers(){
		for (Unit s : sappers.values()) {
			if (s.getHealth() <= 0 || !s.getCurrentCommands().isEmpty()){continue;}

			List<Unit> enemies = callback.getEnemyUnitsIn(s.getPos(), 350f);
			Unit enemy = null;
			for (Unit e:enemies){
				if (e.getDef() != null && e.getDef().getCost(m) > 100){
					enemy = e;
					break;
				}
			}
			if (enemy != null){
				s.attack(enemy, (short) 0, frame+300);
				continue;
			}

			MetalSpot ms = null;
			if (s.getDef().getName().equals("blastwing")){
				ms = graphManager.getClosestEnemySpot(s.getPos());
				if (ms != null) {
					s.fight(ms.getPos(), (short) 0, frame + 300);
				}
				continue;
			}else {
				ms = graphManager.getClosestFrontLineSpot(s.getPos());
			}

			if (ms == null){
				ms = graphManager.getClosestNeutralSpot(s.getPos());
			}

			if (ms != null && graphManager.groundDistance(s.getPos(), ms.getPos()) > 500) {
				s.fight(getDirectionalPoint(ms.getPos(), graphManager.getEnemyCenter(), 350f), (short) 0, frame + 300);
			}
		}
	}

	void cleanUnits(){
		// remove dead/captured units because spring devs are stupid and call update before unitDestroyed.
		List<Integer> invalidFighters = new ArrayList<Integer>();
		for (Fighter f:fighters.values()){
			if (f.getUnit().getHealth() <= 0 || f.getUnit().getTeam() != parent.teamID){
				if (f.squad != null) {
					f.squad.removeUnit(f);
				}
				invalidFighters.add(f.id);
			}
		}
		for (Integer key:invalidFighters){
			fighters.remove(key);
		}
		invalidFighters.clear();

		for (Fighter f:loners.values()){
			if (f.getUnit().getHealth() <= 0 || f.getUnit().getTeam() != parent.teamID){
				invalidFighters.add(f.id);
			}
		}
		for (Integer key:invalidFighters){
			loners.remove(key);
		}
		invalidFighters.clear();

		for (Fighter f:supports.values()){
			if (f.getUnit().getHealth() <= 0 || f.getUnit().getTeam() != parent.teamID){
				invalidFighters.add(f.id);
			}
		}
		for (Integer key:invalidFighters){
			supports.remove(key);
		}
	}
    
    @Override
    public int update(int frame) {
    	this.frame = frame;
		cleanUnits();

		if (frame % 300 == 0){
			for (Fighter l:loners.values()){
				Unit u = l.getUnit();
				if (!retreatingUnits.contains(u)){
					AIFloat3 target = getTarget(l.getPos(), true);
					l.fightTo(target, frame);
				}
			}
		}

		if (frame % 6 == 0){
			retreatCowards();
		}

		if(frame%15 == 0) {
			// move raiders from the holding squad into the main raider pool.
			if (raidQueue.size() > factoryManager.raiderCount){
				raiders.addAll(raidQueue);
				raidQueue.clear();
			}

			if (mediumRaidQueue.size() > 3){
				raiders.addAll(mediumRaidQueue);
				mediumRaidQueue.clear();
			}

			paintThreatMap();

			updateTargets();
			createScoutTasks();
			checkScoutTasks();

			assignRaiders();
			updateSupports();
			updateSappers();

			if (frame % 90 == 0){
				updateSquads();
			}

			for (Fighter f:fighters.values()){
				Unit u = f.getUnit();
				if (!retreatingUnits.contains(u) && f.squad == null) {
					if (unitTypes.airMobs.contains(u.getDef().getName())){
						addToAirSquad(f);
					}else if (unitTypes.assaults.contains(u.getDef().getName())){
						addToSquad(f);
					}
				}
			}

			for (Strider st:striders){
				Unit u = st.getUnit();
				if (!retreatingUnits.contains(u)){
					AIFloat3 target = getTarget(st.getPos(), false);
					st.fightTo(target, frame);
				}
			}

			dgunStriders();
		}
        return 0; // signaling: OK
    }
	
    @Override
    public int enemyEnterLOS( Unit enemy) {
    	Resource metal = parent.getCallback().getResourceByName("Metal");
    	if(targets.containsKey(enemy.getUnitId())){
    		Enemy e = targets.get(enemy.getUnitId()); 
    		e.visible = true;
    		e.setIdentified();
    		e.updateFromUnitDef(enemy.getDef(), enemy.getDef().getCost(metal));
			e.lastSeen = frame;
    	}else{
    		Enemy e = new Enemy(enemy, enemy.getDef().getCost(metal));
    		targets.put(enemy.getUnitId(),e);
    		e.visible = true;
    		e.setIdentified();
			e.lastSeen = frame;
    	}
        return 0; // signaling: OK
    }

    @Override
    public int enemyLeaveLOS( Unit enemy) {
    	if(targets.containsKey(enemy.getUnitId())){
			targets.get(enemy.getUnitId()).visible = false;
			targets.get(enemy.getUnitId()).lastSeen = frame;
    	}	
        return 0; // signaling: OK
    }

    @Override
    public int enemyEnterRadar( Unit enemy) {
    	if(targets.containsKey(enemy.getUnitId())){
    		targets.get(enemy.getUnitId()).isRadarVisible = true;
			targets.get(enemy.getUnitId()).lastSeen = frame;
    	}else{
    		if(enemy.getDef() != null){
	    		Enemy e = new Enemy(enemy, enemy.getDef().getCost(parent.getCallback().getResourceByName("Metal")));
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
    public int enemyLeaveRadar( Unit enemy) {
    	if(targets.containsKey(enemy.getUnitId())){
			targets.get(enemy.getUnitId()).isRadarVisible = false;
			targets.get(enemy.getUnitId()).lastSeen = frame;
    	}
        return 0; // signaling: OK
    }
    
    @Override
    public int enemyDestroyed(Unit unit, Unit attacker) {
        if(targets.containsKey(unit.getUnitId())){
        	targets.remove(unit.getUnitId());
        }	    
        return 0; // signaling: OK
    }
    
    @Override
    public int unitFinished(Unit unit) {
    	
    	if(unit.getDef().getUnitDefId() == nano){
    		havens.add(unit);
    	}

		String defName = unit.getDef().getName();

		// enable snipers to shoot radar dots
		if (defName.equals("armsnipe")){
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

		// Activate outlaws
		if (defName.equals("cormak")){
			unit.setOn(true, (short) 0, frame+300);
		}

		if (unitTypes.striders.contains(defName)){
			Strider st = new Strider(unit, unit.getDef().getCost(m));
			striders.add(st);
		}else if (unitTypes.smallRaiders.contains(defName)) {
			Raider r = new Raider(unit, unit.getDef().getCost(m));
			raidQueue.add(r);
			AIFloat3 pos = graphManager.getAllyCenter();
			unit.fight(pos, (short) 0, frame + 300);
		}else if (unitTypes.mediumRaiders.contains(defName)) {
			Raider r = new Raider(unit, unit.getDef().getCost(m));
			mediumRaidQueue.add(r);
			AIFloat3 pos = graphManager.getAllyCenter();
			unit.fight(pos, (short) 0, frame + 300);
		}else if (unitTypes.soloRaiders.contains(defName)) {
			Raider r = new Raider(unit, unit.getDef().getCost(m));
			raiders.add(r);
		}else if (unitTypes.assaults.contains(defName)) {
			Fighter f = new Fighter(unit, unit.getDef().getCost(m));
			fighters.put(f.id, f);
			addToSquad(f);
		}else if (unitTypes.shieldMobs.contains(defName)){
			Fighter f = new Fighter(unit, unit.getDef().getCost(m));
			fighters.put(f.id, f);
			addToShieldSquad(f);
		}else if (unitTypes.airMobs.contains(defName)){
			Fighter f = new Fighter(unit, unit.getDef().getCost(m));
			fighters.put(f.id, f);
			addToAirSquad(f);
		}else if(unitTypes.mobSupports.contains(defName)){
			Fighter f = new Fighter(unit, unit.getDef().getCost(m));
			supports.put(f.id, f);
    	}else if(unitTypes.loners.contains(defName)) {
			if (defName.equals("cormist") || defName.equals("armcrabe")){
				unit.setMoveState(0, (short) 0, frame + 10);
				ArrayList<Float> params = new ArrayList<>();
				params.add((float) 1);
				unit.executeCustomCommand(CMD_UNIT_AI, params, (short) 0, frame+30);
			}else{
				unit.setMoveState(2, (short) 0, frame + 10);
			}
			Fighter f = new Fighter(unit, unit.getDef().getCost(m));
			loners.put(f.id, f);
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
			sappers.put(unit.getUnitId(), unit);
		}
    	
    	if (unit.getMaxSpeed() > 0 && unit.getDef().getBuildOptions().size() == 0
				&& !unitTypes.smallRaiders.contains(defName) && !unitTypes.shieldMobs.contains(defName) && !unitTypes.noRetreat.contains(defName)){
    		cowardUnits.add(unit);
    	}
        return 0; // signaling: OK
    }
    
    @Override
    public int unitDestroyed(Unit unit, Unit attacker) {
        cowardUnits.remove(unit);
        havens.remove(unit);
		retreatingUnits.remove(unit);

		if (fighters.containsKey(unit.getUnitId())){
			Fighter f = fighters.get(unit.getUnitId());
			if (f.squad != null) {
				f.squad.removeUnit(f);
			}
			fighters.remove(f.id);
		}

		if (loners.containsKey(unit.getUnitId())){
			loners.remove(unit.getUnitId());
		}

		if (AAs.containsKey(unit.getUnitId())){
			AAs.remove(unit.getUnitId());
		}

		if (supports.containsKey(unit.getUnitId())){
			supports.remove(unit.getUnitId());
		}

		if (sappers.containsKey(unit.getUnitId())){
			// rally units to fight if a tick/roach etc dies.
			DefenseTarget dt = new DefenseTarget(unit.getPos(), 1000f, frame);
			for (Raider r: raidQueue){
				r.fightTo(dt.position, frame);
			}
			sappers.remove(unit.getUnitId());
		}

		Fighter dead = null;
		for (Raider r:raiders){
			if (r.id == unit.getUnitId()){
				dead = r;
				if (r.getTask() != null) {
					r.getTask().removeRaider(r);
				}
			}
		}
		for (Raider r:raidQueue){
			if (r.id == unit.getUnitId()){
				dead = r;
				if (r.getTask() != null) {
					r.getTask().removeRaider(r);
				}
			}
		}
		for (Raider r:mediumRaidQueue){
			if (r.id == unit.getUnitId()){
				dead = r;
				if (r.getTask() != null) {
					r.getTask().removeRaider(r);
				}
			}
		}
		raiders.remove(dead);
		raidQueue.remove(dead);
		mediumRaidQueue.remove(dead);

		for (Strider s:striders){
			if (s.id == unit.getUnitId()){
				dead = s;
			}
		}
		striders.remove(dead);

		// create a defense task, if appropriate.
		if ((!unit.getDef().isAbleToAttack() || unit.getMaxSpeed() == 0 || graphManager.groundDistance(unit.getPos(), graphManager.getAllyCenter()) < graphManager.groundDistance(unit.getPos(), graphManager.getEnemyCenter()))
				&& frame - lastDefenseFrame > 150){
			lastDefenseFrame = frame;
			DefenseTarget dt = null;
			if (attacker != null){
				if (attacker.getPos() != null) {
					dt = new DefenseTarget(attacker.getPos(), 2000, frame);
				}
			}
			if (dt == null){
				dt = new DefenseTarget(unit.getPos(), 2000, frame);
			}
			defenseTargets.add(dt);
			for (Raider r: raidQueue){
				r.fightTo(dt.position, frame);
			}

			if (attacker != null) {
				if (attacker.getDef() != null) {
					if (attacker.getDef().isAbleToFly()) {
						for (Fighter f : AAs.values()) {
							if (f.getUnit().getHealth() > 0 && f.getUnit().getTeam() == parent.teamID) {
								f.fightTo(unit.getPos(), frame);
							}
						}
					}
				}
			}
		}

        return 0; // signaling: OK
    }
    
    @Override
    public int unitDamaged(Unit h, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzed) {
		// check if the damaged unit is on fire.
		boolean on_fire = false;
		List<UnitRulesParam> urps = h.getUnitRulesParams();
		for (UnitRulesParam urp: urps) {
			if (urp.getName().equals("on_fire")){
				on_fire = true;
			}
		}

		// retreat damaged mob units
		if(cowardUnits.contains(h) && !retreatingUnits.contains(h) && h.getHealth()/h.getMaxHealth() < 0.4){
			retreatingUnits.add(h);
			if (fighters.containsKey(h.getUnitId())){
				Fighter f = fighters.get(h.getUnitId());
				if (f.squad != null) {
					f.squad.removeUnit(f);
					f.squad = null;
				}
			}
		}

		// retreat scouting raiders so that they don't suicide into enemy raiders
		for (Raider r: raiders){
			if (r.id == h.getUnitId() && h.getHealth()/h.getMaxHealth() < 0.8 && attacker != null && (attacker.getMaxSpeed() > 0 || getEffectiveThreat(h.getPos()) > 0) && getEffectiveThreat(h.getPos()) <= 0
					&& r.scouting && !on_fire && !r.getUnit().getDef().getName().equals("corgator")){
				float movdist = -100;
				if (r.getUnit().getDef().getName().equals("spherepole") || r.getUnit().getDef().getName().equals("corsh") || getEffectiveThreat(h.getPos()) > 0){
					movdist = -450;
				}
				float x = movdist*dir.x;
				float z = movdist*dir.z;
				AIFloat3 pos = h.getPos();
				AIFloat3 target = new AIFloat3();
				 target.x = pos.x+x;
				 target.z = pos.z+z;
				h.moveTo(target, (short) 0, frame);
			}
		}

		for (Raider r: raidQueue){
			if (r.id == h.getUnitId() && h.getHealth()/h.getMaxHealth() < 0.6 && !on_fire){
				float x = -100*dir.x;
				float z = -100*dir.z;
				AIFloat3 pos = h.getPos();
				AIFloat3 target = new AIFloat3();
				target.x = pos.x+x;
				target.z = pos.z+z;
				h.moveTo(target, (short) 0, frame);
			}
		}

		// create a defense task, if appropriate.
		if ((h.getMaxSpeed() == 0 || !h.getDef().isAbleToAttack() || graphManager.groundDistance(h.getPos(), graphManager.getAllyCenter()) < graphManager.groundDistance(h.getPos(), graphManager.getEnemyCenter()) || (attacker == null || attacker.getPos() == null || attacker.getPos().equals(nullpos)))
				&& frame - lastDefenseFrame > 150 && !on_fire){
			lastDefenseFrame = frame;
			DefenseTarget dt = null;
			if (attacker != null){
				if (attacker.getPos() != null) {
					dt = new DefenseTarget(attacker.getPos(), damage, frame);
				}
			}
			if (dt == null){
				float x = 500*dir.x;
				float z = 500*dir.z;
				AIFloat3 pos = h.getPos();
				AIFloat3 target = new AIFloat3();
				target.x = pos.x+x;
				target.z = pos.z+z;
				dt = new DefenseTarget(target, damage, frame);
			}
			defenseTargets.add(dt);
			for (Raider r: raidQueue){
				r.fightTo(dt.position, frame);
			}
		}

		// Mobilize anti-air units vs enemy air
		if (attacker != null && attacker.getDef() != null && attacker.getDef().isAbleToFly() && frame - lastAirDefenseFrame > 300){
			lastAirDefenseFrame = frame;
			for (Fighter f : AAs.values()) {
				if (f.getUnit().getHealth() > 0 && f.getUnit().getTeam() == parent.teamID) {
					f.fightTo(h.getPos(), frame);
				}
			}
		}
		return 0;
    }

	@Override
	public int unitCaptured(Unit unit, int oldTeamID, int newTeamID){
		if (oldTeamID == parent.teamID){
			return unitDestroyed(unit, null);
		}else if (newTeamID == parent.teamID){
			unitFinished(unit);
			if (cowardUnits.contains(unit) && unit.getHealth()/unit.getMaxHealth() < 0.6){
				retreatingUnits.add(unit);
				if (fighters.containsKey(unit.getUnitId())){
					Fighter f = fighters.get(unit.getUnitId());
					if (f.squad != null) {
						f.squad.removeUnit(f);
						f.squad = null;
					}
				}
			}
		}
		return 0;
	}

	private AIFloat3 getRadialPoint(AIFloat3 position, Float radius){
		// returns a random point lying on a circle around the given position.
		AIFloat3 pos = new AIFloat3();
		double angle = Math.random()*2*Math.PI;
		double vx = Math.cos(angle);
		double vz = Math.sin(angle);
		pos.x = (float) (position.x + radius*vx);
		pos.z = (float) (position.z + radius*vz);
		return pos;
	}

	private AIFloat3 getDirectionalPoint(AIFloat3 start, AIFloat3 dest, float distance){
		AIFloat3 dir = new AIFloat3();
		float x = dest.x - start.x;
		float z = dest.z - start.z;
		float d = (float) Math.sqrt((x*x) + (z*z));
		x /= d;
		z /= d;
		dir.x = start.x + (x * distance);
		dir.z = start.z + (z * distance);
		return dir;
	}
}

