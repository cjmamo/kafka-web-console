app.controller("TopicsController", function ($scope, $location, $http) {
    $http.get('/topics.json').
        success(function (data) {
            $scope.topics = data;
        });

    $scope.getTopic = function (topic) {
        $location.path('/topics/' + topic.name + '/' + topic.zookeeper);
    };
});