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
package scouter.client.util;

import java.io.BufferedWriter;
import java.io.FileWriter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import scouter.util.DateUtil;

public class MyTableKeyAdapter extends KeyAdapter {

    private Shell shell = null;

    public MyTableKeyAdapter(Shell shell) {
        this.shell = shell;
    }
    public void save(String file, String value) throws Exception {
        BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
        bw.write(value);
        bw.close();
    }

    public void keyPressed(KeyEvent ev) {
        if (ev.stateMask == SWT.CTRL) {
            //System.out.println("ev.keyCode=" + ev.keyCode);
            if (ev.keyCode == 115) {
                Table table = (Table) ev.getSource();
                TableItem[] items = table.getItems();
                StringBuffer sb = new StringBuffer();
                
                sb.append("No").append('\t');
                sb.append("Object").append('\t');
                sb.append("Txid").append('\t');
                sb.append("Service").append('\t');
                sb.append("StartTime").append('\t');
                sb.append("EndTime").append('\t');
                sb.append("Elapsed").append('\t');
                sb.append("ERROR").append('\t');
                sb.append("Extension").append('\n');
                for (int i = 1; i < items.length; i++) {
                    sb.append(items[i].getText(0)).append('\t');
                    sb.append(items[i].getText(1)).append('\t');
                    sb.append(items[i].getText(2)).append('\t');
                    sb.append(items[i].getText(3)).append('\t');
                    sb.append(items[i].getText(4)).append('\t');
                    sb.append(items[i].getText(5)).append('\t');
                    sb.append(items[i].getText(6)).append('\t');
                    sb.append(items[i].getText(7)).append('\t');
                    sb.append(items[i].getText(8)).append('\n');
                }
                FileDialog dialog = new FileDialog(shell, SWT.SAVE);
               
                // DO NOT USE "TimeUtil.getCurrentTime()" BECAUSE THIS MUST BE CLIENT TIME..!
                dialog.setFileName(DateUtil.format(System.currentTimeMillis(), "yyyyMMdd_HHmm")+"-TranxList.txt");
                String file = dialog.open();
                if (file == null)
                    return;
                try {
                    save(file, sb.toString());
                } catch (Exception ex) {
                }
            }
        }
        super.keyPressed(ev);
    }
}