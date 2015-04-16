package com.alexecollins.docker.orchestration;

import com.alexecollins.docker.orchestration.model.Conf;
import com.alexecollins.docker.orchestration.model.Id;

import java.util.ArrayList;
import java.util.List;

public class ExcludeFilter implements DefinitionFilter {
    private final List<Id> ids = new ArrayList<Id>();

    public ExcludeFilter(String... ids) {
        for (String id : ids) {
            this.ids.add(new Id(id));
        }
    }

    @Override
    public boolean test(Id id, Conf conf) {
        return ids.contains(id);
    }
}
