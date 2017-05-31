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
package scouter.server

import scouter.util.StringKeyLinkedMap
import scouter.server.util.EnumerScala

object ConfObserver {

    private val observers = new StringKeyLinkedMap[Runnable]();

    def put(name: String)(code: => Any) {
        observers.put(name, new Runnable() {
            override def run() {
                code
            }
        })
    }
    def put(name: String, code: Runnable) {
        observers.put(name,code)
    }
    def exec() {
        val en = observers.values()
        while (en.hasMoreElements()) {
            val r = en.nextElement();
            try {
                r.run()
            } catch {
                case t: Throwable => t.printStackTrace();
            }
        }
    }
}