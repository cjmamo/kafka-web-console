app.controller("ZookeepersController", function ($scope, $http, $location) {

    $('input').tooltip()

    $scope.groups = [
        {name: 'All'},
        {name: 'Development'},
        {name: 'Production'},
        {name: 'Staging'},
        {name: 'Test'}
    ];

    $scope.zookeeper = {};
    $scope.zookeeper.group = $scope.groups[0];

    var ws = new WebSocket('ws://' + $location.host() + ':' + $location.port() + '/zookeepers.json/feed');

    ws.onmessage = function (message) {
        var serverZk = angular.fromJson(message.data);
        var modelName = angular.lowercase(serverZk.group) + 'Zookeepers';
        var isNewZookeeper = true;

        angular.forEach($scope[modelName], function (clientZk) {
                if (clientZk.name === serverZk.name) {
                    clientZk.status = serverZk.status;
                    isNewZookeeper = false;
                }
            }
        );

        angular.forEach($scope['allZookeepers'], function (clientZk) {
                if (clientZk.name === serverZk.name) {
                    clientZk.status = serverZk.status;
                    isNewZookeeper = false;
                }
            }
        );

        if (isNewZookeeper && typeof($scope[modelName]) !== 'undefined') {
            $scope[modelName].push(serverZk);
        }
        else if (typeof($scope[modelName]) === 'undefined') {
            $scope[modelName] = [serverZk];
        }

        $scope.$apply();
    };

    $scope.$on('$destroy', function () {
        ws.close();
    });

    $scope.getZookeepers = function (group) {
        $http.get('/zookeepers.json/' + group).
            success(function (data) {
                $scope[group + 'Zookeepers'] = data;
            });
    };

    $scope.createZookeeper = function (zookeeper) {
        $http.post('/zookeepers.json', { name: zookeeper.name, host: zookeeper.host, port: zookeeper.port, group: zookeeper.group.name, chroot: zookeeper.chroot}).success(function () {
            $location.path("/");
        });
    };

    $scope.removeZookeeper = function (zookeeper) {
        $http.delete('/zookeepers.json/' + zookeeper.name).success(function () {
            $location.path("/");
        });
    };
});