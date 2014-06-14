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

var app = angular.module('app', ['ngRoute', 'ngAnimate'])
    .config(function ($routeProvider) {
        $routeProvider
            .when('/settings', {
                controller: 'SettingsController',
                templateUrl: '/settings'
            })
            .when('/zookeepers', {
                controller: 'ZookeepersController',
                templateUrl: '/zookeepers'
            })
            .when('/topics', {
                controller: 'TopicsController',
                templateUrl: '/topics'
            })
            .when('/brokers', {
                controller: 'BrokersController',
                templateUrl: '/brokers'
            })
            .when('/topics/:topic/:zookeeper', {
                controller: 'TopicController',
                templateUrl: function (params) {
                    return '/topics/' + params.topic + '/' + params.zookeeper
                }
            })
            .when('/offsethistory/:consumerGroup/:topic/:zookeeper', {
                controller: 'OffsetHistoryController',
                templateUrl: function (params) {
                    return '/offsethistory/' + params.consumerGroup + '/' + params.topic + '/' + params.zookeeper
                }
            })
            .otherwise({
                redirectTo: '/zookeepers'
            });
    });

app.run(function ($rootScope, $location) {
    $rootScope.isActive = function (route) {
        return route === $location.path();
    };
});