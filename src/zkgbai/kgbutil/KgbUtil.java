package zkgbai.kgbutil;

import com.springrts.ai.oo.AIFloat3;

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
        float x = dest.x - start.x;
        float z = dest.z - start.z;
        float d = (float) Math.sqrt((x*x) + (z*z));
        x /= d;
        z /= d;
        dir.x = start.x + (x * distance);
        dir.z = start.z + (z * distance);
        return dir;
    }

    public static float distance(AIFloat3 v0, AIFloat3 v1){
        float dx = v0.x - v1.x;
        float dz = v0.z - v1.z;
        return (float) Math.sqrt(dx*dx+dz*dz);
    }
}
