from django.contrib import admin
from .models import Post


@admin.register(Post)
class PostAdmin(admin.ModelAdmin):
    """관리자에서 Post 목록을 쉽게 확인하기 위한 설정."""

    list_display = ("id", "title", "created_at")

# Register your models here.
