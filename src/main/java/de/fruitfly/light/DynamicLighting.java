package de.fruitfly.light;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.newdawn.slick.Color;

import de.fruitfly.Game;
import de.fruitfly.light.Lightmap.Box;
import de.fruitfly.map.BSP.BSPTreeNode;
import de.fruitfly.map.Face;
import de.fruitfly.map.Vertex;

public class DynamicLighting {
	private List<Face> litFacesPerLight = new LinkedList<Face>();

	public void bake(Lightmap lightmap, BSPTreeNode bsp, List<Light> lights, ILightEquation le, Set<Face> affectedFaces) {
		affectedFaces.clear();
		lightmap.reset();
		int[] pixels = lightmap.getPixels();
		int lms = lightmap.getSize();

		for (Light light : lights) {
			litFacesPerLight.clear();

			bsp.collectFacesInRadius(light.getPosition(), light.getDistance(), litFacesPerLight);
			for (Face f : litFacesPerLight) {
				for (Lumen l : f.getLightAttachment().getLumen()) {
					if (l.lastAccess != Game.ticker) {
						l.getEnergy().r = 0.0f;
						l.getEnergy().g = 0.0f;
						l.getEnergy().b = 0.0f;
						l.lastAccess = Game.ticker;
					}
				}
			}
		
			List<Light> singleLight = new LinkedList<Light>();
			singleLight.add(light);
			le.solve(litFacesPerLight, singleLight, lightmap);
			affectedFaces.addAll(litFacesPerLight);
		}
		
		for (Face face : affectedFaces) {
			
			LightAttachment la = face.getLightAttachment();
			Box b = lightmap.getRegion(la.getXLumen() + 2, la.getYLumen() + 2);
			if (b == null) {
				throw new RuntimeException("Lightmap is full!");
			}
			b.face = face;
						
			float uDisp = (b.x + 1) / ((float)lightmap.getWidth());
			float vDisp = (b.y + 1) / ((float)lightmap.getHeight());
			for (Vertex v : face.getVertices()) {
				v.getDyLightUV().set(v.getBaseLightUV());
				v.getDyLightUV().x += uDisp;
				v.getDyLightUV().y += vDisp;
				
			}

			
			
			for (Lumen lumen : la.getLumen()) {
			
				Color c = lumen.getEnergy();
	
				int pOld = pixels[((b.y + lumen.getYLumen() + 1)) * lms + (b.x + lumen.getXLumen() + 1)];
				
				int pNew = 	0xff000000 |
							Math.min((((pOld & 0x00ff0000) >> 16) + c.getBlueByte()), 255) << 16 |
							Math.min((((pOld & 0x0000ff00) >> 8) + c.getGreenByte()), 255) << 8 |
							Math.min((((pOld & 0x000000ff) >> 0) + c.getRedByte()), 255) << 0;
				
				//pixels[((b.y + lumen.getYLumen())) * lms + (b.x + lumen.getXLumen())] = 0xffffffff;
				pixels[((b.y + lumen.getYLumen() + 1)) * lms + (b.x + lumen.getXLumen() + 1)] = pNew;

			}
		}
		
		//lightmap.postProcess();
		lightmap.bind();
		lightmap.upload();
	}
}
