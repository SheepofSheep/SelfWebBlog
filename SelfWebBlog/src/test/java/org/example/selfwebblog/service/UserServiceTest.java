package org.example.selfwebblog.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.example.selfwebblog.entity.User;
import org.example.selfwebblog.interaction.security.VisitorIdentityService;
import org.example.selfwebblog.mapper.UserMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.UncategorizedSQLException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class UserServiceTest {

    @BeforeAll
    static void initializeTableMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "user-service-test"),
                User.class);
    }

    @Test
    void githubLoginDoesNotFailOrLinkAccountsWhenALocalUsernameCollides() {
        User local = new User();
        local.setId(1L);
        local.setUsername("octocat");
        UserService service = spy(new UserService(mock(VisitorIdentityService.class)));
        doReturn(null).when(service).getByGithubId("42");
        doAnswer(invocation -> java.util.Set.of("octocat", "github_42").contains(invocation.getArgument(0)) ? local : null)
                .when(service).getByUsername(anyString());
        doAnswer(invocation -> {
            User created = invocation.getArgument(0);
            if (java.util.Set.of("octocat", "github_42").contains(created.getUsername())) {
                throw new DuplicateKeyException("username collision");
            }
            created.setId(2L);
            return true;
        }).when(service).save(any(User.class));

        User githubUser = service.findOrCreateGithubUser("42", "octocat", "https://example.test/avatar", null, null);

        assertThat(githubUser.getId()).isEqualTo(2L);
        assertThat(githubUser.getGithubId()).isEqualTo("42");
        assertThat(githubUser.getUsername()).isNotIn("octocat", "github_42");
        assertThat(local.getGithubId()).isNull();
    }

    @Test
    void concurrentGithubCallbacksReuseTheIdentityThatAlreadyWonTheInsert() {
        User winner = new User();
        winner.setId(9L);
        winner.setGithubId("42");
        UserService service = spy(new UserService(mock(VisitorIdentityService.class)));
        doReturn(null, winner).when(service).getByGithubId("42");
        doReturn(null).when(service).getByUsername(anyString());
        doThrow(new UncategorizedSQLException("insert", "user", new SQLException("unique conflict")))
                .when(service).save(any(User.class));

        assertThat(service.findOrCreateGithubUser("42", "octocat", null, null, null)).isSameAs(winner);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void adminPageAppliesIdentityRoleAndTitleFiltersWithStableOrdering() {
        UserMapper mapper = mock(UserMapper.class);
        UserService service = new UserService(mock(VisitorIdentityService.class));
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ArgumentCaptor<LambdaQueryWrapper<User>> wrapper = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        when(mapper.selectPage(any(Page.class), wrapper.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Page<User> result = service.listForAdmin("  visitor  ", " user ", " 贡献者 ", 2, 10);

        assertThat(result.getCurrent()).isEqualTo(2);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(wrapper.getValue().getSqlSegment())
                .contains("username", "nickname", "email", "title_name", "role", "create_time", "id");
        assertThat(wrapper.getValue().getParamNameValuePairs().values())
                .contains("%visitor%", "USER", "%贡献者%");
    }

    @Test
    void adminPageRejectsInvalidPaginationRoleAndOversizedFilters() {
        UserService service = new UserService(mock(VisitorIdentityService.class));

        assertThatThrownBy(() -> service.listForAdmin(null, null, null, 0, 20))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.listForAdmin(null, "OWNER", null, 1, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("角色");
        assertThatThrownBy(() -> service.listForAdmin("x".repeat(101), null, null, 1, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("关键词");
        assertThatThrownBy(() -> service.listForAdmin(null, null, "x".repeat(21), 1, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("称号");
    }
}
