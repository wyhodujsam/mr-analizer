package com.mranalizer.adapter.in.rest;

import com.mranalizer.adapter.in.rest.dto.MrBrowseResponse;
import com.mranalizer.application.dto.AnalysisRequestDto;
import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.port.in.BrowseMrUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/browse")
public class BrowseRestController {

    private final BrowseMrUseCase browseMrUseCase;

    public BrowseRestController(BrowseMrUseCase browseMrUseCase) {
        this.browseMrUseCase = browseMrUseCase;
    }

    @PostMapping
    public ResponseEntity<List<MrBrowseResponse>> browse(@RequestBody AnalysisRequestDto request) {
        List<MergeRequest> mergeRequests = browseMrUseCase.browse(request.toFetchCriteria());
        List<MrBrowseResponse> response = mergeRequests.stream()
                .map(MrBrowseResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
}
