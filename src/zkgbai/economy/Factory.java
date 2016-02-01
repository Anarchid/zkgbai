package zkgbai.economy;

import com.springrts.ai.oo.clb.OOAICallback;
import com.springrts.ai.oo.clb.Unit;
import zkgbai.ZKGraphBasedAI;

/**
 * Created by haplo on 1/16/2016.
 */
public class Factory extends Worker {
    public float raiderSpam = 0;

    public Factory(Unit u, boolean firstFac){
        super(u);

        if (firstFac){
            OOAICallback callback = ZKGraphBasedAI.getInstance().getCallback();
            this.raiderSpam = ((int) Math.ceil(Math.random()*9.0));
            raiderSpam = Math.max(4, raiderSpam)* -1;

            if (callback.getMap().getHeight() > 896 || callback.getMap().getWidth() > 896){
                raiderSpam = -9;
            }

            if (u.getDef().getName().equals("factorycloak")){
                raiderSpam = -9;
                if (callback.getMap().getHeight() < 640 && callback.getMap().getWidth() < 640){
                    raiderSpam = -2;
                }
            }

            if (u.getDef().getName().equals("factoryhover")){
                raiderSpam = -2;
            }

            if (u.getDef().getName().equals("factoryveh")){
                raiderSpam = -4;
            }

            if (callback.getMap().getHeight() < 640 && callback.getMap().getWidth() < 640){
                raiderSpam /= 2;
            }
        }
    }
}
