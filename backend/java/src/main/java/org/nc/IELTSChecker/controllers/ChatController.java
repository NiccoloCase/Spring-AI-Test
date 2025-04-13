package org.nc.IELTSChecker.controllers;

import org.nc.IELTSChecker.dto.EssayRequest;
import org.nc.IELTSChecker.dto.EvaluationResponse;
import org.nc.IELTSChecker.services.IeltsScoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

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
    public ResponseEntity<Object> scoreEssay(@Valid @RequestBody EssayRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            // Handle the validation errors
            Map<String, String> errors = new HashMap<>();
            for (FieldError fieldError : bindingResult.getFieldErrors()) {
                errors.put(fieldError.getField(), fieldError.getDefaultMessage());
            }
            return ResponseEntity.badRequest().body(errors);
        }

        try {
            EvaluationResponse evaluation = scoringService.scoreEssay(request);

            return ResponseEntity.ok(evaluation);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

}
