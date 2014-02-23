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
        );

        if (isNewZookeeper && typeof($scope.zookeepers) !== 'undefined') {
            $scope.zookeepers.push(serverZookeeper);
        }
        else if (typeof($scope.zookeepers) === 'undefined') {
            $scope.zookeepers = [serverZookeeper];
        }

        $scope.$apply();
    };

    $scope.getZookeepers = function (group) {
        $http.get('/zookeepers.json', {params: {group: group}}).
            success(function (data, status, headers, config) {
                $scope.zookeepers = angular.fromJson(data);
            });
    };

    $scope.createZookeeper = function (zookeeper) {
        $http.post('/zookeepers.json', { name: zookeeper.name, host: zookeeper.host, port: zookeeper.port});
    };

});

app.controller("TopicsController", function ($scope, $location, $http, topicService) {
    $http.get('/topics.json').
        success(function (data, status, headers, config) {
            $scope.topics = data
        });

    $scope.getTopic = function (topic) {
        $http.post('/topics.json/' + topic.name, {zookeeper: topic.zookeeper}).success(function (data, status, headers, config) {
            topicService.setTopic(data)
            $location.path('/topics/' + topic.name);
        });
    };
});

app.controller("TopicController", function ($scope, topicService) {
    var maxPartitionCount = 0;
    $scope.topic = topicService.getTopic();
    angular.forEach($scope.topic, function (consumer) {
        maxPartitionCount = consumer.offsets.length;

        if (maxPartitionCount < consumer.offsets.length) {
            maxPartitionCount = consumer.offsets.length;
        }
    });

    $scope.maxPartitionCount = new Array(maxPartitionCount)

    $scope.getTopicFeed = function (topic) {
        $location.path('/consumergroups/zookeeper/' + zookeeper + '/topic/' + topic);
    };
});

app.controller("BrokersController", function ($scope, $http) {
    $http.get('/brokers.json').
        success(function (data, status, headers, config) {
            $scope.brokers = data
        });
});