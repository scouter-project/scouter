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

/*!
Copyright (c) 2013 Bugsnag, https://bugsnag.com/
Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the "Software"),
to deal in the Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the Software
is furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.*/

/**
 * @author <a href="mailto:gunlee01@gmail.com">Gun Lee</a>
 */

/**
 * catch browser's errors and send it to the collection service of scouter APM.
 * modified from Bugsnag // https://github.com/bugsnag/bugsnag-js
 * -- browser support --
 * -- all browsers (IE9, IOS6, Chrome any, Safari any, FF any)
 */
// The `Scouter` object is the only globally exported variable
(function (window, old) {
  //var self = {};
  var self = old;

  var lastEvent,
    lastScript,
    previousNotification,
    shouldCatch = true,
    ignoreOnError = 0,

  // We've seen cases where individual clients can infinite loop sending us errors
  // (in some cases 10,000+ errors per page). This limit is at the point where
  // you've probably learned everything useful there is to debug the problem,
  // and we're happy to under-estimate the count to save the client (and Scouter's) resources.
    eventsRemaining = 10;

  // #### Scouter.noConflict
  //
  // This is obsolete with UMD, as we cannot assume the global scope is polluted with
  // the Scouter object anyway. In this case, it's up to the host Javascript file to
  // correctly utilise this functionality.
  //
  // Maybe it's worth removing all together, if we're loading via any UMD method.
  self.noConflict = function() {
    window.Scouter = old;
    return self;
  };

  // ### Scouter.refresh
  //
  // Resets the Scouter rate limit. If you have a large single-page app, you may
  // wish to call this in your router to avoid exception reports being thrown
  // away.
  //
  // By default Scouter aggressively limits the number of exception reports from
  // one page load. This protects both the client's browser and our servers in
  // cases where exceptions are thrown in tight loops or scroll handlers.
  self.refresh = function() {
    eventsRemaining = 10;
  };

  //
  // ### Manual error notification (public methods)
  //

  // #### Scouter.notifyException
  //
  // Notify Scouter about a given `exception`, typically that you've caught
  // with a `try/catch` statement or that you've generated yourself.
  //
  // It's almost always better to let an exception bubble rather than catching
  // it, as that gives more consistent behaviour across browsers. Consider
  // re-throwing instead of calling .notifyException.
  //
  // Since most JavaScript exceptions use the `Error` class, we also allow
  // you to provide a custom error name when calling `notifyException`.
  //
  // The default value is "warning" and "error" and "info" are also supported by the
  // backend, all other values cause the notification to be dropped; and you
  // will not see it in your dashboard.
  self.notifyException = function (exception, name, metaData, severity) {
    if (name && typeof name !== "string") {
      metaData = name;
      name = undefined;
    }
    if (!metaData) {
      metaData = {};
    }
    addScriptToMetaData(metaData);

    sendToScouter({
      name: name || exception.name,
      message: exception.message || exception.description,
      stacktrace: stacktraceFromException(exception) || generateStacktrace(exception.message || exception.description),
      file: exception.fileName || exception.sourceURL,
      lineNumber: exception.lineNumber || exception.line,
      columnNumber: exception.columnNumber ? exception.columnNumber + 1 : undefined,
      severity: severity || "warning"
    }, metaData);
  };

  // #### Scouter.notify
  //
  // Notify Scouter about an error by passing in a `name` and `message`,
  // without requiring an exception.
  self.notify = function (name, message, metaData, severity) {
    sendToScouter({
      name: name,
      message: message,
      stacktrace: generateStacktrace(message),
      // These are defaults so that 'scouter.notify()' calls show up in old IE,
      // newer browsers get a legit stacktrace from generateStacktrace().
      file: window.location.toString(),
      lineNumber: 1,
      severity: severity || "warning"
    }, metaData);
  };

  // Return a function acts like the given function, but reports
  // any exceptions to Scouter before re-throwing them.
  //
  // This is not a public function because it can only be used if
  // the exception is not caught after being thrown out of this function.
  //
  // If you call wrap twice on the same function, it'll give you back the
  // same wrapped function. This lets removeEventListener to continue to
  // work.
  function wrap(_super, options) {
    try {
      if (typeof _super !== "function") {
        return _super;
      }
      if (!_super.scouter) {
        var currentScript = getCurrentScript();
        _super.scouter = function (event) {
          if (options && options.eventHandler) {
            lastEvent = event;
          }
          lastScript = currentScript;

          // We set shouldCatch to false on IE < 10 because catching the error ruins the file/line as reported in window.onerror,
          // We set shouldCatch to false on Chrome/Safari because it interferes with "break on unhandled exception"
          // All other browsers need shouldCatch to be true, as they don't pass the exception object to window.onerror
          if (shouldCatch) {
            try {
              return _super.apply(this, arguments);
            } catch (e) {
              // We do this rather than stashing treating the error like lastEvent
              // because in FF 26 onerror is not called for synthesized event handlers.
              if (getSetting("autoNotify", true)) {
                self.notifyException(e, null, null, "error");
                ignoreNextOnError();
              }
              throw e;
            } finally {
              lastScript = null;
            }
          } else {
            var ret = _super.apply(this, arguments);
            // in case of error, this is set to null in window.onerror
            lastScript = null;
            return ret;
          }
        };
        _super.scouter.scouter = _super.scouter;
      }
      return _super.scouter;

      // This can happen if _super is not a normal javascript function.
      // For example, see https://github.com/bugsnag/bugsnag-js/issues/28
    } catch (e) {
      return _super;
    }
  }

  //
  // ### Script tag tracking
  //

  // To emulate document.currentScript we use document.scripts.last.
  // This only works while synchronous scripts are running, so we track
  // that here.
  var synchronousScriptsRunning = document.readyState !== "complete";
  function loadCompleted() {
    synchronousScriptsRunning = false;
  }

  // from jQuery. We don't have quite such tight bounds as they do if
  // we end up on the window.onload event as we don't try and hack
  // the .scrollLeft() fix in because it doesn't work in frames so
  // we'd need these fallbacks anyway.
  // The worst that can happen is we group an event handler that fires
  // before us into the last script tag.
  if (document.addEventListener) {
    document.addEventListener("DOMContentLoaded", loadCompleted, true);
    window.addEventListener("load", loadCompleted, true);
  } else {
    window.attachEvent("onload", loadCompleted);
  }

  function getCurrentScript() {
    var script = document.currentScript || lastScript;

    if (!script && synchronousScriptsRunning) {
      var scripts = document.scripts || document.getElementsByTagName("script");
      script = scripts[scripts.length - 1];
    }

    return script;
  }

  function addScriptToMetaData(metaData) {
    var script = getCurrentScript();

    if (script) {
      metaData.script = {
        src: script.src,
        content: getSetting("inlineScript", true) ? script.innerHTML : ""
      };
    }
  }

  //
  // ### Helpers & Setup
  //

  // Compile regular expressions upfront.
  var API_KEY_REGEX = /^[0-9a-f]{32}$/i;
  var FUNCTION_REGEX = /function\s*([\w\-$]+)?\s*\(/i;

  // Set up default notifier settings.
  var DEFAULT_NOTIFIER_ENDPOINT = "/_scouter_browser.jsp";
  var NOTIFIER_VERSION = "1.0.0";

  // Keep a reference to the currently executing script in the DOM.
  // We'll use this later to extract settings from attributes.
  var scripts = document.getElementsByTagName("script");
  var thisScript = scripts[scripts.length - 1];

  // Simple logging function that wraps `console.log` if available.
  // This is useful for warning about configuration issues
  // eg. forgetting to set an API key.
  function log(msg) {
    var disableLog = getSetting("disableLog");

    var console = window.console;
    if (console !== undefined && console.log !== undefined && !disableLog) {
      console.log("[Scouter] " + msg);
    }
  }

  // Deeply serialize an object into a query string. We use the PHP-style
  // nested object syntax, `nested[keys]=val`, to support heirachical
  // objects. Similar to jQuery's `$.param` method.
  function serialize(obj, prefix, depth) {
    if (depth >= 5) {
      return encodeURIComponent(prefix) + "=[RECURSIVE]";
    }
    depth = depth + 1 || 1;

    try {
      if (window.Node && obj instanceof window.Node) {
        return encodeURIComponent(prefix) + "=" + encodeURIComponent(targetToString(obj));
      }

      var str = [];
      for (var p in obj) {
        if (obj.hasOwnProperty(p) && p != null && obj[p] != null) {
          var k = prefix ? prefix + "[" + p + "]" : p, v = obj[p];
          str.push(typeof v === "object" ? serialize(v, k, depth) : encodeURIComponent(k) + "=" + encodeURIComponent(v));
        }
      }
      return str.join("&");
    } catch (e) {
      return encodeURIComponent(prefix) + "=" + encodeURIComponent("" + e);
    }
  }

  // Deep-merge the `source` object into the `target` object and return
  // the `target`. Properties in source that will overwrite those in target.
  // Similar to jQuery's `$.extend` method.
  function merge(target, source) {
    if (source == null) {
      return target;
    }

    target = target || {};
    for (var key in source) {
      if (source.hasOwnProperty(key)) {
        try {
          if (source[key].constructor === Object) {
            target[key] = merge(target[key], source[key]);
          } else {
            target[key] = source[key];
          }
        } catch (e) {
          target[key] = source[key];
        }
      }
    }

    return target;
  }

  // Make a HTTP request with given `url` and `params` object.
  // For maximum browser compatibility and cross-domain support, requests are
  // made by creating a temporary JavaScript `Image` object.
  function request(url, params) {
    url += "?" + serialize(params) + "&p=err&z=" + new Date().getTime();
    if (typeof SCOUTER_TESTING !== "undefined" && self.testRequest) {
      self.testRequest(url, params);
    } else {
      var img = new Image();
      img.src = url;
    }
  }

  // Extract all `data-*` attributes from a DOM element and return them as an
  // object. This is used to allow Scouter settings to be set via attributes
  // on the `script` tag, eg. `<script data-apikey="xyz">`.
  // Similar to jQuery's `$(el).data()` method.
  function getData(node) {
    var dataAttrs = {};
    var dataRegex = /^data\-([\w\-]+)$/;

    // If the node doesn't exist due to being loaded as a commonjs module,
    // then return an empty object and fallback to self[].
    if (node) {
      var attrs = node.attributes;
      for (var i = 0; i < attrs.length; i++) {
        var attr = attrs[i];
        if (dataRegex.test(attr.nodeName)) {
          var key = attr.nodeName.match(dataRegex)[1];
          dataAttrs[key] = attr.value || attr.nodeValue;
        }
      }
    }

    return dataAttrs;
  }

  // Get configuration settings from either `self` (the `Scouter` object)
  // or `data` (the `data-*` attributes).
  var data;
  function getSetting(name, fallback) {
    data = data || getData(thisScript);
    var setting = self[name] !== undefined ? self[name] : data[name.toLowerCase()];
    if (setting === "false") {
      setting = false;
    }
    return setting !== undefined ? setting : fallback;
  }

  // Validate a Scouter API key exists and is of the correct format.
  function validateApiKey(apiKey) {
    //Skip api key validation
    if(1===1) { return true; }

    if (!apiKey || !apiKey.match(API_KEY_REGEX)) {
      log("Invalid API key '" + apiKey + "'");
      return false;
    }

    return true;
  }

  // Send an error to Scouter.
  function sendToScouter(details, metaData) {
    // Validate the configured API key.
    var apiKey = getSetting("apiKey");
    if (!validateApiKey(apiKey) || !eventsRemaining) {
      return;
    }
    eventsRemaining -= 1;

    // Check if we should notify for this release stage.
    var releaseStage = getSetting("releaseStage", "production");
    var notifyReleaseStages = getSetting("notifyReleaseStages");
    if (notifyReleaseStages) {
      var shouldNotify = false;
      for (var i = 0; i < notifyReleaseStages.length; i++) {
        if (releaseStage === notifyReleaseStages[i]) {
          shouldNotify = true;
          break;
        }
      }

      if (!shouldNotify) {
        return;
      }
    }

    // Don't send multiple copies of the same error.
    // This fixes a problem when a client goes into an infinite loop,
    // and starts wasting all their bandwidth sending messages to scouter.
    var deduplicate = [details.name, details.message, details.stacktrace].join("|");
    if (deduplicate === previousNotification) {
      return;
    } else {
      previousNotification = deduplicate;
    }

    if (lastEvent) {
      metaData = metaData || {};
      metaData["Last Event"] = eventToMetaData(lastEvent);
    }

    // Build the request payload by combining error information with other data
    // such as user-agent and locale, `metaData` and settings.
    var payload = {
      notifierVersion: NOTIFIER_VERSION,

      apiKey: apiKey,
      host: getSetting("host") || window.location.protocol + "//" + window.location.host,
      uri: getSetting("uri") || window.location.pathname,
      metaData: merge(merge({}, getSetting("metaData")), metaData),
      releaseStage: releaseStage,
      appVersion: getSetting("appVersion"),

      url: window.location.href,
      userAgent: navigator.userAgent,
      language: navigator.language || navigator.userLanguage,

      severity: details.severity,

      name: details.name,
      message: details.message,
      stacktrace: details.stacktrace,
      file: details.file,
      lineNumber: details.lineNumber,
      columnNumber: details.columnNumber,
      payloadVersion: "2"
    };

    // Run any `beforeNotify` function
    var beforeNotify = self.beforeNotify;
    if (typeof(beforeNotify) === "function") {
      var retVal = beforeNotify(payload, payload.metaData);
      if (retVal === false) {
        return;
      }
    }

    if (payload.lineNumber === 0 && (/Script error\.?/).test(payload.message)) {
      return log("Ignoring cross-domain script error. See https://bugsnag.com/docs/notifiers/js/cors");
    }

    // Make the HTTP request
    request(getSetting("endPoint") || DEFAULT_NOTIFIER_ENDPOINT, payload);
  }

  // Generate a browser stacktrace (or approximation) from the current stack.
  // This is used to add a stacktrace to `Scouter.notify` calls, and to add a
  // stacktrace approximation where we can't get one from an exception.
  function generateStacktrace(message) {
    var generated, stacktrace;
    var MAX_FAKE_STACK_SIZE = 10;
    var ANONYMOUS_FUNCTION_PLACEHOLDER = "[anonymous]";

    // Try to generate a real stacktrace (most browsers, except IE9 and below).
    try {
      throw new Error("");
    } catch (exception) {
      generated = "<generated>\n";
      stacktrace = stacktraceFromException(exception);
    }

    // Otherwise, build a fake stacktrace from the list of method names by
    // looping through the list of functions that called this one (and skip
    // whoever called us).
    if (!stacktrace) {
      generated = "<generated-ie>\n";
      var functionStack = [];
      try {
        var curr = arguments.callee.caller.caller;
        while (curr && functionStack.length < MAX_FAKE_STACK_SIZE) {
          var fn = FUNCTION_REGEX.test(curr.toString()) ? RegExp.$1 || ANONYMOUS_FUNCTION_PLACEHOLDER : ANONYMOUS_FUNCTION_PLACEHOLDER;
          functionStack.push(fn);
          curr = curr.caller;
        }
      } catch (e) {
        log(e);
      }
      stacktrace = functionStack.join("\n");
    }

    // Tell the backend to ignore the first two lines in the stack-trace.
    // generateStacktrace() + window.onerror,
    // generateStacktrace() + notify,
    // generateStacktrace() + notifyException
    return generated + "<msg:"+ message + ">" + stacktrace;
  }

  // Get the stacktrace string from an exception
  function stacktraceFromException(exception) {
    return exception.stack || exception.backtrace || exception.stacktrace;
  }

  // Populate the event tab of meta-data.
  function eventToMetaData(event) {
    var tab = {
      millisecondsAgo: new Date() - event.timeStamp,
      type: event.type,
      which: event.which,
      target: targetToString(event.target)
    };

    return tab;
  }

  // Convert a DOM element into a string suitable for passing to Scouter.
  function targetToString(target) {
    if (target) {
      var attrs = target.attributes;

      if (attrs) {
        var ret = "<" + target.nodeName.toLowerCase();
        for (var i = 0; i < attrs.length; i++) {
          if (attrs[i].value && attrs[i].value.toString() != "null") {
            ret += " " + attrs[i].name + "=\"" + attrs[i].value + "\"";
          }
        }
        return ret + ">";
      } else {
        // e.g. #document
        return target.nodeName;
      }
    }
  }

  // If we've notified scouter of an exception in wrap, we don't want to
  // re-notify when it hits window.onerror after we re-throw it.
  function ignoreNextOnError() {
    ignoreOnError += 1;
    window.setTimeout(function () {
      ignoreOnError -= 1;
    });
  }

  // Disable catching on IE < 10 as it destroys stack-traces from generateStackTrace()
  if (!window.atob) {
    shouldCatch = false;

    // Disable catching on browsers that support HTML5 ErrorEvents properly.
    // This lets debug on unhandled exceptions work.
  } else if (window.ErrorEvent) {
    try {
      if (new window.ErrorEvent("test").colno === 0) {
        shouldCatch = false;
      }
    } catch(e){ }
  }


  //
  // ### Polyfilling
  //

  // Add a polyFill to an object
  function polyFill(obj, name, makeReplacement) {
    var original = obj[name];
    var replacement = makeReplacement(original);
    obj[name] = replacement;

    if (typeof SCOUTER_TESTING !== "undefined" && window.undo) {
      window.undo.push(function () {
        obj[name] = original;
      });
    }
  }

  if (getSetting("autoNotify", true)) {
    //
    // ### Automatic error notification
    //
    // Attach to `window.onerror` events and notify Scouter when they happen.
    // These are mostly js compile/parse errors, but on some browsers all
    // "uncaught" exceptions will fire this event.
    //
    polyFill(window, "onerror", function (_super) {
      // Keep a reference to any existing `window.onerror` handler
      if (typeof SCOUTER_TESTING !== "undefined") {
        self._onerror = _super;
      }

      return function scouter(message, url, lineNo, charNo, exception) {
        var shouldNotify = getSetting("autoNotify", true);
        var metaData = {};

        // IE 6+ support.
        if (!charNo && window.event) {
          charNo = window.event.errorCharacter;
        }

        addScriptToMetaData(metaData);
        lastScript = null;

        if (shouldNotify && !ignoreOnError) {

          sendToScouter({
            name: exception && exception.name || "window.onerror",
            message: message,
            file: url,
            lineNumber: lineNo,
            columnNumber: charNo,
            stacktrace: (exception && stacktraceFromException(exception)) || generateStacktrace(message),
            severity: "error"
          }, metaData);
        }

        if (typeof SCOUTER_TESTING !== "undefined") {
          _super = self._onerror;
        }

        // Fire the existing `window.onerror` handler, if one exists
        if (_super) {
          _super(message, url, lineNo, charNo, exception);
        }
      };
    });

    var hijackTimeFunc = function (_super) {
      // Note, we don't do `_super.call` because that doesn't work on IE 8,
      // luckily this is implicitly window so it just works everywhere.
      //
      // setTimout in all browsers except IE <9 allows additional parameters
      // to be passed, so in order to support these without resorting to call/apply
      // we need an extra layer of wrapping.
      return function (f, t) {
        if (typeof f === "function") {
          f = wrap(f);
          var args = Array.prototype.slice.call(arguments, 2);
          return _super(function () {
            f.apply(this, args);
          }, t);
        } else {
          return _super(f, t);
        }
      };
    };

    polyFill(window, "setTimeout", hijackTimeFunc);
    polyFill(window, "setInterval", hijackTimeFunc);

    if (window.requestAnimationFrame) {
      polyFill(window, "requestAnimationFrame", function (_super) {
        return function (callback) {
          return _super(wrap(callback));
        };
      });
    }

    if (window.setImmediate) {
      polyFill(window, "setImmediate", function (_super) {
        return function (f) {
          var args = Array.prototype.slice.call(arguments);
          args[0] = wrap(args[0]);
          return _super.apply(this, args);
        };
      });
    }

    // EventTarget is all that's required in modern chrome/opera
    // EventTarget + Window + ModalWindow is all that's required in modern FF (there are a few Moz prefixed ones that we're ignoring)
    // The rest is a collection of stuff for Safari and IE 11. (Again ignoring a few MS and WebKit prefixed things)
    "EventTarget Window Node ApplicationCache AudioTrackList ChannelMergerNode CryptoOperation EventSource FileReader HTMLUnknownElement IDBDatabase IDBRequest IDBTransaction KeyOperation MediaController MessagePort ModalWindow Notification SVGElementInstance Screen TextTrack TextTrackCue TextTrackList WebSocket WebSocketWorker Worker XMLHttpRequest XMLHttpRequestEventTarget XMLHttpRequestUpload".replace(/\w+/g, function (global) {
      var prototype = window[global] && window[global].prototype;
      if (prototype && prototype.hasOwnProperty && prototype.hasOwnProperty("addEventListener")) {
        polyFill(prototype, "addEventListener", function (_super) {
          return function (e, f, capture, secure) {
            // HTML lets event-handlers be objects with a handlEvent function,
            // we need to change f.handleEvent here, as self.wrap will ignore f.
            try {
              if (f && f.handleEvent) {
                f.handleEvent = wrap(f.handleEvent, {eventHandler: true});
              }
            } catch (err) {
              // When selenium is installed, we sometimes get 'Permission denied to access property "handleEvent"'
              // Because this catch is around Scouter library code, it won't catch any user errors
              log(err);
            }
            return _super.call(this, e, wrap(f, {eventHandler: true}), capture, secure);
          };
        });

        // We also need to hack removeEventListener so that you can remove any
        // event listeners.
        polyFill(prototype, "removeEventListener", function (_super) {
          return function (e, f, capture, secure) {
            _super.call(this, e, f, capture, secure);
            return _super.call(this, e, wrap(f), capture, secure);
          };
        });
      }
    });
  }

  window.Scouter = self;
  // If people are using a javascript loader, we should integrate with it.
  // We don't want to defer instrumenting their code with callbacks however,
  // so we slightly abuse the intent and continue with our plan of polyfilling
  // the browser whether or not they ever actually require the module.
  // This has the nice side-effect of continuing to work when people are using
  // AMD but loading Scouter via a CDN.
  // It has the not-so-nice side-effect of polluting the global namespace, but
  // you can easily call Scouter.noConflict() to fix that.
  if (typeof define === "function" && define.amd) {
    // AMD
    define([], function () {
      return self;
    });
  } else if (typeof module === "object" && typeof module.exports === "object") {
    // CommonJS/Browserify
    module.exports = self;
  }

})(window, window.Scouter);
