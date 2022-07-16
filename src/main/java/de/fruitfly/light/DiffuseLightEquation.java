package de.fruitfly.light;

import java.util.List;

import org.lwjgl.util.vector.Vector3f;
import org.newdawn.slick.Color;

import de.fruitfly.Game;
import de.fruitfly.light.Lightmap.Box;
import de.fruitfly.map.Face;
import de.fruitfly.map.Ray;
import de.fruitfly.map.Vertex;

public class DiffuseLightEquation implements ILightEquation {

	@Override
	public void solve(List<Face> faces, List<Light> lights, Lightmap lightmap) {

		for (Face face : faces) {
			
			LightAttachment la = face.getLightAttachment();

			for (Lumen lumen : la.getLumen()) {
				for (Light light : lights) {
					Vector3f direction = new Vector3f();
					Ray r = new Ray(light.getPosition(), direction);
					Vector3f.sub(lumen.getWorldCoord(), light.getPosition(), direction);
					float lightLumenDistance = direction.length();
					
					float angle = Vector3f.dot(direction, face.getPlane().getNormal());
					if (angle >= 0.0f) continue;
					
					boolean inShadow = false;
					
					for (Face f2 : faces) {
						if (f2 == face) continue;
						Float dist = r.intersects(f2);
						if (dist == null) continue;
						else if (dist > lightLumenDistance - 0.001f) continue;
						else {
							inShadow = true;
							break; 
						}
					}
					
					
					if (inShadow) continue;
					
					Color c = new Color(light.getColor());
					Vector3f d = new Vector3f();
					Vector3f.sub(light.getPosition(), lumen.getWorldCoord(), d);
					c.scale(Math.max(1.0f - d.length() / (light.getDistance()), 0.0f));

					lumen.getEnergy().add(c);
				}
			}
		}

	}

	@Override
	public boolean isDone() {
		return true;
	}

}
