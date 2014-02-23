var app = angular.module('app', ['ngRoute', 'ui.bootstrap'])
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
//            .when('/topics/:name/:zookeeper', {
//                controller: 'TopicsController'
//                template: function($scope) {
//                    alert($scope.zookeeper)
//                    window.location.hash = '/topics/' + $scope.name
//                    return "assdds"
//                }
//                templateUrl: '/topics'
//            })
            .when('/brokers', {
                controller: 'BrokersController',
                templateUrl: '/brokers'
            })
            .when('/topics/:name', {
                controller: 'TopicController',
                templateUrl: function (params) {
                    return '/topics/' + params.name
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

//app.service('feedService', function ($location) {
//    ws = new WebSocket('ws://' + $location.host() + ':' + $location.port() + '/feed');
//    return ws
//});

app.filter('reverse', function () {
    return function (items) {
        return items.slice().reverse();
    };
});