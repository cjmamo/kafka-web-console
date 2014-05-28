app.controller("TopicController", function ($http, $scope, $location, $routeParams, $filter) {
    $http.get('/topics.json/' + $routeParams.name + '/' + $routeParams.zookeeper).success(function (data) {
        $scope.topic = data;
        angular.forEach($scope.topic, function (consumerGroup) {
            angular.forEach(consumerGroup.partitions, function (partition) {
                partition.id = parseInt(partition.id)
            });

            consumerGroup.partitions = $filter('orderObjectBy')(consumerGroup.partitions, 'id')
        });
    });

    var ws = new WebSocket('ws://' + $location.host() + ':' + $location.port() + '/topics.json/' + $routeParams.name + '/' + $routeParams.zookeeper + '/feed');
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
        $http.get('/consumergroups.json/' + consumerGroup + '/' + $routeParams.name + '/' + $routeParams.zookeeper).success(function (data) {
            angular.forEach($scope.topic, function (consumerGroup_) {
                if (consumerGroup === consumerGroup_.consumerGroup) {
                    consumerGroup_.consumers = data;
                }
            });
        });
    };

});