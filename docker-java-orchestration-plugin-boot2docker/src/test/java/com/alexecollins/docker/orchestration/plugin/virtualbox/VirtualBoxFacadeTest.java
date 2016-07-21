package com.alexecollins.docker.orchestration.plugin.virtualbox;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VirtualBoxFacadeTest {

    private final CommandExecutor commandExecutor = mock(CommandExecutor.class);
    private final VirtualBoxFacade virtualBoxFacade = new VirtualBoxFacade(commandExecutor);

    @Test
    public void canParseVmNames() throws Exception {

        when(commandExecutor.exec(anyString())).thenReturn(
                "\"db2-972-exc-5_default_1433864805042_17400\" {e7fbeef6-f9f9-4c5a-a57a-9571c1fe7e67}" +
                        System.getProperty("line.separator") +
                        "\"default\" {90d48200-4395-4346-83af-8cbd537e7009}"
        );


        assertEquals(Arrays.asList("db2-972-exc-5_default_1433864805042_17400", "default"), virtualBoxFacade.getVmNames());

    }
}