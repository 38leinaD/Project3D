package de.fruitfly.collision;

import java.util.List;

import org.lwjgl.util.vector.Vector3f;

import de.fruitfly.Entity;
import de.fruitfly.Game;
import de.fruitfly.map.Const;
import de.fruitfly.map.Face;
import de.fruitfly.map.Plane;

public class Collision {
	
	public static float getCollisionRadius(Entity entity,  Vector3f direction) {
		float a = entity.getXYCollisionRadius() * direction.x;
		float b = entity.getXYCollisionRadius() * direction.y;
		float c = entity.getZCollisionRadius() * direction.z;
		return (float) Math.sqrt(a * a + b * b + c * c);
	}
	
	private static boolean collisionRejected(Entity entity, Face face, Vector3f startPos, Vector3f endPos) {
		float collRadius = getCollisionRadius(entity, face.getPlane().getNormal());
		
		float startSide = Vector3f.dot(startPos, face.getPlane().getNormal()) - face.getPlane().getDistance();
		
		float endSide = Vector3f.dot(endPos, face.getPlane().getNormal()) - face.getPlane().getDistance();
	
		if ((startSide - collRadius) * (endSide - collRadius) < 0) {
			return false;
		}
		else {
			return true;
		}
	}
	
	public static PolygonCollision getPolygonCollision(Entity entity, List<Face> faces, Vector3f startPos, Vector3f endPos) {
		for (Face face : faces) {
			if (collisionRejected(entity, face, startPos, endPos)) continue;
			if (Game.markedFaces.size() > 0 && Game.markedFaces.get(0) == face) {
				//System.out.println("HERE!" + startPos + " -> " + endPos);
			}
			boolean in = true;
			
			for (Plane plane : face.getPerpendicularPlanes()) {
				float collRadius = getCollisionRadius(entity, plane.getNormal());
				float adjDistance = plane.getDistance() - collRadius;
				float side = Vector3f.dot(endPos, plane.getNormal()) - adjDistance;
				if (side - Const.EPSILON < 0) {
					in = false;
					break;
				}
			}

			if (in) {
				float endSide = Vector3f.dot(endPos, face.getPlane().getNormal()) - face.getPlane().getDistance();
				float collRadius = getCollisionRadius(entity, face.getPlane().getNormal());
				Vector3f newPos = new Vector3f(face.getPlane().getNormal());
				newPos.scale((collRadius - endSide));
				Vector3f.add(endPos, newPos, newPos);
				
				PolygonCollision pc = new PolygonCollision(face, startPos);
				return pc;
			}
		}
		return null;
	}
	
	public static Vector3f handleCollision(Entity entity, List<Face> faces, Vector3f startPos, Vector3f endPos) {
		int i=0;
		Vector3f currentEndPos = new Vector3f(endPos);
		Game._debugFaces.clear();
		while(i++ < 3) {
			PolygonCollision cpc = getPolygonCollision(entity, faces, startPos, currentEndPos);
			
			if (cpc == null) {
				break;
			}
			else {
				
				Game._debugFaces.add(cpc.getFace());
				currentEndPos = cpc.getNewPosition();
			}
		}
		
		if (i==3) {
			return startPos;
		}
		else {
			return currentEndPos;
		}
	}
}
