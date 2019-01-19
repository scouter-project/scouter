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
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import scouter.server.support.telegraf.TgCounterMapping;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 06/01/2019
 */
public class CounterMappingTgFieldEditingSupport extends EditingSupport {

    private final TableViewer viewer;
    private final CellEditor editor;

    public CounterMappingTgFieldEditingSupport(TableViewer viewer) {
        super(viewer);
        this.viewer = viewer;
        this.editor = new TextCellEditor(viewer.getTable());
    }

    @Override
    protected CellEditor getCellEditor(Object element) {
        return editor;
    }

    @Override
    protected boolean canEdit(Object element) {
        return true;
    }

    @Override
    protected Object getValue(Object element) {
        return ((TgCounterMapping) element).tgFieldName;
    }

    @Override
    protected void setValue(Object element, Object userInputValue) {
        ((TgCounterMapping) element).tgFieldName = String.valueOf(userInputValue);
        viewer.update(element, null);
    }
}
