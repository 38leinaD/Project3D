package de.fruitfly.map;

import java.util.LinkedList;
import java.util.List;

import org.lwjgl.util.vector.Vector3f;
import org.newdawn.slick.Color;

import de.fruitfly.light.LightAttachment;

public class Face {
	private static final float BOGUS_MAX = 999999.0f;
	private static final float EPSILON = 0.01f;
	
	private List<Vertex> vertices;
	private BoundingBox bb;
	private Plane plane;
	private List<Plane> perpendicularPlanes;
	
	private LightAttachment lightAttachment;
	private EdgeAttachment edgeAttachment;
	
	public boolean usedAsDivider = false;
	public Color markColor;
	public Color color;
	public Vector3f axisA, axisB, axisOrigin;
	
	public boolean isLightSource = false;
	
	public boolean wasCut = false;
	
	public Vector3f center;
	
	public Face(Plane plane, boolean buildMaxFace, Color color) {
		this(plane, buildMaxFace ? 8000 : -1, color);
	}
		
	public Face(Plane plane, int size, Color color) {
		this.markColor = new Color((float)Math.random(), (float)Math.random(), (float)Math.random(), 1.0f);
		//this.color = new Color(0.9f, 0.9f, 0.9f);
		this.vertices = new LinkedList<Vertex>();
		this.bb = new BoundingBox();
		this.plane = plane;
		this.color = color;

		if (size == -1) return;

		int majorAxis = -1;
		float maxNormalCompLength = -BOGUS_MAX;
		
		if (Math.abs(plane.getNormal().x) > maxNormalCompLength) {
			maxNormalCompLength = Math.abs(plane.getNormal().x);
			majorAxis = 0;
		}
		
		if (Math.abs(plane.getNormal().y) > maxNormalCompLength) {
			maxNormalCompLength = Math.abs(plane.getNormal().y);
			majorAxis = 1;
		}

		if (Math.abs(plane.getNormal().z) > maxNormalCompLength) {
			maxNormalCompLength = Math.abs(plane.getNormal().z);
			majorAxis = 2;
		}

		if (majorAxis == -1) {
			throw new RuntimeException("No major axis found");
		}

		Vector3f v1 = new Vector3f(0.0f, 0.0f, 0.0f);
		Vector3f v2 = new Vector3f();

		if (majorAxis == 0 || majorAxis == 1) {
			v1.z = 1.0f;
		}
		else {
			v1.x = 1.0f;
		}

		Vector3f vtmp = new Vector3f();
		Vector3f orig = new Vector3f();

		float nn = Vector3f.dot(v1, plane.getNormal());
		
		vtmp.set(plane.getNormal());
		vtmp.scale(-nn);
		Vector3f.add(v1, vtmp ,v1);
		v1.normalise();

		orig.set(plane.getNormal());
		orig.scale(plane.getDistance());
		Vector3f.cross(plane.getNormal(), v1, v2);

		v1.scale(size);
		v2.scale(size);

		// Project a big bounding box via defined plane-axis'

		Vector3f p1 = new Vector3f(orig);
		Vector3f.add(p1, v1, p1);
		Vector3f.add(p1, v2, p1);
		this.vertices.add(new Vertex(p1));

		Vector3f p2 = new Vector3f(orig);
		Vector3f.sub(p2, v1, p2);
		Vector3f.add(p2, v2, p2);
		this.vertices.add(new Vertex(p2));

		Vector3f p3 = new Vector3f(orig);
		Vector3f.sub(p3, v1, p3);
		Vector3f.sub(p3, v2, p3);
		this.vertices.add(new Vertex(p3));

		Vector3f p4 = new Vector3f(orig);
		Vector3f.add(p4, v1, p4);
		Vector3f.sub(p4, v2, p4);
		this.vertices.add(new Vertex(p4));

		this.bb = new BoundingBox(p1, p2, p3, p4);
	}
	
	public LightAttachment getLightAttachment() {
		return lightAttachment;
	}

	public void setLightAttachment(LightAttachment lightAttachment) {
		this.lightAttachment = lightAttachment;
	}

	public Face(Face f) {
		this.markColor = new Color((float)Math.random(), (float)Math.random(), (float)Math.random(), 1.0f);
		//this.color = new Color(0.9f, 0.9f, 0.9f);
		
		this.vertices = new LinkedList<Vertex>();
		this.bb = new BoundingBox(f.bb);
		this.plane = new Plane(f.plane);
		this.color = f.color;
		for (Vertex v : f.vertices) {
			this.addVertex(new Vertex(v));
		}
	}
	
	public void calcSideData() {
		// perp planes
		perpendicularPlanes = new LinkedList<Plane>();
		
		for (int i=0; i<vertices.size(); i++) {
			Vector3f p0 = vertices.get(i).getPosition();
			Vector3f p1 = vertices.get((i + 1) % vertices.size()).getPosition();
			Vector3f v = new Vector3f();
			Vector3f.sub(p1, p0, v);
			
			Vector3f normal = new Vector3f();
			Vector3f.cross(plane.getNormal(), v, normal);
			normal.normalise();
			
			float distance = Vector3f.dot(normal, p1);
			
			Plane p = new Plane(normal, distance);
			perpendicularPlanes.add(p);
		}
		
		// calc face center
		center = new Vector3f();
		for (Vertex vtx : vertices) {
			Vector3f v = vtx.getPosition();
			Vector3f.add(center, v, center);
		}
		center.scale(1.0f/vertices.size());
	}

	public class ClipResult {
		public Face front, back;
	}
	
	public ClipResult clip(Plane plane) {
		Side[] sides = new Side[this.vertices.size()];
		float[] dists = new float[this.vertices.size()];
		int[] side_counts = { 0, 0, 0 };
		ClipResult result = new ClipResult();

		for (int i=0; i<this.vertices.size(); i++) {
			Vector3f wp = this.vertices.get(i).getPosition();
			float dist = Vector3f.dot(wp, plane.getNormal()) - plane.getDistance();
			dists[i] = dist;

			if (dist > EPSILON) {
				sides[i] = Side.Front;
			}
			else if (dist < -EPSILON) {
				sides[i] = Side.Back;
			}
			else {
				sides[i] = Side.On;
			}

			side_counts[sides[i].ordinal()]++;
		}

		if (side_counts[Side.Front.ordinal()] == 0) {
			result.back = new Face(this);
			return result;
		}

		if (side_counts[Side.Back.ordinal()] == 0) {
			result.front = new Face(this);
			return result;
		}

		result.front = new Face(this.plane, false, this.color);
		result.back = new Face(this.plane, false, this.color);

		for (int i=0; i<this.vertices.size(); i++) {
			Vector3f p = this.vertices.get(i).getPosition();

			if (sides[i] == Side.On) {
				Vector3f fp = new Vector3f(p);
				result.front.addVertex(new Vertex(fp));

				Vector3f bp = new Vector3f(p);
				result.back.addVertex(new Vertex(bp));
				continue;
			}

			if (sides[i] == Side.Front) {
				Vector3f fp = new Vector3f(p);
				result.front.addVertex(new Vertex(fp));
			}

			if (sides[i] == Side.Back) {
				Vector3f bp = new Vector3f(p);
				result.back.addVertex(new Vertex(bp));
			}

			if (sides[(i+1) % this.vertices.size()] == Side.On ||
				sides[(i+1) % this.vertices.size()] == sides[i]) {
				continue;
			}

			Vector3f p2 = this.vertices.get((i+1) % this.vertices.size()).getPosition();
			float alpha = dists[i] / (dists[i] - dists[(i+1) % this.vertices.size()]);
			Vector3f mid = new Vector3f(p2);
			// mid = p + alpha * (p2 - p)
			Vector3f.sub(mid, p, mid);
			mid.scale(alpha);
			Vector3f.add(mid, p, mid);

			Vector3f mid_cpy = new Vector3f(mid);

			result.front.addVertex(new Vertex(mid));
			result.back.addVertex(new Vertex(mid_cpy));
		}

		return result;
	}
	
	public void addVertex(Vertex p) {
		this.vertices.add(p);
		this.bb.expand(p.getPosition());
	}

	public boolean contains(Vector3f p) {
		float dist = Vector3f.dot(p, this.plane.getNormal());
		if (Math.abs(dist - this.plane.getDistance()) < EPSILON) return true;
		else return false;
	}

	public boolean touchesVertexOfFace(Face f) {
		for (Vertex p : f.vertices) {
			if (this.contains(p.getPosition())) return true;
		}
		return false;
	}

	// todo: here also merge lines
	public void collapse() {
		Vector3f v = new Vector3f();
		List<Vector3f> removedVertices = new LinkedList<Vector3f>();
		for (int i=0; i<this.vertices.size(); i++) {
			for (int j=0; j<this.vertices.size(); j++) {
				if (i == j) continue;
				Vector3f.sub(this.vertices.get(i).getPosition(), this.vertices.get(j).getPosition(), v);
				if (Math.sqrt(Vector3f.dot(v, v)) < EPSILON) {
					// collapse
					removedVertices.add(this.vertices.get(j).getPosition());
				}
			}				
		}

		this.vertices.removeAll(removedVertices);
		
		if (this.vertices.size() < 3) {
			throw new RuntimeException("Face collapsed.");
		}
	}

	public Side classifyPoint(Vector3f p) {
		float sideValue = Vector3f.dot(this.getPlane().getNormal(), p);
		if (Fuzzy.equal(sideValue, this.getPlane().getDistance())) {
			return Side.On;
		}
		else if (Fuzzy.less(sideValue, this.getPlane().getDistance())) {
			return Side.Back;
		}
		else {
			return Side.Front;
		}
	}
	
	public Side classifyFace(Face f) {
		int numFront = 0;
		int numBack = 0;
		
		for (Vertex p : f.getVertices()) {
			Side sp = this.classifyPoint(p.getPosition());
			if (sp == Side.Front) {
				numFront++;
			}
			else if (sp == Side.Back) {
				numBack++;
			}
		}
		
		if (numFront > 0 && numBack == 0) {
			return Side.Front;
		}
		else if (numFront == 0 && numBack > 0) {
			return Side.Back;
		}
		else if (numFront == 0 && numBack == 0) {
			return Side.On;
		}
		else {
			return Side.Spanning;
		}
	}
	
	public boolean on(Vector3f p) {
		if (this.plane.classifyPoint(p) != Side.On) return false;
		for (Plane plane : getPerpendicularPlanes()) {
			if (Vector3f.dot(p, plane.getNormal()) - plane.getDistance() < -EPSILON) {
				return false;
			}
		}
		return true;
	}
	
	public boolean contains(Face f) {
		for (Vertex v : f.getVertices()) {
			if (!this.on(v.getPosition())) return false;
		}
		return true;
	}
	
	public static boolean isConvexSet(List<Face> faces) {
		for (int i=0; i<faces.size(); i++) {
			for (int j=0; j<faces.size(); j++) {
				if (i == j) continue;
				Face f1 = faces.get(i);
				Face f2 = faces.get(j);
				Side side = f1.classifyFace(f2);
				if (side != Side.Front) {
					if (side == Side.On && Vector3f.dot(f1.getPlane().getNormal(), f2.getPlane().getNormal()) > 0.1f) continue; // on face and same direction of normal
					return false;
				}
			}
		}
		return true;
	}
	
	// http://openinggl.blogspot.de/2012/03/sphere-intersecting-polygon.html
	public boolean intersectsSphere(Vector3f center, float radius) {
		// 1. Check if the sphere lies out the plane formed by the polygon
		float distancePlaneSphereCenter = Vector3f.dot(this.getPlane().getNormal(), center) - this.getPlane().getDistance();
		
		if (Math.abs(distancePlaneSphereCenter) > radius) return false;
		
		// 2. Projection of Sphere lies inside polygon
		Vector3f normal = getPlane().getNormal();
		Vector3f centerProjectedToPlane = new Vector3f(
			center.x - normal.x * distancePlaneSphereCenter,
			center.y - normal.y * distancePlaneSphereCenter,
			center.z - normal.z * distancePlaneSphereCenter
		);
		
		boolean inside = true;
		for (Plane plane : getPerpendicularPlanes()) {
			if (Vector3f.dot(centerProjectedToPlane, plane.getNormal()) - plane.getDistance() < -EPSILON) {
				inside = false;
				break;
			}
		}
		
		if (inside) return true;
		
		// 3. Detecting Sphere-Edge Collisions
		Vector3f E = new Vector3f();
		Vector3f P = new Vector3f();
		Vector3f C = new Vector3f();
		for (int i=0; i<getVertices().size(); i++) {
			Vector3f curPoint = getVertices().get(i).getPosition();
			Vector3f nextPoint = getVertices().get((i+1) % getVertices().size()).getPosition();

			Vector3f.sub(nextPoint, curPoint, E);
			float edgeLength = E.length();
			E.normalise();
			Vector3f.sub(center, curPoint, P);
			
			float cd = Vector3f.dot(E, P);
			
			if (cd < 0 || cd > edgeLength) continue;
			
			C.x = curPoint.x + E.x * cd;
			C.y = curPoint.y + E.y * cd;
			C.z = curPoint.z + E.z * cd;
			
			if (Math.sqrt((C.x - center.x) * (C.x - center.x) + (C.y - center.y) * (C.y - center.y) + (C.z - center.z) * (C.z - center.z)) < radius) {
				return true;
			}
		}
		return false;
		
	}
	
	public List<Face> minus(List<Face> planeFaces) {
		List<Face> queue = new LinkedList<Face>();
		List<Face> subFaces = new LinkedList<Face>();
		queue.add(this);
next:	while(queue.size() != 0) {
			Face subFace = queue.remove(0);
			for (Face planeFace : planeFaces) {
				if (subFace.getPlane().classifyFace(planeFace) != Side.On) continue;
				if (planeFace.contains(subFace)) continue next;
				for (Plane edgePlane : planeFace.getPerpendicularPlanes()) {
					Side side = edgePlane.classifyFace(subFace);
					
					if (side == Side.Spanning) {
						ClipResult clip = subFace.clip(edgePlane);
						queue.add(clip.front);
						queue.add(clip.back);
						continue next;
					}
				}
			}
			subFaces.add(subFace);
		}
		
		return subFaces;
	}
	
	public List<Vertex> getVertices() {
		return vertices;
	}

	public BoundingBox getBoundingBox() {
		return bb;
	}

	public Plane getPlane() {
		return plane;
	}
	
	public List<Plane> getPerpendicularPlanes() {
		return perpendicularPlanes;
	}

	public EdgeAttachment getEdgeAttachment() {
		return edgeAttachment;
	}

	public void setEdgeAttachment(EdgeAttachment edgeAttachment) {
		this.edgeAttachment = edgeAttachment;
	}
	
	@Override
	public String toString() {
		return "Face [vertices=" + vertices + "]";
	}
}
