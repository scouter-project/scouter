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
 */
package scouterx.webapp.framework.client.model;

import lombok.extern.slf4j.Slf4j;
import scouter.lang.constants.ParamConstant;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.pack.TextPack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouterx.webapp.framework.client.net.TcpProxy;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Gun Lee(gunlee01@gmail.com) on 2017. 9. 8.
 */
@Slf4j
public class TextLoader {
	private EnumMap<TextTypeEnum, Set<Integer>> typedHashes = new EnumMap<>(TextTypeEnum.class);

	private int serverId;
	public TextLoader(int serverId) {
		this.serverId = serverId;
	}

	/**
	 * add hash to load value after invoking load method
	 * @param textType
	 * @param hash
	 */
	public void addTextHash(TextTypeEnum textType, int hash) {
		if (hash == 0) {
			return;
		}
		Set<Integer> hashSet = typedHashes.computeIfAbsent(textType, k -> new HashSet<>());
		hashSet.add(hash);
	}

	/**
	 * load dictionary values added from scouter server and cache them
	 * @return
	 */
	public boolean loadAll() {
		MapPack param = new MapPack();
		ListValue typeList = param.newList(ParamConstant.TYPE);
		ListValue hashList = param.newList(ParamConstant.HASH);

		for (Map.Entry<TextTypeEnum, Set<Integer>> e : typedHashes.entrySet()) {
			String type = e.getKey().getTypeName();
			Iterator<Integer> iter = e.getValue().iterator();
			while (iter.hasNext()) {
				int hash = iter.next();
				if(TextTypeEnum.of(type).getTextModel().getCachedText(hash) == null) {
					if (TextModel.isScopeStarted() && TextModel.isFailedInScope(hash)) {
						//skip
					} else {
						typeList.add(type);
						hashList.add(hash);
					}
				}
			}
		}

		if(hashList.size() > 0) {
			try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(serverId)) {
				List<Pack> valueList = tcpProxy.process(RequestCmd.GET_TEXT_ANY_TYPE, param);
				for (Pack pack : valueList) {
					TextPack textPack = (TextPack) pack;
					TextTypeEnum.of(textPack.xtype).getTextModel().cache(textPack);
				}
			} catch (Exception e) {
				log.error(e.getMessage());
				return false;
			}

			if (TextModel.isScopeStarted()) {
				for(int i = 0; i < hashList.size(); i++) {
					int hash = hashList.getInt(i);
					TextModel textModel = TextTypeEnum.of(typeList.getString(i)).getTextModel();
					if (textModel.getCachedText(hash) == null) {
						TextModel.addFailedList(hashList.getInt(i));
					}
				}
			}
		}

		return true;
	}
}
