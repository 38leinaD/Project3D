package de.fruitfly.map;

public class EdgeAttachment {
	public Face[] links;
	
	public EdgeAttachment(Face f) {
		links = new Face[f.getVertices().size()];
		f.setEdgeAttachment(this);
	}
	
	public Face[] getLinks() {
		return links;
	}
}
