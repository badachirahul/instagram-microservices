package com.instagram.postservice.web;

import com.instagram.postservice.dto.PostResponse;
import com.instagram.postservice.security.AuthUser;
import com.instagram.postservice.security.CurrentUser;
import com.instagram.postservice.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Simple reverse-chronological feed — no ranking. */
@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

    private final PostService service;

    @GetMapping
    public List<PostResponse> feed(@CurrentUser AuthUser me) {
        return service.feed(me);
    }
}
