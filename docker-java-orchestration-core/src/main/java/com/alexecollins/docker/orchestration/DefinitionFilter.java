package com.alexecollins.docker.orchestration;

import com.alexecollins.docker.orchestration.model.Conf;

public interface DefinitionFilter {

    DefinitionFilter ANY = new DefinitionFilter() {
        @Override
        public boolean test(Conf conf) {
            return true;
        }
    };

    /**
     * @param conf Not null.
     * @return If we should include in actions.
     */
    boolean test(Conf conf);
}
