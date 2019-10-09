package zkgbai.military.unitwrappers;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.WeaponDef;
import com.springrts.ai.oo.clb.WeaponMount;
import zkgbai.ZKGraphBasedAI;

import java.util.Map;

public class Porc {
	Unit unit;
	public int id;
	float power;
	public int radius;
	static int team = -1;
	
	public Porc(Unit u){
		unit = u;
		id = u.getUnitId();
		radius = Math.round(u.getMaxRange() / 64f);
		
		for (WeaponMount w:u.getDef().getWeaponMounts()){
			WeaponDef wd = w.getWeaponDef();
			if (!wd.getName().contains("fake")) {
				// take the max between burst damage and continuous dps as weapon power.
				float wepShot = wd.getDamage().getTypes().get(1) * wd.getProjectilesPerShot() * wd.getSalvoSize();
				for (Map.Entry<String, String> param: wd.getCustomParams().entrySet()){
					if (param.getKey().equals("extra_damage")) wepShot += Float.parseFloat(param.getValue()); // emp
					if (param.getKey().equals("impulse")) wepShot += Float.parseFloat(param.getValue())/10f; // impulse
					if (param.getKey().equals("timeslow_damagefactor")) wepShot += Math.min((wepShot * Float.parseFloat(param.getValue())), wepShot/2f); // slow
					if (param.getKey().equals("disarmDamageOnly")) wepShot /= 10f;
					if (param.getKey().equals("setunitsonfire")) wepShot *= 2f;
				}
				if (wd.isParalyzer()) wepShot *= 2f;
				wepShot *= 1f + wd.getAreaOfEffect()/100f;
				power += wepShot/wd.getReload();
			}
		}
		
		if (team == -1) team = ZKGraphBasedAI.getInstance().teamID;
	}
	
	public int getPower(){
		float pow = power;
		pow += unit.getMaxHealth();
		pow /= 10f;
		if (unit.isBeingBuilt()) pow *= unit.getBuildProgress() * unit.getBuildProgress();
		return Math.round(pow);
	}
	
	public AIFloat3 getPos(){ return unit.getPos(); }
	
	public boolean isDead(){
		return (unit.getHealth() <= 0 || unit.getTeam() != team);
	}
}
