package zkgbai.economy;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.OOAICallback;
import com.springrts.ai.oo.clb.Unit;
import zkgbai.ZKGraphBasedAI;

/**
 * Created by haplo on 1/16/2016.
 */
public class Factory extends Worker {
    public float raiderSpam = 0;
    public float expensiveRaiderSpam = 0;
    AIFloat3 pos;

    public Factory(Unit u, boolean firstFac){
        super(u);
        pos = u.getPos();

        if (firstFac){
            OOAICallback callback = ZKGraphBasedAI.getInstance().getCallback();
            this.raiderSpam = ((int) Math.ceil(Math.random()*9.0));
            raiderSpam = Math.max(6, raiderSpam) * -1;

            boolean bigMap = (callback.getMap().getHeight() + callback.getMap().getWidth() > 1664);
            if (bigMap){
                raiderSpam = -9;

                if (u.getDef().getName().equals("factorytank")){
                    raiderSpam = -6;
                }
            }

            if (u.getDef().getName().equals("factorygunship")){
                raiderSpam = -4;
            }

            if (u.getDef().getName().equals("factoryveh")) {
                if (bigMap) {
                    raiderSpam = -5;
                } else {
                    raiderSpam = -4;
                }
            }

            if (u.getDef().getName().equals("factoryhover")){
                if (bigMap) {
                    raiderSpam = -7;
                } else {
                    raiderSpam = -6;
                }
                expensiveRaiderSpam = -1;
            }

            if (u.getDef().getName().equals("factoryspider")){
                raiderSpam = -9;
            }

            if (callback.getMap().getHeight() < 640 && callback.getMap().getWidth() < 640){
                raiderSpam /= 2;
            }

            if (u.getDef().getName().equals("factoryplane")){
                raiderSpam = 0;
            }
        }
    }

    @Override
    public AIFloat3 getPos(){
        return pos;
    }
}
