package zkgbai.military.unitwrappers;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;
import static zkgbai.kgbutil.KgbUtil.*;

/**
 * Created by aeonios on 11/14/2015.
 */
public class Strider extends Fighter {
    public int dgunReload;
    public int lastDgunFrame = 0;
    public Strider(Unit u, Float metal){
        super(u, metal);
        if (u.getDef().getName().equals("dante")) {
            this.dgunReload = 150;
        }else{
            this.dgunReload = 200;
        }
    }

    @Override
    public void fightTo(AIFloat3 pos, int frame){
        if (unit.getDef().getName().equals("scorpion")) {
            AIFloat3 target = getDirectionalPoint(pos, getPos(), 250f);
            unit.moveTo(target, (short) 0, frame + 6000);
        }else if (unit.getDef().getName().equals("armbanth") || unit.getDef().getName().equals("armorco")){
            AIFloat3 target = getDirectionalPoint(pos, getPos(), 350f);
            unit.moveTo(target, (short) 0, frame + 6000);
        }else{ // dante
            AIFloat3 target = getRadialPoint(pos, 50f);
            unit.moveTo(target, (short) 0, frame + 6000);
        }
    }
}
