package zkgbai.los;

import java.util.List;

import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.clb.Map;
import com.springrts.ai.oo.clb.OOAICallback;

import org.newdawn.slick.Image;
import org.newdawn.slick.ImageBuffer;
import zkgbai.Module;
import zkgbai.ZKGraphBasedAI;

public class LosManager extends Module {
	ZKGraphBasedAI parent;
	
	private List<Integer> losMap;
	private ImageBuffer losImage;

	private int mapWidth;
	private int mapHeight;
	private int losResolution;
	private int gridWidth;
	private int gridHeight;
	private Map map;

	private int losGridSize;
	
	public LosManager(ZKGraphBasedAI parent){
		this.parent = parent;
		OOAICallback callback = parent.getCallback();
		this.map = callback.getMap();
		this.mapHeight = map.getHeight();
		this.mapWidth = map.getWidth();
		this.losResolution = callback.getMod().getLosMipLevel();
		this.losGridSize = (int) Math.pow((double)2,(double)losResolution);
		this.losMap = map.getLosMap();
		this.gridWidth = mapWidth / losGridSize;
		this.gridHeight = mapHeight / losGridSize;
		this.losImage = new ImageBuffer(gridWidth+1, gridHeight+1);

		this.updateLosImage();
	}
	
	@Override
	public String getModuleName() {
		// TODO Auto-generated method stub
		return "LosManager";
	}
	
	@Override
	public int update(int frame){
		this.losMap = map.getLosMap();
		
		try{
			if(frame%5 == 0){
				updateLosImage();
			}
		}
		catch(Exception e){
			parent.printException(e);
		}
		
		return 0;
	}
	
	private void updateLosImage(){
		if(losImage != null){
			for(int x=0;x<gridWidth;x++){
				for(int z=0;z<gridHeight;z++){
					int coord = Math.min(x+z*gridWidth,losMap.size()-1);
					
					int value = losMap.get(coord) * 4;

					try{
						losImage.setRGBA(x, z, value, value, value, 255);
					}catch(Exception e){
						parent.debug("Exception when setting lospixel <"+x+","+z+"> out of <"+gridWidth+"x"+gridHeight+">");
						parent.printException(e);
					}
				}
			}
		}else{
			parent.debug("losImage is null!");
		}
	}
	
	public Image getImage(){
		return this.losImage.getImage();
	}
	
	public boolean isInLos(AIFloat3 position){
		return isInLos(position,0);
	}
	
	public boolean isInLos(AIFloat3 position, int level){
		//the value for the full resolution position (x, z) is at index ((z * width + x) / res) -
		//the last value, bottom right, is at index (width/res * height/res - 1)
		
		// convert from world coordinates to heightmap coordinates
		double x = (int) Math.floor(position.x/8);
		double z = (int) Math.floor(position.z/8);
		
		int gridX = (int)Math.floor((x/mapWidth)* gridWidth);
		int gridZ = (int)Math.floor((z/mapHeight)* gridHeight);
		
		int index = Math.min(gridX + gridZ * gridWidth,losMap.size()-1);  
		
		if(index >= losMap.size()){
			return false;
		}
		return (losMap.get(index) > level);
	}
	
}  