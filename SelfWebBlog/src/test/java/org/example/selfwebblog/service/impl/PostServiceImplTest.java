package org.example.selfwebblog.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.example.selfwebblog.entity.Post;
import org.example.selfwebblog.mapper.PostMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PostServiceImplTest {

    @BeforeAll
    static void initializeTableMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "post-service-test"),
                Post.class);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void adminPageAppliesStatusKeywordPaginationAndArchivedOrderingInTheDatabase() {
        PostMapper mapper = mock(PostMapper.class);
        PostServiceImpl service = new PostServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ArgumentCaptor<LambdaQueryWrapper<Post>> wrapper = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        when(mapper.selectPage(any(Page.class), wrapper.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Page<Post> result = service.listForAdmin(" archived ", " needle ", 2, 10);

        assertThat(result.getCurrent()).isEqualTo(2);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(wrapper.getValue().getSqlSegment())
                .contains("status", "title", "summary", "content", "category", "tags", "archived_time");
        assertThat(wrapper.getValue().getParamNameValuePairs().values())
                .contains("ARCHIVED", "%needle%");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void publicListAndSearchBothRequirePublishedStatusInTheDatabase() {
        PostMapper mapper = mock(PostMapper.class);
        PostServiceImpl service = new PostServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ArgumentCaptor<LambdaQueryWrapper<Post>> wrappers = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        when(mapper.selectPage(any(Page.class), wrappers.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.listByPage(1, 10);
        service.search("needle", null, null, "date", 1, 10);

        assertThat(wrappers.getAllValues()).hasSize(2);
        assertThat(wrappers.getAllValues())
                .allSatisfy(wrapper -> {
                    assertThat(wrapper.getSqlSegment()).contains("status");
                    assertThat(wrapper.getParamNameValuePairs().values()).contains("PUBLISHED");
                });
    }
}
