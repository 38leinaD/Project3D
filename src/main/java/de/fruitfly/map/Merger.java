package de.fruitfly.map;

import java.util.List;

import org.lwjgl.util.vector.Vector3f;

public class Merger {
	private static final float EPSILON = 0.1f;
	
	public void process(List<Face> faces) {
		int numFacesBefore = faces.size();
		for (int i=0; i<faces.size(); i++) {
			Face faceA = faces.get(i);
			for (int j=0; j<faces.size(); j++) {
				if (i == j)  continue;
				Face faceB = faces.get(j);

				if (!faceA.getPlane().equals(faceB.getPlane())) continue;

				Face newFace = this.tryMerge(faceA, faceB);

				if (newFace != null) {
					faces.set(i, newFace);
					faces.remove(j--);
					faceA = newFace;
				}
			}
		}

		System.out.println("Merging reduced number of faces from " + numFacesBefore + " to " + faces.size());
	}

	public Face tryMerge(Face faceA, Face faceB) {
		Vector3f v1 = new Vector3f();
		Vector3f v2 = new Vector3f();
		for (int i=0; i<faceA.getVertices().size(); i++) {
			Vector3f a1 = faceA.getVertices().get(i).getPosition();
			Vector3f a2 = faceA.getVertices().get((i+1) % faceA.getVertices().size()).getPosition();
			for (int j=0; j<faceB.getVertices().size(); j++) {
				Vector3f b1 = faceB.getVertices().get(j).getPosition();
				Vector3f b2 = faceB.getVertices().get((j+1) % faceB.getVertices().size()).getPosition();

				Vector3f d1 = new Vector3f(a1);
				Vector3f.sub(d1, b2, d1);
				
				Vector3f d2 = new Vector3f(a2);
				Vector3f.sub(d2, b1, d2);
				
				if (d1.length() > EPSILON || d2.length() > EPSILON) continue;

				// todo: need to check if merged polygon is still convex

				Face newFace = new Face(faceA.getPlane(), false, faceA.color);

				newFace.addVertex(new Vertex(a2));
				for (int k=(i+2) % faceA.getVertices().size(); k!=i; k=(k+1) % faceA.getVertices().size()) {
					newFace.addVertex(faceA.getVertices().get(k));
				}
				newFace.addVertex(new Vertex(a1));
				for (int l=(j+2) % faceB.getVertices().size(); l!=j; l=(l+1) % faceB.getVertices().size()) {
					newFace.addVertex(faceB.getVertices().get(l));
				}

				if (newFace.getVertices().size() != (faceA.getVertices().size() + faceB.getVertices().size() - 2)) {
					System.out.println("** Merging went wrong...");
					return null;
				}

				// merge vertices that lie on the same line
				
				for (int k=0; k<newFace.getVertices().size(); k++) {
					int numVerts = newFace.getVertices().size();
					int kminus = (k - 1 + numVerts) % numVerts;
					int kplus = (k + 1) % numVerts;
					
					v1.set(newFace.getVertices().get(k).getPosition());
					Vector3f.sub(v1, newFace.getVertices().get(kminus).getPosition(), v1);
					
					v2.set(newFace.getVertices().get(kplus).getPosition());
					Vector3f.sub(v2, newFace.getVertices().get(k).getPosition(), v2);
					
					v1.normalise();
					v2.normalise();
					if (Fuzzy.equal(v1, v2)) {
						// merge
						newFace.getVertices().remove(k--);
					}
				}
				
				return newFace;
			}	
		}

		return null;
	}
}
