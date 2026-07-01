package com.instagram.postservice.service;

import com.instagram.postservice.domain.Comment;
import com.instagram.postservice.domain.Like;
import com.instagram.postservice.domain.Post;
import com.instagram.postservice.dto.CommentResponse;
import com.instagram.postservice.dto.PostResponse;
import com.instagram.postservice.event.PostEventPublisher;
import com.instagram.postservice.event.PostEvents;
import com.instagram.postservice.repo.CommentRepository;
import com.instagram.postservice.repo.LikeRepository;
import com.instagram.postservice.repo.PostRepository;
import com.instagram.postservice.security.AuthUser;
import com.instagram.postservice.web.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository posts;
    private final LikeRepository likes;
    private final CommentRepository comments;
    private final PostEventPublisher events;

    @Transactional
    public PostResponse create(AuthUser me, String imageUrl, String caption) {
        Post post = posts.save(new Post(me.id(), me.username(), caption, imageUrl));
        events.postCreated(PostEvents.PostCreated.from(post));
        return toResponse(post, me);
    }

    @Transactional(readOnly = true)
    public PostResponse get(UUID postId, AuthUser me) {
        return toResponse(require(postId), me);
    }

    @Transactional(readOnly = true)
    public List<PostResponse> byAuthor(UUID authorId, AuthUser me) {
        return posts.findByAuthorIdOrderByCreatedAtDesc(authorId).stream()
                .map(p -> toResponse(p, me))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PostResponse> feed(AuthUser me) {
        return posts.findAllByOrderByCreatedAtDesc().stream()
                .map(p -> toResponse(p, me))
                .toList();
    }

    @Transactional
    public void like(UUID postId, AuthUser me) {
        Post post = require(postId);
        if (likes.existsByPostIdAndUserId(postId, me.id())) {
            return; // idempotent — already liked, no duplicate event
        }
        likes.save(new Like(postId, me.id(), me.username()));
        events.postLiked(PostEvents.PostLiked.of(post, me.id(), me.username()));
    }

    @Transactional
    public void unlike(UUID postId, AuthUser me) {
        require(postId);
        likes.deleteByPostIdAndUserId(postId, me.id());
    }

    @Transactional
    public CommentResponse addComment(UUID postId, AuthUser me, String text) {
        Post post = require(postId);
        Comment comment = comments.save(new Comment(postId, me.id(), me.username(), text));
        events.commentAdded(PostEvents.CommentAdded.of(post, comment));
        return CommentResponse.of(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> comments(UUID postId) {
        require(postId);
        return comments.findByPostIdOrderByCreatedAtAsc(postId).stream()
                .map(CommentResponse::of)
                .toList();
    }

    private Post require(UUID postId) {
        return posts.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post " + postId + " not found"));
    }

    private PostResponse toResponse(Post post, AuthUser me) {
        return PostResponse.of(post,
                likes.countByPostId(post.getId()),
                comments.countByPostId(post.getId()),
                likes.existsByPostIdAndUserId(post.getId(), me.id()));
    }
}
