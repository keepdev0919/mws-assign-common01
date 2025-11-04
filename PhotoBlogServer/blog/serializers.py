from rest_framework import serializers
from django.contrib.auth.models import User
from .models import Post


class PostSerializer(serializers.ModelSerializer):
    # 로그인하지 않은 경우에도 업로드 가능하도록 author를 선택 입력으로 둔다
    author = serializers.PrimaryKeyRelatedField(
        queryset=User.objects.all(), required=False, allow_null=True, default=None
    )
    """Post 모델을 직렬화하여 API로 노출.

    이미지 필드 URL을 포함해 클라이언트가 바로 다운로드할 수 있도록 한다.
    """

    class Meta:
        model = Post
        fields = [
            "id",
            "author",
            "title",
            "text",
            "created_date",
            "published_date",
            "image",
            "created_at",
        ]

