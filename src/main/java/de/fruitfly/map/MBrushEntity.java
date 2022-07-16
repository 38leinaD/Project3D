package de.fruitfly.map;

import java.util.LinkedList;
import java.util.List;

import de.fruitfly.map.MapFile.BrushType;

public class MBrushEntity {
	private List<MBrush> structuralBrushes;
	private List<MBrush> detailBrushes;
	
	public MBrushEntity(MapFile.BrushEntity mentity) {
		this.structuralBrushes = new LinkedList<MBrush>();
		this.detailBrushes = new LinkedList<MBrush>();
		
		for (MapFile.Brush mbrush : mentity.getBrushes()) {
			if (mbrush.getType() == BrushType.STRUCTURAL) {
				this.structuralBrushes.add(new MBrush(mbrush));
			}
			else {
				this.detailBrushes.add(new MBrush(mbrush));
			}
		}
	}

	public List<MBrush> getStructuralBrushes() {
		return structuralBrushes;
	}
	
	public List<MBrush> getDetailBrushes() {
		return detailBrushes;
	}
}
