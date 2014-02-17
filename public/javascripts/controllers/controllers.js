app.controller("ZookeepersController", function ($scope, $http, feedService) {
//    $scope.tabs = [
//        { title:"Dynamic Title 1", content:"Dynamic content 1" },
//        { title:"Dynamic Title 2", content:"Dynamic content 2", disabled: true }
//    ];
//
//    $scope.alertMe = function() {
//        setTimeout(function() {
//            alert("You've selected the alert tab!");
//        });
//    };

//    $scope.navType = 'pills';

    feedService.onmessage = function (message) {
        var zookeeper = angular.fromJson(message.data);
        var isNewZookeeper = true;

        angular.forEach($scope.zookeepers, function (zookeeper) {
                if (zookeeper.host === zookeeper.host && zookeeper.port === zookeeper.port) {
                    zookeeper.status= zookeeper.status;
                    isNewZookeeper = false;
                }
            }
        )

        if (isNewZookeeper && typeof($scope.zookeepers) !== 'undefined') {
            $scope.zookeepers.push(zookeeper)
        }
        else if (typeof($scope.zookeepers) === 'undefined') {
            $scope.zookeepers = [zookeeper]
        }

        $scope.$apply()
    }

    $scope.getZookeepers = function (group) {
        $http.get('/zookeepers', {params: {group: group}, headers: {Accept: 'application/json'}}).
            success(function (data, status, headers, config) {
                $scope.zookeepers = angular.fromJson(data)
            })
    }

    $scope.createZookeeper = function (zookeeper) {
        $http.post('/zookeepers', { host: zookeeper.host, port: zookeeper.port});
    }

});