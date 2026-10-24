package com.aviral.transact.controller;

import com.aviral.transact.dto.*;
import com.aviral.transact.service.AnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private final AnalyticsService service;

    public AnalyticsController(AnalyticsService service) { this.service = service; }

    @GetMapping("/summary")
    public AnalyticsSummaryResponse summary() { return service.summary(); }

    @GetMapping("/daily-volume")
    public List<DailyVolumeResponse> dailyVolume(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return service.dailyVolume(startDate, endDate);
    }
}
