/*
 *  Copyright 2015 the original author or authors.
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package scouter.client.configuration.views.sub;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import scouter.server.support.telegraf.TgCounterMapping;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 06/01/2019
 */
public class CounterMappingTotalizableEditingSupport extends EditingSupport {

    private final TableViewer viewer;

    public CounterMappingTotalizableEditingSupport(TableViewer viewer) {
        super(viewer);
        this.viewer = viewer;
    }

    @Override
    protected CellEditor getCellEditor(Object element) {
        String[] tf = new String[2];
        tf[0] = "N";
        tf[1] = "Y";

        return new ComboBoxCellEditor(viewer.getTable(), tf);
    }

    @Override
    protected boolean canEdit(Object element) {
        return true;
    }

    @Override
    protected Object getValue(Object element) {
        TgCounterMapping mapping = (TgCounterMapping) element;
        return mapping.totalizable ? 1 : 0;
    }

    @Override
    protected void setValue(Object element, Object userInputValue) {
        TgCounterMapping mapping = (TgCounterMapping) element;
        boolean totalizable = 1 == (int) userInputValue;
        mapping.totalizable = totalizable;
        viewer.update(element, null);
    }
}
