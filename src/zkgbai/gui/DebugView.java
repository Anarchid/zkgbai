package zkgbai.gui;

import zkgbai.ZKGraphBasedAI;


public class DebugView {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	ZKGraphBasedAI ai;
	//Image losImage;
	//Image threatImage;
	//Image mapTexture;
	//Graphics bufferGraphics;
	int mapWidth;
	int mapHeight;
	//private Image graphImage;
	private Boolean visible = false;
	
	public DebugView(ZKGraphBasedAI parent){
		this.ai = parent;
		this.mapWidth  =  parent.getCallback().getMap().getWidth();
		this.mapHeight = parent.getCallback().getMap().getHeight();
	}

	/*public void render(){
		Color threatColor = new Color(1f, 1f, 1f, 0.5f);
		Color graphColor = new Color(1f, 1f, 1f, 1f);

		bufferGraphics.setBackground(new Color(0,0,0,1));
		bufferGraphics.clear();

		//bufferGraphics.drawImage(losImage, 0, 0, graphColor);
		bufferGraphics.drawImage(threatImage, 0, 0, threatColor);
		bufferGraphics.drawImage(graphImage, 0, 0, graphColor);
	}
	
	public void setLosImage(Image bu){
		losImage = bu;
	}
	
	public void setMapTexture(Image bu){
		mapTexture = bu;
	}

	public void setThreatImage(Image threatMap) {
		threatImage = threatMap;
	}

	public void setGraphImage(Image graphImage) {
		this.graphImage = graphImage;
	}*/
}
