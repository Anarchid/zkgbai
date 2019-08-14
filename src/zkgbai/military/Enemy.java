package zkgbai.military;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;
import com.springrts.ai.oo.clb.WeaponDef;
import com.springrts.ai.oo.clb.WeaponMount;

public class Enemy {
	public Unit unit;
	public UnitDef ud;
	int unitID;
	public AIFloat3 position;
	float threatRadius = 0;
	public float value = 0;
	float speed = 0;
	float lastHealth = 0;
	public int lastSeen = 0;
	public boolean isPainted = false;
	boolean visible = false;
	public boolean isStatic = false;
	boolean isRadarOnly = true;
	boolean isRadarVisible = false;
	public boolean identified = false;
	public boolean isWorker = false;
	public boolean isRiot = false;
	public boolean isRaider = false;
	boolean isFlamer = false;
	public boolean isArty = false;
	public boolean isAA = false;
	public boolean isFlexAA = false;
	public boolean isPorc = false;
	boolean isSuperWep = false;
	boolean isMinorCancer = false;
	boolean isMajorCancer = false;
	boolean isNanoSpam = false;
	boolean isStrong = false;
	float maxObservedSpeed = 0;
	
	Enemy(Unit unit, float cost){
		this.unit = unit;
		this.unitID = unit.getUnitId();
		this.position = unit.getPos();
		this.ud = null;

		UnitDef def = unit.getDef();

		if(def != null){
			updateFromUnitDef(def, cost);
			this.ud = def;
			checkNano();
		}else{
			this.value = 50;
			this.position = unit.getPos();
			this.isStatic = false;
		}
	}
	
	@Override
	public boolean equals(Object other){
		if (other instanceof Enemy){
			return unitID == ((Enemy) other).unitID;
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		return unitID;
	}
	
	void checkNano(){
		if (unit.isBeingBuilt() && unit.getHealth()/unit.getMaxHealth() < 0.25){
			isNanoSpam = true;
		}else{
			isNanoSpam = false;
		}
	}
	
	void setIdentified(){
		this.identified = true;
	}
	
	boolean getIdentified(){
		return this.identified;
	}

	public void updateFromUnitDef(UnitDef u, float cost){
		this.identified = true;
		this.value = cost;
		this.isStatic = (u.getSpeed() == 0);
		this.ud = u;

		if (ud.isBuilder()){
			this.isWorker = true;
		}

		String defName = ud.getName();

		if (defName.equals("corgarp") || defName.equals("armsnipe") || defName.equals("armmanni")
				|| defName.equals("amphfloater") || defName.equals("armsptk")){
			this.isMinorCancer = true;
		}

		if (defName.equals("dante") || defName.equals("scorpion") || defName.equals("funnelweb") || defName.equals("armbanth") || defName.equals("armorco")
				|| defName.equals("amphassault") || defName.equals("armraven") || defName.equals("armcrabe") || defName.equals("corgol") || defName.equals("correap")
				|| defName.equals("shieldfelon") || u.getName().equals("capturecar") || u.getName().contains("com")){
			this.isMajorCancer = true;
		}
		
		if(u.getWeaponMounts().size() > 0){
			this.threatRadius = u.getMaxWeaponRange();
			if ((u.getTooltip().contains("Riot") || u.getTooltip().contains("Anti-Swarm") || u.getName().equals("screamer") || u.getName().equals("corflak"))
					&& !defName.equals("dante")){
				// identify riots
				this.isRiot = true;
			}
			
			if (defName.equals("corhlt") || defName.equals("amphraider3")){
				isStrong = true;
			}

			for (WeaponMount w:u.getWeaponMounts()){
				WeaponDef wd = w.getWeaponDef();
				if (wd.getCustomParams().containsKey("setunitsonfire")){
					if (!u.getTooltip().contains("Arti")){
						this.isStrong = true;
					}
					this.isFlamer = true;
				}
			}

			if (u.getTooltip().contains("aider") || u.getTooltip().contains("cout")){
				// this is for keeping assault mobs from chasing raiders around.
				this.isRaider = true;
			}

			if ((u.getTooltip().contains("Arti") || u.getTooltip().contains("Skirm") || u.getName().equals("cormist")) || u.isBuilder() && !u.getTooltip().contains("Riot")){
				// identify arty/skirms
				this.isArty = true;
			}

			if (u.getTooltip().contains("Anti-Air")){
				this.isAA = true;
			}
			
			if (ud.getName().equals("gunshipsupport") || ud.getName().equals("armbrawl") || ud.getName().equals("corcrw") || ud.getName().equals("slowmort")){
				isFlexAA = true;
			}

			if (ud.getName().equals("armbrtha") || ud.getName().equals("corsilo") || ud.getName().equals("tacnuke")
					|| ud.getName().equals("napalmmissile") || ud.getName().equals("raveparty") || ud.getName().equals("zenith")
					|| ud.getName().equals("mahlazer")){
				this.isSuperWep = true;
			}
			
			if (isStatic && !isAA && getDanger() > 0f){
				isPorc = true;
			}
		}		
		this.speed = u.getSpeed()/30;
	}
	
	public void updateFromRadarDef(RadarDef rd){
		this.identified = true;
		this.value = rd.getValue();
		this.isStatic = (rd.getSpeed() == 0);
		this.speed = rd.getSpeed();
		this.threatRadius = rd.getRange();
	}

	public float getDanger(){
		float health = 0;
		float danger = 0;
		if (ud != null) {
			if (!ud.isAbleToAttack() || isSuperWep){
				return 0;
			}
			
			if (unit.getRulesParamFloat("comm_level", 0f) > 1) {
				isStrong = true;
			}

			if (unit.getHealth() > 0) {
				health = unit.getHealth();
			}else if (lastHealth > 0){
				health = lastHealth;
			}else{
				health = ud.getHealth();
			}
			danger = (ud.getPower() + health)/10f;

			if (isFlamer || ud.getName().equals("arm_venom") || ud.getName().equals("amphraider2")){
				danger += 100f;
			}
			if (isRiot || isStrong){
				danger *= 2f;
			}else if ((isArty && !ud.getName().equals("amphfloater"))
					|| ud.isBuilder()){
				danger /= 3f;
			}else if ((isStatic && isAA)){
				danger *= 5f;
			}
		}
		return danger;
	}
}
