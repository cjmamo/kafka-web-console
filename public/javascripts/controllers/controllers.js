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
    $scope.getConsumerGroup = function (zookeeper, topic) {
        $location.path('/consumergroups/zookeeper/' + zookeeper + '/topic/' + topic)
    }
});

app.controller("ConsumerGroupsController", function ($scope) {
});

app.controller("BrokersController", function ($scope) {


    $scope.activate = function () {
        alert('asd')
        $scope.isActive = true;
    };



//    $scope.isActive = true;
////    $scope.globalContext = globalContext;
//
////    alert($location.path())
//    $scope.locationPath = $location.path()
////  alert($scope.locationPath)
//
//    $scope.isActive = function (route) {
//        alert("ASd")
//        return route === $location.path();
//    };

});