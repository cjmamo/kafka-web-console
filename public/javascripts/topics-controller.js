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

app.controller("TopicsController", function ($scope, $location, $http, $filter) {
    $http.get('/topics.json').
        success(function (data) {
            $scope.topics = data;

            angular.forEach($scope.topics, function (topic) {
                angular.forEach(topic.partitions, function (partition) {
                    partition.id = parseInt(partition.id)
                });

                topic.partitions = $filter('orderObjectBy')(topic.partitions, 'id')
            });
        });

    $scope.getTopic = function (topic) {
        $location.path('/topics/' + topic.name + '/' + topic.zookeeper);
    };
});