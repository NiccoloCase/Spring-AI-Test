package org.nc.IELTSChecker.controllers;

import org.nc.IELTSChecker.dto.EssayRequest;
import org.nc.IELTSChecker.dto.EvaluationResponse;
import org.nc.IELTSChecker.services.IeltsScoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
@CrossOrigin
public class ChatController {

    private final IeltsScoringService scoringService;

    @Autowired
    public ChatController(IeltsScoringService scoringService) {
        this.scoringService = scoringService;
    }

    @PostMapping("/scoreEssay")
    public ResponseEntity<EvaluationResponse> scoreEssay(@RequestBody EssayRequest request) {
        try {
            System.out.println("Received request to score essay: " + request.essay());
            System.out.println("Received request to score question: " + request.question());

            EvaluationResponse evaluation = scoringService.scoreEssay(request);


            return ResponseEntity.ok(evaluation);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}
