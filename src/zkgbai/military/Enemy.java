package zkgbai.military;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;
import com.springrts.ai.oo.clb.WeaponDef;
import com.springrts.ai.oo.clb.WeaponMount;

public class Enemy {
	Unit unit;
	public UnitDef ud;
	int unitID;
	AIFloat3 position;
	float threatRadius = 0;
	float value = 0;
	float speed = 0;
	public int lastSeen = 0;
	public boolean isPainted = false;
	boolean visible = false;
	boolean isStatic = false;
	boolean isRadarOnly = true;
	boolean isRadarVisible = false;
	boolean identified = false;
	boolean isRiot = false;
	boolean isFlamer = false;
	boolean isArty = false;
	boolean isAA = false;
	boolean isSuperWep = false;
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
		
		if(u.getWeaponMounts().size() > 0){
			this.threatRadius = u.getMaxWeaponRange();
			if (u.getTooltip().contains("Riot") || u.getTooltip().contains("Anti-Swarm") || u.getName().contains("com") || u.getName().equals("screamer") || u.getName().equals("corflak")){
				// identify riots
				this.isRiot = true;
			}

			for (WeaponMount w:u.getWeaponMounts()){
				WeaponDef wd = w.getWeaponDef();
				if (wd.getCustomParams().containsKey("setunitsonfire")){
					if (!u.getTooltip().contains("Arti")){
						this.isRiot = true;
					}
					this.isFlamer = true;
				}
			}

			if ((u.getTooltip().contains("Arti") || u.getTooltip().contains("Skirm")) && !u.getTooltip().contains("Riot")){
				// identify arty/skirms
				this.isArty = true;
			}

			if (u.getTooltip().contains("Anti-Air")){
				this.isAA = true;
			}

			if (ud.getName().equals("armbrtha") || ud.getName().equals("corsilo") || ud.getName().equals("tacnuke")
					|| ud.getName().equals("napalmmissile") || ud.getName().equals("raveparty") || ud.getName().equals("zenith")
					|| ud.getName().equals("mahlazer")){
				this.isSuperWep = true;
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

			if (unit.getHealth() > 0 && !isStatic) {
				health = unit.getHealth();
				danger = ud.getPower() + health;
				danger *= 3f;
			} else {
				health = ud.getHealth();
				danger = ud.getPower() + health;
			}

			if (isFlamer || ud.getName().equals("arm_venom")){
				danger += 400;
			}
			if (isRiot){
				danger *= 2;
			}
			if (isArty){
				danger /= 2;
			}
		}
		return danger;
	}
}
