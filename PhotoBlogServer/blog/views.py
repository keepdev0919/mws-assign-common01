from rest_framework import viewsets
from django.shortcuts import render
from .models import Post
from .serializers import PostSerializer


class BlogImages(viewsets.ModelViewSet):

    queryset = Post.objects.all()
    serializer_class = PostSerializer


def post_list(request):
    """브라우저에서 간단히 확인할 수 있는 게시글 목록 페이지를 렌더링한다.

    - 최신순으로 최대 100개까지 노출
    - 템플릿: `blog/index.html`
    """
    posts = Post.objects.order_by('-created_at')[:100]
    return render(request, 'blog/index.html', {"posts": posts})
