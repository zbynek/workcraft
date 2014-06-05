package org.workcraft.plugins.circuit;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;

import org.workcraft.dom.math.MathConnection;
import org.workcraft.dom.visual.DrawRequest;
import org.workcraft.dom.visual.VisualComponent;
import org.workcraft.dom.visual.connections.VisualConnection;
import org.workcraft.gui.Coloriser;
import org.workcraft.gui.graph.tools.Decoration;
import org.workcraft.plugins.petri.Place;
import org.workcraft.util.Geometry;

public class VisualCircuitConnection extends VisualConnection {
	private Place referencedZeroPlace = null;
	private Place referencedOnePlace = null;

	public VisualCircuitConnection() {
		super();
	}

	public VisualCircuitConnection(MathConnection c) {
		super();
	}

	public VisualCircuitConnection(MathConnection con, VisualComponent c1, VisualComponent c2) {
		super(con, c1, c2);
	}

	@Override
	public double getLineWidth() {
		return CircuitSettings.getWireWidth();
	}

	public void setReferencedZeroPlace(Place referencedPlace) {
		this.referencedZeroPlace = referencedPlace;
	}

	public Place getReferencedZeroPlace() {
		return referencedZeroPlace;
	}

	public void setReferencedOnePlace(Place referencedPlace) {
		this.referencedOnePlace = referencedPlace;
	}

	public Place getReferencedOnePlace() {
		return referencedOnePlace;
	}

	@Override
	public void draw(DrawRequest r) {
		// Draw a small pieces of line at the beginning and at the end of connection arc when the gate contacts are hidden.
		Graphics2D g = r.getGraphics();
		Decoration d = r.getDecoration();
		boolean inSimulationMode = ((d.getColorisation() != null) || (d.getBackground() != null));
		Color colorisation = d.getColorisation();
		g.setColor(Coloriser.colorise(getColor(), colorisation));
		g.setStroke(new BasicStroke((float)CircuitSettings.getWireWidth()));

		if (!inSimulationMode && !CircuitSettings.getShowContacts() && (getFirst().getParent() instanceof VisualCircuitComponent)) {
			double tStart = Geometry.getBorderPointParameter(getFirstShape(), getGraphic(), 0, 1);
			g.draw(new Line2D.Double(getFirstCenter(), getGraphic().getPointOnCurve(tStart)));
		}

		if (!inSimulationMode && !CircuitSettings.getShowContacts() && (getSecond().getParent() instanceof VisualCircuitComponent)) {
			double tEnd = Geometry.getBorderPointParameter(getSecondShape(), getGraphic(), 1, 0);
			g.draw(new Line2D.Double(getGraphic().getPointOnCurve(tEnd), getSecondCenter()));
		}
	}

}
