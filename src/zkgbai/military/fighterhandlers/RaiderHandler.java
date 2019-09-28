package zkgbai.military.fighterhandlers;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.OOAICallback;
import com.springrts.ai.oo.clb.Unit;
import zkgbai.ZKGraphBasedAI;
import zkgbai.economy.EconomyManager;
import zkgbai.economy.tasks.CombatReclaimTask;
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
	public Deque<Raider> soloRaiders = new LinkedList<>();
	public List<Raider> kodachis = new ArrayList<>();
	public Map<Integer, Raider> smallRaiders = new HashMap<>();
	public Map<Integer, Raider> mediumRaiders = new HashMap<>();
	public Deque<RaiderSquad> raiderSquads = new LinkedList<>();

	List<ScoutTask> scoutTasks = new ArrayList<>();
	List<ScoutTask> soloScoutTasks = new ArrayList<>();

	Pathfinder pathfinder;
	
	int pantherID;
	int halberdID;
	
	int lastDamagedID = 0;
	Raider lastDamagedRaider = null;

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
		}else if (defName.equals("amphbomb") || defName.equals("amphraid")) {
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

		if(frame % 15 == (ai.offset + 4) % 15) {
			List<Integer> invalidFighters = new ArrayList<Integer>();
			for (Raider r:smallRaiders.values()){
				if (r.isDead()){
					r.clearTask();
					invalidFighters.add(r.id);
				}else if (r.squad == null && !retreatHandler.isRetreating(r.getUnit())){
					addSmallRaider(r);
				}
			}
			
			for (Integer key:invalidFighters){
				smallRaiders.remove(key);
			}
			invalidFighters.clear();
			
			for (Raider r:mediumRaiders.values()){
				if (r.isDead()){
					r.clearTask();
					invalidFighters.add(r.id);
				}else if (r.squad == null && !retreatHandler.isRetreating(r.getUnit())){
					addMediumRaider(r);
				}
			}
			
			for (Integer key:invalidFighters){
				mediumRaiders.remove(key);
			}
			
			List<Raider> invalidRaiders = new ArrayList<>();
			for (Raider r:kodachis){
				if (r.getUnit().getHealth() <= 0 || r.getUnit().getTeam() != ai.teamID){
					invalidRaiders.add(r);
					r.clearTask();
				}
			}
			kodachis.removeAll(invalidRaiders);
			
			createScoutTasks();
			checkScoutTasks();
			
			superKodachiRally();
		}
		
		if (frame % 3 == ai.offset % 3) assignRaiderSquads();
		
		if (frame % 2 == ai.offset % 2) assignSoloRaiders();
		
		// unstick raiders that are caught on buildings or wrecks
		if (frame % 30 == (ai.offset + 1) % 30) {
			for (Raider r : smallRaiders.values()) {
				if (!r.isDead()) r.unstick(frame);
			}
		}
		
		if (frame % 30 == (ai.offset + 5) % 30) {
			List<Integer> stuckRaiders = new ArrayList<>();
			for (Raider r : mediumRaiders.values()) {
				if (!r.isDead() && r.squad != null && r.squad.status != 'f'){
					if (r.unstick(frame)){
						if (r.squad != null) r.squad.removeUnit(r);
						stuckRaiders.add(r.id);
						ai.ecoManager.combatReclaimTasks.add(new CombatReclaimTask(r.getUnit()));
						warManager.retreatHandler.removeUnit(r.getUnit());
					}
				}
			}
			for (int id: stuckRaiders){
				mediumRaiders.remove(id);
			}
		}
		
		if (frame % 30 == (ai.offset + 8) % 30) {
			for (Raider r : soloRaiders) {
				if (!r.isDead()) r.unstick(frame);
			}
			for (Raider r : kodachis) {
				if (!r.isDead()) r.unstick(frame);
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
			if (!soloRaiders.isEmpty() || !kodachis.isEmpty()) {
				// use the raider with the highest individual threat for maxthreat and the raider with the greatest
				// all-terrain mobility for determining reachability, just to be conservative.
				float maxThreat = 0;
				float maxSlope = 0;
				for (Raider ra: soloRaiders){
					if (ra.isDead()) continue;
					float tmpthreat = ((ra.getUnit().getPower() + ra.getUnit().getMaxHealth()) / 14f)/500f;
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
				
				for (Raider ra: kodachis){
					if (ra.isDead()) continue;
					float tmpthreat = ((ra.getUnit().getPower() + ra.getUnit().getMaxHealth()) / 14f)/500f;
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
	
	private void assignRaiderSquads(){
		// Assign up to one solo raider per frame, with each being assigned up to twice per second.
		while (!raiderSquads.isEmpty()){
			RaiderSquad rs = raiderSquads.poll();
			if (rs.isDead()){
				if (rs.status == 'f'){
					raiderSquads.add(rs);
					return;
				}
				continue;
			}else if (frame - rs.lastAssignmentFrame < 15){
				raiderSquads.push(rs);
				return;
			}else{
				assignRaiderSquad(rs);
				rs.lastAssignmentFrame = frame;
				raiderSquads.add(rs);
				return;
			}
		}
	}
	
	private void assignSoloRaiders(){
		// Assign up to one solo raider per frame, with each being assigned up to twice per second.
		while (!soloRaiders.isEmpty()){
			Raider r = soloRaiders.poll();
			if (r.isDead()){
				r.clearTask();
			}else if (r.getUnit().isBeingBuilt() || retreatHandler.isRetreating(r.getUnit())){
				soloRaiders.add(r);
				return;
			}else if (frame - r.lastTaskFrame < 15){
				soloRaiders.push(r);
				return;
			}else{
				assignSoloRaider(r);
				r.lastTaskFrame = frame;
				soloRaiders.add(r);
				return;
			}
		}
	}
	
	private void assignSoloRaider(Raider r){
		AIFloat3 pos = r.getPos();
		boolean overThreat = (warManager.getThreat(pos) * (1f + warManager.getRiotThreat(pos)) > warManager.getFriendlyRaiderThreat(pos)
			                        || (r.target != null && warManager.getThreat(r.target) > warManager.getFriendlyRaiderThreat(pos)));
		if (overThreat){
			// try to keep raiders from suiciding.
			r.sneak(graphManager.getClosestRaiderHaven(pos), frame);
			return;
		}
		
		ScoutTask bestTask = null;
		AIFloat3 bestTarget = null;
		float cost = Float.MAX_VALUE;
		boolean porc = true;
		
		for (ScoutTask s:soloScoutTasks){
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
			float ethreat = warManager.getThreat(e.position);
			
			float tmpcost = distance(pos, e.position);
			if (!e.isArty && !e.isWorker && !e.isStatic && (!e.isAA || e.ud.getName().equals("turretaalaser"))){
				tmpcost += 1250f;
			}else if (e.isPorc) {
				tmpcost -= 400f;
				//tmpcost += 500f * Math.ceil(ethreat);
			}else if (e.isCom){
				tmpcost -= 250f;
			}else if (e.isWorker){
				tmpcost = (tmpcost/2f) - 500f;
				tmpcost += 1000f * Math.ceil(ethreat);
			}else if (e.isMex){
				tmpcost = (tmpcost/2f) - 300f;
				tmpcost += 1000f * Math.ceil(ethreat);
			}else {
				tmpcost += 500f * ethreat;
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
			
			for (ScoutTask s:soloScoutTasks){
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
			
			if (!reloading) {
				for (Enemy e : warManager.getTargets()) {
					if ((!e.isMex && warManager.getThreat(e.position) > 0) || warManager.getThreat(e.position) > 1f || warManager.getRiotThreat(e.position) > 0) {
						continue;
					}
					
					float tmpcost = distance(r.getPos(), e.position);
					if (e.isMex) {
						tmpcost = (tmpcost / 4) - 500;
					}
					
					if (tmpcost < cost) {
						cost = tmpcost;
						bestTarget = e.position;
						porc = e.isStatic;
					}
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

	private void assignRaiderSquad(RaiderSquad rs){
		AIFloat3 pos = rs.getPos();
		boolean overThreat = false;
		if (rs.status == 'f'){
			// Assign forming squads.
			if ((rs.target != null && (warManager.getPorcThreat(rs.target) > 0
				                             || warManager.getTacticalThreat(rs.target) > warManager.availableMobileThreat))){
				overThreat = true;
			}else {
				for (Raider r : rs.raiders) {
					AIFloat3 rpos = r.getPos();
					if (warManager.getThreat(rpos) * (1f + warManager.getRiotThreat(rpos)) > warManager.getTotalFriendlyThreat(rpos)
						      || warManager.getPorcThreat(rpos) > 0) {
						overThreat = true;
						break;
					}
				}
			}
			if (!overThreat) {
				rs.raid(warManager.getRaiderRally(pos), frame);
			}else{
				rs.sneak(graphManager.getClosestHaven(pos), frame);
			}
		}else if (rs.status == 'r'){
			// Rally rallying squads.
			rs.sneak(graphManager.getClosestHaven(pos), frame);
			if (rs.isRallied(frame)){
				rs.status = 'a';
				if (rs.type == 's'){
					for (Raider r: rs.raiders) retreatHandler.removeUnit(r.getUnit());
				}
			}
		}else{
			// Get targets for actively raiding squads.
			float threat = warManager.getFriendlyRaiderThreat(pos);
			if (warManager.getRiotThreat(rs.target) > 0 || warManager.getThreat(rs.target) > threat){
				overThreat = true;
			}else {
				for (Raider r:rs.raiders) {
					AIFloat3 rpos = r.getPos();
					if (!r.getUnit().isCloaked() && (warManager.getThreat(rpos) > threat || warManager.getRiotThreat(rpos) > 0)){
						overThreat = true;
						break;
					}
				}
			}
			if (overThreat){
				// try to keep raider squads from suiciding.
				rs.sneak(graphManager.getClosestRaiderHaven(pos), frame);
				return;
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
				tmpcost = (tmpcost/4f)-1000f;

				if (tmpcost < cost){
					cost = tmpcost;
					bestTarget = d.position;
				}
			}
			
			boolean porc = false;
			for (Enemy e: warManager.getTargets()){
				float ethreat = warManager.getThreat(e.position) * (1f + warManager.getRiotThreat(e.position));
				if (ethreat > threat || !e.identified){
					continue;
				}

				float tmpcost = distance(pos, e.position);
				if (!e.isArty && !e.isRaider && !e.isWorker && !e.isStatic && (!e.isAA || e.ud.getName().equals("turretaalaser"))){
					tmpcost += 1250f;
				}else if (e.isPorc) {
					tmpcost -= 400f;
				}else if (e.isCom){
					tmpcost -= 250f;
				}else if (e.isWorker){
					tmpcost = (tmpcost/2f) - 500f;
					tmpcost += 1000f * Math.ceil(ethreat);
				}else if (e.isMex){
					tmpcost = (tmpcost/2f) - 300f;
					tmpcost += 1000f * Math.ceil(ethreat);
				}else {
					tmpcost += 500f * ethreat;
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

	public void avoidEnemies(Unit h, Unit attacker, AIFloat3 dir){
		if (h.getUnitId() != lastDamagedID){
			lastDamagedID = h.getUnitId();
			lastDamagedRaider = smallRaiders.get(h.getUnitId());
		}
		
		Raider r = lastDamagedRaider;
		if (r == null || frame - r.lastRotationFrame < 5) return;
		
		if (h.getHealth() > 0 && (h.getHealth() / h.getMaxHealth() < 0.8f || h.getParalyzeDamage() > 0) && attacker != null && attacker.getMaxSpeed() > 0) {
			r.lastRotationFrame = frame;
			float movdist = -100f;
			if (h.getDef().getName().charAt(0) == 'h' || h.getParalyzeDamage() > 0) {
				// for daggers
				movdist = -450f;
			}
			float x = movdist * dir.x;
			float z = movdist * dir.z;
			AIFloat3 pos = h.getPos();
			AIFloat3 target = new AIFloat3();
			target.x = pos.x + x;
			target.z = pos.z + z;
			h.moveTo(target, (short) 0, Integer.MAX_VALUE);
		}
	}
}