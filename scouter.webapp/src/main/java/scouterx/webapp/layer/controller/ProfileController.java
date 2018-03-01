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

package scouterx.webapp.layer.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import scouter.lang.step.Step;
import scouterx.webapp.layer.service.ProfileService;
import scouterx.webapp.request.ProfileRequest;
import scouterx.webapp.view.CommonResultView;

import javax.inject.Singleton;
import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 9. 3.
 */
@Path("/v1/profile")
@Api("Raw profile")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class ProfileController {
    private final ProfileService profileService;

    public ProfileController() {
        this.profileService = new ProfileService();
    }

    /**
     * get profile from txid
     * uri : /profile/{yyyymmdd}/{txid}?serverId=12345 (serverId is optional)
     *
     * @param profileRequest @see {@link ProfileRequest}
     */
    @GET
    @Path("/{yyyymmdd}/{txid}")
    public CommonResultView<List<Step>> retrieveProfile(@BeanParam @Valid final ProfileRequest profileRequest) {
        List<Step> steps = profileService.retrieveProfile(profileRequest);

        return CommonResultView.success(steps);
    }
}
