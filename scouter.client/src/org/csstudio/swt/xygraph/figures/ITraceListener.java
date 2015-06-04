package org.csstudio.swt.xygraph.figures;

import org.csstudio.swt.xygraph.figures.Trace.TraceType;
import org.eclipse.swt.graphics.Color;

public interface ITraceListener {

	void traceNameChanged(Trace trace, String oldName, String newName);

	void traceYAxisChanged(Trace trace, Axis oldName, Axis newName);

	void traceTypeChanged(Trace trace, TraceType old, TraceType newTraceType);

	void traceColorChanged(Trace trace, Color old, Color newColor);
}
