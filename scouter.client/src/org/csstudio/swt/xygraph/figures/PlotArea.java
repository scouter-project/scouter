/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.swt.xygraph.figures;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

import org.csstudio.swt.xygraph.linearscale.Range;
import org.csstudio.swt.xygraph.undo.SaveStateCommand;
import org.csstudio.swt.xygraph.undo.ZoomCommand;
import org.csstudio.swt.xygraph.undo.ZoomType;
import org.csstudio.swt.xygraph.util.SWTConstants;
import org.csstudio.swt.xygraph.util.XYGraphMediaFactory;
import org.csstudio.swt.xygraph.util.XYGraphMediaFactory.CURSOR_TYPE;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.draw2d.MouseMotionListener;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

/**
 * The plot area figure.
 * 
 * @author Xihui Chen
 * @author Kay Kasemir - Axis zoom/pan tweaks
 * @author Laurent PHILIPPE - Add property change event for annotation
 */
public class PlotArea extends Figure {

	// Added by Laurent PHILIPPE
	private PropertyChangeSupport changeSupport = new PropertyChangeSupport(
			this);

	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		//System.out.println("**** PlotArea.addPropertyChangeListener() ****");
		changeSupport.addPropertyChangeListener(listener);
	}

	@Override
	public void addPropertyChangeListener(String property,
			PropertyChangeListener listener) {
		//System.out.println("**** PlotArea.addPropertyChangeListener() ****");
		changeSupport.addPropertyChangeListener(property, listener);
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		changeSupport.removePropertyChangeListener(listener);
	}

	@Override
	public void removePropertyChangeListener(String property,
			PropertyChangeListener listener) {
		changeSupport.removePropertyChangeListener(property, listener);
	}

	public static final String BACKGROUND_COLOR = "background_color"; //$NON-NLS-1$
	final private XYGraph xyGraph;
	final private List<Trace> traceList = new ArrayList<Trace>();
	final private List<Grid> gridList = new ArrayList<Grid>();
	final private List<Annotation> annotationList = new ArrayList<Annotation>();

	final private Cursor grabbing;

	private boolean showBorder;

	private ZoomType zoomType;

	private Point start;
	private Point end;
	private boolean armed;

	private Color revertBackColor;
	
	// scouter.porject 20150707
	private boolean horizontalZoomEnabled = true;

	public PlotArea(final XYGraph xyGraph) {
		this.xyGraph = xyGraph;
		setBackgroundColor(XYGraphMediaFactory.getInstance().getColor(255, 255,
				255));
		setForegroundColor(XYGraphMediaFactory.getInstance().getColor(0, 0, 0));
		setOpaque(true);
		RGB backRGB = getBackgroundColor().getRGB();
		revertBackColor = XYGraphMediaFactory.getInstance().getColor(
				255 - backRGB.red, 255 - backRGB.green, 255 - backRGB.blue);
		PlotMouseListener zoomer = new PlotMouseListener();
		addMouseListener(zoomer);
		addMouseMotionListener(zoomer);
		grabbing = XYGraphMediaFactory.getCursor(CURSOR_TYPE.GRABBING);
		zoomType = ZoomType.NONE;
	}

	@Override
	public void setBackgroundColor(final Color bg) {
		//System.out.println("**** PlotArea.setBackgroundColor() ****");
		RGB backRGB = bg.getRGB();
		revertBackColor = XYGraphMediaFactory.getInstance().getColor(
				255 - backRGB.red, 255 - backRGB.green, 255 - backRGB.blue);
		Color oldColor = getBackgroundColor();
		super.setBackgroundColor(bg);
		
		changeSupport.firePropertyChange(BACKGROUND_COLOR, oldColor, bg);

	}

	/**
	 * Add a trace to the plot area.
	 * 
	 * @param trace
	 *            the trace to be added.
	 */
	public void addTrace(final Trace trace) {
		traceList.add(trace);
		add(trace);
		revalidate();
	}

	/**
	 * Remove a trace from the plot area.
	 * 
	 * @param trace
	 * @return true if this plot area contained the specified trace
	 */
	public boolean removeTrace(final Trace trace) {
		boolean result = traceList.remove(trace);
		if (result) {
			remove(trace);
			revalidate();
		}
		return result;
	}

	/**
	 * Add a grid to the plot area.
	 * 
	 * @param grid
	 *            the grid to be added.
	 */
	public void addGrid(final Grid grid) {
		gridList.add(grid);
		add(grid);
		revalidate();
	}

	/**
	 * Remove a grid from the plot area.
	 * 
	 * @param grid
	 *            the grid to be removed.
	 * @return true if this plot area contained the specified grid
	 */
	public boolean removeGrid(final Grid grid) {
		final boolean result = gridList.remove(grid);
		if (result) {
			remove(grid);
			revalidate();
		}
		return result;
	}

	/**
	 * Add an annotation to the plot area.
	 * 
	 * @param annotation
	 *            the annotation to be added.
	 */
	public void addAnnotation(final Annotation annotation) {
		annotationList.add(annotation);
		annotation.setxyGraph(xyGraph);
		add(annotation);
		revalidate();
		
		//Laurent PHILIPPE send event
		changeSupport.firePropertyChange("annotationList", null , annotation);
	}

	/**
	 * Remove a annotation from the plot area.
	 * 
	 * @param annotation
	 *            the annotation to be removed.
	 * @return true if this plot area contained the specified annotation
	 */
	public boolean removeAnnotation(final Annotation annotation) {
		final boolean result = annotationList.remove(annotation);
		if (!annotation.isFree())
			annotation.getTrace().getDataProvider()
					.removeDataProviderListener(annotation);
		if (result) {
			remove(annotation);
			revalidate();
			
			//Laurent PHILIPPE send event
			changeSupport.firePropertyChange("annotationList", annotation, null);
		}
		return result;
	}

	@Override
	protected void layout() {
		final Rectangle clientArea = getClientArea();
		for (Trace trace : traceList) {
			if (trace != null && trace.isVisible())
				// Shrink will make the trace has no intersection with axes,
				// which will make it only repaints the trace area.
				trace.setBounds(clientArea);// .getCopy().shrink(1, 1));
		}
		for (Grid grid : gridList) {
			if (grid != null && grid.isVisible())
				grid.setBounds(clientArea);
		}

		for (Annotation annotation : annotationList) {
			if (annotation != null && annotation.isVisible())
				annotation.setBounds(clientArea);// .getCopy().shrink(1, 1));
		}
		super.layout();
	}

	@Override
	protected void paintClientArea(final Graphics graphics) {
		super.paintClientArea(graphics);
		if (showBorder) {
			graphics.setLineWidth(2);
			graphics.drawLine(bounds.x, bounds.y, bounds.x + bounds.width,
					bounds.y);
			graphics.drawLine(bounds.x + bounds.width, bounds.y, bounds.x
					+ bounds.width, bounds.y + bounds.height);
		}
		// Show the start/end cursor or the 'rubberband' of a zoom operation?
		if (armed && end != null && start != null) {
			switch (zoomType) {
			case RUBBERBAND_ZOOM:
			case HORIZONTAL_ZOOM:
			case VERTICAL_ZOOM:
				graphics.setLineStyle(SWTConstants.LINE_DOT);
				graphics.setLineWidth(1);
				graphics.setForegroundColor(revertBackColor);
				graphics.drawRectangle(start.x, start.y, end.x - start.x, end.y
						- start.y);
				break;

			default:
				break;
			}
		}
	}

	/**
	 * @param showBorder
	 *            the showBorder to set
	 */
	public void setShowBorder(final boolean showBorder) {
		this.showBorder = showBorder;
		repaint();
	}

	/**
	 * @return the showBorder
	 */
	public boolean isShowBorder() {
		return showBorder;
	}

	/**
	 * @param zoomType
	 *            the zoomType to set
	 */
	public void setZoomType(final ZoomType zoomType) {
		this.zoomType = zoomType;
		if (zoomType != ZoomType.HORIZONTAL_ZOOM) { // add condition. 20150902 scouter
			setCursor(zoomType.getCursor());
		} else {
			setCursor(ZoomType.NONE.getCursor());
		}
	}

	/**
	 * Zoom 'in' or 'out' by a fixed factor
	 * 
	 * @param horizontally
	 *            along x axes?
	 * @param vertically
	 *            along y axes?
	 * @param factor
	 *            Zoom factor. Positive to zoom 'in', negative 'out'.
	 */
	private void zoomInOut(final boolean horizontally,
			final boolean vertically, final double factor) {
		if (horizontally)
			for (Axis axis : xyGraph.getXAxisList()) {
				final double center = axis.getPositionValue(start.x, false);
				axis.zoomInOut(center, factor);
			}
		if (vertically)
			for (Axis axis : xyGraph.getYAxisList()) {
				final double center = axis.getPositionValue(start.y, false);
				axis.zoomInOut(center, factor);
			}
	}

	/**
	 * @return the traceList
	 */
	public List<Trace> getTraceList() {
		return traceList;
	}

	/**
	 * @return the annotationList
	 */
	public List<Annotation> getAnnotationList() {
		return annotationList;
	}

	/**
	 * Listener to mouse events, performs panning and some zooms Is very similar
	 * to the Axis.AxisMouseListener, but unclear how easy/useful it would be to
	 * base them on the same code.
	 */
	class PlotMouseListener extends MouseMotionListener.Stub implements
			MouseListener {
		final private List<Range> xAxisStartRangeList = new ArrayList<Range>();
		final private List<Range> yAxisStartRangeList = new ArrayList<Range>();

		private SaveStateCommand command;

		public void mousePressed(final MouseEvent me) {
			// Only react to 'main' mouse button, only react to 'real' zoom
			if (me.button != 1 || zoomType == ZoomType.NONE)
				return;
			armed = true;
			// get start position
			switch (zoomType) {
			case RUBBERBAND_ZOOM:
				start = me.getLocation();
				end = null;
				break;
			case HORIZONTAL_ZOOM:
				start = new Point(me.getLocation().x, bounds.y);
				end = null;
				break;
			case VERTICAL_ZOOM:
				start = new Point(bounds.x, me.getLocation().y);
				end = null;
				break;
			case PANNING:
				setCursor(grabbing);
				start = me.getLocation();
				end = null;
				xAxisStartRangeList.clear();
				yAxisStartRangeList.clear();
				for (Axis axis : xyGraph.getXAxisList())
					xAxisStartRangeList.add(axis.getRange());
				for (Axis axis : xyGraph.getYAxisList())
					yAxisStartRangeList.add(axis.getRange());
				break;
			case ZOOM_IN:
			case ZOOM_IN_HORIZONTALLY:
			case ZOOM_IN_VERTICALLY:
			case ZOOM_OUT:
			case ZOOM_OUT_HORIZONTALLY:
			case ZOOM_OUT_VERTICALLY:
				start = me.getLocation();
				end = new Point();
				// Start timer that will zoom while mouse button is pressed
				Display.getCurrent().timerExec(Axis.ZOOM_SPEED, new Runnable() {
					public void run() {
						if (!armed)
							return;
						performInOutZoom();
						Display.getCurrent().timerExec(Axis.ZOOM_SPEED, this);
					}
				});
				break;
			default:
				break;
			}

			// add command for undo operation
			command = new ZoomCommand(zoomType.getDescription(),
					xyGraph.getXAxisList(), xyGraph.getYAxisList());
			me.consume();
		}

		public void mouseDoubleClicked(final MouseEvent me) { /* Ignored */
		}

		@Override
		public void mouseDragged(final MouseEvent me) {
			if (!armed)
				return;
			switch (zoomType) {
			case RUBBERBAND_ZOOM:
				end = me.getLocation();
				break;
			case HORIZONTAL_ZOOM:
				if (getCursor() != zoomType.getCursor()) {
					setCursor(zoomType.getCursor());  // scouter.porject 20150902
				}
				end = new Point(me.getLocation().x, bounds.y + bounds.height);
				break;
			case VERTICAL_ZOOM:
				end = new Point(bounds.x + bounds.width, me.getLocation().y);
				break;
			case PANNING:
				end = me.getLocation();
				pan();
				break;
			default:
				break;
			}
			PlotArea.this.repaint();
		}

		@Override
		public void mouseExited(final MouseEvent me) {
			// Treat like releasing the button to stop zoomIn/Out timer
			switch (zoomType) {
			case ZOOM_IN:
			case ZOOM_IN_HORIZONTALLY:
			case ZOOM_IN_VERTICALLY:
			case ZOOM_OUT:
			case ZOOM_OUT_HORIZONTALLY:
			case ZOOM_OUT_VERTICALLY:
				mouseReleased(me);
			default:
			}
		}

		public void mouseReleased(final MouseEvent me) {
			if (!armed)
				return;
			armed = false;
			if (zoomType == ZoomType.PANNING)
				setCursor(zoomType.getCursor());
			if (end == null || start == null)
				return;

			switch (zoomType) {
			case RUBBERBAND_ZOOM:
				for (Axis axis : xyGraph.getXAxisList()) {
					final double t1 = axis.getPositionValue(start.x, false);
					final double t2 = axis.getPositionValue(end.x, false);
					axis.setRange(t1, t2, true);
				}
				for (Axis axis : xyGraph.getYAxisList()) {
					final double t1 = axis.getPositionValue(start.y, false);
					final double t2 = axis.getPositionValue(end.y, false);
					axis.setRange(t1, t2, true);
				}
				break;
			case HORIZONTAL_ZOOM:
				setCursor(ZoomType.NONE.getCursor()); // scouter.porject 20150902
				for (Axis axis : xyGraph.getXAxisList()) {
					final double t1 = axis.getPositionValue(start.x, false);
					final double t2 = axis.getPositionValue(end.x, false);

					// scouter.porject 20150707
					changeSupport.firePropertyChange("horizontal_range", axis.getRange(), new Range(t1, t2));
					if (horizontalZoomEnabled) {
						axis.setRange(t1, t2, true);
					}
				}
				break;
			case VERTICAL_ZOOM:
				for (Axis axis : xyGraph.getYAxisList()) {
					final double t1 = axis.getPositionValue(start.y, false);
					final double t2 = axis.getPositionValue(end.y, false);
					axis.setRange(t1, t2, true);
				}
				break;
			case PANNING:
				pan();
				break;
			case ZOOM_IN:
			case ZOOM_IN_HORIZONTALLY:
			case ZOOM_IN_VERTICALLY:
			case ZOOM_OUT:
			case ZOOM_OUT_HORIZONTALLY:
			case ZOOM_OUT_VERTICALLY:
				performInOutZoom();
				break;
			default:
				break;
			}

			if (zoomType != ZoomType.NONE && command != null) {
				command.saveState();
				xyGraph.getOperationsManager().addCommand(command);
				command = null;
			}
			start = null;
			end = null;
			PlotArea.this.repaint();
		}

		/** Pan axis according to start/end from mouse listener */
		private void pan() {
			List<Axis> axes = xyGraph.getXAxisList();
			for (int i = 0; i < axes.size(); ++i) {
				final Axis axis = axes.get(i);
				axis.pan(xAxisStartRangeList.get(i),
						axis.getPositionValue(start.x, false),
						axis.getPositionValue(end.x, false));
			}
			axes = xyGraph.getYAxisList();
			for (int i = 0; i < axes.size(); ++i) {
				final Axis axis = axes.get(i);
				axis.pan(yAxisStartRangeList.get(i),
						axis.getPositionValue(start.y, false),
						axis.getPositionValue(end.y, false));
			}
		}

		/** Perform the in or out zoom according to zoomType */
		private void performInOutZoom() {
			switch (zoomType) {
			case ZOOM_IN:
				zoomInOut(true, true, Axis.ZOOM_RATIO);
				break;
			case ZOOM_IN_HORIZONTALLY:
				zoomInOut(true, false, Axis.ZOOM_RATIO);
				break;
			case ZOOM_IN_VERTICALLY:
				zoomInOut(false, true, Axis.ZOOM_RATIO);
				break;
			case ZOOM_OUT:
				zoomInOut(true, true, -Axis.ZOOM_RATIO);
				break;
			case ZOOM_OUT_HORIZONTALLY:
				zoomInOut(true, false, -Axis.ZOOM_RATIO);
				break;
			case ZOOM_OUT_VERTICALLY:
				zoomInOut(false, true, -Axis.ZOOM_RATIO);
				break;
			default: // NOP
			}
		}
	}
	
	// scouter.porject 20150707
	public void enableZoom(boolean enableZoom) {
		this.horizontalZoomEnabled = enableZoom;
	}
}
