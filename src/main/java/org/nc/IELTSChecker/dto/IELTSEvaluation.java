package org.nc.IELTSChecker.dto;

import com.fasterxml.jackson.databind.ObjectMapper;

public class IELTSEvaluation {
    public int taskResponse;
    public int coherenceCohesion;
    public int lexicalResource;
    public int grammaticalRangeAccuracy;
    public int overall;
    public String comment;

    public static IELTSEvaluation fromJson(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, IELTSEvaluation.class);
    }
}
