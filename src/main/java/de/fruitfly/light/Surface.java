package de.fruitfly.light;

import static org.lwjgl.opengl.GL11.*;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.lwjgl.util.vector.Vector3f;
import org.newdawn.slick.Color;

import de.fruitfly.Game;
import de.fruitfly.map.EdgeAttachment;
import de.fruitfly.map.Face;
import de.fruitfly.map.Fuzzy;
import de.fruitfly.map.Plane;
import de.fruitfly.map.Vertex;

public class Surface {
	private Plane plane;
	private List<Face> faces = new LinkedList<Face>();
	
	public Color _debugColor = new Color((float)Math.random(), (float)Math.random(), (float)Math.random());
	
	public Surface(Plane p) {
		this.plane = new Plane(p);
	}
	
	public static List<Surface> generateSurfaces(List<Face> faces) {
		for (Face f1 : faces) {
			for (Face f2 : faces) {
				if (f1 == f2) continue;
				for (int i=0; i<f1.getVertices().size(); i++) {
					Vector3f a1 = f1.getVertices().get(i).getPosition();
					Vector3f a2 = f1.getVertices().get((i+1) % f1.getVertices().size()).getPosition();
					for (int j=0; j<f2.getVertices().size(); j++) {
						Vector3f b1 = f2.getVertices().get(j).getPosition();
						Vector3f b2 = f2.getVertices().get((j+1) % f2.getVertices().size()).getPosition();
						
						if (Fuzzy.equal(a1, b2) && Fuzzy.equal(a2, b1)) {
							EdgeAttachment ea1 = f1.getEdgeAttachment();
							if (ea1 == null) {
								ea1 = new EdgeAttachment(f1);
							}
							
							EdgeAttachment ea2 = f2.getEdgeAttachment();
							if (ea2 == null) {
								ea2 = new EdgeAttachment(f2);
							}
							
							if (ea1.links[i] != null && ea2.links[j] != null) {
								// already set
							}
							else {
								ea1.links[i] = f2;
								ea2.links[j] = f1;
							}
						}
					}
				}
			}
		}
		
		Map<Face, Surface> faceSurfaces = new HashMap<Face, Surface>();
		List<Surface> surfaces = new LinkedList<Surface>();
		
		for (Face f0 : faces) {
			if (faceSurfaces.get(f0) != null) continue; // already assigned to a surface
			
			Surface surf = new Surface(f0.getPlane());
			surfaces.add(surf);
			
			List<Face> Q = new LinkedList<Face>();
			Q.add(f0);
			
			while (Q.size() > 0) {
				Face f = Q.remove(0);
				
				if (!f.getPlane().equals(surf.plane)) continue;

				surf.getFaces().add(f);
				faceSurfaces.put(f, surf);
				
				EdgeAttachment ea = f.getEdgeAttachment();
				for (int edge=0; edge<ea.links.length; edge++) {
					if (ea.links[edge] == null) continue;
					Face cf = ea.links[edge];
					
					if (faceSurfaces.get(cf) != null) continue; // already assigned  
					Q.add(cf);
				}
			}
		}
		
		return surfaces;
	}

	public Plane getPlane() {
		return plane;
	}

	public List<Face> getFaces() {
		return faces;
	}

	public void _debugRender() {
		glColor3f(_debugColor.r, _debugColor.g, _debugColor.b);
		for (Face f : faces) {
			glBegin(GL_TRIANGLE_FAN);
			for (Vertex vtx : f.getVertices()) {
				Vector3f v = vtx.getPosition();
				glVertex3f(v.x, v.y, v.z);
			}
			glEnd();
		}
	}
}
