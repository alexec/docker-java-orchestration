package com.alexecollins.docker.orchestration.model;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("CanBeFinal")
public class Packaging {
    @JsonProperty
    private List<Item> add = new ArrayList<Item>();

    public List<Item> getAdd() {
        return add;
    }
}
