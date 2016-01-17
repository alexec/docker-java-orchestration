package com.alexecollins.docker.orchestration;

import com.alexecollins.docker.orchestration.model.Conf;
import com.alexecollins.docker.orchestration.model.Id;
import com.alexecollins.docker.orchestration.model.Packaging;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;


@RunWith(Parameterized.class)
public class RepoTest {

    private static final String PROJECT_VERSION = "1.0";
    private final Id appId = new Id("app");
    private final Id filterId = new Id("filter");
    private final Repo sut;

    public RepoTest(String child) {
        Properties properties = new Properties();
        properties.setProperty("project.version", PROJECT_VERSION);
        sut = new Repo("test", "test", new File("src/test", child), properties);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"docker-repo-v1"},
                {"docker-repo-v2"},
        });
    }

    @Test
    public void explicitStartOrder() throws Exception {
        assertEquals(Arrays.asList(filterId, appId), sut.ids(false));
    }

    @Test
    public void testSingleDependencies() throws Exception {
        final Map<Id, List<Id>> links = new HashMap<>();
        final Id a = new Id("a"), b = new Id("b");
        links.put(b, Collections.singletonList(a));
        links.put(a, Collections.<Id>emptyList());
        final ArrayList<Id> expected = new ArrayList<>();
        expected.add(a);
        expected.add(b);
        assertEquals(
                expected,
                sut.sort(links));
    }

    @Test
    public void testDoubleDependencies() throws Exception {
        final Map<Id, List<Id>> links = new HashMap<>();
        final Id a = new Id("a"), b = new Id("b"), c = new Id("c");
        links.put(c, Collections.singletonList(b));
        links.put(b, Collections.singletonList(a));
        links.put(a, Collections.<Id>emptyList());
        final ArrayList<Id> expected = new ArrayList<>();
        expected.add(a);
        expected.add(b);
        expected.add(c);
        assertEquals(
                expected,
                sut.sort(links));
    }

    @Test(expected = IllegalStateException.class)
    public void testCircularDependencies() throws Exception {
        final Map<Id, List<Id>> links = new HashMap<>();
        final Id a = new Id("a"), b = new Id("b"), c = new Id("c"), d = new Id("d"), e = new Id("e");
        links.put(c, Collections.singletonList(b));
        links.put(b, Collections.singletonList(a));
        links.put(a, Collections.singletonList(c));
        links.put(d, Collections.singletonList(e));
        links.put(e, Collections.<Id>emptyList());
        sut.sort(links);
    }

    @Test(expected = IllegalStateException.class)
    public void testSelfCircularDependencies() throws Exception {
        final Map<Id, List<Id>> links = new HashMap<>();
        final Id a = new Id("a");
        links.put(a, Collections.singletonList(a));
        sut.sort(links);
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingDependencies() throws Exception {
        final Map<Id, List<Id>> links = new HashMap<>();
        final Id a = new Id("a");
        final Id b = new Id("b");
        links.put(a, Collections.singletonList(b));
        sut.sort(links);
    }

    @Test
    public void appHasPacking() throws Exception {
        Conf conf = sut.conf(appId);
        Packaging packaging = conf.getPackaging();
        assertNotNull(packaging.getAdd().get(0));
    }

    @Test
    public void testPropertiesReplaced() throws Exception {

        Packaging packaging = sut.conf(appId).getPackaging();
        assertEquals("example-" + PROJECT_VERSION + ".jar", packaging.getAdd().get(0).getPath());
    }

    @Test
    public void filesAreNotIncludedInIds() throws Exception {
        List<Id> identifiers = sut.ids(false);
        assertEquals(identifiers.size(), 2);
        assertThat(identifiers, hasItems(appId, filterId));
    }

    @Test(expected = IllegalStateException.class)
    public void testDuplicateLinksSimple() throws Exception {
        final Map<Id, List<Id>> links = new HashMap<>();
        final Id a = new Id("a");
        final Id b1 = new Id("b"), b2 = new Id("b");
        links.put(a, Arrays.asList(b1, b2));
        sut.sort(links);
    }

    @Test(expected = IllegalStateException.class)
    public void testDuplicateLinksWithAlias() throws Exception {
        final Map<Id, List<Id>> links = new HashMap<>();
        final Id a = new Id("a");
        final Id b1 = new Id("foo:b"), b2 = new Id("bar:b");
        links.put(a, Arrays.asList(b1,b2));
        sut.sort(links);
    }

    @Test(expected = IllegalStateException.class)
    public void testDuplicateLinksMixed() throws Exception {
        final Map<Id, List<Id>> links = new HashMap<>();
        final Id a = new Id("a");
        final Id b1 = new Id("foo:b"), b2 = new Id("b");
        links.put(a, Arrays.asList(b1,b2));
        sut.sort(links);
    }
}
