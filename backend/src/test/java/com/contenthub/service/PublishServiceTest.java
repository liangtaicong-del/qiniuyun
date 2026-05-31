package com.contenthub.service;

import com.contenthub.common.Constants;
import com.contenthub.dto.PublishRequest;
import com.contenthub.dto.PublishTaskResponse;
import com.contenthub.entity.Article;
import com.contenthub.entity.PlatformAccount;
import com.contenthub.entity.PublishTask;
import com.contenthub.entity.User;
import com.contenthub.exception.BadRequestException;
import com.contenthub.exception.ResourceNotFoundException;
import com.contenthub.platform.*;
import com.contenthub.repository.ArticleRepository;
import com.contenthub.repository.PlatformAccountRepository;
import com.contenthub.repository.PublishTaskRepository;
import com.contenthub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublishServiceTest {

    @Mock
    private PublishTaskRepository publishTaskRepository;
    @Mock
    private ArticleRepository articleRepository;
    @Mock
    private PlatformAccountRepository platformAccountRepository;
    @Mock
    private UserRepository userRepository;

    private PlatformPublisherRegistry publisherRegistry;
    private PublishService publishService;
    private User testUser;
    private Article testArticle;

    @BeforeEach
    void setUp() {
        publisherRegistry = new PlatformPublisherRegistry(List.of(new GenericPublisher()));
        publishService = new PublishService(
                publishTaskRepository, articleRepository, platformAccountRepository, userRepository, publisherRegistry);
        testUser = User.builder().id(1L).username("tester").build();
        testArticle = Article.builder().id(10L).title("Test Article").user(testUser).build();
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("发布文章")
    class PublishArticleTests {

        @Test
        @DisplayName("未绑定平台应返回失败状态")
        void publishArticle_unboundPlatform_returnsFailedStatus() {
            PublishRequest req = new PublishRequest();
            req.setArticleId(10L);
            req.setPlatforms(List.of("WECHAT"));

            when(articleRepository.findById(10L)).thenReturn(Optional.of(testArticle));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(platformAccountRepository.findByUserIdAndPlatform(eq(1L), any()))
                    .thenReturn(Optional.empty());
            when(publishTaskRepository.save(any(PublishTask.class))).thenAnswer(inv -> {
                PublishTask t = inv.getArgument(0);
                setField(t, "id", 100L);
                return t;
            });

            List<PublishTaskResponse> results = publishService.publishArticle(req, 1L);

            assertEquals(1, results.size());
            assertEquals("WECHAT", results.get(0).getPlatform());
            assertEquals("发布失败", results.get(0).getStatusName());
            assertNotNull(results.get(0).getErrorMsg());
        }

        @Test
        @DisplayName("不存在文章应抛异常")
        void publishArticle_articleNotFound_throwsException() {
            PublishRequest req = new PublishRequest();
            req.setArticleId(999L);
            req.setPlatforms(List.of("WECHAT"));

            when(articleRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> publishService.publishArticle(req, 1L));
        }

        @Test
        @DisplayName("非所有者应无法发布他人文章")
        void publishArticle_notOwner_throwsException() {
            User otherUser = User.builder().id(99L).username("other").build();
            Article otherArticle = Article.builder().id(20L).title("Other").user(otherUser).build();

            PublishRequest req = new PublishRequest();
            req.setArticleId(20L);
            req.setPlatforms(List.of("WECHAT"));

            when(articleRepository.findById(20L)).thenReturn(Optional.of(otherArticle));

            assertThrows(ResourceNotFoundException.class,
                    () -> publishService.publishArticle(req, 1L));
        }

        @Test
        @DisplayName("无效平台应被跳过")
        void publishArticle_invalidPlatform_skipped() {
            PublishRequest req = new PublishRequest();
            req.setArticleId(10L);
            req.setPlatforms(List.of("INVALID_PLATFORM", "WECHAT"));

            when(articleRepository.findById(10L)).thenReturn(Optional.of(testArticle));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(platformAccountRepository.findByUserIdAndPlatform(eq(1L), any()))
                    .thenReturn(Optional.empty());
            when(publishTaskRepository.save(any())).thenAnswer(inv -> {
                PublishTask t = inv.getArgument(0);
                setField(t, "id", System.nanoTime());
                return t;
            });

            List<PublishTaskResponse> results = publishService.publishArticle(req, 1L);

            assertEquals(1, results.size());
            assertEquals("WECHAT", results.get(0).getPlatform());
        }

        @Test
        @DisplayName("多平台应创建多个任务")
        void publishArticle_multiplePlatforms_createsMultipleTasks() {
            PublishRequest req = new PublishRequest();
            req.setArticleId(10L);
            req.setPlatforms(List.of("WECHAT", "WEIBO", "ZHIHU"));

            when(articleRepository.findById(10L)).thenReturn(Optional.of(testArticle));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(platformAccountRepository.findByUserIdAndPlatform(eq(1L), any()))
                    .thenReturn(Optional.empty());
            when(publishTaskRepository.save(any())).thenAnswer(inv -> {
                PublishTask t = inv.getArgument(0);
                setField(t, "id", System.nanoTime());
                return t;
            });

            List<PublishTaskResponse> results = publishService.publishArticle(req, 1L);

            assertEquals(3, results.size());
            verify(publishTaskRepository, atLeast(3)).save(any(PublishTask.class));
        }

        @Test
        @DisplayName("定时发布应设置PENDING状态")
        void publishArticle_withSchedule_setsPendingStatus() {
            PublishRequest req = new PublishRequest();
            req.setArticleId(10L);
            req.setPlatforms(List.of("WECHAT"));
            req.setScheduledAt(System.currentTimeMillis() + 3600000);

            when(articleRepository.findById(10L)).thenReturn(Optional.of(testArticle));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(platformAccountRepository.findByUserIdAndPlatform(eq(1L), any()))
                    .thenReturn(Optional.empty());
            when(publishTaskRepository.save(any())).thenAnswer(inv -> {
                PublishTask t = inv.getArgument(0);
                setField(t, "id", 1L);
                return t;
            });

            List<PublishTaskResponse> results = publishService.publishArticle(req, 1L);

            ArgumentCaptor<PublishTask> captor = ArgumentCaptor.forClass(PublishTask.class);
            verify(publishTaskRepository, atLeast(1)).save(captor.capture());
            assertEquals(PublishTask.TaskStatus.PENDING, captor.getValue().getStatus());
        }
    }

    @Nested
    @DisplayName("任务管理")
    class TaskManagementTests {

        @Test
        @DisplayName("应正确分页获取任务列表")
        void getTasks_withStatus_filtersCorrectly() {
            org.springframework.data.domain.Page<PublishTask> mockPage =
                    new org.springframework.data.domain.PageImpl<>(java.util.List.of());
            when(publishTaskRepository.findByUserIdAndStatus(eq(1L), eq(PublishTask.TaskStatus.SUCCESS), any()))
                    .thenReturn(mockPage);

            var result = publishService.getTasks(1L, 0, 10, "SUCCESS");

            assertNotNull(result);
            verify(publishTaskRepository).findByUserIdAndStatus(eq(1L), eq(PublishTask.TaskStatus.SUCCESS), any());
        }

        @Test
        @DisplayName("只能取消PENDING状态任务")
        void cancelTask_nonPending_throwsException() {
            PublishTask task = PublishTask.builder()
                    .id(1L).user(testUser).status(PublishTask.TaskStatus.SUCCESS)
                    .article(testArticle).platform(Constants.Platform.WECHAT).build();

            when(publishTaskRepository.findById(1L)).thenReturn(Optional.of(task));

            assertThrows(BadRequestException.class, () -> publishService.cancelTask(1L, 1L));
        }

        @Test
        @DisplayName("只能重试FAILED状态任务")
        void retryTask_nonFailed_throwsException() {
            PublishTask task = PublishTask.builder()
                    .id(1L).user(testUser).status(PublishTask.TaskStatus.SUCCESS)
                    .article(testArticle).platform(Constants.Platform.WECHAT).build();

            when(publishTaskRepository.findById(1L)).thenReturn(Optional.of(task));

            assertThrows(BadRequestException.class, () -> publishService.retryTask(1L, 1L));
        }
    }
}
