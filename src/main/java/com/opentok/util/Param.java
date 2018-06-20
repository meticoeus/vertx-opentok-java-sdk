package com.opentok.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Param {
    private final String name;
    private final String value;

    public Param(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Param param = (Param) o;
        return Objects.equals(name, param.name) &&
                Objects.equals(value, param.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }

    public static List<Param> fromMap(Map<String, List<String>> map) {
        if (map == null)
            return null;

        List<Param> params = new ArrayList<>(map.size());
        for (Map.Entry<String, List<String>> entries : map.entrySet()) {
            String name = entries.getKey();
            for (String value : entries.getValue())
                params.add(new Param(name, value));
        }

        return params;
    }
}
