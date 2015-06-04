/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.swt.xygraph.dataprovider;

/**
 * A listener on data provider data change.
 * @author Xihui Chen
 *
 */
public interface IDataProviderListener {
	
	/**
	 * This method will be notified by data provider whenever the data changed in data provider
	 */
	public void dataChanged(IDataProvider dataProvider);
	
		
}
