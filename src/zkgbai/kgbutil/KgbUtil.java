package zkgbai.kgbutil;

import com.springrts.ai.AI;
import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Unit;

/**
 * Created by haplo on 1/4/2016.
 */
public class KgbUtil {
    public static AIFloat3 getRadialPoint(AIFloat3 position, Float radius){
        // returns a random point lying on a circle around the given position.
        AIFloat3 pos = new AIFloat3();
        double angle = Math.random()*2*Math.PI;
        double vx = Math.cos(angle);
        double vz = Math.sin(angle);
        pos.x = (float) (position.x + radius*vx);
        pos.z = (float) (position.z + radius*vz);
        return pos;
    }

    public static AIFloat3 getDirectionalPoint(AIFloat3 start, AIFloat3 dest, float distance){
        AIFloat3 dir = new AIFloat3();

        // First derive a normalized direction vector.
        float x = dest.x - start.x;
        float z = dest.z - start.z;
        float d = (float) Math.sqrt((x*x) + (z*z));
        x /= d;
        z /= d;

        // Then apply it relative to the start position with the given distance.
        dir.x = start.x + (x * distance);
        dir.z = start.z + (z * distance);
        return dir;
    }
    
    public static AIFloat3 getAngularPoint(AIFloat3 start, AIFloat3 dest, float radius){
        AIFloat3 radir = new AIFloat3();
        float x = dest.x - start.x;
        float z = dest.z - start.z;
        double angle = Math.atan2(z, x);
        if (Math.random() > 0.5){
            angle -= Math.random()*0.3*Math.PI;
        }else{
            angle += Math.random()*0.3*Math.PI;
        }
    
        double vx = Math.cos(angle);
        double vz = Math.sin(angle);
        radir.x = (float) (start.x + radius*vx);
        radir.z = (float) (start.z + radius*vz);
        return radir;
    }
    
    public static AIFloat3 getFormationPoint(AIFloat3 start, AIFloat3 compare, AIFloat3 dest){
        float x = start.x - compare.x;
        float z = start.z - compare.z;
        return new AIFloat3(dest.x + x, dest. y, dest.z + z);
    }
    
    public static float getSpeed(Unit u){
	    AIFloat3 vel = u.getVel();
	    return (float) (Math.sqrt((vel.x * vel.x) + (vel.z * vel.z)) * 30d);
    }
    
    public static float lerp(float start, float end, double scale){
        float amount = (float) scale;
        return (end * amount) + (start * (1f - amount));
    }

    public static float distance(AIFloat3 v0, AIFloat3 v1){
        float dx = v0.x - v1.x;
        float dz = v0.z - v1.z;
        return (float) Math.sqrt(dx*dx+dz*dz);
    }
}
