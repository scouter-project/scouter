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

package scouter.server.netio.service.handle;

import org.junit.Test;
import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.TextValue;
import scouter.lang.value.Value;
import scouter.net.TcpFlag;
import scouter.test.support.function.Reader;

import java.io.EOFException;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2018. 3. 1.
 */
public class KeyValueStoreServiceTest {
    private static final String vutDiv = "junit-test2";
    private static final String vutKey1 = "testkey-01";
    private static final String vutValue1 = "testvalue-01";
    private static final String vutKey2 = "testkey-02";
    private static final String vutValue2 = "testvalue-02";

    KeyValueStoreService sut = new KeyValueStoreService();

    private DataInputX toDataInputXFromPack(Pack pack) {
        DataOutputX dout = new DataOutputX();
        try {
            dout.writePack(pack);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new DataInputX(dout.toByteArray());
    }

    private DataInputX toDataInputXFromValue(Value value) {
        DataOutputX dout = new DataOutputX();
        try {
            dout.writeValue(value);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new DataInputX(dout.toByteArray());
    }

    private void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void read(DataOutputX out, Reader reader) throws IOException {
        DataInputX resultIn = new DataInputX(out.toByteArray());
        try {
            while (resultIn.readByte() == TcpFlag.HasNEXT) {
                reader.read(resultIn);
            }
        } catch (EOFException e) {
        }
    }

    @Test
    public void setText() throws IOException {
        //parameters
        MapPack mapPack = new MapPack();
        mapPack.put("key", vutKey1);
        mapPack.put("value", vutValue1);

        //when
        DataOutputX out = new DataOutputX();
        sut.setText(toDataInputXFromPack(mapPack), out, false);
        read(out, new Reader() {
            @Override public void read(DataInputX in) throws IOException {
                assertTrue((Boolean) in.readValue().toJavaObject());
            }
        });

        //then
        DataOutputX out2 = new DataOutputX();
        sut.getText(toDataInputXFromValue(new TextValue(vutKey1)), out2, false);
        read(out2, new Reader() {
            @Override public void read(DataInputX in) throws IOException {
                assertEquals(vutValue1, in.readValue().toString());
            }
        });
    }

    @Test
    public void setText_with_ttl() throws IOException {
        //parameters
        MapPack mapPack = new MapPack();
        mapPack.put("key", vutKey1);
        mapPack.put("value", vutValue1);
        mapPack.put("ttl", 3);

        //when
        DataOutputX out = new DataOutputX();
        sut.setText(toDataInputXFromPack(mapPack), out, false);
        read(out, new Reader() {
            @Override public void read(DataInputX in) throws IOException {
                assertTrue((Boolean) in.readValue().toJavaObject());
            }
        });

        //then
        DataOutputX out2 = new DataOutputX();
        sut.getText(toDataInputXFromValue(new TextValue(vutKey1)), out2, false);
        read(out2, new Reader() {
            @Override public void read(DataInputX in) throws IOException {
                assertEquals(vutValue1, in.readValue().toString());
            }
        });
    }

    @Test
    public void setText_with_ttl_expired() throws IOException {
        //parameters
        MapPack mapPack = new MapPack();
        mapPack.put("key", vutKey1);
        mapPack.put("value", vutValue1);
        mapPack.put("ttl", 2);

        //when - set and wait
        DataOutputX out = new DataOutputX();
        sut.setText(toDataInputXFromPack(mapPack), out, false);
        read(out, new Reader() {
            @Override public void read(DataInputX in) throws IOException {
                assertTrue((Boolean) in.readValue().toJavaObject());
            }
        });

        sleep(3000);

        //then - expired
        DataOutputX out2 = new DataOutputX();
        sut.getText(toDataInputXFromValue(new TextValue(vutKey1)), out2, false);
        read(out2, new Reader() {
            @Override public void read(DataInputX in) throws IOException {
                assertEquals("", in.readValue().toString());
            }
        });
    }
}