/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.swt.xygraph.util;

/** Helper for log10-related computations.
 *  @author Kay Kasemir
 */
public class Log10
{
    final static double HUGE_NEGATIVE=-1e100;

    /** Adjusted log10 to handle values less or equal to zero.
     *  <p>
     *  The logarithm does not result in real numbers for arguments
     *  less or equal to zero, but the plot should still somehow handle
     *  such values without crashing.
     *  So anything &le; 0 is mapped to a 'really big negative' number
     *  just for the sake of plotting.
     *  <p>
     *  Note that LogarithmicAxis.java in the JFreeChart has another interesting
     *  idea for modifying the log10 of values &le; 10, resulting in a smooth
     *  plot for the full real argument range.
     *  Unfortunately that clobbers values like 1e-7, which might be a
     *  very real vacuum reading.
     *
     *  @param val  value for which log<sub>10</sub> should be calculated.
     *
     *  @return an adjusted log<sub>10</sub>(val).
     */
    public static double log10(double val)
    {
        if (val > 0.0)
            return Math.log10(val);
        return HUGE_NEGATIVE;
    }

    /** @return pow(10, value) */
    public static double pow10(double value)
    {
        return Math.pow(10.0, value);
    }
}
