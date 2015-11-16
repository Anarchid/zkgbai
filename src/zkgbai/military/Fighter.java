package zkgbai.military;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;


public class Fighter {
    public float metalValue;
    public int id;
    public Squad squad;
    protected Unit unit;

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

    public float getMaxHealth(){
        return unit.getMaxHealth();
    }

    public float getCurrentHealth(){
        return unit.getHealth();
    }

    public void fightTo(AIFloat3 pos, int frame){
        AIFloat3 target = getRadialPoint(pos, 200f);
        unit.fight(target, (short) 0, frame+1000);
    }

    public void moveTo(AIFloat3 pos, int frame){
        AIFloat3 target = getRadialPoint(pos, 100f);
        unit.moveTo(target, (short) 0, frame+1000);
    }

    protected AIFloat3 getRadialPoint(AIFloat3 position, Float radius){
        // returns a random point lying on a circle around the given position.
        AIFloat3 pos = new AIFloat3();
        double angle = Math.random()*2*Math.PI;
        double vx = Math.cos(angle);
        double vz = Math.sin(angle);
        pos.x = (float) (position.x + radius*vx);
        pos.z = (float) (position.z + radius*vz);
        return pos;
    }
}
