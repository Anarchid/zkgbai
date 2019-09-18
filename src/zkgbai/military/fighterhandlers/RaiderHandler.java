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
import zkgbai.military.unitwrappers.Fighter;
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
	public List<Raider> soloRaiders;
	public Map<Integer, Raider> smallRaiders;
	public Map<Integer, Raider> mediumRaiders;
	List<RaiderSquad> raiderSquads;

	List<ScoutTask> scoutTasks;
	List<ScoutTask> soloScoutTasks;

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

		this.soloRaiders = new ArrayList<Raider>();
		this.smallRaiders = new HashMap<Integer, Raider>();
		this.mediumRaiders = new HashMap<Integer, Raider>();
		this.raiderSquads = new ArrayList<RaiderSquad>();

		this.scoutTasks = new ArrayList<ScoutTask>();
		this.soloScoutTasks = new ArrayList<ScoutTask>();
		OOAICallback callback = ai.getCallback();
		pantherID = callback.getUnitDefByName("tankheavyraid").getUnitDefId();
		halberdID = callback.getUnitDefByName("hoverassault").getUnitDefId();
	}

	public void addSmallRaider(Raider r){
		if (soloRaiders.contains(r)) return;
		String defName = r.getUnit().getDef().getName();

        /*if (defName.equals("shieldraid") && !warManager.squadHandler.shieldSquads.isEmpty()){
            // convert bandits into assaults when there's shieldballs.
            Fighter f = new Fighter(r.getUnit(), r.getUnit().getDef().getCost(ai.getCallback().getResourceByName("Metal")));
            warManager.squadHandler.addAssault(f);
            return;
        }*/

		smallRaiders.put(r.id, r);

		// glaive, bandit, duck/archer, dagger
		if (defName.equals("cloakraid")) {
			if (nextGlaiveSquad == null) {
				nextGlaiveSquad = new RaiderSquad();
				nextGlaiveSquad.raid(warManager.getRaiderRally(r.getPos()), frame);
				nextGlaiveSquad.type = 's';
			}
			nextGlaiveSquad.addUnit(r, frame);

			if (nextGlaiveSquad.raiders.size() >= min(12, max(6, (int) floor(ecoManager.adjustedIncome / (5f))))) {
				raiderSquads.add(nextGlaiveSquad);
				nextGlaiveSquad.status = 'r';
				nextGlaiveSquad = null;
			}
		}else if (defName.equals("shieldraid")) {
			if (nextBanditSquad == null) {
				nextBanditSquad = new RaiderSquad();
				nextBanditSquad.raid(warManager.getRaiderRally(r.getPos()), frame);
				nextBanditSquad.type = 's';
			}
			nextBanditSquad.addUnit(r, frame);

			if (nextBanditSquad.raiders.size() >= min(12, max(6, (int) floor(ecoManager.adjustedIncome / (5f))))) {
				raiderSquads.add(nextBanditSquad);
				nextBanditSquad.status = 'r';
				nextBanditSquad = null;
			}
		}else if (defName.equals("amphimpulse") || defName.equals("amphraid")) {
			if (nextAmphSquad == null) {
				nextAmphSquad = new RaiderSquad();
				nextAmphSquad.raid(warManager.getRaiderRally(r.getPos()), frame);
				nextAmphSquad.type = 's';
			}
			nextAmphSquad.addUnit(r, frame);

			if (nextAmphSquad.raiders.size() >= min(12, max(6, (int) floor(ecoManager.adjustedIncome / (5f))))) {
				raiderSquads.add(nextAmphSquad);
				nextAmphSquad.status = 'r';
				nextAmphSquad = null;
			}
		}else if (defName.equals("hoverraid")) {
			if (nextDaggerSquad == null) {
				nextDaggerSquad = new RaiderSquad();
				nextDaggerSquad.raid(warManager.getRaiderRally(r.getPos()), frame);
				nextDaggerSquad.type = 's';
			}
			nextDaggerSquad.addUnit(r, frame);

			if (nextDaggerSquad.raiders.size() >= min(12, max(6, (int) floor(ecoManager.adjustedIncome / (5f))))) {
				raiderSquads.add(nextDaggerSquad);
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
				nextScorcherSquad.raid(warManager.getRaiderRally(r.getPos()), frame);
				nextScorcherSquad.type = 'm';
			}
			nextScorcherSquad.addUnit(r, frame);

			if (nextScorcherSquad.raiders.size() >= (int) min(8f, max(4f, floor(ecoManager.adjustedIncome / 4f)))){
				raiderSquads.add(nextScorcherSquad);
				nextScorcherSquad.status = 'r';
				nextScorcherSquad = null;
			}

		}else if (defName.equals("hoverassault")){
			// for halberds
			if (nextHalberdSquad == null){
				nextHalberdSquad = new RaiderSquad();
				nextHalberdSquad.raid(warManager.getRaiderRally(r.getPos()), frame);
				nextHalberdSquad.type = 'm';
			}
			nextHalberdSquad.addUnit(r, frame);

			if (nextHalberdSquad.raiders.size() >= (int) min(6, max(2, floor(ecoManager.adjustedIncome / 8f)))){
				raiderSquads.add(nextHalberdSquad);
				nextHalberdSquad.status = 'r';
				nextHalberdSquad = null;
			}
		}else if (defName.equals("cloakheavyraid")) {
			// for scythes
			if (nextScytheSquad == null) {
				nextScytheSquad = new RaiderSquad();
				nextScytheSquad.raid(warManager.getRaiderRally(r.getPos()), frame);
				nextScytheSquad.type = 'm';
			}
			nextScytheSquad.addUnit(r, frame);

			if (nextScytheSquad.raiders.size() >= (int) min(4, max(2, floor(ecoManager.adjustedIncome / 10f)))) {
				raiderSquads.add(nextScytheSquad);
				nextScytheSquad.status = 'r';
				nextScytheSquad = null;
			}
		}else if (defName.equals("tankheavyraid")) {
			// for panthers
			if (nextPantherSquad == null) {
				nextPantherSquad = new RaiderSquad();
				nextPantherSquad.raid(warManager.getRaiderRally(r.getPos()), frame);
				nextPantherSquad.type = 'm';
			}
			nextPantherSquad.addUnit(r, frame);

			if (nextPantherSquad.raiders.size() >= (int) min(8, max(4, floor(ecoManager.adjustedIncome / 5f)))) {
				raiderSquads.add(nextPantherSquad);
				nextPantherSquad.status = 'r';
				nextPantherSquad = null;
			}
		}
	}

	public void addSoloRaider(Raider r){
		soloRaiders.add(r);
	}

	public void removeUnit(Unit u){
		for (Raider r: soloRaiders){
			if (r.id == u.getUnitId()){
				if (r.getTask() != null){
					r.getTask().removeRaider(r);
				}
			}
		}
		Raider r = new Raider(u, 0);
		soloRaiders.remove(r);
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
		if (smallRaiders.containsKey(u.getUnitId())){
			Raider r = smallRaiders.get(u.getUnitId());
			if (r.squad != null){
				r.squad.removeUnit(r);
				r.squad = null;
			}

			if (r.getTask() != null){
				r.getTask().removeRaider(r);
				r.clearTask();
			}
		}else if (mediumRaiders.containsKey(u.getUnitId())){
			Raider r = mediumRaiders.get(u.getUnitId());
			if (r.squad != null){
				r.squad.removeUnit(r);
				r.squad = null;
			}

			if (r.getTask() != null){
				r.getTask().removeRaider(r);
				r.clearTask();
			}
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

			List<Integer> converts = new ArrayList<>();
			for (Raider r:mediumRaiders.values()){
				int defID = r.getUnit().getDef().getUnitDefId();
				if (graphManager.eminentTerritory && (defID == halberdID || defID == pantherID) && (r.squad == null || r.squad.status != 'a') && !retreatHandler.isRetreating(r.getUnit())){
					if (r.squad != null){
						r.squad.removeUnit(r);
						r.squad = null;
					}
					warManager.squadHandler.addRaidAssault(new Fighter(r.getUnit(), r.metalValue));
					converts.add(r.id);
				}else if (r.squad == null && !retreatHandler.isRetreating(r.getUnit())){
					addMediumRaider(r);
				}
			}
			for (int id: converts) {
				mediumRaiders.remove(id);
			}

			assignRaiders();
		}
		
		// unstick raiders that are caught on buildings or wrecks
		if (frame % 60 == 1) {
			for (Raider r : smallRaiders.values()) {
				if (!retreatHandler.isRetreating(r.getUnit())) r.unstick(frame);
			}
		}
		
		if (frame % 60 == 2) {
			for (Raider r : mediumRaiders.values()) {
				if (!retreatHandler.isRetreating(r.getUnit())) r.unstick(frame);
			}
		}
		
		if (frame % 60 == 3) {
			for (Raider r : soloRaiders) {
				if (!retreatHandler.isRetreating(r.getUnit())) r.unstick(frame);
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

		//cost += Math.max(0f, 2000f * (warManager.getThreat(task.target) - warManager.getFriendlyThreat(raider.getPos())));

		return cost;
	}

	private void assignRaiders(){
		// assign rally points for new squads
		if (nextGlaiveSquad != null && !nextGlaiveSquad.raiders.isEmpty()){
			boolean overThreat = (warManager.getEffectiveThreat(nextGlaiveSquad.getPos()) > 0 || warManager.getPorcThreat(nextGlaiveSquad.getPos()) > 0 || warManager.getRiotThreat(nextGlaiveSquad.getPos()) > 0);
			if (!overThreat) {
				nextGlaiveSquad.raid(warManager.getRaiderRally(nextGlaiveSquad.getPos()), frame);
			}else{
				nextGlaiveSquad.sneak(graphManager.getClosestHaven(nextGlaiveSquad.getPos()), frame);
			}
		}

		if (nextBanditSquad != null && !nextBanditSquad.raiders.isEmpty()){
			boolean overThreat = (warManager.getEffectiveThreat(nextBanditSquad.getPos()) > 0 || warManager.getPorcThreat(nextBanditSquad.getPos()) > 0 || warManager.getRiotThreat(nextBanditSquad.getPos()) > 0);
			if (!overThreat) {
				nextBanditSquad.raid(warManager.getRaiderRally(nextBanditSquad.getPos()), frame);
			}else{
				nextBanditSquad.sneak(graphManager.getClosestHaven(nextBanditSquad.getPos()), frame);
			}
		}

		if (nextAmphSquad != null && !nextAmphSquad.raiders.isEmpty()){
			boolean overThreat = (warManager.getEffectiveThreat(nextAmphSquad.getPos()) > 0 || warManager.getPorcThreat(nextAmphSquad.getPos()) > 0 || warManager.getRiotThreat(nextAmphSquad.getPos()) > 0);
			if (!overThreat) {
				nextAmphSquad.raid(warManager.getRaiderRally(nextAmphSquad.getPos()), frame);
			}else{
				nextAmphSquad.sneak(graphManager.getClosestHaven(nextAmphSquad.getPos()), frame);
			}
		}

		if (nextDaggerSquad != null && !nextDaggerSquad.raiders.isEmpty()){
			boolean overThreat = (warManager.getEffectiveThreat(nextDaggerSquad.getPos()) > 0 || warManager.getPorcThreat(nextDaggerSquad.getPos()) > 0 || warManager.getRiotThreat(nextDaggerSquad.getPos()) > 0);
			if (!overThreat) {
				nextDaggerSquad.raid(warManager.getRaiderRally(nextDaggerSquad.getPos()), frame);
			}else{
				nextDaggerSquad.sneak(graphManager.getClosestHaven(nextDaggerSquad.getPos()), frame);
			}
		}

		if (nextScorcherSquad != null && !nextScorcherSquad.raiders.isEmpty()){
			boolean overThreat = (warManager.getEffectiveThreat(nextScorcherSquad.getPos()) > 0 || warManager.getPorcThreat(nextScorcherSquad.getPos()) > 0 || warManager.getRiotThreat(nextScorcherSquad.getPos()) > 0);
			if (!overThreat) {
				nextScorcherSquad.raid(warManager.getRaiderRally(nextScorcherSquad.getPos()), frame);
			}else{
				nextScorcherSquad.sneak(graphManager.getClosestHaven(nextScorcherSquad.getPos()), frame);
			}
		}

		if (nextHalberdSquad != null && !nextHalberdSquad.raiders.isEmpty()){
			boolean overThreat = (warManager.getEffectiveThreat(nextHalberdSquad.getPos()) > 0 || warManager.getPorcThreat(nextHalberdSquad.getPos()) > 0 || warManager.getRiotThreat(nextHalberdSquad.getPos()) > 0);
			if (!overThreat) {
				nextHalberdSquad.raid(warManager.getRaiderRally(nextHalberdSquad.getPos()), frame);
			}else{
				nextHalberdSquad.sneak(graphManager.getClosestHaven(nextHalberdSquad.getPos()), frame);
			}
		}

		if (nextScytheSquad != null && !nextScytheSquad.raiders.isEmpty()){
			boolean overThreat = (warManager.getEffectiveThreat(nextScytheSquad.getPos()) > 0 || warManager.getPorcThreat(nextScytheSquad.getPos()) > 0 || warManager.getRiotThreat(nextScytheSquad.getPos()) > 0);
			if (!overThreat) {
				nextScytheSquad.raid(warManager.getRaiderRally(nextScytheSquad.getPos()), frame);
			}else{
				nextScytheSquad.sneak(graphManager.getClosestHaven(nextScytheSquad.getPos()), frame);
			}
		}

		if (nextPantherSquad != null && !nextPantherSquad.raiders.isEmpty()){
			boolean overThreat = (warManager.getEffectiveThreat(nextPantherSquad.getPos()) > 0 || warManager.getPorcThreat(nextPantherSquad.getPos()) > 0 || warManager.getRiotThreat(nextPantherSquad.getPos()) > 0);
			if (!overThreat) {
				nextPantherSquad.raid(warManager.getRaiderRally(nextPantherSquad.getPos()), frame);
			}else{
				nextPantherSquad.sneak(graphManager.getClosestHaven(nextPantherSquad.getPos()), frame);
			}
		}

		// assign soloraid/scout group
		for (Raider r: soloRaiders){
			if (r.getUnit().isBeingBuilt()) continue;
			boolean overThreat = (warManager.getEffectiveThreat(r.getPos()) > 0 || warManager.getRiotThreat(r.getPos()) > 0);
			if (retreatHandler.isRetreating(r.getUnit()) || r.getUnit().getHealth() <= 0){
				continue;
			}

			ScoutTask bestTask = null;
			AIFloat3 bestTarget = null;
			float cost = Float.MAX_VALUE;

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

			boolean porc = false;
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

				if (overThreat || distance(bestTarget, r.getPos()) > 500 || porc) {
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

				if (overThreat || distance(bestTask.target, r.getPos()) > 500) {
					r.sneak(bestTask.target, frame);
				} else {
					r.raid(bestTask.target, frame);
				}
			}else{
				r.clearTask();
				r.sneak(warManager.getRaiderRally(r.getPos()), frame);
			}
		}

		// assign raider squads
		List<RaiderSquad> deadSquads = new ArrayList<RaiderSquad>();
		for (RaiderSquad rs:raiderSquads){
			if (rs.isDead()){
				deadSquads.add(rs);
				continue;
			}

			boolean overThreat = false;
			for (Raider r:rs.raiders){
				if (warManager.getEffectiveThreat(r.getPos()) > 0 || warManager.getRiotThreat(r.getPos()) > 0){
					overThreat = true;
				}
			}

			if (rs.status == 'r'){
				// rally rallying squads
				rs.sneak(graphManager.getClosestHaven(rs.getPos()), frame);
				if (rs.isRallied(frame)) rs.status = 'a';
			}else{
				// for ready squads, assign to a target
				ScoutTask bestTask = null;
				AIFloat3 bestTarget = null;
				float cost = Float.MAX_VALUE;

				float threat = rs.getThreat();
				for (ScoutTask s:scoutTasks){
					if (warManager.getRiotThreat(s.target) > 0 || !pathfinder.isRaiderReachable(rs.leader.getUnit(), s.target, threat)){
						continue;
					}
					float tmpcost = getScoutCost(s, rs.leader);
					if (tmpcost < cost){
						cost = tmpcost;
						bestTask = s;
					}
				}

				for (DefenseTarget d: warManager.sniperSightings){
					if (warManager.getRiotThreat(d.position) > 0) continue;
					float tmpcost = distance(rs.getPos(), d.position);
					tmpcost = (tmpcost/4)-100;

					if (tmpcost < cost){
						cost = tmpcost;
						bestTarget = d.position;
					}
				}

				boolean porc = false;
				for (Enemy e: warManager.getTargets()){
					if (e.isRiot || warManager.getRiotThreat(e.position) > 0 || warManager.getEffectiveThreat(e.position) > threat){
						continue;
					}

					float tmpcost = distance(rs.getPos(), e.position);
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
					if (distance(bestTarget, rs.getPos()) > 400 || porc) {
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

					if (distance(bestTask.target, rs.getPos()) > 400) {
						rs.sneak(bestTask.target, frame);
					}else{
						rs.raid(bestTask.target, frame);
					}
				}else{
					rs.leader.clearTask();
					rs.sneak(warManager.getRaiderRally(rs.getPos()), frame);
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
				if (r.squad != null) {
					r.squad.removeUnit(r);
				}
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
				if (r.squad != null) {
					r.squad.removeUnit(r);
				}
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
	}
}