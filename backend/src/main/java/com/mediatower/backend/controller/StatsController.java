package com.mediatower.backend.controller;

import com.mediatower.backend.dto.AdminUserDto;
import com.mediatower.backend.dto.DashboardSummaryDto;
import com.mediatower.backend.dto.UserStatsDto;
import com.mediatower.backend.security.FirebaseUser;
//import com.mediatower.backend.service.PresenceService;
import com.mediatower.backend.service.StatsService;
import com.mediatower.backend.service.UserService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private static final int CACHE_DURATION = 5; // seconds

    private final StatsService statsService;
//    private final PresenceService presenceService;
    private final UserService userService;

    public StatsController(StatsService statsService,  UserService userService) {
        this.statsService = statsService;

        this.userService = userService;
    }

    @GetMapping("/dashboard-summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DashboardSummaryDto> getDashboardSummary() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(CACHE_DURATION, TimeUnit.SECONDS))
                .body(statsService.getDashboardSummary());
    }

//    @GetMapping("/support/online-agents")
//    public ResponseEntity<Collection<AdminUserDto>> getOnlineAgents() {
//        List<AdminUserDto> onlineAdmins = presenceService.getOnlineAdminUids().stream()
//                .flatMap(uid -> userService.findUserByUid(uid).stream())
//                .map(userService::convertToAdminDto)
//                .collect(Collectors.toList());
//
//        return ResponseEntity.ok()
//                .cacheControl(CacheControl.noCache())
//                .body(onlineAdmins);
//    }

//    @GetMapping("/support/status")
//    public ResponseEntity<Map<String, Boolean>> getSupportStatus() {
//        boolean areAdminsAvailable = !presenceService.getOnlineAdminUids().isEmpty();
//        return ResponseEntity.ok()
//                .cacheControl(CacheControl.noCache())
//                .body(Collections.singletonMap("agentsAvailable", areAdminsAvailable));
//    }

    @GetMapping("/my-stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserStatsDto> getMyStats(@AuthenticationPrincipal FirebaseUser user) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(CACHE_DURATION, TimeUnit.SECONDS))
                .body(statsService.getUserStats(user.getUid()));
    }
}