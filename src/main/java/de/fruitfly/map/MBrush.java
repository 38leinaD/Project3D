package de.fruitfly.map;

import java.util.LinkedList;
import java.util.List;

import org.newdawn.slick.Color;

import de.fruitfly.map.Face.ClipResult;

public class MBrush {
	private static final float EPSILON = 0.01f;
	
	private List<Face> faces;
	private BoundingBox bb;
	
	public MBrush(MapFile.Brush mbrush) {
		this.faces = new LinkedList<Face>();
		this.bb = new BoundingBox();
	
		if (mbrush == null) return;
	
		for (int j=0; j<mbrush.getPlanes().size(); j++) {
			Face f = new Face(mbrush.getPlanes().get(j), true, mbrush.getColor());
	
			for (int k=0; k<mbrush.getPlanes().size(); k++) {
				if (j == k) continue;
	    		
	    		Plane p = mbrush.getPlanes().get(k);
	    		ClipResult clippedWindings = f.clip(p);
	
	    		if (clippedWindings.back != null) {
	    			f = clippedWindings.back;	
	    		}
	    		else {
	    			f = null;
	    		}
	
	    		if (f == null) break;
			}
	
			if (f != null) {
	    		this.faces.add(f);
	
	    		for (int k=0; k<f.getVertices().size(); k++) {
	    			this.bb.expand(f.getVertices().get(k).getPosition());
	    		}
	    	}
		}
	}
	
	public MBrush(MBrush b) {
		this.faces = new LinkedList<Face>();
		for (int i=0; i<b.faces.size(); i++) {
			this.faces.add(new Face(b.faces.get(i)));
		}
		this.bb = new BoundingBox(b.bb);
	}

	public List<Face> getFaces() {
		return faces;
	}

	public BoundingBox getBoundingBox() {
		return bb;
	}
}
