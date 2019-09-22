package zkgbai.kgbutil;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.OOAICallback;

import java.util.List;

public class HeightMap {
	float[] data;
	int width;
	int height;
	
	public HeightMap(OOAICallback callback){
		width = callback.getMap().getWidth();
		height = callback.getMap().getHeight();
		data = new float[width * height];
		List<Float> map = callback.getMap().getHeightMap();
		for (int i = 0; i < data.length; i++){
			data[i] = map.get(i);
		}
	}
	
	public AIFloat3 getHighestPointInRadius(AIFloat3 pos, float r){
		int cx = Math.round(pos.x/8f);
		int cy = Math.round(pos.z/8f);
		int radius = Math.round(r/8f);
		
		int beginX = Math.max(cx - radius + 1, 0);
		int endX =   Math.min(cx + radius, width);
		
		int beginY = Math.max(cy - radius + 1, 0);
		int endY =   Math.min(cy + radius, height);
		
		float maxHeight = -1f;
		int maxX = 0;
		int maxY = 0;
		// technically this searches a square. Calculating a circle isn't difficult but it's probably expensive,
		// and for the usage here there's no harm and probably some benefit in searching the extra points.
		for (int x = beginX; x < endX; x++) {
			for (int y = beginY; y < endY; y++) {
				int index = (y * width) + x;
				float tmpheight = data[index];
				if(tmpheight > maxHeight) {
					maxHeight = tmpheight;
					maxX = x;
					maxY = y;
				}
			}
		}
		return new AIFloat3(maxX * 8f, 0, maxY * 8f);
	}
}
