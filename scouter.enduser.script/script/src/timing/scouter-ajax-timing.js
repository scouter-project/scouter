 /*!
 *    Copyright 2009-2015 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

/**
 * @author <a href="mailto:gunlee01@gmail.com">Gun Lee</a>
 */

/**
 * measure browser's ajax timing and send it to the collection service of scouter APM.
 * -- browser support --
 * -- all modern browsers ( IE9+, IOS6+, Chrome any, Safari any, FF any)
 */
(function(XHR) {
  "use strict";

  var _p = window.Scouter || {};
  var DEFAULT_END_POINT = '/_scouter_browser.jsp';
  var DEFAULT_GXID_HEADER = 'X-Scouter-Gxid';
  var DEFAULT_GATHER_RATIO = 100.0; //unit:% - default:100.0%

  //options
  _p.endPoint = _p.endPoint || DEFAULT_END_POINT;
  _p.debug = _p.debug || false;
  _p.gxidHeader = _p.gxidHeader || DEFAULT_GXID_HEADER;
  _p.gatherRatio = _p.gatherRatio || DEFAULT_GATHER_RATIO;

  var stats = [];
  var timeoutId = null;

  var open = XHR.prototype.open;
  var send = XHR.prototype.send;

  XHR.prototype.open = function(method, url, async, user, pass) {
    this._url = url;
    open.call(this, method, url, async, user, pass);
  };

  XHR.prototype.send = function(data) {
    var self = this;
    var start;
    var oldOnReadyStateChange;
    var url = this._url;

    function onReadyStateChange() {
      var random1000 = Math.floor(Math.random()*1000);

      if(self.readyState == 4 && (random1000 <= Math.floor(_p.gatherRatio * 10))) {
        var resGxid = self.getResponseHeader(_p.gxid_header);

        var time = new Date() - start;
        stats.push({
          url: url,
          duration: time,
          gxid: resGxid,
          userAgent: navigator.userAgent
        });

        if(!timeoutId) {
          timeoutId = window.setTimeout(function() {
            var queryString = JSON.stringify({stats:stats}, undefined, 0);
            var xhr = new XHR();
            xhr.noIntercept = true;
            var fullQuery = _p.endPoint + '?p=ax&z=' + new Date().getTime() + '&q=' + encodeURIComponent(queryString);
            if(_p.debug) {
              console.log('fullQuery = ' + fullQuery);
            }

            xhr.open("GET", fullQuery, true);
            xhr.send();

            timeoutId = null;
            stats = [];
          }, 1000);
        }
      }

      if(oldOnReadyStateChange) {
        oldOnReadyStateChange();
      }
    }

    if(!this.noIntercept) {
      start = new Date();

      if(this.addEventListener) {
        this.addEventListener("readystatechange", onReadyStateChange, false);
      } else {
        oldOnReadyStateChange = this.onreadystatechange;
        this.onreadystatechange = onReadyStateChange;
      }
    }

    send.call(this, data);
  }
})(XMLHttpRequest);