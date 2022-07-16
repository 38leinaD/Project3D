package de.fruitfly.map.vis;

import de.fruitfly.map.Face;

public class Portal {

	public Face face;
	public int id;
	public Portal(Face f, int id) {
		face = f;
		this.id = id;
	}

	public Portal(Portal p) {
		this.face = new Face(p.face);
		this.id = p.id;
	}
	
	public Sector[] sectors = new Sector[2];
}
