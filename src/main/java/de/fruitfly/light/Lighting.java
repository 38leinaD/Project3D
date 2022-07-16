package de.fruitfly.light;

import java.util.LinkedList;
import java.util.List;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import de.fruitfly.Game;
import de.fruitfly.MathUtil;
import de.fruitfly.map.Face;
import de.fruitfly.map.Plane;
import de.fruitfly.map.Ray;
import de.fruitfly.map.Vertex;

public class Lighting {
	private final static float lumenPerWorldUnit = 2.0f;
	private final static float worldUnitsPerLumen = 1.0f/lumenPerWorldUnit;

	private LightWorker lightworker = null;
	private ILightEquation le = null;
	private Lightmap lightmap = null;
	private List<Face> faces = null;
	private List<Light> lights = null;
	
	public void bake(Lightmap lightmap, List<Face> faces, List<Light> lights, ILightEquation le) {
		if (lightworker != null) {
			System.out.println("Currently baking");
		}
		
		this.lightmap = lightmap;
		this.faces = faces;
		this.lights = lights;
		this.le = le;
		
		for (Face face : faces) {
			Vector3f normal = face.getPlane().getNormal();
			Vector3f absNormal = new Vector3f(normal);
			absNormal.x = Math.abs(normal.x);
			absNormal.y = Math.abs(normal.y);
			absNormal.z = Math.abs(normal.z);
			
			List<Vector2f> uvs = new LinkedList<Vector2f>();
			
			// bbox
			float minA = Float.POSITIVE_INFINITY;
			float minB = Float.POSITIVE_INFINITY;
			float maxA = Float.NEGATIVE_INFINITY;
			float maxB = Float.NEGATIVE_INFINITY;
			
			// texture space axis + origin
			Vector3f axisA = new Vector3f();
			Vector3f axisB = new Vector3f();
			Vector3f axisOrigin = new Vector3f();
			
			// what is the main axis: 1: positiv in this direction; -1: opposite
			int xAxis = 0;
			int yAxis = 0;
			int zAxis = 0;
			int axis = 0;

			if (absNormal.x > absNormal.y && absNormal.x > absNormal.z) {
				// x-axis is main -> project to y-z plane
				axis = xAxis = normal.x > 0.0f ? 1 : -1;
				for (Vertex v : face.getVertices()) {
					Vector3f p = v.getPosition();
					uvs.add(new Vector2f(p.y, p.z));
				}
				
				axisA.y = axis;
				axisB.z = 1.0f;
				
				axisOrigin.x = face.getPlane().getDistance() * axis;
			}
			else if (absNormal.y > absNormal.x && absNormal.y > absNormal.z) {
				axis = yAxis = normal.y > 0.0f ? 1 : -1;
				for (Vertex v : face.getVertices()) {
					Vector3f p = v.getPosition();
					uvs.add(new Vector2f(p.x, p.z));
				}
				
				axisA.x = -axis;
				axisB.z = 1.0f;
			
				axisOrigin.y = face.getPlane().getDistance() * axis;
			}
			else {
				axis = zAxis = normal.z > 0.0f ? 1 : -1;
				for (Vertex v : face.getVertices()) {
					Vector3f p = v.getPosition();
					uvs.add(new Vector2f(p.x, p.y));
				}
				
				axisA.x = 1.0f;
				axisB.y = axis;

				axisOrigin.z = face.getPlane().getDistance() * axis;
			}
			
			// project axis onto plane
			axisA = face.getPlane().projectOntoPlane(axisA);
			axisA.normalise();
			
			axisB = face.getPlane().projectOntoPlane(axisB);
			axisB.normalise();
			
			face.axisA = axisA;
			face.axisB = axisB;
			face.axisOrigin = axisOrigin;
			
			for (Vector2f uv : uvs) {
				if (uv.x < minA) minA = uv.x;
				if (uv.x > maxA) maxA = uv.x;
				
				if (uv.y < minB) minB = uv.y;
				if (uv.y > maxB) maxB = uv.y;
			}
			float aShift = 0;
			float bShift = 0;

			
			if (xAxis != 0) {
				if (axis > 0) {
					aShift = (float) (worldUnitsPerLumen * Math.floor(minA / worldUnitsPerLumen));
				}
				else {
					aShift = (float) (worldUnitsPerLumen * Math.ceil(maxA / worldUnitsPerLumen));
				}
				bShift = (float) (worldUnitsPerLumen * Math.floor(minB / worldUnitsPerLumen)); 
			}
			else if (yAxis != 0) {
				if (axis > 0) {
					aShift = (float) (worldUnitsPerLumen * Math.ceil(maxA / worldUnitsPerLumen));
				}
				else {
					aShift = (float) (worldUnitsPerLumen * Math.floor(minA / worldUnitsPerLumen));
				}
				bShift = (float) (worldUnitsPerLumen * Math.floor(minB / worldUnitsPerLumen)); 
			}
			else if (zAxis != 0) {
				if (axis > 0) {
					bShift = (float) (worldUnitsPerLumen * Math.floor(minB / worldUnitsPerLumen));
				}
				else {
					bShift = (float) (worldUnitsPerLumen * Math.ceil(maxB / worldUnitsPerLumen));
				}
				
				aShift = (float) (worldUnitsPerLumen * Math.floor(minA / worldUnitsPerLumen));
			}

			Vector3f aa = new Vector3f(axisA);

			if (xAxis != 0) {
				aa.scale(aShift * axis);
			}
			else if (yAxis != 0){
				aa.scale(aShift * -axis);
			}
			else if (zAxis != 0) {
				aa.scale(aShift);
			}
			Vector3f bb = new Vector3f(axisB);
			
			if (xAxis != 0) {
				bb.scale(bShift);
			}
			else if (yAxis != 0) {
				bb.scale(bShift);
			}
			else if (zAxis != 0) {
				bb.scale(bShift * axis);
			}
			
			Vector3f.add(axisOrigin, aa, axisOrigin);
			Vector3f.add(axisOrigin, bb, axisOrigin);
			
			for (Vector2f uv : uvs) {
				uv.scale(lumenPerWorldUnit);
				uv.x -= Math.floor(minA * lumenPerWorldUnit);
				uv.y -= Math.floor(minB * lumenPerWorldUnit);
				uv.scale(1.0f/lightmap.getSize());
			}
			
			float aGap = 0;
			if (axis > 0) {
				aGap = (float)(minA - worldUnitsPerLumen * Math.floor(axisOrigin.x / worldUnitsPerLumen)) * lumenPerWorldUnit;
			}
			else {
				aGap = (float)(maxA - worldUnitsPerLumen * Math.ceil(axisOrigin.x / worldUnitsPerLumen)) * lumenPerWorldUnit;
			}
			float bGap = axisOrigin.y - bShift;
						
			maxA *= lumenPerWorldUnit;
			maxB *= lumenPerWorldUnit;
			minA *= lumenPerWorldUnit;
			minB *= lumenPerWorldUnit;
			
			maxA -= Math.floor(minA);
			maxB -= Math.floor(minB);
			minA -= Math.floor(minA);
			minB -= Math.floor(minB);
			
			// 0..1 range uv coords
			float maxUV_A = maxA / lightmap.getSize();
			float maxUV_B = maxB / lightmap.getSize();
			float minUV_A = minA / lightmap.getSize();
			float minUV_B = minB / lightmap.getSize();
			
			float uvDispForSingleLumen = 1.0f / lightmap.getSize();
			for (int i=0; i<face.getVertices().size(); i++) {
				Vector2f uv = uvs.get(i);
				if (axis == -1) {
					if (xAxis != 0) uv.x = minUV_A + (maxUV_A - uv.x);
					if (zAxis != 0) uv.y = minUV_B + (maxUV_B - uv.y);
				}
				else if (axis == 1) {
					 if (yAxis != 0) uv.x = minUV_A + (maxUV_A - uv.x);
				}
				//uv.x += uvDispForSingleLumen;
				//uv.y += uvDispForSingleLumen;
				face.getVertices().get(i).getLightUV().set(uvs.get(i));
				face.getVertices().get(i).getBaseLightUV().set(uvs.get(i));
			}
			
			Vector3f axisC = new Vector3f();
			Vector3f.cross(axisA, axisB, axisC);
			
			int xLumen = (int)(Math.ceil(maxA) - Math.floor(minA));
			int yLumen = (int)(Math.ceil(maxB) - Math.floor(minB));
			
			LightAttachment la = new LightAttachment(face, xLumen, yLumen);
			
			Matrix4f scale = new Matrix4f();
			scale.setIdentity();
			scale.m00 = worldUnitsPerLumen;
			scale.m11 = worldUnitsPerLumen;
			scale.m22 = worldUnitsPerLumen;
			
			Matrix4f rotation = new Matrix4f();
			rotation.setIdentity();
			rotation.m00 = axisA.x;
			rotation.m01 = axisA.y;
			rotation.m02 = axisA.z;
			
			rotation.m10 = axisB.x;
			rotation.m11 = axisB.y;
			rotation.m12 = axisB.z;
			
			rotation.m20 = axisC.x;
			rotation.m21 = axisC.y;
			rotation.m22 = axisC.z;
			
			Matrix4f translation = new Matrix4f();
			translation.setIdentity();
			translation.m30 = axisOrigin.x;
			translation.m31 = axisOrigin.y;
			translation.m32 = axisOrigin.z;
			
			// scale also!
			
			Matrix4f transform = null;
			transform = Matrix4f.mul(rotation, scale, null);
			transform = Matrix4f.mul(translation, transform, null);
			
			//Matrix4f invTransform = Matrix4f.invert(transform, null);

			for (int x=-1; x<xLumen + 1; x++) {
				for (int y=-1; y<yLumen + 1; y++) {
					Vector4f pUVSpace = new Vector4f(x + 0.5f, y + 0.5f, 0.0f, 1.0f);	
					
					// todo: this is not really correct
					// we work in UV space but should do the checking in worldspace. skewed surfaces don't work with this...
					/*
					if (pUVSpace.x < Math.ceil(minA)) {
						float dx = (x + 1 - minA);
						pUVSpace.x = x + 1 - dx/2.0f;
					}
					else if (pUVSpace.x > Math.floor(maxA)) {
						float dx = (float) (maxA - Math.floor(maxA));
						pUVSpace.x = x + dx/2.0f;
					}
					
					if (pUVSpace.y < Math.ceil(minB)) {
						float dy = (y + 1 - minB);
						pUVSpace.y = y + 1 - dy/2.0f;
					}
					else if (pUVSpace.y > Math.floor(maxB)) {
						float dy = (float) (maxB - Math.floor(maxB));
						pUVSpace.y = y + dy/2.0f;
					}
					*/
					
					Vector4f pWorldSpace = new Vector4f();
					Matrix4f.transform(transform, pUVSpace, pWorldSpace);
					Vector3f pWorldSpace3D = new Vector3f(pWorldSpace.x, pWorldSpace.y, pWorldSpace.z);
					
					Ray r = Ray.fromPoints(face.center, pWorldSpace3D);
					Face blockFace = Game.bspTree.pick(r, Game.bspTree).getFace();
					if (blockFace != null && blockFace != face) {
						// lumen is outside surface an blocked by adjacent polygon
						if (x == -1) pUVSpace.x += 1;
						if (y == -1) pUVSpace.y += 1;
						if (x == xLumen) pUVSpace.x -= 1;
						if (y == yLumen) pUVSpace.y -= 1;
						Matrix4f.transform(transform, pUVSpace, pWorldSpace);
						pWorldSpace3D = new Vector3f(pWorldSpace.x, pWorldSpace.y, pWorldSpace.z);
					}
					
					Lumen l = new Lumen(face, x, y, pWorldSpace3D);
					la.getLumen().add(l);
					
				}
			}
		}
		
		lightworker = new LightWorker(le, faces, lights, lightmap);
		lightworker.start();
	}
		
	public static void calculatePlanarMappingTransform(Plane plane, List<Vector3f> points, PlanarMapping mapping) {
		Matrix4f worldToTexSpace = mapping.worldToTex;
		Matrix4f texToWorldSpace = mapping.texToWorld;
		mapping.plane = new Plane(plane);
		
		Vector3f axisOrigin = mapping.axisOrigin;
		Vector3f uAxis = mapping.uAxis;
		Vector3f vAxis = mapping.vAxis;
		Vector3f wAxis = mapping.wAxis;
		
		Vector3f normal = plane.getNormal();
		Vector3f absNormal = new Vector3f(plane.getNormal());
		absNormal.x = Math.abs(absNormal.x);
		absNormal.y = Math.abs(absNormal.y);
		absNormal.z = Math.abs(absNormal.z);
		
		// bbox
		float minX = Float.POSITIVE_INFINITY;
		float minY = Float.POSITIVE_INFINITY;
		float minZ = Float.POSITIVE_INFINITY;
		float maxX = Float.NEGATIVE_INFINITY;
		float maxY = Float.NEGATIVE_INFINITY;
		float maxZ = Float.NEGATIVE_INFINITY;
		
		for (Vector3f p : points) {
			if (p.x < minX) minX = p.x;
			if (p.x > maxX) maxX = p.x;
			
			if (p.y < minY) minY = p.y;
			if (p.y > maxY) maxY = p.y;
		
			if (p.z < minZ) minZ = p.z;
			if (p.z > maxZ) maxZ = p.z;
		}

		int axis = 0;
		
		worldToTexSpace.setIdentity();
		texToWorldSpace.setIdentity();

		float scale = lumenPerWorldUnit;
		
		if (absNormal.x > absNormal.y && absNormal.x > absNormal.z) {
			// x-axis is main -> project to y-z plane
			axis = plane.getNormal().x > 0.0f ? 1 : -1;

			// rotation
			worldToTexSpace.m00 = 0.0f; worldToTexSpace.m10 = 1.0f; worldToTexSpace.m20 = 0.0f;
			worldToTexSpace.m01 = 0.0f; worldToTexSpace.m11 = 0.0f; worldToTexSpace.m21 = 1.0f;
			worldToTexSpace.m02 = 0.0f; worldToTexSpace.m12 = 0.0f; worldToTexSpace.m22 = 0.0f;
			
			// translation
			worldToTexSpace.m30 = -minY;
			worldToTexSpace.m31 = -minZ;
			
			texToWorldSpace.m00 = 0.0f; texToWorldSpace.m10 = 1.0f; texToWorldSpace.m20 = 0.0f;
			texToWorldSpace.m01 = 0.0f; texToWorldSpace.m11 = 0.0f; texToWorldSpace.m21 = 1.0f;
			texToWorldSpace.m02 = 0.0f; texToWorldSpace.m12 = 0.0f; texToWorldSpace.m22 = 0.0f;
		}
		else if (absNormal.y > absNormal.x && absNormal.y > absNormal.z) {
			axis =  plane.getNormal().y > 0.0f ? 1 : -1;
			
			// rotation
			worldToTexSpace.m00 = -axis; worldToTexSpace.m10 = 0.0f; worldToTexSpace.m20 = 0.0f;
			worldToTexSpace.m01 = 0.0f; worldToTexSpace.m11 = 0.0f; worldToTexSpace.m21 = 1.0f;
			worldToTexSpace.m02 = 0.0f; worldToTexSpace.m12 = 0.0f; worldToTexSpace.m22 = 0.0f;
			
			// translation
			worldToTexSpace.m30 = -minZ;
			worldToTexSpace.m31 = -minX;
		}
		else {
			axis = plane.getNormal().z > 0.0f ? 1 : -1;
			
			// rotation
			worldToTexSpace.m00 = 1.0f * scale; worldToTexSpace.m10 = 0.0f; worldToTexSpace.m20 = 0.0f;
			worldToTexSpace.m01 = 0.0f; worldToTexSpace.m11 = axis * scale; worldToTexSpace.m21 = 0.0f;
			worldToTexSpace.m02 = 0.0f; worldToTexSpace.m12 = 0.0f; worldToTexSpace.m22 = 0.0f;
			
			// translation
			worldToTexSpace.m30 = -minX * scale;
			worldToTexSpace.m31 = -minY * scale;
			
			/*
			texToWorldSpace.m00 = 0.0f; texToWorldSpace.m10 = 1.0f; texToWorldSpace.m20 = 0.0f;
			texToWorldSpace.m01 = 0.0f; texToWorldSpace.m11 = 0.0f; texToWorldSpace.m21 = 1.0f;
			texToWorldSpace.m02 = 0.0f; texToWorldSpace.m12 = 0.0f; texToWorldSpace.m22 = 0.0f;
			*/
			
//			texToWorldSpace.m02 = 1.0f; texToWorldSpace.m12 = 1.0f; texToWorldSpace.m22 = 0.0f;
//			
//			texToWorldSpace.m22 = 1.0f;
//			texToWorldSpace.m33 = 0.0f;
			
//			texToWorldSpace.m30 = -worldToTexSpace.m30;
//			texToWorldSpace.m31 = -worldToTexSpace.m31;
			
			uAxis.set(1.0f, 0.0f, 0.0f);
			vAxis.set(0.0f, 1.0f, 0.0f);
			
			Vector3f uNormalComp = new Vector3f(plane.getNormal());
			uNormalComp.scale(Vector3f.dot(plane.getNormal(), uAxis));
			Vector3f.sub(uAxis, uNormalComp, uAxis);
			uAxis.normalise();
			
			Vector3f vNormalComp = new Vector3f(plane.getNormal());
			vNormalComp.scale(Vector3f.dot(plane.getNormal(), vAxis));
			Vector3f.sub(vAxis, vNormalComp, vAxis);
			vAxis.normalise();
			
			Vector3f.cross(uAxis, vAxis, wAxis);
			
			axisOrigin.x = minX;
			axisOrigin.y = minY;
			axisOrigin.z = (plane.getDistance() - (normal.x * axisOrigin.x + normal.y * axisOrigin.y)) / normal.z;
			
			texToWorldSpace.m00 = uAxis.x;
			texToWorldSpace.m01 = uAxis.y;
			texToWorldSpace.m02 = uAxis.z;
			
			texToWorldSpace.m10 = vAxis.x;
			texToWorldSpace.m11 = vAxis.y;
			texToWorldSpace.m12 = vAxis.z;
			
			texToWorldSpace.m20 = wAxis.x;
			texToWorldSpace.m21 = wAxis.y;
			texToWorldSpace.m22 = wAxis.z;

			
			texToWorldSpace.m30 = axisOrigin.x;
			texToWorldSpace.m31 = axisOrigin.y;
			texToWorldSpace.m32 = axisOrigin.z;
		}
	}
	
	public void tick() {
		if (lightworker != null && lightworker.isFinished()) {
			lightworker.postProcess();
			
			if (!le.isDone()) {
				lightworker = new LightWorker(le, faces, lights, lightmap);
				lightworker.start();
			}
			else {
				lightworker = null;
			}
		}
	}
	
	public boolean isBaking() {
		return lightworker != null;
	}
	
	public static void main(String[] args) {
		Matrix4f t = new Matrix4f();
		Plane p = new Plane(new Vector3f(0.0f, 0.0f, 1.0f), 1.3f);
		List<Vector3f> points = new LinkedList<Vector3f>();
		points.add(new Vector3f(0.5f, 0.5f, 1.3f));
		points.add(new Vector3f(1.5f, 0.5f, 1.3f));
		points.add(new Vector3f(1.0f, 1.0f, 1.3f));
		
		//calculatePlanarMappingTransform(p, points, t, t);
		
		Vector4f r = new Vector4f();
		
		for (Vector3f point : points) {
			Matrix4f.transform(t, MathUtil.getPoint4f(point), r);
			System.out.println(r);
		}
	}
}
