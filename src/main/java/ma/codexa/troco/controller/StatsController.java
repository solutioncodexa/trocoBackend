package ma.codexa.troco.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.dto.DashboardStatsDTO;
import ma.codexa.troco.dto.RevenueStatsDTO;
import ma.codexa.troco.service.StatsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
@Tag(name = "Stats", description = "Revenus et tableau de bord")
public class StatsController {

    private final StatsService statsService;

    @Operation(summary = "Statistiques de revenus")
    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<RevenueStatsDTO>> revenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(30);
        return ResponseEntity.ok(ApiResponse.success(statsService.getRevenue(start, end)));
    }

    @Operation(summary = "Agrégats tableau de bord admin")
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardStatsDTO>> dashboard() {
        return ResponseEntity.ok(ApiResponse.success(statsService.getDashboard()));
    }
}
