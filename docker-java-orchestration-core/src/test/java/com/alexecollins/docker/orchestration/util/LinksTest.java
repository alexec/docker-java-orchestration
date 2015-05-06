package com.alexecollins.docker.orchestration.util;

import com.alexecollins.docker.orchestration.model.Id;
import com.alexecollins.docker.orchestration.model.Link;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class LinksTest {

    @Test(expected = IllegalArgumentException.class)
    public void illegalNullNames() throws Exception {
        Links.name(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalEmptyNames() throws Exception {
        Links.name(new String[0]);
    }

    @Test
    public void bestName() throws Exception {
        assertEquals("/example-project_mysql", Links.name(new String[]{"/example-project_app/example-project_mysql", "/example-project_mysql", "/silly_kowalevski/example-project_mysql"}));
    }

    @Test
    public void links() throws Exception {
        assertEquals(Collections.singletonList(new Id("rcf")), Links.ids(Collections.singletonList(new Link("rcf:mrc"))));
    }
}