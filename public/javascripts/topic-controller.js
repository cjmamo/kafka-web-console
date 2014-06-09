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

app.controller("TopicController", function ($http, $scope, $location, $routeParams, $filter) {
    $http.get('/topics.json/' + $routeParams.topic + '/' + $routeParams.zookeeper).success(function (data) {
        $scope.topic = data;
        angular.forEach($scope.topic, function (consumerGroup) {
            angular.forEach(consumerGroup.partitions, function (partition) {
                partition.id = parseInt(partition.id)
            });

            consumerGroup.partitions = $filter('orderObjectBy')(consumerGroup.partitions, 'id')
        });
    });

    var ws = new WebSocket('ws://' + $location.host() + ':' + $location.port() + '/topics.json/' + $routeParams.topic + '/' + $routeParams.zookeeper + '/feed');
    ws.onmessage = function (message) {
        var well = angular.element('<div class="well well-sm"/>');
        well.text(message.data);
        $("#messages").append(well);
        $scope.$apply();
    };

    $scope.$on('$destroy', function () {
        ws.close();
    });

    $scope.getConsumerGroup = function (consumerGroup) {
        $http.get('/consumergroups.json/' + consumerGroup + '/' + $routeParams.topic + '/' + $routeParams.zookeeper).success(function (data) {
            angular.forEach($scope.topic, function (consumerGroup_) {
                if (consumerGroup === consumerGroup_.consumerGroup) {
                    consumerGroup_.consumers = data;
                }
            });
        });
    };

    $scope.getOffsetHistory = function (consumerGroup) {
        $location.path('/offsethistory/' + consumerGroup.consumerGroup + '/' + $routeParams.topic + '/' + $routeParams.zookeeper);
    };

});