package zkgbai.military.unitwrappers;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;

import static zkgbai.kgbutil.KgbUtil.*;


public class Fighter {
    public float metalValue;
    public int id;
    public int index;
    public Squad squad;
    protected Unit unit;
    protected static final short OPTION_SHIFT_KEY = (1 << 5); //  32

    public Fighter(Unit u, float metal){
        this.unit = u;
        this.id = u.getUnitId();
        this.metalValue = metal;
    }

    public Unit getUnit(){
        return unit;
    }

    public AIFloat3 getPos(){
        return unit.getPos();
    }

    public void fightTo(AIFloat3 pos, int frame){
        AIFloat3 target = getRadialPoint(pos, 200f);
        unit.fight(target, (short) 0, frame+6000);
    }

    public void moveTo(AIFloat3 pos, int frame){
        unit.moveTo(pos, (short) 0, frame+6000);
    }

    @Override
    public boolean equals(Object o){
        if (o instanceof Fighter){
            Fighter f = (Fighter) o;
            return (f.id == id);
        }
        return false;
    }
}
