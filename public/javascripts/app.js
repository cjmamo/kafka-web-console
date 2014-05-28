var app = angular.module('app', ['ngRoute', 'ngAnimate'])
    .config(function ($routeProvider) {
        $routeProvider
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
            .when('/topics/:name/:zookeeper', {
                controller: 'TopicController',
                templateUrl: function (params) {
                    return '/topics/' + params.name + '/' + params.zookeeper
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

app.service('topicService', function () {
    var topic_ = "";
    var zookeeper_ = "";

    this.setTopic = function (topic) {
        topic_ = topic;
    };

    this.getTopic = function () {
        return topic_;
    };

    this.setZookeeper = function (zookeeper) {
        zookeeper_ = zookeeper;
    };

    this.getZookeeper = function () {
        return zookeeper_;
    };
});

app.filter('reverse', function () {
    return function (items) {
        return items.slice().reverse();
    };
});