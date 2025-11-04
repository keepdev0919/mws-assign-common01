from django.urls import path, include
from rest_framework.routers import DefaultRouter
from .views import BlogImages, post_list


router = DefaultRouter()
router.register('Post', BlogImages, basename='post')

urlpatterns = [
    path('', include(router.urls)),  # ''로 두면 /api_root/ 아래에 포함될 수 있음
    path('page/', post_list, name='post_list'),  # 브라우저 출력용 간단 페이지
]


