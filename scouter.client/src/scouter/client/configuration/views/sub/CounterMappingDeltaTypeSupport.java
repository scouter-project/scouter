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
import scouter.lang.DeltaType;
import scouter.server.support.telegraf.TgCounterMapping;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 06/01/2019
 */
public class CounterMappingDeltaTypeSupport extends EditingSupport {

    private final TableViewer viewer;

    public CounterMappingDeltaTypeSupport(TableViewer viewer) {
        super(viewer);
        this.viewer = viewer;
    }

    @Override
    protected CellEditor getCellEditor(Object element) {
        String[] deltaTypes = new String[3];
        deltaTypes[0] = DeltaType.NONE.name();
        deltaTypes[1] = DeltaType.DELTA.name();
        deltaTypes[2] = DeltaType.BOTH.name();

        return new ComboBoxCellEditor(viewer.getTable(), deltaTypes);
    }

    @Override
    protected boolean canEdit(Object element) {
        return true;
    }

    @Override
    protected Object getValue(Object element) {
        TgCounterMapping mapping = (TgCounterMapping) element;
        switch (mapping.deltaType) {
            case NONE:
                return 0;
            case DELTA:
                return 1;
            case BOTH:
                return 2;
            default:
                return 0;
        }
    }

    @Override
    protected void setValue(Object element, Object userInputValue) {
        TgCounterMapping mapping = (TgCounterMapping) element;
        int num = (int) userInputValue;
        switch (num) {
            case 0:
                mapping.deltaType = DeltaType.NONE;
                break;
            case 1:
                mapping.deltaType = DeltaType.DELTA;
                break;
            case 2:
                mapping.deltaType = DeltaType.BOTH;
                break;
            default:
                mapping.deltaType = DeltaType.NONE;
        }
        viewer.update(element, null);
    }
}
