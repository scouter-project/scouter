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

package scouterx.webapp.api.consumer;

import scouter.lang.constants.ParamConstant;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouterx.client.net.INetReader;
import scouterx.client.net.TcpProxy;
import scouterx.webapp.api.model.SDictionaryText;
import scouterx.webapp.api.requestmodel.DictionaryRequest;

import java.util.Map;
import java.util.Set;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
public class DictionaryConsumer {

	public void retrieveText(DictionaryRequest dictionaryRequest, INetReader reader) {

		for (Map.Entry<String, Set<SDictionaryText>> textSetEntry : dictionaryRequest.getDictSets().entrySet()) {
			MapPack paramPack = new MapPack();
			paramPack.put(ParamConstant.DATE, dictionaryRequest.getDate());
			paramPack.put(ParamConstant.TEXT_TYPE, textSetEntry.getKey());

			ListValue dictKeyLV = paramPack.newList(ParamConstant.TEXT_DICTKEY);
			for (SDictionaryText dictionaryText : textSetEntry.getValue()) {
				dictKeyLV.add(dictionaryText.getDictKey());
			}

			TcpProxy.getTcpProxy(dictionaryRequest.getServerId()).process(RequestCmd.GET_TEXT_PACK, paramPack, reader);
		}
	}
}
