/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package scouter.agent.util;

import scouter.agent.error.CONNECTION_NOT_CLOSE;

public class SampleStatement {
    private static int index = 0;
    private final LeakableObject2 object;
    private int myid = 0;

    public SampleStatement() {
        myid = index++;
        this.object = new LeakableObject2(new CONNECTION_NOT_CLOSE(), this, CloseManager.getInstance(), 0, 0, true, 2);
        System.out.println("created " + myid);
    }

    public void close() {
        object.close();
    }

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 1000; i++) {
            SampleStatement s = new SampleStatement();
            if (i % 100 != 0)
                s.close();
        }
        System.gc();
        Thread.sleep(5000);
    }

    @Override
    public String toString() {
        return "util.SampleStatment#" + myid;
    }

    private static class CloseManager implements ICloseManager {
        private static CloseManager cmanager = new CloseManager();

        public static CloseManager getInstance() {
            return cmanager;
        }

        @Override
        public boolean close(Object o) {
            try {
                ((SampleStatement) o).close();
                return true;
            } catch(Exception e) {
                return false;
            }
        }
    }
}