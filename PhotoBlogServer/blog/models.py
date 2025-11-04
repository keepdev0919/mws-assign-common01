from django.db import models
from django.contrib.auth.models import User

class Post(models.Model):
    title = models.CharField(max_length=200)
    text = models.TextField(blank=True)
    image = models.ImageField(upload_to="posts/")
    # 강의자료 화면과 매칭되는 필드들
    author = models.ForeignKey(
        User,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name="posts",
    )
    created_date = models.DateTimeField(null=True, blank=True)
    published_date = models.DateTimeField(null=True, blank=True)
    # 내부 용도로 유지되는 생성 시각(자동)
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self) -> str:
        return self.title
