package zkgbai.economy;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.OOAICallback;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.UnitDef;
import zkgbai.ZKGraphBasedAI;

/**
 * Created by haplo on 1/16/2016.
 */
public class Factory extends Worker {
	public float raiderSpam = 0;
	public float expensiveRaiderSpam = 0;
	public boolean creatingScout = false;
	public boolean smallRaiderSpam = false;
	public int scoutAllowance = 0;
	public int maxScoutAllowance = 0;
	public float costPerBP = 0;
	public float workerBP = 0;
	AIFloat3 pos;

	public Factory(Unit u, boolean firstFac){
		super(u);
		pos = u.getPos();
		String defName = u.getDef().getName();

		if (firstFac){
			ZKGraphBasedAI ai = ZKGraphBasedAI.getInstance();
			this.raiderSpam = -6;
			this.scoutAllowance = 2;
			this.maxScoutAllowance = 2;
			
			boolean earlyWorker = ai.mapDiag > 910f;
			boolean medMap = ai.mapDiag > 1080f;
			boolean bigMap = ai.mapDiag > 1270f;
			
			if (earlyWorker){
				this.scoutAllowance = 3;
				this.maxScoutAllowance = 4;
			}
			
			//if (medMap) this.maxScoutAllowance = 4;
			
			if (bigMap){
				raiderSpam = -9;
			}

			if (defName.equals("factorytank")){
				scoutAllowance = 2;
			}

			if (defName.equals("factorygunship")){
				raiderSpam = -2;
			}

			if (defName.equals("factoryveh")) {
				scoutAllowance--;
				maxScoutAllowance++;
				raiderSpam = -4;
			}

			if (defName.equals("factoryhover")){
				scoutAllowance--;
				maxScoutAllowance++;
				raiderSpam = -5;
				if (bigMap) raiderSpam = -10;
			}

			if (defName.equals("factoryspider")){
				maxScoutAllowance = 12;
				scoutAllowance = 12;
			}

			if (defName.equals("factoryplane")){
				raiderSpam = -1;
			}
		}else if (defName.equals("factorygunship")){
			expensiveRaiderSpam = -6;
		}

		if (defName.equals("factorygunship") || defName.equals("factoryplane") || defName.equals("striderhub")){
			scoutAllowance = 0;
			maxScoutAllowance = 0;
		}
	}

	@Override
	public AIFloat3 getPos(){
		return pos;
	}
}
