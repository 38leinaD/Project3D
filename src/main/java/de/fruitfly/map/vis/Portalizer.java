package de.fruitfly.map.vis;

import java.util.LinkedList;
import java.util.List;

import org.lwjgl.util.vector.Vector3f;

import de.fruitfly.map.BSP.BSPTreeNode;
import de.fruitfly.map.Face;
import de.fruitfly.map.Merger;
import de.fruitfly.map.Face.ClipResult;
import de.fruitfly.map.Side;

// main idea from here:
// http://www.exaflop.org/docs/naifgfx/naifebsp.html

// http://tower22.blogspot.de/2011/07/x-ray.html
public class Portalizer {
	private List<Sector> sectors;
	private List<Portal> portals;

	public void process(BSPTreeNode root) {
		// create sectors
		sectors = new LinkedList<Sector>();
		for (BSPTreeNode leaf : root.leafs) {
			sectors.add(new Sector(leaf));
		}
		
		// create portals
		portals = new LinkedList<Portal>();
		int numDividers = root.dividers.size();
		int numPortals = numDividers;
				
		for (int i=0; i<numDividers; i++) {
			BSPTreeNode node = root.dividers.get(i);
			Portal p = new Portal(new Face(node.divider, true, null), i);
			portals.add(p);
		}
		
		List<Portal> queue = new LinkedList<Portal>();
		queue.addAll(portals);
		portals = new LinkedList<Portal>();
		
		while (queue.size() > 0) {
			Portal p = queue.remove(0);
			boolean noSplit = true;
			for (BSPTreeNode node : root.dividers) {
				Side side = node.divider.classifyFace(p.face);
				
				if (side == Side.Spanning) {
					ClipResult clip = p.face.clip(node.divider);
					
					Portal pFront = new Portal(clip.front, p.id);
					Portal pBack = new Portal(clip.back, p.id);
					
					queue.add(pFront);
					queue.add(pBack);
					noSplit = false;
					break;
				}
			}
			if (noSplit) {
				portals.add(p); // final
			}
		}

		// Final portal sizes
		for (int x=0; x<portals.size(); x++) {
			Portal p = portals.get(x);
			p.id = x;
		}
		System.out.println("NUM P: " + portals.size());
		// push portals down bsp
		for (Portal p : portals) {
			pushPortal(new Portal(p), root);
		}
		
		int num = 0;
		for (Sector s : sectors) {
			num+=s.getPortals().size();
		}
		System.out.println("NUM P2: " + num);
		
		int dups = 0;
		portals = new LinkedList<Portal>();
		// check for singles
		for (int j=0; j<sectors.size(); j++) {
			Sector s1 = sectors.get(j);
next:		for (int l=0; l<s1.getPortals().size(); l++) {
				Portal p1 = s1.getPortals().get(l);
				
				for (int k=0; k<sectors.size(); k++) {
					if (j==k) continue;
				
					Sector s2 = sectors.get(k);
		
					for (int m=0; m<s2.getPortals().size(); m++) {
						Portal p2 = s2.getPortals().get(m);
						if (p1 == p2) {
							if (p1.sectors[0] == null) {
								p1.sectors[0] = s1;
								p1.sectors[1] = s2;
								portals.add(p1);
							}
							continue next; 
						}
					}
				}
				
				dups++;
				s1.getPortals().remove(l--);
			}

		}
		
		System.out.println("MUM DUPS: " + dups);

		queue = new LinkedList<Portal>();
		queue.addAll(portals);
		System.out.println("Pre final " + queue.size());
		portals = new LinkedList<Portal>();
		
next:	while (queue.size() > 0) {
			Portal p = queue.remove(0);
			
			for (Sector s : p.sectors) {
				
				for (Face f : s.bspLeaf.faces) {
					Side side = f.getPlane().classifyFace(p.face);
					if (side == Side.Spanning) {
						ClipResult clip = p.face.clip(f.getPlane());
						
						Portal pFront = new Portal(clip.front, p.id);
						pFront.sectors[0] = p.sectors[0];
						pFront.sectors[1] = p.sectors[1];
						Portal pBack = new Portal(clip.back, p.id);
						pBack.sectors[0] = p.sectors[0];
						pBack.sectors[1] = p.sectors[1];
						
						queue.add(pFront);
						queue.add(pBack);
						continue next;
					}
					else if (side == Side.Back) {
						continue next;
					}
					else if (f.contains(p.face)) {
						continue next;
					}

				}
			}
			portals.add(p);
		}
		
		System.out.println("FINAL PORTALS: " + portals.size());		
			
		for (int i=0; i<portals.size(); i++) {
			portals.get(i).face.calcSideData();
			for (int j=0; j<portals.size(); j++) {
				if (i==j) continue;
				
				if (portals.get(i).face.contains(portals.get(j).face)) {
					portals.remove(j);
					if (j<=i) i--;
					j--;
					
				}
			}
		}
		
		for (Sector s : sectors) {
			s.bspLeaf.sector = s;
			s.getPortals().clear();
		}

		for (Portal p : portals) {
			p.sectors[0].getPortals().add(p);
			p.sectors[1].getPortals().add(p);
		}
	
		
		System.out.println("FINAL PORTALS: " + portals.size());		
		// make inwards
		
		// remove extras
	}
	
	public void floodFillClean(Vector3f start, BSPTreeNode bsp) {
		BSPTreeNode node = bsp.findLeaf(start);
		Sector s1 = node.sector;
		s1.flooded = true;
		
		List<Sector> queue = new LinkedList<Sector>();
		queue.add(s1);
		
		while (queue.size() > 0) {
			Sector s = queue.remove(0);
			for (Portal p : s.getPortals()) {
				int otherSectorId = 0;
				if (p.sectors[0].equals(s)) {
					otherSectorId = 1;
				}
				
				Sector sNew = p.sectors[otherSectorId];
				if (!sNew.flooded) {
					sNew.flooded = true;
					queue.add(sNew);
				}
			}
		}
		
		// Remove non-flooded sectors and portals and clear those bsp leafs
		for (int i=0; i<sectors.size(); i++) {
			Sector s = sectors.get(i);
			if (!s.flooded) {
				sectors.remove(i--);
				
				// remove portals of sector
				for (Portal p : s.getPortals()) {
					int otherSectorId = 0;
					if (p.sectors[0].equals(s)) {
						otherSectorId = 1;
					}
					
					p.sectors[otherSectorId].getPortals().remove(p);
					portals.remove(p);
				}
				
				// clean bsp node of sector
				s.bspLeaf.faces.clear();
			}
		}
	}

	public void mergePortals() {
		Merger merger = new Merger();
		
		// portals might be cut; merge
		
		for (Sector s : sectors) {
			for (int i=0; i<s.getPortals().size(); i++) {
				Portal p1 = s.getPortals().get(i);
				boolean merged = false;
				for (int j=0; j<s.getPortals().size(); j++) {
					if (i==j) continue;
					Portal p2 = s.getPortals().get(j);
					if (p1.face.classifyFace(p2.face) != Side.On) continue;
					
					int otherSectorId1 = 0;
					if (p1.sectors[0].equals(s)) {
						otherSectorId1 = 1;
					}
					int otherSectorId2 = 0;
					if (p2.sectors[0].equals(s)) {
						otherSectorId2 = 1;
					}
					// same plane but lead into different sectors
					if (p1.sectors[otherSectorId1] != p2.sectors[otherSectorId2]) continue;
					
					Face f = merger.tryMerge(p1.face, p2.face);
					
					if (f != null) {
						// Portals can be merged to one
						Portal pNew = new Portal(f, p1.id);
						pNew.sectors = p1.sectors;
						
						Sector s2 = p1.sectors[otherSectorId1];
						s.getPortals().remove(p1);
						s2.getPortals().remove(p1);

						s.getPortals().remove(p2); 
						s2.getPortals().remove(p2);
						
						s.getPortals().add(pNew);
						s2.getPortals().add(pNew);
						
						// also set list of portals
						portals.set(i, pNew);
						portals.remove(p2);
						
						merged = true;
						break;
					}
				}
				
				if (merged) {
					System.out.println("Merged portals");
					i--;
				}
			}
		}
	}
	
	private void pushPortal(Portal p, BSPTreeNode node) {
		if (node.isLeaf()) {
			sectors.get(node.leafId).getPortals().add(p);
			return;
		}
		Side side = node.divider.classifyFace(p.face);
		if (side == Side.Front) {
			pushPortal(p, node.leftChild);
		}
		else if (side == Side.Back) {
			pushPortal(p, node.rightChild);
		}
		else if (side == Side.On) {
			pushPortal(p, node.leftChild);
			pushPortal(p, node.rightChild);
		}
		else {
			System.out.println("ERROR; no split should happen...");
		}
	}

	public List<Sector> getSectors() {
		return sectors;
	}
	
	public List<Portal> getPortals() {
		return portals;
	}
}
