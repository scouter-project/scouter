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
 * measure browser's performance timing and send it to the collection service of scouter APM.
 * -- browser support --
 * -- all modern browsers ( IE9+, IOS6+, Chrome any, Safari any, FF any)
 */
(function() {
  var _p = window.Scouter || {};
  var DEFAULT_END_POINT = "/_scouter_browser.jsp";
  var DEFAULT_GXID_HEADER = 'X-Scouter-Gxid';
  var DEFAULT_GATHER_RATIO = 100.0; //unit:% - default:100.0%

  _p.endPoint = _p.endPoint || DEFAULT_END_POINT;
  _p.debug = _p.debug || false;
  _p.gxid_header = _p.gxid_header || DEFAULT_GXID_HEADER;
  _p.gatherRatio = _p.gatherRatio || DEFAULT_GATHER_RATIO;

  if(!document.addEventListener) return; //Not support IE8-

  //gather ratio condition
  var random1000 = Math.floor(Math.random()*1000);

  if(random1000 <= Math.floor(_p.gatherRatio * 10)) {
    window.addEventListener("load", function() {
      document.removeEventListener("load", arguments.callee, false);
      navtiming();
    }, false);
  }

  var navtiming = function(){

    //TODO - read gxid from cookie(how to ensure on the case of multiple requests-ex. with browser frames?)
    var resGxid = getCookie(_p.gxid_header);

    setTimeout(function(){
      var performance = window.performance || window.webkitPerformance || window.msPerformance || window.mozPerformance;
      if(performance === undefined) {
        console.log('Unfortunately, your browser does not support the Navigation Timing API');
        return;
      }
      var t = performance.timing;
      var navtiming = {
        gxid: resGxid,
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
        console.log('resGxid = ' + resGxid);
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
      gxid: t.gxid,
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
      loadEventEnd: t.loadEventEnd,
      StartToResponse : t.responseEnd - t.navigationStart,
      StartToDomLoad : t.domComplete - t.navigationStart,
      StartTtoTotalLoad : t.loadEventEnd - t.navigationStart
    };
    if(_p.debug) {
      console.log(sendObj);
    }
    request(_p.endPoint, sendObj);
  }

  function request(url, params) {
    var img = new Image();
    img.src = url + "?" + serialize(params) + "&p=nav&z=" + new Date().getTime();
  }

  function getCookie(name)
  {
  	var i,x,y,cookies=document.cookie.split(";");
  	for (i=0;i<cookies.length;i++)
  	{
  	  x=cookies[i].substr(0,cookies[i].indexOf("="));
  	  y=cookies[i].substr(cookies[i].indexOf("=")+1);
  	  x=x.replace(/^\s+|\s+$/g,"");
  	  if (x==name)
  		{
  		return unescape(y);
  		}
  	  }
  }

})();