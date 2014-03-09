app.controller("BrokersController", function ($scope, $http) {
    $http.get('/brokers.json').success(function (data) {
        $scope.brokers = data;
    });
});