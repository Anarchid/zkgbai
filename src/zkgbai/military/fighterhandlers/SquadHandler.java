package zkgbai.military.fighterhandlers;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import zkgbai.ZKGraphBasedAI;
import zkgbai.economy.EconomyManager;
import zkgbai.economy.tasks.CombatReclaimTask;
import zkgbai.economy.tasks.ReclaimTask;
import zkgbai.graph.GraphManager;
import zkgbai.military.MilitaryManager;
import zkgbai.military.UnitClasses;
import zkgbai.military.unitwrappers.Fighter;
import zkgbai.military.unitwrappers.HoverSquad;
import zkgbai.military.unitwrappers.ShieldSquad;
import zkgbai.military.unitwrappers.Squad;

import java.util.*;

import static java.lang.Math.floor;
import static java.lang.Math.min;
import static zkgbai.kgbutil.KgbUtil.*;

/**
 * Created by haplo on 1/4/2016.
 */
public class SquadHandler {
    ZKGraphBasedAI ai;
    MilitaryManager warManager;
    EconomyManager ecoManager;
    GraphManager graphManager;

    RetreatHandler retreatHandler;

    UnitClasses unitTypes;

    public java.util.Map<Integer, Fighter> fighters = new HashMap<Integer, Fighter>();
    public Squad nextCloakySquad = null;
	public Squad nextShieldSquad = null;
    public Squad nextAmphSquad = null;
	public Squad nextRecluseSquad = null;
    public Squad nextSpiderSquad = null;
    public Squad nextVehSquad = null;
	public HoverSquad nextHalberdSquad = null;
    public Squad nextHoverSquad = null;
    public Squad nextTankSquad = null;
    public Squad nextAirSquad = null;
    public Squad nextBrawlerSquad = null;
    public Deque<ShieldSquad> shieldSquads = new LinkedList<>();
    public Deque<Squad> squads = new LinkedList<>();
	public Deque<Squad> airSquads = new LinkedList<>();

    int frame = 0;
    int squadCounter = 0;

    public SquadHandler(){
        this.ai = ZKGraphBasedAI.getInstance();
        this.warManager = ai.warManager;
        this.ecoManager = ai.ecoManager;
        this.graphManager = ai.graphManager;

        this.retreatHandler = warManager.retreatHandler;
        this.unitTypes = UnitClasses.getInstance();
    }

    public void update(int frame){
        this.frame = frame;

        if (frame % 30 == (ai.offset + 7) % 30){
	        List<Integer> invalidFighters = new ArrayList<Integer>();
            for (Fighter f:fighters.values()){
                if (f.isDead()){
	                invalidFighters.add(f.id);
                }else{
                	boolean retreating = retreatHandler.isRetreating(f.getUnit());
	                if (!retreating && !f.getUnit().getDef().isAbleToFly() && f.isStuck(frame)){
		                if (f.squad != null) f.squad.removeUnit(f);
		                invalidFighters.add(f.id);
		                ai.ecoManager.combatReclaimTasks.add(new CombatReclaimTask(f.getUnit()));
		                warManager.retreatHandler.removeUnit(f.getUnit());
	                }else if (f.squad == null && !retreating) {
		                if (unitTypes.assaults.contains(f.getUnit().getDef().getName())){
			                addAssault(f);
		                }else{
			                addShieldMob(f);
		                }
	                }
                }
            }
	        for (Integer key:invalidFighters){
		        fighters.remove(key);
	        }
        }
	    if (frame % 7 == ai.offset % 7) assignSquads();
	    if (frame % 7 == (ai.offset + 4) % 7) assignShieldSquads();
	    if (frame % 14 == (ai.offset + 9) % 14) assignAirSquads();
    }

    public void addAssault(Fighter f){
        fighters.put(f.id, f);
        String defName = f.getUnit().getDef().getName();

        if (defName.startsWith("cloak")) {
	        // create a new squad if there isn't one
	        if (nextCloakySquad == null) {
		        nextCloakySquad = new Squad();
		        nextCloakySquad.income = ecoManager.effectiveIncome / (1f + (ai.mergedAllies * graphManager.territoryFraction * graphManager.territoryFraction));
		        squads.add(nextCloakySquad);
	        }
	
	        nextCloakySquad.addUnit(f);
	
	        if (((nextCloakySquad.metalValue > nextCloakySquad.income * 45f && nextCloakySquad.metalValue > 1000f) || nextCloakySquad.metalValue > 2500f) && nextCloakySquad.getSize() > 2) {
		        nextCloakySquad.status = 'r';
		        nextCloakySquad = null;
	        }
        }else if (defName.startsWith("shield")){
	        // create a new squad if there isn't one
	        if (nextShieldSquad == null) {
		        nextShieldSquad = new Squad();
		        nextShieldSquad.income = ecoManager.effectiveIncome / (1f + (ai.mergedAllies * graphManager.territoryFraction * graphManager.territoryFraction));
		        squads.add(nextShieldSquad);
	        }
	
	        nextShieldSquad.addUnit(f);
	
	        if (((nextShieldSquad.metalValue > nextShieldSquad.income * 45f && nextShieldSquad.metalValue > 1000f) || nextShieldSquad.metalValue > 2500f) && nextShieldSquad.getSize() > 2) {
		        nextShieldSquad.status = 'r';
		        nextShieldSquad = null;
	        }
        }else if (defName.startsWith("amph")){
	        // create a new squad if there isn't one
	        if (nextAmphSquad == null) {
		        nextAmphSquad = new Squad();
		        nextAmphSquad.income = ecoManager.effectiveIncome / (1f + (ai.mergedAllies * graphManager.territoryFraction * graphManager.territoryFraction));
		        squads.add(nextAmphSquad);
	        }
	
	        nextAmphSquad.addUnit(f);
	
	        if (((nextAmphSquad.metalValue > nextAmphSquad.income * 45f && nextAmphSquad.metalValue > 1000f) || nextAmphSquad.metalValue > 2500f) && nextAmphSquad.getSize() > 2) {
		        nextAmphSquad.status = 'r';
		        nextAmphSquad = null;
	        }
        }else if (defName.equals("spiderskirm")){
	        // create a new squad if there isn't one
	        if (nextRecluseSquad == null) {
		        nextRecluseSquad = new Squad();
		        nextRecluseSquad.income = ecoManager.effectiveIncome / (1f + (ai.mergedAllies * graphManager.territoryFraction * graphManager.territoryFraction));
		        squads.add(nextRecluseSquad);
	        }
	
	        nextRecluseSquad.addUnit(f);
	
	        if (((nextRecluseSquad.metalValue > nextRecluseSquad.income * 45f && nextRecluseSquad.metalValue > 1000f) || nextRecluseSquad.metalValue > 2500f) && nextRecluseSquad.getSize() > 2) {
		        nextRecluseSquad.status = 'r';
		        nextRecluseSquad = null;
	        }
        }else if (defName.startsWith("spider")){
	        // create a new squad if there isn't one
	        if (nextSpiderSquad == null) {
		        nextSpiderSquad = new Squad();
		        nextSpiderSquad.income = ecoManager.effectiveIncome / (1f + (ai.mergedAllies * graphManager.territoryFraction * graphManager.territoryFraction));
		        squads.add(nextSpiderSquad);
	        }
	
	        nextSpiderSquad.addUnit(f);
	
	        if (((nextSpiderSquad.metalValue > nextSpiderSquad.income * 45f && nextSpiderSquad.metalValue > 1000f) || nextSpiderSquad.metalValue > 2500f) && nextSpiderSquad.getSize() > 2) {
		        nextSpiderSquad.status = 'r';
		        nextSpiderSquad = null;
	        }
        }else if (defName.startsWith("veh")){
	        // create a new squad if there isn't one
	        if (nextVehSquad == null) {
		        nextVehSquad = new Squad();
		        nextVehSquad.income = ecoManager.effectiveIncome / (1f + (ai.mergedAllies * graphManager.territoryFraction * graphManager.territoryFraction));
		        squads.add(nextVehSquad);
	        }
	
	        nextVehSquad.addUnit(f);
	
	        if (((nextVehSquad.metalValue > nextVehSquad.income * 45f && nextVehSquad.metalValue > 1000f) || nextVehSquad.metalValue > 2500f) && nextVehSquad.getSize() > 2) {
		        nextVehSquad.status = 'r';
		        nextVehSquad = null;
	        }
        }else if (defName.equals("hoverassault")){
	        // create a new squad if there isn't one
	        if (nextHalberdSquad == null) {
		        nextHalberdSquad = new HoverSquad();
		        nextHalberdSquad.income = ecoManager.effectiveIncome / (1f + (ai.mergedAllies * graphManager.territoryFraction * graphManager.territoryFraction));
		        squads.add(nextHalberdSquad);
	        }
	
	        nextHalberdSquad.addUnit(f);
	
	        if (nextHalberdSquad.getSize() >= (int) min(8, 2 + floor(ecoManager.adjustedIncome / 15f))) {
		        nextHalberdSquad.status = 'r';
		        nextHalberdSquad = null;
	        }
        }else if (defName.startsWith("hover")){
	        // create a new squad if there isn't one
	        if (nextHoverSquad == null) {
		        nextHoverSquad = new Squad();
		        nextHoverSquad.income = ecoManager.effectiveIncome / (1f + (ai.mergedAllies * graphManager.territoryFraction * graphManager.territoryFraction));
		        squads.add(nextHoverSquad);
	        }
	
	        nextHoverSquad.addUnit(f);
	
	        if (((nextHoverSquad.metalValue > nextHoverSquad.income * 45f && nextHoverSquad.metalValue > 1000f) || nextHoverSquad.metalValue > 2500f) && nextHoverSquad.getSize() > 2) {
		        nextHoverSquad.status = 'r';
		        nextHoverSquad = null;
	        }
        }else if (defName.startsWith("tank")){
	        // create a new squad if there isn't one
	        if (nextTankSquad == null) {
		        nextTankSquad = new Squad();
		        nextTankSquad.income = ecoManager.effectiveIncome / (1f + (ai.mergedAllies * graphManager.territoryFraction * graphManager.territoryFraction));
		        squads.add(nextTankSquad);
	        }
	
	        nextTankSquad.addUnit(f);
	
	        if (((nextTankSquad.metalValue > nextTankSquad.income * 45f && nextTankSquad.metalValue > 1000f) || nextTankSquad.metalValue > 2500f) && nextTankSquad.getSize() > 2) {
		        nextTankSquad.status = 'r';
		        nextTankSquad = null;
	        }
        }else if (f.getUnit().getDef().getName().equals("gunshipheavyskirm")){
	        // for brawlers/nimbus
	        if (nextBrawlerSquad == null) {
		        nextBrawlerSquad = new Squad();
		        nextBrawlerSquad.income = ecoManager.effectiveIncome / (ai.mergedAllies == 0 ? 1 : 2);
		        nextBrawlerSquad.isAirSquad = true;
		        airSquads.add(nextBrawlerSquad);
	        }
	
	        nextBrawlerSquad.addUnit(f);
	
	        if (nextBrawlerSquad.metalValue > nextBrawlerSquad.income * 45) {
		        nextBrawlerSquad.status = 'r';
		        nextBrawlerSquad = null;
	        }
        }else {
	        // for rapier/revenant
	        if (nextAirSquad == null) {
		        nextAirSquad = new Squad();
		        nextAirSquad.income = ecoManager.effectiveIncome / (ai.mergedAllies == 0 ? 1f : 2f);
		        nextAirSquad.isAirSquad = true;
		        airSquads.add(nextAirSquad);
	        }
	
	        nextAirSquad.addUnit(f);
	
	        if (nextAirSquad.metalValue > nextAirSquad.income * 45f) {
		        nextAirSquad.status = 'r';
		        nextAirSquad = null;
	        }
        }
    }
	
	public void addShieldMob(Fighter f){
		List<ShieldSquad> dead = new ArrayList<>();
		List<Fighter> fit = new ArrayList<>();
		for (ShieldSquad s:shieldSquads){
			if (s.isDead() || s.numFelons == 0){
				// disband felonless squads and repurpose the units as regular mobs until a new felon appears.
				fit.addAll(s.disband());
				dead.add(s);
			}
		}
		shieldSquads.removeAll(dead);
		for (Fighter fi: fit){
			assignShieldMob(fi);
		}
		
		assignShieldMob(f);
		fighters.put(f.id, f);
	}
	
	private void assignShieldMob(Fighter f){
		if (f.getUnit().getDef().getName().equals("shieldfelon") && (ai.mergedAllies == 0 || shieldSquads.isEmpty())) {
			// Create a new shieldball for each felon, unless on teams where you want just one giant death ball.
			retreatHandler.removeUnit(f.getUnit());
			ShieldSquad s = new ShieldSquad();
			s.status = 'a';
			shieldSquads.add(s);
			s.addUnit(f);
			callShieldMobs(s);
		}else if (f.getUnit().getDef().getName().equals("shieldshield") && !shieldSquads.isEmpty()) {
			// Assign aspis to the squad with the fewest aspis, giving preference to higher metal value shieldballs
			// where they do the most good and are more likely to survive.
			retreatHandler.removeUnit(f.getUnit());
			ShieldSquad needed = null;
			float metalv = Float.MAX_VALUE;
			int minAspis = Integer.MAX_VALUE;
			for (ShieldSquad s: shieldSquads){
				if (s.numAspis < minAspis
					      || (s.numAspis == minAspis && s.metalValue > metalv)){
					needed = s;
					metalv = s.metalValue;
					minAspis = s.numAspis;
				}
			}
			needed.addUnit(f);
		}else {
			// For other units.
			if (shieldSquads.isEmpty()) {
				// If there's no felon, treat shield mobs as regular assaults.
				retreatHandler.addCoward(f.getUnit());
				addAssault(f);
			}else{
				// Add the unit to the squad with the lowest value.
				retreatHandler.removeUnit(f.getUnit());
				ShieldSquad needed = null;
				float metalv = Float.MAX_VALUE;
				for (ShieldSquad s: shieldSquads){
					if (s.metalValue < metalv){
						needed = s;
						metalv = s.metalValue;
					}
				}
				needed.addUnit(f);
			}
		}
	}

    private void callShieldMobs(ShieldSquad s){
        // Call a flash mob when a felon appears.
        for (Fighter f: fighters.values()){
            if (!f.shieldMob || f.squad instanceof ShieldSquad || retreatHandler.isRetreating(f.getUnit()) || f.getUnit().getHealth() <= 0 || f.getUnit().getTeam() != ai.teamID) continue;
            if (f.squad != null) {
                f.squad.removeUnit(f);
                f.squad = null;
            }
            retreatHandler.removeUnit(f.getUnit());
            s.addUnit(f);
        }
    }

    public void removeUnit(Unit u){
            Fighter f = fighters.get(u.getUnitId());
            if (f != null && f.squad != null) {
	            f.squad.removeUnit(f);
	            fighters.remove(f.id);
            }
    }

    public void removeFromSquad(Unit u){
	    Fighter f = fighters.get(u.getUnitId());
        if (f != null && f.squad != null) f.squad.removeUnit(f);
    }
	
	private void assignSquads(){
		// Assign up to one squad at a time, up to four per second.
		while (!squads.isEmpty()){
			Squad s = squads.poll();
			if (s.isDead()){
				if (s.status == 'f'){
					squads.add(s);
					return;
				}
				continue;
			}else if (frame - s.lastAssignmentFrame < 30){
				squads.push(s);
				return;
			}else{
				assignSquad(s);
				s.lastAssignmentFrame = frame;
				squads.add(s);
				return;
			}
		}
	}
	
	private void assignAirSquads(){
		// Assign up to one air squad at a time, up to two per second.
		while (!airSquads.isEmpty()){
			Squad s = airSquads.poll();
			if (s.isDead()){
				if (s.status == 'f'){
					airSquads.add(s);
					return;
				}
				continue;
			}else if (frame - s.lastAssignmentFrame < 60){
				airSquads.push(s);
				return;
			}else{
				assignAirSquad(s);
				s.lastAssignmentFrame = frame;
				airSquads.add(s);
				return;
			}
		}
	}

    private void assignSquad(Squad s){
        AIFloat3 pos = s.getPos();

        if (s.status == 'f'){
            if (warManager.getThreat(pos) > 2f * warManager.getTotalFriendlyThreat(pos) || warManager.getPorcThreat(pos) > 0 || (s.target != null && warManager.getPorcThreat(s.target) > 0)) {
	            s.retreatTo(graphManager.getClosestHaven(pos));
            }else {
	            s.setTarget(warManager.getRallyPoint(pos));
            }
        }else if (s.status == 'r'){
            // set rallying for squads that are finished forming and gathering to attack
            s.setTarget(warManager.getRallyPoint(pos));
            if (s.isRallied(frame)){
                s.status = 'a';
            }
        }else if (s.status == 'a'){
            if (warManager.getEffectiveThreat(pos) > 0 && (!graphManager.isAllyTerritory(pos) || warManager.getPorcThreat(pos) > 0)){
                s.retreatTo(graphManager.getClosestHaven(pos));
            }
            
            Unit leader = s.getLeader();

            AIFloat3 target = warManager.getTarget(leader, true);
            s.setTarget(target);
        }
    }
    
    private void assignAirSquad(Squad s){
	    AIFloat3 pos = s.getPos();
	
	    if (s.status == 'f'){
		    AIFloat3 target = warManager.getAirRallyPoint(pos);
		    if (distance(pos, target) > 1800f){
			    s.retreatTo(target);
		    }else {
			    s.setTarget(target);
		    }
	    }else if (s.status == 'r'){
		    // set rallying for squads that are finished forming and gathering to attack
		    s.setTarget(warManager.getAirRallyPoint(pos));
		    if (s.isRallied(frame)){
			    s.status = 'a';
		    }
	    }else if (s.status == 'a'){
		    if (warManager.getEffectiveAAThreat(pos) > 0){
			    s.retreatTo(graphManager.getClosestAirHaven(pos));
		    }
		
		    Unit leader = s.getLeader();
		
		    AIFloat3 target = warManager.getAirTarget(leader, true);
		    
		    if (distance(target, pos) > 1800f){
			    s.retreatTo(target);
		    }else {
			    s.setTarget(target);
		    }
	    }
    }
	
	private void assignShieldSquads(){
		// Process up to one shield squad at a time, four per second.
		while (!shieldSquads.isEmpty()) {
			ShieldSquad shieldSquad = shieldSquads.poll();
			
			if (shieldSquad.isDead() || shieldSquad.numFelons == 0){
				// disband felonless squads and assign them to other felons if available, else dump them back as assaults.
				List<Fighter> fit = new ArrayList<>();
				fit.addAll(shieldSquad.disband());
				for (Fighter fi: fit){
					assignShieldMob(fi);
				}
				continue;
			}
			
			if (frame - shieldSquad.lastAssignmentFrame < 30){
				shieldSquads.push(shieldSquad);
				return;
			}
			
			if (shieldSquad.lowShields && shieldSquad.getShields() > 0.5f){
				shieldSquad.lowShields = false;
			}else if (!shieldSquad.lowShields && shieldSquad.getShields() < 0.2f){
				shieldSquad.lowShields = true;
			}
			
			if (shieldSquad.lowShields){
				shieldSquad.retreatTo(graphManager.getClosestRaiderHaven(shieldSquad.getPos()));
			}else if (shieldSquad.getHealth() < 0.85f) {
				shieldSquad.retreatTo(graphManager.getClosestHaven(shieldSquad.getPos()));
			} else if (shieldSquad.metalValue > 1300f && shieldSquad.leader.getUnit().getDef().getName().equals("shieldfelon") && shieldSquad.isRallied(frame)) {
				AIFloat3 target = warManager.getTarget(shieldSquad.leader.getUnit(), false);
				shieldSquad.setTarget(target);
			} else if (warManager.getEffectiveThreat(shieldSquad.getPos()) > 0f || warManager.getPorcThreat(shieldSquad.getPos()) > 0f) {
				shieldSquad.retreatTo(graphManager.getClosestHaven(shieldSquad.getPos()));
			} else {
				shieldSquad.setTarget(warManager.getRallyPoint(shieldSquad.getPos()));
			}
			shieldSquad.lastAssignmentFrame = frame;
			shieldSquads.add(shieldSquad);
			return;
		}
	}
}
