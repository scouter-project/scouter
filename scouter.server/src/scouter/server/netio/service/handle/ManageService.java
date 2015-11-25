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
package scouter.server.netio.service.handle;
import java.io.File;
import java.io.IOException;
import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.TextPack;
import scouter.lang.value.BooleanValue;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.net.TcpFlag;
import scouter.server.Logger;
import scouter.server.LoginManager;
import scouter.server.LoginUser;
import scouter.server.db.DBCtr;
import scouter.server.management.RemoteControl;
import scouter.server.management.RemoteControlManager;
import scouter.server.netio.service.anotation.ServiceHandler;
import scouter.util.SystemUtil;
public class ManageService {
	@ServiceHandler(RequestCmd.SERVER_DB_LIST)
	public void listDbFiles(DataInputX din, DataOutputX dout, boolean login) throws IOException {
		String rootPath = DBCtr.getRootPath();
		MapPack m = new MapPack();
		ListValue nameLv = m.newList("name");
		ListValue sizeLv = m.newList("size");
		ListValue lastModifiedLv = m.newList("lastModified");
		File dbDir = new File(rootPath);
		long totalLength = 0;
		if (dbDir.exists() && dbDir.isDirectory()) {
			totalLength = collectDirectory(dbDir, nameLv, sizeLv, lastModifiedLv, dbDir.getAbsolutePath());
		}
		if (SystemUtil.IS_JAVA_1_5 == false) {
			m.put("free", dbDir.getUsableSpace());
		}
		m.put("total", totalLength);
		dout.writeByte(TcpFlag.HasNEXT);
		dout.writePack(m);
		dout.flush();
	}
	@ServiceHandler(RequestCmd.SERVER_DB_DELETE)
	public void deleteDbFiles(DataInputX din, DataOutputX dout, boolean login) throws IOException {
		String rootPath = DBCtr.getRootPath();
		MapPack param = (MapPack) din.readPack();
		ListValue fileLv = param.getList("file");
		MapPack m = new MapPack();
		if (fileLv != null) {
			for (int i = 0; i < fileLv.size(); i++) {
				String filename = fileLv.getString(i);
				File file = new File(rootPath + filename);
				deleteFiles(file);
			}
			m.put("size", fileLv.size());
		}
		dout.writeByte(TcpFlag.HasNEXT);
		dout.writePack(m);
		dout.flush();
	}
	@ServiceHandler(RequestCmd.REMOTE_CONTROL)
	public void remoteControl(DataInputX din, DataOutputX dout, boolean login) throws IOException {
		MapPack param = (MapPack) din.readPack();
		long session = param.getLong("toSession");
		RemoteControl control = new RemoteControl(//
				param.getText("command"), //
				System.currentTimeMillis(), //
				param, param.getLong("fromSession"));
		boolean result = RemoteControlManager.add(session, control);
		Logger.println("[" + RequestCmd.REMOTE_CONTROL + "]" + control.commnad() + " from "
				+ LoginManager.getUser(control.commander()).ip() + " to " + LoginManager.getUser(session).ip() + " "
				+ result);
		MapPack m = new MapPack();
		if (result) {
			m.put("success", new BooleanValue(true));
			dout.writeByte(TcpFlag.HasNEXT);
			dout.writePack(m);
		} else {
			m.put("success", new BooleanValue(false));
			dout.writeByte(TcpFlag.HasNEXT);
			dout.writePack(m);
		}
	}
	@ServiceHandler(RequestCmd.REMOTE_CONTROL_ALL)
	public void remoteControlAll(DataInputX din, DataOutputX dout, boolean login) throws IOException {
		MapPack param = (MapPack) din.readPack();
		RemoteControl control = new RemoteControl(//
				param.getText("command"), //
				System.currentTimeMillis(), //
				param, param.getLong("fromSession"));
		LoginUser[] users = LoginManager.getLoginUserList();
		for (int i = 0, len = (users != null ? users.length : 0); i < len; i++) {
			long session = users[i].session();
			RemoteControlManager.add(session, control);
		}
		Logger.println("[" + RequestCmd.REMOTE_CONTROL_ALL + "]" + control.commnad() + " from "
				+ LoginManager.getUser(control.commander()).ip());
	}
	@ServiceHandler(RequestCmd.CHECK_JOB)
	public void checkJob(DataInputX din, DataOutputX dout, boolean login) throws IOException {
		MapPack param = (MapPack) din.readPack();
		long session = param.getLong("session");
		RemoteControl control = RemoteControlManager.getCommand(session);
		if (control != null) {
			TextPack t = new TextPack();
			t.text = control.commnad();
			dout.writeByte(TcpFlag.HasNEXT);
			dout.writePack(t);
			dout.writeByte(TcpFlag.HasNEXT);
			dout.writePack(control.param());
		}
	}
	private long collectDirectory(File dir, ListValue nameLv, ListValue sizeLv, ListValue lastModifiedLv,
			String rootPath) {
		long length = 0;
		for (File file : dir.listFiles()) {
			if (file.isFile()) {
				length += file.length();
			} else {
				long size = collectDirectory(file, nameLv, sizeLv, lastModifiedLv, rootPath);
				nameLv.add(file.getAbsolutePath().substring(rootPath.length()));
				lastModifiedLv.add(file.lastModified());
				sizeLv.add(size);
				length += size;
			}
		}
		return length;
	}
	void deleteFiles(File file) throws IOException {
		if (file.exists() == false) {
			return;
		}
		if (file.isDirectory()) {
			for (File c : file.listFiles()) {
				deleteFiles(c);
			}
		}
		file.delete();
	}
}
