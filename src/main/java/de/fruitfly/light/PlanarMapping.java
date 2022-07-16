package de.fruitfly.light;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import de.fruitfly.map.Plane;

public class PlanarMapping {
	public Matrix4f worldToTex = new Matrix4f();
	public Matrix4f texToWorld = new Matrix4f();
	
	public Vector3f axisOrigin = new Vector3f();
	public Vector3f uAxis = new Vector3f();
	public Vector3f vAxis = new Vector3f();
	public Vector3f wAxis = new Vector3f();
	
	public Plane plane;
	
	public void toWorld(float u, float v, Vector3f worldPoint) {
		worldPoint.x = u;
		worldPoint.y = v;
		worldPoint.z = (plane.getDistance() - (plane.getNormal().x * worldPoint.x + plane.getNormal().y * worldPoint.y)) / plane.getNormal().z;
		
		Vector3f.add(worldPoint, axisOrigin, worldPoint);
	}
}
