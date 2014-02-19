app.controller("ZookeepersController", function ($scope, $http, feedService) {

    feedService.onmessage = function (message) {
        var serverZookeeper = angular.fromJson(message.data);
        var isNewZookeeper = true;

        angular.forEach($scope.zookeepers, function (clientZookeeper) {
                if (clientZookeeper.name === serverZookeeper.name) {
                    clientZookeeper.status = serverZookeeper.status;
                    isNewZookeeper = false;
                }
            }
        )

        if (isNewZookeeper && typeof($scope.zookeepers) !== 'undefined') {
            $scope.zookeepers.push(serverZookeeper);
        }
        else if (typeof($scope.zookeepers) === 'undefined') {
            $scope.zookeepers = [serverZookeeper];
        }

        $scope.$apply();
    }

    $scope.getZookeepers = function (group) {
        $http.get('/zookeepers', {params: {group: group}, headers: {Accept: 'application/json'}}).
            success(function (data, status, headers, config) {
                $scope.zookeepers = angular.fromJson(data);
            })
    }

    $scope.createZookeeper = function (zookeeper) {
        $http.post('/zookeepers', { name: zookeeper.name, host: zookeeper.host, port: zookeeper.port});
    }

});

app.controller("TopicsController", function ($scope, $location) {
    $scope.getConsumer = function (zookeeper, topic) {
        $location.path('/consumers/' + zookeeper + '/' + topic)
    }
});

app.controller("ConsumersController", function ($scope) {

});