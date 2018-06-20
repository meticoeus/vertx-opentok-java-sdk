package com.opentok.util;

import java.util.List;
import java.util.Map;

public class RequestUtils {
    public static String buildBodyFromParams(Map<String, List<String>> map) {
        List<Param> formParams = Param.fromMap(map);

        if (formParams == null || formParams.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\tformParams:");
        for (Param param : formParams) {
            sb.append("\t");
            sb.append(param.getName());
            sb.append(":");
            sb.append(param.getValue());
        }

        return sb.toString();
    }
}
