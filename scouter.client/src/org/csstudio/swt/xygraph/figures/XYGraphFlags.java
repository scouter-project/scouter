/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.swt.xygraph.figures;

/** Bits for flags that the XYGraph package uses
 *  @author Kay Kasemir
 */
public interface XYGraphFlags
{
    /** Create toolbar with combined zoom in/out buttons (default).
     *  @see ToolbarArmedXYGraph
     */
    final public static int COMBINED_ZOOM = 1 << 0;

    /** Create toolbar with separate horizontal/vertical zoom buttons.
     *  @see ToolbarArmedXYGraph
     */
    final public static int SEPARATE_ZOOM = 1 << 1;

    /** Create toolbar with 'stagger' button instead of 'autoscale'.
     *  @see ToolbarArmedXYGraph
     */
    final public static int STAGGER = 1 << 2;
}
