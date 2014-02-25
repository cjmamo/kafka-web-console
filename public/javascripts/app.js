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
