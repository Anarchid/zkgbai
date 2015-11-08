package zkgbai.military;

import java.util.ArrayList;
import java.util.List;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;

public class Squad {
	List<Fighter> fighters;
	float totalHealth; // the combined health of all fighters in the squad upon its formation, used for deciding when to retreat.
	float currentHealth;
	float metalValue;
	public String status;
	
	public Squad(){
		this.fighters = new ArrayList<Fighter>();
		this.totalHealth = 0;
		this.metalValue = 0;
		this.status = "forming";
	}

	public void addUnit(Fighter f){
		fighters.add(f);
		totalHealth = totalHealth+f.getMaxHealth();
		metalValue = metalValue + f.metalValue;
	}

	public void removeUnit(Fighter f){
		fighters.remove(f);
	}

	public void setTarget(AIFloat3 pos, int frame){
		// set a target for the squad to attack.
		for (Fighter f:fighters){
			f.fight(pos, frame);
		}
	}

	public void update(){
		currentHealth = 0;
		for (Fighter f:fighters){
			currentHealth = currentHealth + f.getCurrentHealth();
		}
	}
}
