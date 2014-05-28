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