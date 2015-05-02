package com.alexecollins.docker.orchestration.model;


import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Packaging {
    private List<Item> add = new ArrayList<>();
}
