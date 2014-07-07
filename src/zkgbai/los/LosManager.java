package zkgbai.los;

import java.util.List;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Map;
import com.springrts.ai.oo.clb.OOAICallback;

import zkgbai.Module;
import zkgbai.ZKGraphBasedAI;

public class LosManager extends Module {
	ZKGraphBasedAI parent;
	
	private List<Integer> losMap;
	private int mapWidth;
	private int mapHeight;
	private int losResolution;
	private Map map;

	private int losGridSize;
	
	public LosManager(ZKGraphBasedAI parent){
		this.parent = parent;
	}
	
	@Override
	public String getModuleName() {
		// TODO Auto-generated method stub
		return "LosManager";
	}
	
	@Override
    public int init(int teamId, OOAICallback callback) {
		this.map = callback.getMap();
		this.mapHeight = map.getHeight();
		this.mapWidth = map.getWidth();
		this.losResolution = callback.getMod().getLosMipLevel();
		this.losGridSize = (int) Math.pow((double)2,(double)losResolution);
		this.losMap = map.getLosMap();
        return 0;
    }
	
	@Override
	public int update(int frame){
		this.losMap = map.getLosMap();
		return 0;
	}
	
	public boolean isInLos(AIFloat3 position){
		//the value for the full resolution position (x, z) is at index ((z * width + x) / res) -
		//the last value, bottom right, is at index (width/res * height/res - 1)
		
		// convert from world coordinates to heightmap coordinates
		double x = (int) Math.floor(position.x/8);
		double z = (int) Math.floor(position.z/8);
		
		int gridWidth = mapWidth / losGridSize;
		int gridHeight = mapHeight / losGridSize;
	

		int gridX = (int)Math.floor((x/mapWidth)* gridWidth);
		int gridZ = (int)Math.floor((z/mapHeight)* gridHeight);
		
		int index = gridX + gridZ * gridWidth;  
		
		if(index > losMap.size()){
			return false;
		}
		return (losMap.get(index) > 0);
	}
	
}  