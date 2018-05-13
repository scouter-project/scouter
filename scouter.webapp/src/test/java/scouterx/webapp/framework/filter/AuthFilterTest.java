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

package scouterx.webapp.framework.filter;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
public class AuthFilterTest {

    @Test
    public void ip_allow_test() {
        List<String> allows = Arrays.asList("localhost", "127.0.0.1", "0:0:0:0:0:0:0:1");
        Assert.assertFalse(allows.stream().anyMatch(ip -> "0.0.0.0".contains(ip)));
        Assert.assertFalse(allows.stream().anyMatch(ip -> "10.0.0.1".contains(ip)));
        Assert.assertFalse(allows.stream().anyMatch(ip -> "192.155.23.1".contains(ip)));
        Assert.assertTrue(allows.stream().anyMatch(ip -> "127.0.0.1".contains(ip)));
        Assert.assertTrue(allows.stream().anyMatch(ip -> "localhost".contains(ip)));
    }
}
