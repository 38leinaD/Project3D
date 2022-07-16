package de.fruitfly.light;

import java.io.File;
import java.util.List;

import org.newdawn.slick.Color;

import de.fruitfly.Game;
import de.fruitfly.light.Lightmap.Box;
import de.fruitfly.map.Face;
import de.fruitfly.map.Vertex;

public class LightWorker extends Thread {
	private boolean isFinished = false;

	private ILightEquation le;
	private List<Face> faces;
	private List<Light> lights;
	private Lightmap lightmap;
	
	public LightWorker(ILightEquation le, List<Face> faces, List<Light> lights, Lightmap lightmap) {
		this.le = le;
		this.faces = faces;
		this.lights = lights;
		this.lightmap = lightmap;
	}
	
	public void run() {
		le.solve(faces, lights, lightmap);
		isFinished = true;
	}
	
	public void postProcess() {
		int[] pixels = lightmap.getPixels();
		int lms = lightmap.getSize();
	
		lightmap.reset();
		
		for (Face face : faces) {
			
			LightAttachment la = face.getLightAttachment();
			Box b = lightmap.getRegion(la.getXLumen() + 2, la.getYLumen() + 2);
			if (b == null) {
				throw new RuntimeException("Lightmap is full!");
			}
			b.face = face;
			
			float uDisp = (b.x + 1) / ((float)lightmap.getWidth());
			float vDisp = (b.y + 1) / ((float)lightmap.getHeight());
			for (Vertex v : face.getVertices()) {
				v.getLightUV().x = v.getBaseLightUV().x + uDisp;
				v.getLightUV().y = v.getBaseLightUV().y + vDisp;
				
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
		
		
		lightmap.postProcess();
		//lightmap.writeToFile(new File("C:/Users/daniel.platz/Dropbox/Dev/Java/Games/Project3D/Project/resources/light.png"));
		lightmap.writeToFile(new File("/Users/daniel/Dropbox/Dev/Java/Games/Project3D/Project/resources/light.png"));

		lightmap.bind();
		lightmap.upload();
		
		System.out.println("PP");
	}
	
	public boolean isFinished() {
		return isFinished;
	}
}
