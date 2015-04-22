package com.alexecollins.docker.orchestration;

import com.alexecollins.docker.orchestration.model.Conf;
import com.alexecollins.docker.orchestration.model.Id;

public interface DefinitionFilter {

    DefinitionFilter ANY = new DefinitionFilter() {
        @Override
        public boolean test(Id id, Conf conf) {
            return true;
        }
    };

    /**
     * @param id   Not null.
     * @param conf Not null.
     * @return If we should include in actions.
     */
    boolean test(Id id, Conf conf);
}
