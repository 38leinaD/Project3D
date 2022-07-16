package de.fruitfly.light;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.lwjgl.util.vector.Vector3f;
import org.newdawn.slick.Color;

import de.fruitfly.Game;
import de.fruitfly.map.BSP.BSPTreeNode.PickResult;
import de.fruitfly.map.Const;
import de.fruitfly.map.Face;
import de.fruitfly.map.Fuzzy;
import de.fruitfly.map.Ray;
import de.fruitfly.util.Pair;

public class RadiosityLightEquation implements ILightEquation {

	private int numPasses;
	private int pass = 0;
	public RadiosityLightEquation(int numPasses) {
		this.numPasses = numPasses;
	}
	private List<Lumen> lumenWE = new ArrayList<Lumen>();
	@Override
	public void solve(List<Face> faces, List<Light> lights, Lightmap lightmap) {

		int numFaces = faces.size();
		int stepSize = numFaces / 10;
		int step = 0;
		int iFace = 0;
				
		if (pass == 0) {
			System.out.print("Base-light " + pass + " [");
			for (Face f : faces) {
				for (Lumen l : f.getLightAttachment().getLumen()) {
					l.getExEnergy().r = 0.0f;
					l.getExEnergy().g = 0.0f;
					l.getExEnergy().b = 0.0f;
					
					l.getInEnergy().r = 0.0f;
					l.getInEnergy().g = 0.0f;
					l.getInEnergy().b = 0.0f;
					
					l.getEnergy().r = 0.0f;
					l.getEnergy().g = 0.0f;
					l.getEnergy().b = 0.0f;
				}
				
				iFace++;
				if (iFace > (step * stepSize)) {
					step++;
					System.out.print(".");
				}
			}
			
			// Map lights to a surface
			// As we are doing radiosity, we do not work with point-lights but with patches of energy
			List<Face> closeFaces = new LinkedList<Face>();
			for (Light l : lights) {
				closeFaces.clear();
				Game.bspTree.collectFacesInRadius(l.getPosition(), 0.5f, closeFaces);
				Face closestFace = null;
				float minDistance = Float.MAX_VALUE;
				Vector3f surfaceToLight = new Vector3f();
				for (Face f : closeFaces) {
					Vector3f v = f.getVertices().get(0).getPosition();
					Vector3f.sub(l.getPosition(), v, surfaceToLight);
					float distance = Vector3f.dot(surfaceToLight, f.getPlane().getNormal());
					if (distance >= 0.0f && distance < minDistance) {
						closestFace = f;
						minDistance = distance;
					}
				}
				
				if (closestFace == null) {
					System.out.println("*** Light (" + l + ") could not be mapped to a surface");
				}
				else {
					closestFace.isLightSource = true;
					for (Lumen lu : closestFace.getLightAttachment().getLumen()) {	
						lu.getInEnergy().r = l.getColor().r;
						lu.getInEnergy().g = l.getColor().g;
						lu.getInEnergy().b = l.getColor().b;
						
						lumenWE.add(lu);
					}
				}
			}
		}
		else if (pass == 1) {
			System.out.print("Form-Factor " + pass + " [");
			
			int numFormFactors = 0;
			// Calculate Form-Factors
			
			Vector3f lumen2ToLumen1 = new Vector3f();
			step = 0;
			iFace = 0;
			/*
			System.out.println("SLEEPING");
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("AWAKE");
			*/
			System.out.println("NUM FACES: " + faces.size());
			for (Face f1 : faces) {
				
				// Do polygons Face each other? Do an early out with the same equation already used for lumen; just need to calculate a face-center
				float formFactorSum = 0.0f;
				for (Lumen l1 : f1.getLightAttachment().getLumen()) {
					l1.setFormFactors(new ArrayList<Pair<Lumen, Float>>(1000));
				
					for (Face f2 : faces) {
						if (f1 == f2) continue;
						if (f2.isLightSource) continue; // light sources cannot collect light; looks odd.
						if (Fuzzy.equal(f1.getPlane().getNormal(), f2.getPlane().getNormal())) continue;

						Color dist = l1.getExEnergy();
						boolean ofInterest = false;
						if (dist.r != 0.0f && dist.g != 0.0f && dist.b != 0.0f) {
							System.out.println("IMN");
							
							if (Game.markedFaces.size() >= 2 && Game.markedFaces.get(0) == f1 && Game.markedFaces.get(1) == f2) {
								System.out.println("HHHHHHHHHHHHHHHHHH");
								ofInterest = true;

							}
						}
						for (Lumen l2 : f2.getLightAttachment().getLumen()) {
							// Distance
							Vector3f.sub(l2.getWorldCoord(), l1.getWorldCoord(), lumen2ToLumen1);
							float lumenDistance = lumen2ToLumen1.length();
							
							if (lumenDistance <= Const.EPSILON) continue; // zero length vectors because of overlapping lumen...
							
							lumen2ToLumen1.normalise();

							// Angles
							float lumen1Angel = Vector3f.dot(f1.getPlane().getNormal(), lumen2ToLumen1);
							float lumen2Angel = -Vector3f.dot(f2.getPlane().getNormal(), lumen2ToLumen1);
							
							float formFactor = lumen1Angel * lumen2Angel;
							
							if (formFactor <= 0.0f) continue;
							
							formFactor /= (lumenDistance * lumenDistance);
							
							Ray r = Ray.fromPoints(l1.getWorldCoord(), l2.getWorldCoord());
							PickResult pick = Game.bspTree.pick(r, Game.bspTree);
							
							if (ofInterest) {
								System.out.println("CHECK");
							}
							
							if (pick != null && pick.getFace() != f1 && pick.getFace() != f2) {
								// blocked
								
							}
							else {
								formFactorSum += formFactor;
								l1.getFormFactors().add(new Pair<Lumen, Float>(l2, formFactor));
								numFormFactors++;
							}
						}

					}
					
					for (Pair<Lumen, Float> formFactor : l1.getFormFactors()) {
						formFactor.right = formFactor.right / formFactorSum;
					}
	
				}
				
				iFace++;
				if (iFace > (step * stepSize)) {
					step++;
					System.out.print(".");
				}
			}
			
			System.out.println("Number of Form Factors generated: " + numFormFactors);
			
		}
		else {
			System.out.print("Baking Pass " + pass + " [");
					
			step = 0;
			iFace = 0;
			
			for (Face distributingFace : faces) {
				for (Lumen distributingLumen : distributingFace.getLightAttachment().getLumen()) {
					Color dist = distributingLumen.getExEnergy();
					if (dist.r == 0.0f && dist.g == 0.0f && dist.b == 0.0f) continue;
					else {
						System.out.println("HHH");
					}
					for (Pair<Lumen, Float> formFactor : distributingLumen.getFormFactors()) {
						Lumen receivingLumen = formFactor.left;
						float ff = formFactor.right;
						
						Color recv = receivingLumen.getInEnergy();
						
						
						recv.r += ff * dist.r * distributingFace.color.r;
						recv.g += ff * dist.g * distributingFace.color.g;
						recv.b += ff * dist.b * distributingFace.color.b;
					}
				}
				
				iFace++;
				if (iFace > (step * stepSize)) {
					step++;
					System.out.print(".");
				}
			}
		}
		
		if (pass != 1) {
			
			for (Face f : faces) {
				for (Lumen l : f.getLightAttachment().getLumen()) {
					
					l.getEnergy().r += l.getInEnergy().r * 2;
					l.getEnergy().g += l.getInEnergy().g * 2;
					l.getEnergy().b += l.getInEnergy().b * 2;
					
					l.getExEnergy().r = l.getInEnergy().r * 7;
					l.getExEnergy().g = l.getInEnergy().g * 7;
					l.getExEnergy().b = l.getInEnergy().b * 7;
					
					l.getInEnergy().r = 0.0f;
					l.getInEnergy().g = 0.0f;
					l.getInEnergy().b = 0.0f;
				}
			}
		}

		pass++;
		System.out.print("] Done");

	}

	@Override
	public boolean isDone() {
		return numPasses == pass;
	}
}
