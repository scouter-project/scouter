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

package scouter.server.core.cache;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2018. 8. 15.
 */
public class InteractionCounterCacheKey {
    private String interactionType;
    private int fromHash;
    private int toHash;

    public InteractionCounterCacheKey(String interactionType, int fromHash, int toHash) {
        this.interactionType = interactionType;
        this.fromHash = fromHash;
        this.toHash = toHash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InteractionCounterCacheKey that = (InteractionCounterCacheKey) o;

        return (fromHash == that.fromHash && toHash == that.toHash &&
                (interactionType == that.interactionType || interactionType.equals(that.interactionType)));
    }

    @Override
    public int hashCode() {
        return interactionType.hashCode() ^ fromHash ^ toHash;
    }
}
