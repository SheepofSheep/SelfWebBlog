package org.example.selfwebblog.content.tag;

import org.example.selfwebblog.entity.Tag;
import org.example.selfwebblog.mapper.TagMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagServiceSyncTest {
    @Mock
    private TagMapper tagMapper;
    @Mock
    private PostTagMapper postTagMapper;

    @Test
    void replacingRelationsAlsoRefreshesTheCompatibilityText() {
        Tag tag = new Tag();
        tag.setId(3L);
        tag.setName("Vue");
        when(postTagMapper.findByName("Vue")).thenReturn(tag);

        new TagService(tagMapper, postTagMapper).replacePostTags(7L, "Vue");

        verify(postTagMapper).deleteRelationsForPost(7L);
        verify(postTagMapper).insertRelation(7L, 3L);
        verify(postTagMapper).syncPostTagText(7L);
    }
}
