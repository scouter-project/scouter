/**
 * measure browser's ajax timing and send it to the collection service of scouter APM.
 * -- browser support --
 * -- all modern browsers ( IE9+, IOS6+, Chrome any, Safari any, FF any)
 */
(function(XHR) {
  "use strict";

  var _p = window.scouter || {};
  var DEFAULT_END_POINT = '/_scouter_browser.jsp';
  var DEFAULT_GXID_HEADER = 'scouter_gxid';

  _p.endPoint = _p.endPoint || DEFAULT_END_POINT;
  _p.debug = _p.debug || false;
  _p.gxid_header = _p.gxid_header || DEFAULT_GXID_HEADER;

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
      if(self.readyState == 4 /* complete */) {
        var resGxid = self.getResponseHeader(_p.gxid_header);

        var time = new Date() - start;
        stats.push({
          url: url,
          duration: time,
          gxid: resGxid
        });

        if(!timeoutId) {
          timeoutId = window.setTimeout(function() {
            var queryString = JSON.stringify({stats:stats}, undefined, 0);
            var xhr = new XHR();
            xhr.noIntercept = true;
            var fullQuery = _p.endPoint + '?p=ax&q=' + encodeURIComponent(queryString);
            console.log('fullQuery = ' + fullQuery);
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