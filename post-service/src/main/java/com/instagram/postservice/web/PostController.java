package com.instagram.postservice.web;

import com.instagram.postservice.dto.CommentRequest;
import com.instagram.postservice.dto.CommentResponse;
import com.instagram.postservice.dto.CreatePostRequest;
import com.instagram.postservice.dto.PostResponse;
import com.instagram.postservice.security.AuthUser;
import com.instagram.postservice.security.CurrentUser;
import com.instagram.postservice.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse create(@CurrentUser AuthUser me, @Valid @RequestBody CreatePostRequest req) {
        return service.create(me, req.imageUrl(), req.caption());
    }

    @GetMapping("/{id}")
    public PostResponse get(@CurrentUser AuthUser me, @PathVariable UUID id) {
        return service.get(id, me);
    }

    @GetMapping(params = "userId")
    public List<PostResponse> byUser(@CurrentUser AuthUser me, @RequestParam UUID userId) {
        return service.byAuthor(userId, me);
    }

    @PostMapping("/{id}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void like(@CurrentUser AuthUser me, @PathVariable UUID id) {
        service.like(id, me);
    }

    @DeleteMapping("/{id}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlike(@CurrentUser AuthUser me, @PathVariable UUID id) {
        service.unlike(id, me);
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse comment(@CurrentUser AuthUser me, @PathVariable UUID id,
                                   @Valid @RequestBody CommentRequest req) {
        return service.addComment(id, me, req.text());
    }

    @GetMapping("/{id}/comments")
    public List<CommentResponse> comments(@PathVariable UUID id) {
        return service.comments(id);
    }
}
