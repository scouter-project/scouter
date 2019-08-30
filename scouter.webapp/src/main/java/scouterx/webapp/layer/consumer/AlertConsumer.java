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

package scouterx.webapp.layer.consumer;

import scouter.lang.constants.ParamConstant;
import scouter.lang.pack.AlertPack;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.net.RequestCmd;
import scouterx.webapp.framework.client.net.TcpProxy;
import scouterx.webapp.model.scouter.SAlert;
import scouterx.webapp.request.RealTimeAlertRequest;
import scouterx.webapp.view.RealTimeAlertView;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
public class AlertConsumer {

	public RealTimeAlertView retrieveRealTimeAlert(final RealTimeAlertRequest request) {
		MapPack paramPack = new MapPack();
		paramPack.put(ParamConstant.OFFSET_LOOP, request.getLoop());
		paramPack.put(ParamConstant.OFFSET_INDEX, request.getIndex());
		paramPack.put(ParamConstant.OBJ_TYPE, request.getObjType());

		RealTimeAlertView alertView = new RealTimeAlertView();
		List<SAlert> alertList = new ArrayList<>();
		alertView.setAlerts(alertList);

		try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(request.getServerId())) {
			tcpProxy.process(RequestCmd.ALERT_REAL_TIME, paramPack, in -> {
				Pack packet = in.readPack();
				if (packet instanceof MapPack) {
					MapPack metaPack = (MapPack) packet;
					alertView.setOffset1(metaPack.getLong(ParamConstant.OFFSET_LOOP));
					alertView.setOffset2(metaPack.getInt(ParamConstant.OFFSET_INDEX));

				} else {
					AlertPack alertPack = (AlertPack) packet;
					if (packet != null) {
						alertList.add(SAlert.of(alertPack));
					}
				}
			});
		}

		return alertView;
	}
}
