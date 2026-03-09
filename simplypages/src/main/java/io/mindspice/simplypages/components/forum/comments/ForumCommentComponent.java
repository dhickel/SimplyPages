package io.mindspice.simplypages.components.forum.comments;

import io.mindspice.simplypages.core.Component;

import java.util.List;

/**
 * Final comment component contract populated by the comment renderer.
 */
public interface ForumCommentComponent extends Component {
    ForumCommentComponent withCommentId(String id);
    ForumCommentComponent withTopicId(String topicId);
    ForumCommentComponent withParentId(String parentId);
    ForumCommentComponent withDepth(int depth);
    ForumCommentComponent withAuthor(String author);
    ForumCommentComponent withAvatarUrl(String avatarUrl);
    ForumCommentComponent withTimestamp(String timestamp);
    ForumCommentComponent withBody(Component body);
    ForumCommentComponent withActions(List<Component> actions);
    ForumCommentComponent withLikes(Integer likes);
    ForumCommentComponent withReplies(Integer replies);
}
