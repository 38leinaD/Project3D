package de.fruitfly.light;

import java.util.List;

import de.fruitfly.map.Face;

public interface ILightEquation {
	public void solve(List<Face> faces, List<Light> lights, Lightmap lightmap);
	public boolean isDone();
}
