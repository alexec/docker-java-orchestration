package com.alexecollins.docker.orchestration.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LinkTest {

    @Test
    public void idNoAlias() throws Exception {
        Link link = new Link("oeu");
        assertEquals(new Id("oeu"), link.getId());
        assertEquals("oeu", link.getAlias());
    }

    @Test
    public void idWithAlias() throws Exception {
        Link link = new Link("eui:nth");
        assertEquals(new Id("eui"), link.getId());
        assertEquals("nth", link.getAlias());
    }
}