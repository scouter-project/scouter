/**
 * measure browser's performance timing and send it to the collection service of scouter APM.
 * -- browser support --
 * -- all modern browsers ( IE9+, IOS6+, Chrome any, Safari any, FF any)
 */
(function() {
  var _p = window.scouter || {};
  var DEFAULT_END_POINT = "/_scouter_browser.jsp";

  _p.endPoint = _p.endPoint || DEFAULT_END_POINT;
  _p.debug = _p.debug || false;

  if(!document.addEventListener) return; //Not support IE8-

  window.addEventListener("load", function() {
    document.removeEventListener("load", arguments.callee, false);
    navtiming();
  }, false);

  var navtiming = function(){
    setTimeout(function(){
      var performance = window.performance || window.webkitPerformance || window.msPerformance || window.mozPerformance;
      if(performance === undefined) {
        console.log('Unfortunately, your browser does not support the Navigation Timing API');
        return;
      }
      var t = performance.timing;
      var navtiming = {
        navigationStart: t.navigationStart,
        unloadEventStart: t.unloadEventStart,
        unloadEventEnd: t.unloadEventEnd,
        redirectStart: t.redirectStart,
        redirectEnd: t.redirectEnd,
        fetchStart: t.fetchStart,
        domainLookupStart: t.domainLookupStart,
        domainLookupEnd: t.domainLookupEnd,
        connectStart: t.connectStart,
        connectEnd: t.connectEnd,
        secureConnectionStart: t.secureConnectionStart,
        requestStart: t.requestStart,
        responseStart: t.responseStart,
        responseEnd: t.responseEnd,
        domLoading: t.domLoading,
        domInteractive: t.domInteractive,
        domContentLoadedEventStart: t.domContentLoadedEventStart,
        domContentLoadedEventEnd: t.domContentLoadedEventEnd,
        domComplete: t.domComplete,
        loadEventStart: t.loadEventStart,
        loadEventEnd: t.loadEventEnd
      };

      if(_p.debug) {
        console.log(navtiming);
        console.log('1st client:%d', navtiming.domainLookupStart - navtiming.navigationStart);
        console.log('1st n/w-req:%d', navtiming.requestStart - navtiming.domainLookupStart);
        console.log('1st server:%d', navtiming.responseStart - navtiming.requestStart);
        console.log('2nd n/w-res:%d', navtiming.responseEnd - navtiming.responseStart);
        console.log('after response, total time:%d', navtiming.loadEventEnd - navtiming.responseEnd);
        console.log('responseEnd to domInteractive:%d', navtiming.domLoading - navtiming.responseEnd);
        console.log('domInteractive to domContentLoadedEventEnd:%d', navtiming.domContentLoadedEventEnd - navtiming.domInteractive);
        console.log('domContentLoadedEventEnd to domComplete:%d', navtiming.domComplete - navtiming.domContentLoadedEventEnd);
        console.log('domComplete to loadEventEnd:%d', navtiming.loadEventEnd - navtiming.domComplete);
        console.log('loadEventStart to loadEventEnd:%d', navtiming.loadEventEnd - navtiming.loadEventStart);
      }

      sendToScouter(navtiming);

    }, 0);
  };

  // Deeply serialize an object into a query string. We use the PHP-style
  // nested object syntax, `nested[keys]=val`, to support hierarchical
  // objects. Similar to jQuery's `$.param` method.
  function serialize(obj, prefix) {
    var str = [];
    for (var p in obj) {
      if (obj.hasOwnProperty(p) && p != null && obj[p] != null) {
        var k = prefix ? prefix + "[" + p + "]" : p, v = obj[p];
        str.push(typeof v === "object" ? serialize(v, k) : encodeURIComponent(k) + "=" + encodeURIComponent(v));
      }
    }
    return str.join("&");
  }

  function sendToScouter(t) {
    var location = window.location;
    var sendObj = {
      host: location.protocol + "//" + location.host,
      uri: location.pathname,
      url: window.location.href,
      userAgent: navigator.userAgent,
      navigationStart: t.navigationStart,
      domainLookupStart: t.domainLookupStart,
      connectStart: t.connectStart,
      requestStart: t.requestStart,
      responseStart: t.responseStart,
      responseEnd: t.responseEnd,
      domLoading: t.domLoading,
      loadEventStart: t.loadEventStart,
      loadEventEnd: t.loadEventEnd
    };
    if(_p.debug) {
      console.log(sendObj);
    }
    request(_p.endPoint, sendObj);
  }

  function request(url, params) {
    var img = new Image();
    img.src = url + "?" + serialize(params) + "&x=nav&z=" + new Date().getTime();
  }

})();