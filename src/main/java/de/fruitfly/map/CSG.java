package de.fruitfly.map;

import java.util.LinkedList;
import java.util.List;

import org.lwjgl.util.vector.Vector3f;

import de.fruitfly.map.Face.ClipResult;

public class CSG {
	
	private static final float EPSILON = 0.01f;
	
	private List<MBrush> brushes;
	private List<Face> faces;
	
	private List<Face> inside;
	private List<Face> outside;
	
	public void process(List<MBrush> brushes) {
		this.brushes = brushes;
		
		// The resulting set of faces; brushes do not exist anymore
		this.faces = new LinkedList<Face>();
		
		for (int i=0; i<this.brushes.size(); i++) {
			MBrush brushA = new MBrush(this.brushes.get(i));
			
			this.inside = new LinkedList<Face>();
			this.outside = new LinkedList<Face>();

			boolean overwrite = false;

			for (int j=0; j<brushA.getFaces().size(); j++) {
				this.outside.add(brushA.getFaces().get(j));
			}

			for (int j=0; j<this.brushes.size(); j++) {
				MBrush brushB = this.brushes.get(j);

				if (i == j) {
					overwrite = true;
					continue;
				}
				if (!brushA.getBoundingBox().intersects(brushB.getBoundingBox())) continue;

				// We put outsides from previous run/brush in insides as we only need to test these;
				// old insides are already inside and need to be discarded

				this.inside = this.outside; 
				this.outside = new LinkedList<Face>();

				// slice up brushA based on ALL other planes from brushBs
				// classify as inside/outside
				for (int k=0; k<brushB.getFaces().size(); k++) {
					Plane clipPlane = brushB.getFaces().get(k).getPlane();

					this.clipInsideList(clipPlane, overwrite);
				}
			}

			for (int j=0; j<this.outside.size(); j++) {
				this.faces.add(this.outside.get(j));
			}
		}

		int facesBefore = 0;
		for (int i=0; i<this.brushes.size(); i++) {
			facesBefore += this.brushes.get(i).getFaces().size();	
		}

		System.out.println("CSG changed number of faces from " + facesBefore + " to " + this.faces.size());
	}
	
	private void clipInsideList(Plane clipPlane, boolean precedence) {
		List<Face> newInsideList = new LinkedList<Face>();
		for (int l=0; l<this.inside.size(); l++) {
			Face face = this.inside.get(l);
			Face front = null;
			Face back = null;

			Vector3f v = new Vector3f(face.getPlane().getNormal());
			Vector3f.add(v, clipPlane.getNormal(), v);
			float length = (float) Math.sqrt(Vector3f.dot(v, v));

			boolean equalOrInverse = face.getPlane().equals(clipPlane) || face.getPlane().inverseOf(clipPlane);
			if (equalOrInverse) {
				if (length < EPSILON || precedence) { // opposing
					// for two coplanar and overlapping faces we need a tie-break; one has to go
					front = null;
					back = face;
				}
				else {
					front = face;
					back = null;
				}						
			}
			else {
				ClipResult clippedFaces = face.clip(clipPlane);
				front = clippedFaces.front;
				back = clippedFaces.back;
			}			

			if (front != null) {
				this.outside.add(front);
			}

			if (back != null) {
				newInsideList.add(back);
			}			
		}

		this.inside = newInsideList;
	}

	public List<Face> getFaces() {
		return this.faces;
	}
}
