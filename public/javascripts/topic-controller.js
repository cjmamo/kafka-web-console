app.controller("TopicController", function ($http, $scope, $location, $routeParams) {
    var maxPartitionCount = 0;
    $http.get('/topics.json/' + $routeParams.name + '/' + $routeParams.zookeeper).success(function (data) {
        $scope.topic = data;
        angular.forEach($scope.topic, function (consumerGroup) {
            consumerGroup['consumers'] = [];

            angular.forEach(consumerGroup.offsets, function (offset) {
                offset.partition = parseInt(offset.partition)
            })

            maxPartitionCount = consumerGroup.offsets.length;

            if (maxPartitionCount < consumerGroup.offsets.length) {
                maxPartitionCount = consumerGroup.offsets.length;
            }
        });

        $scope.maxPartitionCount = new Array(maxPartitionCount);

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