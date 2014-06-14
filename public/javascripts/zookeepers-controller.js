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

app.controller("ZookeepersController", function ($scope, $http, $location) {

    $('input').tooltip()

    $scope.groups = [
        {name: 'All'},
        {name: 'Development'},
        {name: 'Production'},
        {name: 'Staging'},
        {name: 'Test'}
    ];

    $scope.zookeeper = {};
    $scope.zookeeper.group = $scope.groups[0];

    var ws = new WebSocket('ws://' + $location.host() + ':' + $location.port() + '/zookeepers.json/feed');

    ws.onmessage = function (message) {
        var serverZk = angular.fromJson(message.data);
        var modelName = angular.lowercase(serverZk.group) + 'Zookeepers';
        var isNewZookeeper = true;

        angular.forEach($scope[modelName], function (clientZk) {
                if (clientZk.name === serverZk.name) {
                    clientZk.status = serverZk.status;
                    isNewZookeeper = false;
                }
            }
        );

        angular.forEach($scope['allZookeepers'], function (clientZk) {
                if (clientZk.name === serverZk.name) {
                    clientZk.status = serverZk.status;
                    isNewZookeeper = false;
                }
            }
        );

        if (isNewZookeeper && typeof($scope[modelName]) !== 'undefined') {
            $scope[modelName].push(serverZk);
        }
        else if (typeof($scope[modelName]) === 'undefined') {
            $scope[modelName] = [serverZk];
        }

        $scope.$apply();
    };

    $scope.$on('$destroy', function () {
        ws.close();
    });

    $scope.getZookeepers = function (group) {
        $http.get('/zookeepers.json/' + group).
            success(function (data) {
                $scope[group + 'Zookeepers'] = data;
            });
    };

    $scope.createZookeeper = function (zookeeper) {
        $http.put('/zookeepers.json', { name: zookeeper.name, host: zookeeper.host, port: zookeeper.port, group: zookeeper.group.name, chroot: zookeeper.chroot}).success(function () {
            $location.path("/");
        });
    };

    $scope.removeZookeeper = function (zookeeper) {
        $http.delete('/zookeepers.json/' + zookeeper.name).success(function () {
            $location.path("/");
        });
    };
});