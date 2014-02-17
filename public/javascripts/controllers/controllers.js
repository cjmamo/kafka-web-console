app.controller("ServersController", function ($scope, $http, feedService) {
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
        var receivedServer = angular.fromJson(message.data);
        var isNewServer = true;

        angular.forEach($scope.servers, function (server) {
                if (server.address === receivedServer.address && server.port === receivedServer.port) {
                    server.status= receivedServer.status;
                    isNewServer = false;
                }
            }
        )

        if (isNewServer && typeof($scope.servers) !== 'undefined') {
            $scope.servers.push(receivedServer)
        }
        else if (typeof($scope.servers) === 'undefined') {
            $scope.servers = [receivedServer]
        }

        $scope.$apply()
    }

    $scope.getServers = function (group) {
        $http.get('/servers', {params: {group: group}, headers: {Accept: 'application/json'}}).
            success(function (data, status, headers, config) {
                $scope.servers = angular.fromJson(data)
            })
    }

    $scope.createServer = function (zookeeper) {
        $http.post('/servers', { address: zookeeper.address, port: zookeeper.port});
    }

});