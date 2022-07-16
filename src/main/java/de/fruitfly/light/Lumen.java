package de.fruitfly.light;

import java.util.List;

import org.lwjgl.util.vector.Vector3f;
import org.newdawn.slick.Color;

import de.fruitfly.map.Face;
import de.fruitfly.util.Pair;

public class Lumen {
	private int xLumen, yLumen;
	private Vector3f worldCoord;
	private Face face;
	private Color inEnergy = new Color(0.0f, 0.0f, 0.0f);
	private Color exEnergy = new Color(0.0f, 0.0f, 0.0f);
	private Color energy = new Color(0.0f, 0.0f, 0.0f);
	public int lastAccess = -1;
	private List<Pair<Lumen, Float>> formFactors;
	
	public Lumen(Face f, int lmX, int lmY, Vector3f worldCoord) {
		xLumen = lmX;
		yLumen = lmY;
		this.worldCoord = worldCoord;
		this.face = f;
	}

	public int getXLumen() {
		return xLumen;
	}

	public int getYLumen() {
		return yLumen;
	}

	public Vector3f getWorldCoord() {
		return worldCoord;
	}

	public Color getInEnergy() {
		return inEnergy;
	}
	
	public Color getExEnergy() {
		return exEnergy;
	}
	
	public Color getEnergy() {
		return energy;
	}

	public List<Pair<Lumen, Float>> getFormFactors() {
		return formFactors;
	}

	public void setFormFactors(List<Pair<Lumen, Float>> formFactors) {
		this.formFactors = formFactors;
	}
}
