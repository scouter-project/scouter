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

import java.text.Format;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.ImageFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.ToolbarLayout;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;

import scouter.client.Images;
import scouter.client.model.AgentObject;
import scouter.client.server.ServerManager;
import scouter.client.util.ScouterUtil;
import scouter.lang.counters.CounterEngine;
import scouter.lang.pack.AlertPack;
import scouter.lang.value.Value;
import scouter.util.CastUtil;
import scouter.util.FormatUtil;
import scouter.util.FormatUtil;

public class AgentObjectFigure extends Figure {
	
	public AgentObjectFigure(AgentObject ao, Font font) {
		this(ao, font, null);
	}
	
	public AgentObjectFigure(AgentObject ao, Font font, AlertPack alertPack) {
		ToolbarLayout layout = new ToolbarLayout();
		setLayoutManager(layout);
		setOpaque(false);
		Image img = null;
		if (ao.isAlive() == false) {
			img = Images.getObject48Icon(ao.getObjType(), false, ao.getServerId());
		} else {
			if (alertPack != null) {
				img = Images.getObjectAlert48Icon(ao.getObjType(), ao.getServerId());
				Label alertTitle = new Label(alertPack.title, null);
				alertTitle.setFont(font);
				add(alertTitle);
			} else {
				img = Images.getObject48Icon(ao.getObjType(), ao.isAlive(), ao.getServerId());
			}
		}
		if (img == null || img == Images.folder_48) {
			img = Images.getObjectIcon(ao.getObjType(), ao.isAlive(), ao.getServerId());
		}
		add(new ImageFigure(img));
		Label title = new Label(ao.getDisplayName(), null);
		add(title);
		Value v = ao.getMasterCounter();
		if (v != null && ao.isAlive()) {
			CounterEngine engine = ServerManager.getInstance().getServer(ao.getServerId()).getCounterEngine();
			String unit = engine.getMasterCounterUnit(ao.getObjType());
			String value;
			if(unit != null && "bytes".equals(unit)){
				value =ScouterUtil.humanReadableByteCount(CastUtil.cdouble(v), true);
			} else {
				value = FormatUtil.print(v, "#,###") + " " + unit;
			}
			Label masterCounter = new Label(value, null);
			masterCounter.setFont(font);
			add(masterCounter);
		}
		setSize(-1, -1);
	}
}
