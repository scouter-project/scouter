/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 *
 */
package scouter.client.dashboard.figure;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.ImageFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.ToolbarLayout;

import scouter.client.Images;
import scouter.client.model.DummyObject;

public class DummyObjectFigure extends Figure {
	public DummyObjectFigure(DummyObject dummy) {
		ToolbarLayout layout = new ToolbarLayout();
		setLayoutManager(layout);
		setOpaque(false);
		add(new ImageFigure(Images.folder_48));
		Label title = new Label(dummy.getDisplayName(), null);
		add(title);
		setSize(-1, -1);
	}
}
