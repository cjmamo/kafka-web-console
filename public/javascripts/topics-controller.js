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

    $scope.ctopic = {};
    $scope.ctopic.partitions = 1;
    $scope.ctopic.replications = 1;
    $scope.groups = [
            {name:'All'},
            {name:'Development'},
            {name:'Production'},
            {name:'Staging'},
            {name:'Test'}];

    $scope.zookeepers = {};

    $scope.getTopic = function (topic) {
        $location.path('/topics/' + topic.name + '/' + topic.zookeeper);
    };
    $scope.getZookeepers = function (group) {
        $http.get('/zookeepers.json/' + group).
            success(function (data) {
                $scope.zookeepers = data
            });
    };

    $scope.selectGroup = function () {
        $scope.getZookeepers($scope.ctopic.group.name)
    };

    $scope.createTopic = function (ctopic) {
        console.log(ctopic)
        $http.post('/topics.json', { name: ctopic.name, group: ctopic.group.name, zookeeper: ctopic.zookeeper.name, partitions: ctopic.partitions, replications: ctopic.replications}).success(function () {
            location.reload();
        });
    };

    $scope.removeTopic = function (topic) {
        $http.delete('/topics.json/' + topic.name + '/'+topic.zookeeper).success(function () {
            location.reload();
        });
    };
});
