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

package scouterx.webapp.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import scouterx.webapp.framework.client.server.ServerManager;
import scouterx.webapp.framework.util.ZZ;
import scouterx.webapp.model.scouter.SDictionaryText;

import javax.validation.constraints.NotNull;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 30.
 */
@Getter
@Setter
@ToString
public class DictionaryRequest {
	private static final char COLON = ':';

	@NotNull
	private Map<String, Set<SDictionaryText>> dictSets;

	private int serverId;

	/**
	 * some sorts of dictionary are created every day. so date is needed to find exact text.
	 * format : yyyymmdd
	 */
	@NotNull
	@PathParam("yyyymmdd")
	String yyyymmdd;

	/**
	 * dictionary key list to find text from dictionary
	 *
	 * @param dictKeys - format : [service:10001,service:10002,obj:20001,sql:55555] (bracket is optional)
	 */
	@QueryParam("dictKeys")
	public void setDictSets(String dictKeys) {
		Set<String> textList = ZZ.splitParamStringSet(dictKeys);
		dictSets = textList.stream()
				.map(s -> {
					String[] parts = StringUtils.split(s, COLON);
					return new SDictionaryText(parts[0], Integer.valueOf(parts[1]), null);
				})
				.collect(Collectors.groupingBy(SDictionaryText::getTextType, Collectors.toSet()));

	}

	@QueryParam("serverId")
	public void setServerId(int serverId) {
		this.serverId = ServerManager.getInstance().getServerIfNullDefault(serverId).getId();
	}
}
