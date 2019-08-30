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

// Code goes here

$(document).ready(function () {
  var urlVars = getUrlVars();
  var source = decodeURIComponent(urlVars['source']);
  if(source.charAt(0) != '/' && !(source.startsWith('http://') || source.startsWith('https://'))) {
    source = encodeURI('/scouter/v1/counter/' + source);
  }

  $.get(source, function (data) {
    init(data);
  });

  var colors = {
    defaultRgbMap: [
      [55, 78, 179],
      [5, 128, 100],
      [55, 178, 180],
      [105, 128, 181],
      [156, 128, 163],
      [157, 178, 182],
      [105, 128, 203],
      [158, 128, 161],
      [1, 2, 222],
      [0, 128, 10],
      [101, 9, 251],
      [41, 121, 138],
      [11, 50, 249]
    ],
    assignedColorIndex: 0,
    assignedObject: {},
    getColorByOrder: function (objectKey) {
      if (!this.assignedObject[objectKey]) {
        var color = this.defaultRgbMap[this.assignedColorIndex];
        this.assignedColorIndex = this.assignedColorIndex + 1;
        if (this.assignedColorIndex == this.defaultRgbMap.length) {
          this.assignedColorIndex = 0;
        }
        var rgb = "rgb(" + color[0] + "," + color[1] + "," + color[2] + ")";
        this.assignedObject[objectKey] = rgb;
        return rgb;
      } else {
        return this.assignedObject[objectKey];
      }
    }
  };

  function getUrlVars()
  {
    var vars = [], hash;
    var hashes = window.location.href.slice(window.location.href.indexOf('?') + 1).split('&');
    for(var i = 0; i < hashes.length; i++)
    {
      hash = hashes[i].split('=');
      vars.push(hash[0]);
      vars[hash[0]] = hash[1];
    }
    return vars;
  }

  function init(data) {
    var sourceData = data;

    if (sourceData.status != '200') {
      $('#counterName').text("error");
      return;
    }

    if (!sourceData || !sourceData.result) {
      $('#counterName').text("no data");
      return;
    }

    if (sourceData.result.length == 0) {
      $('#counterName').text("no data");
      return;
    }

    $('#counterName').text(sourceData.result[0].displayName);
    $('#unit').text(sourceData.result[0].unit);

    var from;
    var to;
    if(sourceData.result[0].startTimeMillis) {
      from = moment(Number(sourceData.result[0].startTimeMillis)).format('MMM Do HH:mm');
      to = moment(Number(sourceData.result[0].endTimeMillis)).format('HH:mm Z');
    } else {
      from = moment(sourceData.result[0].fromYmd).format('MMM Do');
      to = moment(sourceData.result[0].toYmd).format('MMM Do (Z)');
    }

    $('#dataRange').text(from + " ~ " + to);

    var datasets = [];

    sourceData.result.forEach(function (result) {
      var dataset = {};
      dataset.label = result.objName.substr(result.objName.lastIndexOf('/') + 1);
      dataset.data = result.timeList.map(function (t, i) {
        return {x: moment(Number(t)), y: Number(result.valueList[i]).toFixed(2)};
      });
      dataset.fill = false;
      dataset.pointRadius = 0;
      dataset.borderWidth = 1;
      dataset.borderColor = colors.getColorByOrder(result.objName);
      dataset.backgroundColor = colors.getColorByOrder(result.objName);
      datasets.push(dataset);
    });

    // console.log(datasets);

    function arraysEqual(a, b) {
      if (a === b) return true;
      if (a == null || b == null) return false;
      if (a.length != b.length) return false;

      // If you don't care about the order of the elements inside
      // the array, you should sort both arrays here.

      for (var i = 0; i < a.length; ++i) {
        if (a[i] !== b[i]) return false;
      }
      return true;
    }

    var ctx = document.getElementById('myChart').getContext('2d');
    var myChart = new Chart(ctx, {
      type: 'line',
      data: {
        "datasets": datasets
      },
      options: {
        elements: {
          line: {
            tension: 0, // disables bezier curves
          }
        },
        animation: {
          duration: 0, // general animation time
        },
        hover: {
          animationDuration: 0, // duration of animations when hovering an item
        },
        responsiveAnimationDuration: 0, // animation duration after a resize
        responsive: true,
        legend: {
          display: false,
          position: 'bottom'
        },
        tooltips: {
          enabled: true,
          mode: 'nearest',
          intersect: false,
          callbacks: {
            title: function (tooltipItem, data) {
              if (tooltipItem[0]) {
                return tooltipItem[0].xLabel.format('HH:mm:ss (MMM Do) Z');
              } else {
                return 'data';
              }
            },
            label: function (tooltipItem, data) {
              var label = ' ' + data.datasets[tooltipItem.datasetIndex].label || '';
              if (label) {
                label += ': ';
              }
              var value = Math.round(tooltipItem.yLabel * 100) / 100;
              if (value >= 1000) {
                value = Math.round(value);
              }
              var parts = value.toString().split(".");
              parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ",");
              label += parts.join(".")
              return label;
            }
          }
        },
        scales: {
          xAxes: [{
            type: 'time',
            offset: false,
            bounds: 'data',
            time: {
              displayFormats: {
                minute: 'HH:mm',
                second: 'HH:mm:ss'
              },
              unit: 'second',
              minUnit: 'minute'
            },
            distribution: 'linear',
            ticks: {
              // callback: function (value, index, values) {
              //   if (value.substr(value.length - 2) != '00') {
              //     return undefined;
              //   }
              //   return value;
              // },
              maxRotation: 0,
              autoSkip: true,
              autoSkipPadding: 5,
              padding: 5,
              source: 'auto'
            },
            scaleLabel: {
              display: false,
              labelString: "Time"
            },
            gridLines: {
              offsetGridLines: false,
              drawBorder: false,
              lineWidth: 1,
              drawTicks: true,
              tickMarkLength: 5
            }
            // bounds: 'ticks',
            // //distribution: 'linear',
            // display: true,

          }],
          yAxes: [{
            display: true,
            scaleLabel: {
              display: false,
              labelString: "Value"
            },
            gridLines: {
              drawTicks: false,
              tickMarkLength: 5
            },
            ticks: {
              min: 0,
              callback: function (value, index, values) {
                if (value > 1000) {
                  return value / 1000 + "k";
                } else if (value > 1000 * 1000) {
                  return value / (1000 * 1000) + "m";
                } else if (value > 1000 * 1000 * 1000) {
                  return value / (1000 * 1000 * 1000) + "g";
                }
                return value;
              },
              padding: 5
            }
          }]
        }
      }
    });
  }

});
