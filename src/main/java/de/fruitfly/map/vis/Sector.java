package de.fruitfly.map.vis;

import java.util.LinkedList;
import java.util.List;

import de.fruitfly.map.BSP.BSPTreeNode;

public class Sector {
	public BSPTreeNode bspLeaf;
	private List<Portal> portals;
	public boolean flooded = false;
	public int frame = -1;
	public Sector(BSPTreeNode bspLeaf) {
		this.bspLeaf = bspLeaf;
		this.portals = new LinkedList<Portal>();
	}

	public List<Portal> getPortals() {
		return portals;
	}
}
