package de.fruitfly.light;

import java.util.LinkedList;
import java.util.List;

import de.fruitfly.map.Face;

public class LightAttachment {
	private List<Lumen> lumen;
	private int xLumen, yLumen;
	private Surface surface;
	
	public LightAttachment(Face face, int xLumen, int yLumen) {
		this.lumen = new LinkedList<Lumen>();
		this.xLumen = xLumen;
		this.yLumen = yLumen;
		face.setLightAttachment(this);
	}

	public List<Lumen> getLumen() {
		return lumen;
	}

	public int getXLumen() {
		return xLumen;
	}

	public int getYLumen() {
		return yLumen;
	}
	
	public void setSurface(Surface s) {
		this.surface = s;
	}
	
	public Surface getSurface()
	{
		return surface;
	}
}