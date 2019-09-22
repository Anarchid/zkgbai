package zkgbai.military.fighterhandlers;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.OOAICallback;
import com.springrts.ai.oo.clb.Unit;
import zkgbai.ZKGraphBasedAI;
import zkgbai.economy.EconomyManager;
import zkgbai.graph.GraphManager;
import zkgbai.graph.MetalSpot;
import zkgbai.military.Enemy;
import zkgbai.military.MilitaryManager;
import zkgbai.kgbutil.Pathfinder;
import zkgbai.military.tasks.DefenseTarget;
import zkgbai.military.tasks.ScoutTask;
import zkgbai.military.unitwrappers.Raider;
import zkgbai.military.unitwrappers.RaiderSquad;

import java.util.*;

import static zkgbai.kgbutil.KgbUtil.*;
import static java.lang.Math.*;

/**
 * Created by haplo on 1/3/2016.
 */
public class RaiderHandler {
	ZKGraphBasedAI ai;
	MilitaryManager warManager;
	RetreatHandler retreatHandler;
	GraphManager graphManager;
	EconomyManager ecoManager;

	RaiderSquad nextGlaiveSquad = null;
	RaiderSquad nextBanditSquad = null;
	RaiderSquad nextAmphSquad = null;
	RaiderSquad nextDaggerSquad = null;
	RaiderSquad nextScorcherSquad = null;
	RaiderSquad nextScytheSquad = null;
	RaiderSquad nextHalberdSquad = null;
	RaiderSquad nextPantherSquad = null;
	public List<Raider> soloRaiders = new ArrayList<>();
	public List<Raider> kodachis = new ArrayList<>();
	public Map<Integer, Raider> smallRaiders = new HashMap<>();
	public Map<Integer, Raider> mediumRaiders = new HashMap<>();
	public List<RaiderSquad> raiderSquads = new ArrayList<>();

	List<ScoutTask> scoutTasks = new ArrayList<>();
	List<ScoutTask> soloScoutTasks = new ArrayList<>();

	Pathfinder pathfinder;
	
	int pantherID;
	int halberdID;

	int frame;

	public RaiderHandler(){
		this.ai = ZKGraphBasedAI.getInstance();
		this.warManager = ai.warManager;
		this.graphManager = ai.graphManager;
		this.ecoManager = ai.ecoManager;
		this.pathfinder = Pathfinder.getInstance();
		this.retreatHandler = warManager.retreatHandler;
		
		OOAICallback callback = ai.getCallback();
		pantherID = callback.getUnitDefByName("tankheavyraid").getUnitDefId();
		halberdID = callback.getUnitDefByName("hoverassault").getUnitDefId();
	}

	public void addSmallRaider(Raider r){
		if (soloRaiders.contains(r)) return;
		String defName = r.getUnit().getDef().getName();

		smallRaiders.put(r.id, r);

		// glaive, bandit, duck/archer, dagger
		if (defName.equals("cloakraid")) {
			if (nextGlaiveSquad == null) {
				nextGlaiveSquad = new RaiderSquad();
				nextGlaiveSquad.type = 's';
				raiderSquads.add(nextGlaiveSquad);
			}
			nextGlaiveSquad.addUnit(r, frame);

			if (nextGlaiveSquad.raiders.size() >= min(12, max(6, (int) floor(ecoManager.adjustedIncome / (5f))))) {
				nextGlaiveSquad.status = 'r';
				nextGlaiveSquad = null;
			}
		}else if (defName.equals("shieldraid")) {
			if (nextBanditSquad == null) {
				nextBanditSquad = new RaiderSquad();
				nextBanditSquad.type = 's';
				raiderSquads.add(nextBanditSquad);
			}
			nextBanditSquad.addUnit(r, frame);

			if (nextBanditSquad.raiders.size() >= min(12, max(6, (int) floor(ecoManager.adjustedIncome / (5f))))) {
				nextBanditSquad.status = 'r';
				nextBanditSquad = null;
			}
		}else if (defName.equals("amphimpulse") || defName.equals("amphraid")) {
			if (nextAmphSquad == null) {
				nextAmphSquad = new RaiderSquad();
				nextAmphSquad.type = 's';
				raiderSquads.add(nextAmphSquad);
			}
			nextAmphSquad.addUnit(r, frame);

			if (nextAmphSquad.raiders.size() >= min(12, max(6, (int) floor(ecoManager.adjustedIncome / (5f))))) {
				nextAmphSquad.status = 'r';
				nextAmphSquad = null;
			}
		}else if (defName.equals("hoverraid")) {
			if (nextDaggerSquad == null) {
				nextDaggerSquad = new RaiderSquad();
				nextDaggerSquad.type = 's';
				raiderSquads.add(nextDaggerSquad);
			}
			nextDaggerSquad.addUnit(r, frame);

			if (nextDaggerSquad.raiders.size() >= min(12, max(6, (int) floor(ecoManager.adjustedIncome / (5f))))) {
				nextDaggerSquad.status = 'r';
				nextDaggerSquad = null;
			}
		}
	}

	public void addMediumRaider(Raider r) {
		mediumRaiders.put(r.id, r);

		String defName = r.getUnit().getDef().getName();

		if (defName.equals("vehraid")){
			//for scorchers
			if (nextScorcherSquad == null){
				nextScorcherSquad = new RaiderSquad();
				nextScorcherSquad.type = 'm';
				raiderSquads.add(nextScorcherSquad);
			}
			nextScorcherSquad.addUnit(r, frame);

			if (nextScorcherSquad.raiders.size() >= (int) min(8f, max(4f, floor(ecoManager.adjustedIncome / 4f)))){
				nextScorcherSquad.status = 'r';
				nextScorcherSquad = null;
			}

		}else if (defName.equals("hoverassault")){
			// for halberds
			if (nextHalberdSquad == null){
				nextHalberdSquad = new RaiderSquad();
				nextHalberdSquad.type = 'm';
				raiderSquads.add(nextHalberdSquad);
			}
			nextHalberdSquad.addUnit(r, frame);

			if (nextHalberdSquad.raiders.size() >= (int) min(8, max(2, floor(ecoManager.adjustedIncome / 8f)))){
				nextHalberdSquad.status = 'r';
				nextHalberdSquad = null;
			}
		}else if (defName.equals("cloakheavyraid")) {
			// for scythes
			if (nextScytheSquad == null) {
				nextScytheSquad = new RaiderSquad();
				nextScytheSquad.type = 'm';
				raiderSquads.add(nextScytheSquad);
			}
			nextScytheSquad.addUnit(r, frame);

			if (nextScytheSquad.raiders.size() >= (int) min(4, max(2, floor(ecoManager.adjustedIncome / 10f)))) {
				nextScytheSquad.status = 'r';
				nextScytheSquad = null;
			}
		}else if (defName.equals("tankheavyraid")) {
			// for panthers
			if (nextPantherSquad == null) {
				nextPantherSquad = new RaiderSquad();
				nextPantherSquad.type = 'm';
				raiderSquads.add(nextPantherSquad);
			}
			nextPantherSquad.addUnit(r, frame);

			if (nextPantherSquad.raiders.size() >= (int) min(8, max(4, floor(ecoManager.adjustedIncome / 5f)))) {
				nextPantherSquad.status = 'r';
				nextPantherSquad = null;
			}
		}
	}

	public void addSoloRaider(Raider r){
		if (r.getUnit().getDef().getName().equals("tankraid")){
			kodachis.add(r);
		}else {
			soloRaiders.add(r);
		}
	}

	public void removeUnit(Unit u){
		for (Raider r: soloRaiders){
			if (r.id == u.getUnitId()){
				r.clearTask();
			}
		}
		for (Raider r: kodachis){
			if (r.id == u.getUnitId()){
				r.clearTask();
			}
		}
		Raider r = new Raider(u, 0);
		soloRaiders.remove(r);
		kodachis.remove(r);
		if (smallRaiders.containsKey(u.getUnitId())){
			Raider sr = smallRaiders.get(u.getUnitId());
			if (sr.squad != null){
				sr.squad.removeUnit(sr);
			}

			if (sr.getTask() != null){
				sr.getTask().removeRaider(r);
			}
			smallRaiders.remove(u.getUnitId());
		}else if (mediumRaiders.containsKey(u.getUnitId())){
			Raider mr = mediumRaiders.get(u.getUnitId());
			if (mr.squad != null){
				mr.squad.removeUnit(mr);
			}

			if (mr.getTask() != null){
				mr.getTask().removeRaider(r);
			}
			mediumRaiders.remove(u.getUnitId());
		}
	}

	public void removeFromSquad(Unit u){
		Raider r = smallRaiders.get(u.getUnitId());
		if (r != null){
			if (r.squad != null) r.squad.removeUnit(r);
			r.clearTask();
			return;
		}
		
		r = mediumRaiders.get(u.getUnitId());
		if (r != null){
			if (r.squad != null) r.squad.removeUnit(r);
			r.clearTask();
		}
	}

	public void update(int frame){
		this.frame = frame;

		if(frame%15 == 4) {
			cleanUnits();
			createScoutTasks();
			checkScoutTasks();

			for (Raider r:smallRaiders.values()){
				if (r.squad == null && !retreatHandler.isRetreating(r.getUnit())){
					addSmallRaider(r);
				}
			}
			
			for (Raider r:mediumRaiders.values()){
				 if (r.squad == null && !retreatHandler.isRetreating(r.getUnit())){
					addMediumRaider(r);
				}
			}

			assignSoloRaiders();
			superKodachiRally();
			assignRaiderSquads();
		}
		
		// unstick raiders that are caught on buildings or wrecks
		if (frame % 30 == 1) {
			for (Raider r : smallRaiders.values()) {
				if (r.getUnit().getHealth() > 0 && !retreatHandler.isRetreating(r.getUnit())) r.unstick(frame);
			}
		}
		
		if (frame % 30 == 2) {
			for (Raider r : mediumRaiders.values()) {
				if (r.getUnit().getHealth() > 0 && !retreatHandler.isRetreating(r.getUnit())) r.unstick(frame);
			}
		}
		
		if (frame % 30 == 3) {
			for (Raider r : soloRaiders) {
				if (r.getUnit().getHealth() > 0 && !retreatHandler.isRetreating(r.getUnit())) r.unstick(frame);
			}
			for (Raider r : kodachis) {
				if (r.getUnit().getHealth() > 0 && !retreatHandler.isRetreating(r.getUnit())) r.unstick(frame);
			}
		}
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
			if (!soloScoutTasks.contains(st)){
				soloScoutTasks.add(st);
			}
		}
	}

	private void checkScoutTasks(){
		List<ScoutTask> finished = new ArrayList<ScoutTask>();
		List<ScoutTask> unreachable = new ArrayList<>();
		if (graphManager.getEnemyTerritory().isEmpty()){
			for (ScoutTask st: scoutTasks){
				if (st.spot.visible){
					st.endTask();
					finished.add(st);
				}
			}
		}else {
			for (ScoutTask st : scoutTasks) {
				if (!st.spot.hostile && !st.spot.enemyShadowed) {
					st.endTask();
					finished.add(st);
				}
			}
			Raider r = null;
			if (!soloRaiders.isEmpty()) {
				// use the raider with the highest individual threat for maxthreat and the raider with the greatest
				// all-terrain mobility for determining reachability, just to be conservative.
				float maxThreat = 0;
				float maxSlope = 0;
				for (Raider ra: soloRaiders){
					float tmpthreat = ((ra.getUnit().getPower() + ra.getUnit().getMaxHealth()) / 16f)/500f;
					if (tmpthreat > maxThreat){
						maxThreat = tmpthreat;
					}
					if (maxSlope == 2f) continue;
					if (ra.getUnit().getDef().getMoveData() != null){
						float tmpSlope = ra.getUnit().getDef().getMoveData().getMaxSlope();
						if (tmpSlope > maxSlope){
							maxSlope = tmpSlope;
							r = ra;
						}
					}else if (ra.getUnit().getHealth() > 0){
						maxSlope = 2f;
						r = ra;
					}
				}

				if (r != null) {
					for (ScoutTask st : soloScoutTasks) {
						if (!pathfinder.isRaiderReachable(r.getUnit(), st.target, maxThreat)) unreachable.add(st);
					}
				}
			}
		}
		scoutTasks.removeAll(finished);
		soloScoutTasks.removeAll(finished);
		soloScoutTasks.removeAll(unreachable);
	}

	private float getScoutCost(ScoutTask task,  Raider raider){
		float cost = distance(task.target, raider.getPos());
		if (task.spot.hostile){
			cost /= 3f + Math.max(1f, ((frame - task.spot.getLastSeen()) / 900f));
		}else {
			cost /= Math.max(task.spot.enemyShadowed ? 2f : 1f, ((frame - task.spot.getLastSeen()) / 900f));
			cost += Math.max(0, 4 * (900 - (frame - task.spot.getLastSeen())));

			// disprefer scouting the same non-hostile spot with more than one raider.
			if (!task.assignedRaiders.isEmpty() && !task.assignedRaiders.contains(raider)){
				cost += 2000f;
			}
		}

		return cost;
	}
	
	private void assignSoloRaiders(){
		for (Raider r: soloRaiders){
			if (r.getUnit().isBeingBuilt() || r.getUnit().getHealth() <= 0 || r.getUnit().getTeam() != ai.teamID || retreatHandler.isRetreating(r.getUnit())){
				continue;
			}
			
			AIFloat3 pos = r.getPos();
			boolean overThreat = (warManager.getEffectiveThreat(pos) > 0 || warManager.getRiotThreat(pos) > 0
				                        || (r.target != null && (warManager.getRiotThreat(r.target) > 0 || warManager.getThreat(r.target) > warManager.getFriendlyThreat(pos))));
			if (overThreat){
				// try to keep raiders from suiciding.
				r.sneak(graphManager.getClosestRaiderHaven(pos), frame);
				continue;
			}
			
			ScoutTask bestTask = null;
			AIFloat3 bestTarget = null;
			float cost = Float.MAX_VALUE;
			boolean porc = true;
			
			for (ScoutTask s:scoutTasks){
				if (warManager.getRiotThreat(s.target) > 0 || warManager.getEffectiveThreat(s.target) > warManager.getFriendlyThreat(r.getPos())){
					continue;
				}
				
				float tmpcost = getScoutCost(s, r);
				if (tmpcost < cost){
					cost = tmpcost;
					bestTask = s;
				}
			}
			
			for (DefenseTarget d: warManager.sniperSightings){
				if (warManager.getRiotThreat(d.position) > 0) continue;
				float tmpcost = distance(r.getPos(), d.position);
				tmpcost = (tmpcost/4)-100;
				
				if (tmpcost < cost){
					cost = tmpcost;
					bestTarget = d.position;
				}
			}
			
			for (Enemy e: warManager.getTargets()){
				if (r.getUnit().getDef().getName().equals("gunshipbomb")){
					break;
				}
				if (e.isRiot || warManager.getRiotThreat(e.position) > 0 || warManager.getEffectiveThreat(e.position) > warManager.getFriendlyThreat(r.getPos())){
					continue;
				}
				
				float tmpcost = distance(r.getPos(), e.position);
				if (!e.isArty && !e.isWorker && !e.isStatic && !e.isAA){
					tmpcost += 1000;
				}
				if (e.isWorker){
					tmpcost = (tmpcost/4)-100;
					tmpcost += 750f * warManager.getThreat(e.position);
				}
				if (e.isPorc){
					tmpcost = (tmpcost/4)-100;
					tmpcost += 500f * warManager.getThreat(e.position);
				}
				
				if (tmpcost < cost){
					cost = tmpcost;
					bestTarget = e.position;
					porc = e.isStatic;
				}
			}
			
			if (bestTarget != null){
				r.clearTask();
				
				if (distance(bestTarget, r.getPos()) > 500 || porc) {
					r.sneak(bestTarget, frame);
				} else {
					r.raid(bestTarget, frame);
				}
			}else if (bestTask != null){
				if (r.getTask() == null || !r.getTask().equals(bestTask)) {
					r.clearTask();
					bestTask.addRaider(r);
					r.setTask(bestTask);
				}
				
				if (distance(bestTask.target, r.getPos()) > 500) {
					r.sneak(bestTask.target, frame);
				} else {
					r.raid(bestTask.target, frame);
				}
			}else{
				r.clearTask();
				r.sneak(warManager.getRaiderRally(r.getPos()), frame);
			}
		}
	}
	
	private void superKodachiRally(){
		for (Raider r: kodachis){
			if (r.getUnit().isBeingBuilt() || r.getUnit().getHealth() <= 0 || r.getUnit().getTeam() != ai.teamID || retreatHandler.isRetreating(r.getUnit())){
				continue;
			}
			
			boolean reloading = r.isReloading(frame);
			AIFloat3 pos = r.getPos();
			boolean overThreat = (warManager.getEffectiveThreat(pos) > 1f || warManager.getRiotThreat(pos) > 0
				                        || (r.target != null && (warManager.getRiotThreat(r.target) > 0 || warManager.getThreat(r.target) > (reloading ? 0 : 1f))));
			if (overThreat){
				// try to keep raiders from suiciding.
				r.sneak(graphManager.getClosestRaiderHaven(pos), frame);
				continue;
			}
			
			ScoutTask bestTask = null;
			AIFloat3 bestTarget = null;
			float cost = Float.MAX_VALUE;
			boolean porc = true;
			
			for (ScoutTask s:scoutTasks){
				if (warManager.getRiotThreat(s.target) > 0 || warManager.getThreat(s.target) > (reloading ? 0 : 1f)){
					continue;
				}
				
				float tmpcost = getScoutCost(s, r);
				if (tmpcost < cost){
					cost = tmpcost;
					bestTask = s;
				}
			}
			
			for (DefenseTarget d: warManager.sniperSightings){
				if (warManager.getRiotThreat(d.position) > 0) continue;
				float tmpcost = distance(r.getPos(), d.position);
				tmpcost = (tmpcost/4)-100;
				
				if (tmpcost < cost){
					cost = tmpcost;
					bestTarget = d.position;
				}
			}
			
			for (Enemy e: warManager.getTargets()){
				if (((reloading || !e.isMex) && warManager.getThreat(e.position) > 0) || warManager.getThreat(e.position) > 1f || warManager.getRiotThreat(e.position) > 0){
					continue;
				}
				
				float tmpcost = distance(r.getPos(), e.position);
				if (e.isWorker){
					tmpcost = (tmpcost/4)-100;
				}
				
				if (tmpcost < cost){
					cost = tmpcost;
					bestTarget = e.position;
					porc = e.isStatic;
				}
			}
			
			if (bestTarget != null){
				r.clearTask();
				
				if (distance(bestTarget, r.getPos()) > 500 || porc) {
					r.sneak(bestTarget, frame);
				} else {
					r.raid(bestTarget, frame);
				}
			}else if (bestTask != null){
				if (r.getTask() == null || !r.getTask().equals(bestTask)) {
					r.clearTask();
					bestTask.addRaider(r);
					r.setTask(bestTask);
				}
				
				if (distance(bestTask.target, r.getPos()) > 500) {
					r.sneak(bestTask.target, frame);
				} else {
					r.raid(bestTask.target, frame);
				}
			}else{
				r.clearTask();
				r.sneak(warManager.getRaiderRally(r.getPos()), frame);
			}
		}
	}

	private void assignRaiderSquads(){
		List<RaiderSquad> deadSquads = new ArrayList<RaiderSquad>();
		for (RaiderSquad rs:raiderSquads){
			if (rs.isDead()){
				if (rs.status != 'f') deadSquads.add(rs);
				continue;
			}
			
			AIFloat3 pos = rs.getPos();
			if (rs.status == 'f'){
				// Assign forming squads.
				boolean overThreat = (warManager.getEffectiveThreat(pos) > 0 || warManager.getThreat(pos) > warManager.availableMobileThreat
					                        || warManager.getPorcThreat(pos) > 0 || warManager.getRiotThreat(pos) > 0
					                        || (rs.target != null && (warManager.getPorcThreat(rs.target) > 0 || warManager.getRiotThreat(rs.target) > 0
						                                                    || warManager.getThreat(rs.target) > warManager.availableMobileThreat)) );
				if (!overThreat) {
					rs.raid(warManager.getRaiderRally(pos), frame);
				}else{
					rs.sneak(graphManager.getClosestHaven(pos), frame);
				}
			}else if (rs.status == 'r'){
				// Rally rallying squads.
				rs.sneak(graphManager.getClosestHaven(pos), frame);
				if (rs.isRallied(frame)) rs.status = 'a';
			}else{
				// Get targets for actively raiding squads.
				float threat = rs.getThreat()/500f;
				boolean overThreat = (!rs.leader.getUnit().isCloaked()
					                        && (warManager.getThreat(pos) > threat || warManager.getRiotThreat(pos) > 0 || warManager.getRiotThreat(rs.target) > 0 || warManager.getThreat(rs.target) > threat));
				if (overThreat){
					// try to keep raider squads from suiciding.
					rs.sneak(graphManager.getClosestRaiderHaven(pos), frame);
					continue;
				}
				
				// for ready squads, assign to a target
				ScoutTask bestTask = null;
				AIFloat3 bestTarget = null;
				float cost = Float.MAX_VALUE;
				
				for (ScoutTask s:scoutTasks){
					if (warManager.getRiotThreat(s.target) > 0 || warManager.getThreat(s.target) > threat || !pathfinder.isRaiderReachable(rs.leader.getUnit(), s.target, threat)){
						continue;
					}
					float tmpcost = getScoutCost(s, rs.leader);
					if (tmpcost < cost){
						cost = tmpcost;
						bestTask = s;
					}
				}

				for (DefenseTarget d: warManager.sniperSightings){
					if (warManager.getRiotThreat(d.position) > 0 || warManager.getThreat(d.position) > warManager.availableMobileThreat) continue;
					float tmpcost = distance(pos, d.position);
					tmpcost = (tmpcost/4)-100;

					if (tmpcost < cost){
						cost = tmpcost;
						bestTarget = d.position;
					}
				}
				
				boolean porc = false;
				for (Enemy e: warManager.getTargets()){
					if (e.isRiot || warManager.getRiotThreat(e.position) > 0 || warManager.getThreat(e.position) > threat || !e.identified){
						continue;
					}

					float tmpcost = distance(pos, e.position);
					if (!e.isArty && !e.isWorker && !e.isStatic && (!e.isAA || e.ud.getName().equals("turretaalaser"))){
						tmpcost += 1000;
					}
					if (e.isWorker){
						tmpcost = (tmpcost/4)-100;
						tmpcost += 750f * warManager.getThreat(e.position);
					}

					if (e.isStatic && e.getDanger() > 0f){
						tmpcost = (tmpcost/4)-100;
						tmpcost += 500f * warManager.getThreat(e.position);
					}

					if (tmpcost < cost){
						cost = tmpcost;
						bestTarget = e.position;
						porc = e.isStatic;
					}
				}

				if (bestTarget != null){
					if (rs.leader.getTask() != null) {
						rs.leader.clearTask();
					}
					if (distance(bestTarget, pos) > 400 || porc) {
						rs.sneak(bestTarget, frame);
					}else{
						rs.raid(bestTarget, frame);
					}
				}else if (bestTask != null){
					if (rs.leader.getTask() == null || !rs.leader.getTask().equals(bestTask)) {
						rs.leader.clearTask();
						bestTask.addRaider(rs.leader);
						rs.leader.setTask(bestTask);
					}

					rs.sneak(bestTask.target, frame);
				}else{
					rs.leader.clearTask();
					rs.sneak(warManager.getRaiderRally(pos), frame);
				}
			}
		}
		raiderSquads.removeAll(deadSquads);
	}

	public void avoidEnemies(Unit h, Unit attacker, AIFloat3 dir){
		if (smallRaiders.containsKey(h.getUnitId())) {
			Raider r = smallRaiders.get(h.getUnitId());
			if (h.getHealth() / h.getMaxHealth() < 0.8 && attacker != null && attacker.getMaxSpeed() > 0 && warManager.getEffectiveThreat(h.getPos()) <= 0) {
				float movdist = -100;
				if (r.getUnit().getDef().getName().equals("hoverraid")) {
					movdist = -450;
				}
				float x = movdist * dir.x;
				float z = movdist * dir.z;
				AIFloat3 pos = h.getPos();
				AIFloat3 target = new AIFloat3();
				target.x = pos.x + x;
				target.z = pos.z + z;
				h.moveTo(target, (short) 0, frame);
			}
		}
	}

	private void cleanUnits(){
		List<Integer> invalidFighters = new ArrayList<Integer>();
		for (Raider r:smallRaiders.values()){
			if (r.getUnit().getHealth() <= 0 || r.getUnit().getTeam() != ai.teamID){
				r.clearTask();
				invalidFighters.add(r.id);
			}
		}
		for (Integer key:invalidFighters){
			smallRaiders.remove(key);
		}
		invalidFighters.clear();

		for (Raider r:mediumRaiders.values()){
			if (r.getUnit().getHealth() <= 0 || r.getUnit().getTeam() != ai.teamID){
				r.clearTask();
				invalidFighters.add(r.id);
			}
		}
		for (Integer key:invalidFighters){
			mediumRaiders.remove(key);
		}
		invalidFighters.clear();

		List<Raider> invalidRaiders = new ArrayList<>();
		for (Raider r:soloRaiders){
			if (r.getUnit().getHealth() <= 0 || r.getUnit().getTeam() != ai.teamID){
				invalidRaiders.add(r);
				r.clearTask();
			}
		}
		soloRaiders.removeAll(invalidRaiders);
		
		invalidRaiders.clear();
		for (Raider r:kodachis){
			if (r.getUnit().getHealth() <= 0 || r.getUnit().getTeam() != ai.teamID){
				invalidRaiders.add(r);
				r.clearTask();
			}
		}
		kodachis.removeAll(invalidRaiders);
	}
}