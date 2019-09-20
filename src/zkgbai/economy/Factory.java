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
			OOAICallback callback = ZKGraphBasedAI.getInstance().getCallback();
			this.raiderSpam = -6;
			this.scoutAllowance = 2;
			this.maxScoutAllowance = 2;

			boolean earlyWorker = Math.max(callback.getMap().getHeight(), callback.getMap().getWidth()) > 640 && callback.getMap().getHeight() + callback.getMap().getWidth() >= 1280;
			boolean bigMap = callback.getMap().getHeight() + callback.getMap().getWidth() >= 1536;
			if (bigMap){
				raiderSpam = -9;
			}
			
			if (earlyWorker){
				this.scoutAllowance = 3;
				this.maxScoutAllowance = 3;
			}

			if (defName.equals("factorytank")){
				scoutAllowance = 2;
				expensiveRaiderSpam = -4;
			}

			if (defName.equals("factorygunship")){
				raiderSpam = -2;
			}

			if (defName.equals("factoryveh")) {
				scoutAllowance--;
				raiderSpam = -4;
			}

			if (defName.equals("factoryhover") && bigMap){
				expensiveRaiderSpam = -2;
			}

			if (defName.equals("factoryspider")){
				raiderSpam = -9;
			}

			if (callback.getMap().getHeight() + callback.getMap().getWidth() < 1280){
				raiderSpam /= 2;
				this.scoutAllowance = 1;
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
