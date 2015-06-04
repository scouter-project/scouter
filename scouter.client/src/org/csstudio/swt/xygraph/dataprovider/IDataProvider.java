/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.swt.xygraph.dataprovider;

import org.csstudio.swt.xygraph.linearscale.Range;

/**
 * Interface for the data provider of trace. This gives the possibilities to implement 
 * different data provider, which could have different data source or data storage structure.
 * For example: the data source could be from user input, database, files, etc,. 
 * The storage structure could be array, queue, circular buffer, bucket buffer, etc,.
 * <p>
 * An API like
 * <code>
 *  public ISample[] getSamples()
 * </code>
 * would be much easier to use by the XY Graph, but it forces the data
 * provider to copy its samples into an array, which might be a performance
 * and memory problem if considerable amounts of data are held in some other
 * data structure that is more suitable to the application.
 * <p>
 * The API is therefore based on single-sample access, allowing the application
 * to store the samples in arbitrary data structures.
 * <p>
 * <b>Synchronization</b><br>
 * Since the application data might change dynamically, the XY Graph
 * <code>synchronizes</code> on the <code>IDataProvider</code> like this
 * to assert that the sample count does not change while accessing individual
 * samples:
 * <pre>
 * IDataProvider data = ...;
 * synchronized (data)
 * {
 *     int count = data.getSize();
 *     ...
 *     ... getSample(i) ...
 * }
 * </pre>
 * Implementations of the <code>IDataProvider</code> should likewise synchronize
 * on it whenever the data is changed, and other methods like
 * <code>getXDataMinMax</code> should probably be synchronized implementations.
 * 
 * @author Xihui Chen
 * @author Kay Kasemir (synchronization)
 */
public interface IDataProvider {

	/**Total number of samples.
	 * @return the size.
     * @see #getSample(int)
	 */
	public int getSize();

	/**Get sample by index
     * <p>
     * <b>Synchronization:</b> 
     * Since the data might change dynamically, <code>synchronize</code> on the
     * <code>IDataProvider</code> around calls to <code>getSize()</code>
     * and <code>getSample()</code>.
     *
	 * @param index Sample index, 0...<code>getSize()-1</code>
	 * @return the sample.
	 */
	public ISample getSample(int index);

	/**Get the minimum and maximum xdata.
	 * @return a range includes the min and max as lower and upper. 
	 * return null if there is no data.
	 */
	public Range getXDataMinMax();

	/**Get the minimum and maximum ydata.
	 * @return a range includes the min and max as lower and upper.
	 * return null if there is no data.
	 */
	public Range getYDataMinMax();

	/**
	 * @return true if data is ascending sorted on X axis; false otherwise 
	 */
	public boolean isChronological();

	/** @param listener New listener to notify when data changes */
	public void addDataProviderListener(
			final IDataProviderListener listener);

    /** @param listener Listener to no longer notify when data changes
     *  @return <code>true</code> if listener was known and removed
     */
	public boolean removeDataProviderListener(
			final IDataProviderListener listener);	

}
