package zkgbai;

import java.awt.*;
import java.util.*;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import javafx.scene.shape.Path;
import zkgbai.economy.EconomyManager;
import zkgbai.economy.Factory;
import zkgbai.economy.FactoryManager;
import zkgbai.economy.Worker;
import zkgbai.economy.tasks.CombatReclaimTask;
import zkgbai.economy.tasks.ConstructionTask;
import zkgbai.graph.GraphManager;
import zkgbai.graph.Link;
import zkgbai.graph.MetalSpot;
import zkgbai.kgbutil.Pathfinder;
import zkgbai.los.LosManager;
import zkgbai.military.Enemy;
import zkgbai.military.MilitaryManager;
import zkgbai.military.UnitClasses;
import zkgbai.military.tasks.DefenseTarget;
import zkgbai.military.unitwrappers.Fighter;
import zkgbai.military.unitwrappers.Raider;
import zkgbai.military.unitwrappers.Squad;

import static zkgbai.kgbutil.KgbUtil.*;

public class ZKGraphBasedAI extends com.springrts.ai.oo.AbstractOOAI {
	private static ZKGraphBasedAI instance = null;

	private OOAICallback callback;
    private List<Module> modules = new LinkedList<Module>();
	HashSet<Integer> enemyTeams = new HashSet<Integer>();
	HashSet<Integer> enemyAllyTeams = new HashSet<Integer>();
	public List<Integer> allies = new ArrayList<>();
	public Set<Integer> comrades = new HashSet<>();
    public int teamID;
    public int allyTeamID;
    public int currentFrame = 0;
    //DebugView debugView;
    boolean debugActivated;
    boolean rsgn = false;
    boolean resigned = false;

	private boolean slave = false;
	private int mergeTarget = Integer.MAX_VALUE;
	public int mergedAllies = 0;
	public int unmergedAllies = 0;
	public int mergedAllyID = 0;
	
	public boolean isFFA = false;
 
	public LosManager losManager;
	public GraphManager graphManager;
	public EconomyManager ecoManager;
	public MilitaryManager warManager;
	public FactoryManager facManager;
    
    public static enum StartType{
    	SPRING_BOX,
    	ZK_BOX,
    	ZK_STARTPOS,
    	SPRING_STARTPOS, // unsupported
    }
    
    public StartType startType;

	// Set singleton instance
	public ZKGraphBasedAI(){
		super();
		instance = this;
	}

	public static ZKGraphBasedAI getInstance(){
		return instance;
	}
 
	@Override
    public int init(int AIId, OOAICallback callback) {
		this.debugActivated = false;
        this.callback = callback;
        this.teamID = callback.getGame().getMyTeam();
        this.allyTeamID = callback.getGame().getMyAllyTeam();
		
		try {
			identifyEnemyTeams();
			identifyAllyTeams();
			checkTeamsMerging();
		}catch (Throwable e){
			debug(e);
		}

		try{
			graphManager = new GraphManager(this);
			log(graphManager.getModuleName() + " initialized.");
		} catch (Throwable e){
			debug(e);
		}

		if (slave){
			selectRandomCommander();
			chooseStartPos();
			log("Entering merge mode! Discarding modules.");
			graphManager = null;
			return 0;
		}

		// load modules
        try {
			losManager = new LosManager(this);
			log(losManager.getModuleName() + " initialized.");
		} catch (Throwable e){
			debug(e);
		}
		try {
        	ecoManager = new EconomyManager(this);
			log(ecoManager.getModuleName() + " initialized.");
		} catch (Throwable e){
			debug(e);
		}
        try {
			warManager = new MilitaryManager(this);
			log(warManager.getModuleName() + " initialized.");
		} catch (Throwable e){
			debug(e);
		}
		try {
			facManager = new FactoryManager(this);
			log(facManager.getModuleName() + " initialized.");
		} catch (Throwable e){
			debug(e);
		}

		try {
			UnitClasses unitTypes = UnitClasses.getInstance();
			List<UnitDef> porcdefs = new ArrayList<>();
			porcdefs.add(callback.getUnitDefByName("turretmissile"));
			porcdefs.add(callback.getUnitDefByName("turretlaser"));
			porcdefs.add(callback.getUnitDefByName("turretriot"));
			porcdefs.add(callback.getUnitDefByName("turretheavylaser"));
			porcdefs.add(callback.getUnitDefByName("turretgauss"));
			porcdefs.add(callback.getUnitDefByName("turretemp"));
			porcdefs.add(callback.getUnitDefByName("turretimpulse"));
			porcdefs.add(callback.getUnitDefByName("turretheavy"));
			porcdefs.add(callback.getUnitDefByName("turretantiheavy"));

			for (UnitDef ud : porcdefs) {
				for (WeaponMount wm : ud.getWeaponMounts()) {
					unitTypes.porcWeps.add(wm.getWeaponDef().getWeaponDefId());
				}
			}
		}catch (Throwable e){
			debug(e);
		}

		/*try{
			debugView = new DebugView(this);
			debugView.setLosImage(losManager.getImage());
			debugView.setThreatImage(warManager.getThreatMap());
			debugView.setGraphImage(graphManager.getGraphImage());
		}
		catch(Throwable e){
			debug(e);
		}*/


		modules.add(losManager);
        modules.add(graphManager);
        modules.add(ecoManager);
        modules.add(warManager);
		modules.add(facManager);

		try {
			for (Module m:modules){
				m.init(AIId, callback);
			}
		} catch (Throwable e){
			debug(e);
		}
		
		try {
			selectRandomCommander();
			chooseStartPos();
		}catch (Throwable e){
			debug(e);
		}
        
        return 0;
    }

	private void selectRandomCommander(){
		String name = "dyntrainer_support_base";
		callback.getLua().callRules("ai_commander:"+name, -1);
		log("Selected Commander: " + callback.getUnitDefByName(name).getHumanName() + ".");
	}

    @Override
    public int luaMessage(java.lang.String inData){
	    for (Module module : modules) {
	    	try {
	            module.luaMessage(inData);
	    	} catch (Throwable e) {
	    		debug(e);
	    	}
        }
	       
    	return 0; //signaling: OK
    }
    
    @Override
    public int update(int frame) {
		if (slave) {
			/*if (frame % 30 == 0) {
				// give away income from communism when merged with another AI instance.
				Resource metal = callback.getResources().get(0);
				Resource energy = callback.getResources().get(1);
				callback.getEconomy().sendResource(metal, callback.getEconomy().getIncome(metal), mergeTarget);
				callback.getEconomy().sendResource(energy, callback.getEconomy().getIncome(energy), mergeTarget);
			}*/
			return 0;
		}
		
		if (frame == 0){
			say("glhf!");
		}

		if (frame % 1800 == 0){
			Pathfinder.getInstance().updateSlopeMap();
		}
		
		if (frame % 300 == 0 && frame > 18000){
			if (!resigned) {
				List<MetalSpot> espots = graphManager.getEnemySpots();
				List<MetalSpot> ospots = graphManager.getOwnedSpots();
				if (!isFFA && espots.size() > 1.5f * ospots.size() && (warManager.enemyFighterValue > facManager.armyValue * 1.5f || facManager.factories.isEmpty()) && !resigned) {
					resigned = true;
					say("gg!");
					emote("resigned");
					modules.clear();
					for (Unit u : callback.getTeamUnits()) {
						u.selfDestruct((short) 0, Integer.MAX_VALUE);
					}
				}
			}else{
				for (Unit u : callback.getTeamUnits()) {
					u.selfDestruct((short) 0, Integer.MAX_VALUE);
				}
			}
		}
		
		if (frame % 1350 == 0 && frame > 18000){
			List<MetalSpot> espots = graphManager.getEnemySpots();
			List<MetalSpot> ospots = graphManager.getOwnedSpots();
			if (!isFFA && graphManager.territoryFraction > 0.5f && ospots.size() > 1.5f * espots.size() && facManager.armyValue > warManager.enemyFighterValue * 2f && (!rsgn || Math.random() > 0.5) && !graphManager.getEnemySpots().isEmpty()){
				rsgn = true;
				double rand = Math.random();
				if (rand > 0.75) {
					say("Do you know what time it is?");
					say("It's time to resign!");
				}else if (rand > 0.5){
					if (enemyTeams.size() > 1) {
						say("Resign lobsters!");
					}else {
						say("Resign lobster!");
					}
				}else if (rand > 0.25){
					say("It's over, resign already.");
				}else{
					say("Having trouble?");
					say("Type \"!resign\" for help.");
				}
			}
		}
		
    	currentFrame = frame;

		for (Module module : modules) {
			try {
				module.update(frame);
			} catch (Throwable e) {
				debug(e);
			}
		}
        return 0; // signaling: OK
    }

    @Override
    public int message(int player, String message) {
	    if (!slave && message.equals("kgbdebug")){
	    
		}

		for (Module module : modules) {
	    	try {
	            module.message(player, message);
	    	} catch (Throwable e) {
	    		debug(e);
	    	}
	    }
	    
        return 0; // signaling: OK
    }
    
    @Override
    public int unitCreated(Unit unit, Unit builder) {
		if (slave){
			List<Unit> l = new ArrayList();
			l.add(unit);
			callback.getEconomy().sendUnits(l, mergeTarget);
		}

	    for (Module module : modules) {
	    	try {
	            module.unitCreated(unit, builder);
	    	} catch (Throwable e) {
	    		debug(e);
	    	}
	    }
        return 0;
    }

    @Override
    public int unitFinished(Unit unit) {
	    for (Module module : modules) {
	        try {
	            module.unitFinished(unit);
	    	} catch (Throwable e) {
	    		debug(e);
	    	}
	    }
        return 0; // signaling: OK
    }

    @Override
    public int unitIdle(Unit unit) {
	    for (Module module : modules) {
	    	try {
	    		module.unitIdle(unit);
	    	} catch (Throwable e) {
	    		debug(e);
	    	}
	    }
        return 0; // signaling: OK
    }

    @Override
    public int unitMoveFailed(Unit unit) {
	    for (Module module : modules) {
	    	try {
	            module.unitMoveFailed(unit);
	    	} catch (Throwable e) {
	    		debug(e);
	    	}
	    }
        return 0; // signaling: OK
    }

    @Override
    public int unitDamaged(Unit unit, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzed) {
	    for (Module module : modules) {
        	try {
        		module.unitDamaged(unit, attacker, damage, dir, weaponDef, paralyzed);
	    	} catch (Throwable e) {
	    		debug(e);
	    	}
        }
        return 0; // signaling: OK
    }

    @Override
    public int unitDestroyed(Unit unit, Unit attacker) {
	    for (Module module : modules) {
	    	try {
	            module.unitDestroyed(unit, attacker);
	    	} catch (Throwable e) {
	    		debug(e);
	    	}
	    }
        return 0; // signaling: OK
    }

    @Override
    public int unitGiven(Unit unit, int oldTeamId, int newTeamId) {
		if (slave && newTeamId == teamID){
			if (comrades.contains(oldTeamId)) {
				// Because now every damned AI has to resign by self-d individually until the team is exhausted.
				unit.selfDestruct((short) 0, Integer.MAX_VALUE);
			}else{
				// if we got the unit from somewhere else, give it to the leader.
				List<Unit> units = new ArrayList<>();
				units.add(unit);
				callback.getEconomy().sendUnits(units, mergeTarget);
			}
			return 0;
		}

        for (Module module : modules) {
        	try {
        		module.unitGiven(unit, oldTeamId, newTeamId);
	    	} catch (Throwable e) {
	    		debug(e);
	    	}
        }
        return 0; // signaling: OK
    }

    @Override
    public int unitCaptured(Unit unit, int oldTeamId, int newTeamId) {
        for (Module module : modules) {
        	try {
            	module.unitCaptured(unit, oldTeamId, newTeamId);
	    	} catch (Throwable e) {
	    		debug(e);
	    	}
        }
	    
        return 0; // signaling: OK
    }

    @Override
    public int enemyEnterLOS(Unit enemy) {
	    for (Module module : modules) {
	    	try {
	    		module.enemyEnterLOS(enemy);
	    	} catch (Throwable e) {
	    		debug(e);
	    	}
	    }
        return 0; // signaling: OK
    }

    @Override
    public int enemyLeaveLOS(Unit enemy) {
    	for (Module module : modules) {
            try {
            	module.enemyLeaveLOS(enemy);
	    	} catch (Throwable e) {
				debug(e);
	    	}
        }
        return 0; // signaling: OK
    }

	@Override
	public int enemyCreated(Unit enemy) {
		for (Module module : modules) {
			try {
				module.enemyCreated(enemy);
			} catch (Throwable e) {
				debug(e);
			}
		}
		return 0; // signaling: OK
	}

	@Override
	public int enemyFinished(Unit enemy) {
		for (Module module : modules) {
			try {
				module.enemyFinished(enemy);
			} catch (Throwable e) {
				debug(e);
			}
		}
		return 0; // signaling: OK
	}

    @Override
    public int enemyEnterRadar(Unit enemy) {
    	for (Module module : modules) {
    		try {
    			module.enemyEnterRadar(enemy);
	    	} catch (Throwable e) {
	    		debug(e);
	    	}
        }
        return 0; // signaling: OK
    }

    @Override
    public int enemyLeaveRadar(Unit enemy) {
		for (Module module : modules) {
			try {
				module.enemyLeaveRadar(enemy);
	    	} catch (Throwable e) {
	    		debug(e);
	    	}
	    }
        return 0; // signaling: OK
    }

    @Override
    public int enemyDamaged(Unit enemy, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzed) {
    	for (Module module : modules) {
    		try {
    			module.enemyDamaged(enemy, attacker, damage, dir, weaponDef, paralyzed);
	    	} catch (Throwable e) {
	    		debug(e);
	    	}
        }
        return 0; // signaling: OK
    }

    @Override
    public int enemyDestroyed(Unit enemy, Unit attacker) {
    	for (Module module : modules) {
    		try {
    			module.enemyDestroyed(enemy, attacker);
	    	} catch (Throwable e) {
	    		debug(e);
	    	}
        }
        return 0; // signaling: OK
    }

    @Override
    public int weaponFired(Unit unit, WeaponDef weaponDef) {
    	for (Module module : modules) {
    		try {
    			module.weaponFired(unit, weaponDef);
	    	} catch (Throwable e) {
	    		debug(e);
	    	}
        }
        return 0; // signaling: OK
    }

    @Override
    public int commandFinished(Unit unit, int commandId, int commandTopicId) {
	    for (Module module : modules) {
	    	try {
	    		module.commandFinished(unit, commandId, commandTopicId);
	    	} catch (Throwable e) {
	    		debug(e);
	    	}
	    }
        return 0; // signaling: OK
    }

    @Override
    public int seismicPing(AIFloat3 pos, float strength) {
    	for (Module module : modules) {
    		try {
    			module.seismicPing(pos, strength);
	    	} catch (Throwable e) {
	    		debug(e);
	    	}
        }
        return 0; // signaling: OK
    }

	@Override
	public int release(int reason) {
		warManager = null;
		ecoManager = null;
		facManager = null;
		losManager = null;
		graphManager = null;
		modules = null;
		enemyTeams = null;
		enemyAllyTeams = null;
		allies = null;
		callback = null;
		instance = null;
		UnitClasses.release();
		Pathfinder.release();

		return 0; // signaling: OK
	}
    
    private void identifyEnemyTeams(){
    	List<Team> enemies = callback.getEnemyTeams();
		
    	for(Team i:enemies){
    		int enemyTeam = i.getTeamId();
    		int allyTeam = callback.getGame().getTeamAllyTeam(enemyTeam);
    		if (!callback.getGame().getRulesParamString("allyteam_short_name_" + allyTeam, "").equals("Neutral")) {
			    enemyTeams.add(enemyTeam);
			    if (allyTeam != this.allyTeamID) {
				    if (!enemyAllyTeams.contains(allyTeam)) {
					    enemyAllyTeams.add(allyTeam);
				    }
			    }
		    }
    	}
    	this.isFFA = enemyAllyTeams.size() > 1;
    }

	// creates a list of teams that are allied with the AI.
	private void identifyAllyTeams(){
		int teamCount = callback.getGame().getTeams();
		for (int i = 0; i < teamCount; i++){
			if (callback.getGame().getTeamAllyTeam(i) == allyTeamID && i != teamID){
				allies.add(i);
			}
		}
	}
    
    public OOAICallback getCallback(){
    	return callback;
    }
    
    public void log(String s) {
    	callback.getLog().log(s);
    }
    
    public Set<Integer> getEnemyAllyTeamIDs(){
    	return this.enemyAllyTeams;
    }
    
    public Set<Integer> getEnemyTeamIDs(){
    	return this.enemyTeams;
    }

	public void debug(Throwable ex) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		ex.printStackTrace(pw);
		String s = "exception(" + sw.toString().replace("\n", " ") + ") " + ex;
		if (currentFrame > 0) {
			say(s);
		}else{
			log(s);
		}
	}

	public void say(String s){
		callback.getGame().sendTextMessage("/say <ZKGBAI> " + s, 0);
	}

	public void emote(String s){
		callback.getGame().sendTextMessage("/say <ZKGBAI " + s + ">", 0);
	}

	private void chooseStartPos(){
		List<MetalSpot> spots = graphManager.getAllyTerritory();
		List<MetalSpot> allspots = graphManager.getMetalSpots();
		int priorityStarts = 0;

		for (int allyid: allies){
			if (allyid < teamID){
				priorityStarts++;
			}
		}

		if (!spots.isEmpty()) {
			if (allies.size() == 0 || (allies.size() > 1 && priorityStarts == 0)) { // when starting solo, pick a spot near map center
				MetalSpot best = null;
				float dist = Float.MAX_VALUE;
				for (MetalSpot ms : spots) {
					float tmpdist = distance(ms.getPos(), graphManager.getMapCenter());
					
					float tmpcost = Float.MAX_VALUE;
					for (MetalSpot m: allspots){
						if (ms.equals(m)){
							continue;
						}
						float dst = distance(m.getPos(), ms.getPos());
						if (dst < tmpcost){
							tmpcost = dst;
						}
					}
					
					if (tmpcost != Float.MAX_VALUE) {
						tmpdist += tmpcost;
					}
					
					if (tmpdist < dist) {
						best = ms;
						dist = tmpdist;
					}
				}

				AIFloat3 startpos = getDirectionalPoint(best.getPos(), graphManager.getMapCenter(), 75f);
				callback.getGame().sendStartPosition(false, startpos);
				graphManager.setStartPos(startpos);
				log("Solo Mode: Start Position Selected!");
			}else{ // when playing on teams, generate a position based on relative teamID
				AIFloat3 startPos;
				List<AIFloat3> cachedPositions = new ArrayList<>();
				AIFloat3 mapCenter = graphManager.getMapCenter();
				
				MetalSpot best = null;
				float distance = Float.MAX_VALUE;
				
				if (allies.size() > 1){
					for (MetalSpot ms : spots) {
						float tmpdist = distance(ms.getPos(), graphManager.getMapCenter());
						
						float tmpcost = Float.MAX_VALUE;
						for (MetalSpot m: allspots){
							if (ms.equals(m)){
								continue;
							}
							float dst = distance(m.getPos(), ms.getPos());
							if (dst < tmpcost){
								tmpcost = dst;
							}
						}
						
						if (tmpcost != Float.MAX_VALUE) {
							tmpdist += tmpcost;
						}
						
						if (tmpdist < distance) {
							best = ms;
							distance = tmpdist;
						}
					}
					cachedPositions.add(best.getPos());
				}
				
				distance = 0;
				
				for (MetalSpot ms: spots){
					float tmpdist = distance(ms.getPos(), mapCenter);
					
					if (tmpdist > distance){
						distance = tmpdist;
						best = ms;
					}
				}

				startPos = best.getPos();
				cachedPositions.add(startPos);
				spots.remove(best);

				if (priorityStarts > 1 || (allies.size() == 1 && priorityStarts > 0)){
					for (int i = 0; i < Math.max(1, priorityStarts - 1); i++){
						if (spots.isEmpty()) break;
						best = null;
						distance = 0;
						for (MetalSpot ms: spots){
							// for each metal spot
							float tmpdist = Float.MAX_VALUE;
							for (AIFloat3 spos: cachedPositions){
								// for each previously produced startpos,
								// find the closest one to the current metal spot
								// and use its distance as cost.

								// basically we're looking for the metal spot with the largest distance
								// from the closest previously chosen spot to it.
								float tmpclose = distance(spos, ms.getPos());
								if (tmpclose < tmpdist){
									tmpdist = tmpclose;
								}
							}
							if (tmpdist > distance){
								distance = tmpdist;
								best = ms;
							}
						}
						startPos = best.getPos();
						cachedPositions.add(startPos);
						spots.remove(best);
						if (spots.isEmpty()) break;
					}
				}

				if (best != null) {
					startPos = getDirectionalPoint(startPos, graphManager.getMapCenter(), 75f);
					callback.getGame().sendStartPosition(false, startPos);
					graphManager.setStartPos(startPos);
					log("Teams Mode: Start Position Selected!");
				}else{
					log("Teams Mode: No start positions available!");
				}
			}
		}else{
			log("chooseStartPos: Startbox inference failed, or there were no mexes within the startbox.");
		}
	}

	private void checkTeamsMerging(){
		for (int tID : allies){
			boolean inGroup = isIngroup(tID);
			if (inGroup) comrades.add(tID);
			if (inGroup && tID < teamID){
				slave = true;
				mergedAllies++;
				if (tID < mergeTarget){
					mergeTarget = tID;
				}
			}else if (inGroup) {
				mergedAllies++;
				mergedAllyID = tID;
			}else{
				unmergedAllies++;
			}
		}
	}

	private boolean isIngroup(int tId) {
		String[] script = callback.getGame().getSetupScript().split("\n");
		
		boolean parse = false;
		boolean rightID = false;
		boolean rightAI = false;
		int depth = 0;
		
		for (int line = 0; line < script.length; line++) {
			if (script[line].contains("{")){
				depth++;
			}
			
			if (script[line].contains("}")){
				depth--;
				if (parse && depth == 1){
					parse = false;
					rightID = false;
					rightAI = false;
				}
			}
			
			if (parse){
				if (script[line].startsWith("team=" + tId)){
					rightID = true;
				}
				if (script[line].startsWith("shortname=ZKGBAI")){
					rightAI = true;
				}
				if (rightID && rightAI){
					return true;
				}
			}else if (script[line].contains("[ai")){
				parse = true;
			}
		}
		return false;
	}
}
