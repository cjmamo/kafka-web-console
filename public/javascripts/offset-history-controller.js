/*
 * Copyright 2014 Claude Mamo
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

app.controller("OffsetHistoryController", function ($http, $scope, $location, $routeParams) {
    $http.get('offsethistory.json/' + $routeParams.consumerGroup + '/' + $routeParams.topic + '/' + $routeParams.zookeeper).success(function (data) {

        var offsetsGroupedByTimestamp = _.groupBy(data, function (offsetPoint) {
            return offsetPoint.consumerGroup.toString() + offsetPoint.timestamp.toString();
        });

        buildOffsetHistoryGraph(offsetsGroupedByTimestamp)
        buildMessageThroughputGraph(offsetsGroupedByTimestamp)

    });

    function buildMessageThroughputGraph(offsetsGroupedByTimestamp) {
        var previousOffsetPoint;
        var consumerThroughput = [];
        var producerThroughput = [];

        angular.forEach(offsetsGroupedByTimestamp, function (offsetPoint) {
            if (previousOffsetPoint !== undefined) {
                consumerThroughput.push({y: (offsetPoint[0].offset - previousOffsetPoint[0].offset) / 60, x: offsetPoint[0].timestamp});
                producerThroughput.push({y: (offsetPoint[0].logSize - previousOffsetPoint[0].logSize) / 60, x: offsetPoint[0].timestamp});
            }
            previousOffsetPoint = offsetPoint
        });

        var consumerMaxMessages = _.max(consumerThroughput, function (dataPoint) {
            return dataPoint.y;
        }).y;

        var producerMaxMessages = _.max(producerThroughput, function (dataPoint) {
            return dataPoint.y;
        }).y;

        nv.addGraph(function () {
            var chart = nv.models.lineChart().margin({left: 100, right: 40}).forceY(Math.ceil(Math.max(consumerMaxMessages, producerMaxMessages)))

            chart.xAxis.tickFormat(function (d) {
                return d3.time.format('%H:%M:%S')(new Date(d));
            }).axisLabel('Time');

            chart.yAxis.tickFormat(d3.format('d')).axisLabel('Messages per second');

            var dataPoints = [
                {
                    key: 'Consumer ',
                    values: consumerThroughput,
                    color: '#ff7f0e'
                },
                {
                    key: 'Producer/s',
                    values: producerThroughput,
                    color: '#2ca02c'
                }
            ];

            d3.select('#message-throughput-chart svg').datum(dataPoints).transition().duration(500).call(chart);
            nv.utils.windowResize(chart.update);

            return chart;
        });
    }

    function buildOffsetHistoryGraph(offsetsGroupedByTimestamp) {

        var chartData = _.map(offsetsGroupedByTimestamp, function (offsetPoint) {
            return {
                logSize: _(offsetPoint).pluck("logSize").reduce(function (sum, num) {
                    return sum + num;
                }),
                offset: _(offsetPoint).pluck("offset").reduce(function (sum, num) {
                    return sum + num;
                }),
                timestamp: offsetPoint[0].timestamp
            }
        });

        var lagDataPoints = _.map(chartData, function (offsetPoint) {
            return {
                y: offsetPoint.logSize - offsetPoint.offset,
                x: offsetPoint.timestamp
            }
        });

        var offsetDataPoints = _.map(chartData, function (offsetPoint) {
            return {
                y: offsetPoint.offset,
                x: offsetPoint.timestamp
            }
        });

        nv.addGraph(function () {

            var chart = nv.models.lineChart().margin({left: 100, right: 40}).useInteractiveGuideline(true).forceY(_.max(chartData, function (offsetPoint) {
                return offsetPoint.logSize;
            }));

            chart.xAxis.tickFormat(function (d) {
                return d3.time.format('%H:%M:%S')(new Date(d));
            }).axisLabel('Time');

            chart.yAxis.tickFormat(d3.format('d')).axisLabel('Messages');

            var dataPoints = [
                {
                    key: 'Offset',
                    values: offsetDataPoints,
                    color: '#ff7f0e'
                },
                {
                    key: 'Lag',
                    values: lagDataPoints,
                    color: '#2ca02c'
                }
            ];

            d3.select('#offset-history-chart svg').datum(dataPoints).transition().duration(500).call(chart);
            nv.utils.windowResize(chart.update);

            return chart;
        });
    }
});