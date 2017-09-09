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

package scouterx.webapp.api.service;

import scouter.lang.step.Step;
import scouterx.webapp.api.consumer.ProfileConsumer;
import scouterx.webapp.api.request.ProfileRequest;

import java.util.List;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 9. 3.
 */
public class ProfileService {
    private final ProfileConsumer profileConsumer;

    public ProfileService() {
        this.profileConsumer = new ProfileConsumer();
    }

    /**
     * retrieve profile
     */
    public List<Step> retrieveProfile(final ProfileRequest request) {
        return profileConsumer.retrieveProfile(request);
    }

}
