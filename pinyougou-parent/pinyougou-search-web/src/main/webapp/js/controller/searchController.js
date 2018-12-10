app.controller('searchController', function($scope, searchService) {

    $scope.searchMap = {'keywords':'','category':'', 'brand':'', 'spec':{}, 'price':'', 'pageNo':1, 'pageSize':20, 'sort':'1', 'sortField':''};
    $scope.search=function() {

        searchService.search( $scope.searchMap).success(function (response) {

            $scope.resultMap = response;
            console.log($scope.resultMap);
            console.log(JSON.stringify($scope.resultMap));
            console.log('------------');
            console.log(JSON.stringify($scope.searchMap));

            //根据传入参数及搜索结果构建分页标签
            $scope.buildPageLable();
        });
    }

    $scope.addSearchItem=function(key, value) {

        if('category'==key||'brand'==key||'price' == key||'pageNo'==key) {
            if('pageNo'==key) {
                value = parseInt(value);
            }
            $scope.searchMap[key]=value;
        }else {
            $scope.searchMap.spec[key]=value;
        }

        //点击添加搜索选项的时候提交查询
        $scope.search();
    }

    $scope.removeSearchItem=function(key) {

        if('category'==key||'brand'==key||'price' == key) {
            $scope.searchMap[key]='';
        }else {
            delete $scope.searchMap.spec[key];
        }

        //点击撤销搜索选项的时候提交查询
        $scope.search();
    }

    $scope.buildPageLable=function () {
        $scope.pageLable = [];

        $scope.showStartDot = false;
        $scope.showEndDot = false;

        var totalPages = $scope.resultMap.totalPages;
        var start = 1;
        var end = totalPages;

        //显示五个分页标签
        if(totalPages>5) {
            //显示前面5页
            if($scope.searchMap.pageNo<=3) {
                start=1;
                end = 5;
                $scope.showEndDot = true;

            }else if($scope.searchMap.pageNo>=totalPages-2) {   //显示最后5页
                start = totalPages - 4;
                end = totalPages;
                $scope.showStartDot = true;

            } else {   //显示中间的5页
                start = $scope.searchMap.pageNo - 2;
                end = $scope.searchMap.pageNo + 2;
                $scope.showStartDot = true;
                $scope.showEndDot = true;
            }
        }

        for(var i=start;i<=end;i++) {
            $scope.pageLable.push(i);
        }
    }

    //判断当前是否已经到了第一页
    $scope.isTopPage=function () {
        return $scope.searchMap.pageNo<=1;
    }
    
    //判断当前是否到了最后一页
    $scope.isEndPage=function () {
        return $scope.searchMap.pageNo==$scope.searchMap.pageSize;
    }

    $scope.sort=function (sortField, sort) {
        $scope.searchMap.sortField= sortField;
        $scope.searchMap.sort = sort;
        $scope.search();
    }

    //如果输入的关键字包含品牌讲品牌面板隐藏
    $scope.keywordsContainBrand=function () {
        var brandList = $scope.resultMap.brandList;
        for(var i=0;i<brandList.length;i++) {
            if($scope.searchMap.keywords.indexOf(brandList[i].text)>=0) {
                return true;
            }
        }
        return false;
    }



});