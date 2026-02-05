package com.example.systemparam.service;

import com.example.systemparam.domain.ParamDataType;
import com.example.systemparam.domain.SystemParamDto;
import com.example.systemparam.domain.TagGroupDto;
import com.example.systemparam.domain.TagGroupViewDto;
import com.example.systemparam.exception.ParamNotFoundException;
import com.example.systemparam.port.SystemParamRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class SystemParamsTest {

    static class InMemoryRepo implements SystemParamRepository {
        private final Map<String, SystemParamDto> params = new ConcurrentHashMap<>();
        private final Map<String, TagGroupDto> tags = new ConcurrentHashMap<>();

        @Override
        public SystemParamDto findByKey(String key) {
            return params.get(key);
        }

        @Override
        public List<SystemParamDto> findAllParams() {
            return new ArrayList<>(params.values());
        }

        @Override
        public List<TagGroupDto> findAllTags() {
            return new ArrayList<>(tags.values());
        }

        @Override
        public void save(SystemParamDto param) {
            assertNotNull(param);
            assertNotNull(param.getKey());
            params.put(param.getKey(), cloneParam(param));
        }

        @Override
        public void save(TagGroupDto tag) {
            assertNotNull(tag);
            assertNotNull(tag.getTagCode());
            tags.put(tag.getTagCode(), cloneTag(tag));
        }

        private static SystemParamDto cloneParam(SystemParamDto p) {
            SystemParamDto c = new SystemParamDto();
            c.setKey(p.getKey());
            c.setValue(p.getValue());
            c.setDescription(p.getDescription());
            c.setType(p.getType());
            c.setTagCode(p.getTagCode());
            c.setDisplayPriority(p.getDisplayPriority());
            return c;
        }

        private static TagGroupDto cloneTag(TagGroupDto t) {
            TagGroupDto c = new TagGroupDto();
            c.setTagCode(t.getTagCode());
            c.setTagName(t.getTagName());
            c.setDescription(t.getDescription());
            c.setPriority(t.getPriority());
            return c;
        }
    }

    static class CountingRepo extends InMemoryRepo {
        int saveParamCalls = 0;
        int saveTagCalls = 0;

        @Override
        public void save(SystemParamDto param) {
            saveParamCalls++;
            super.save(param);
        }

        @Override
        public void save(TagGroupDto tag) {
            saveTagCalls++;
            super.save(tag);
        }
    }

    @Test
    void get_missing_throws() {
        SystemParams sp = new SystemParams(new InMemoryRepo());
        assertThrows(ParamNotFoundException.class, () -> sp.get("missing"));
    }

    @Test
    void set_then_get_ok() {
        InMemoryRepo repo = new InMemoryRepo();
        SystemParams sp = new SystemParams(repo);

        sp.set("max_retry", "5", ParamDataType.NUMBER, "SYSTEM", 10, "Maximum retry");
        assertEquals("5", sp.get("max_retry"));
        assertEquals(5, sp.getAs("max_retry", Integer.class));
    }

    @Test
    void update_changes_value_only() {
        InMemoryRepo repo = new InMemoryRepo();
        SystemParams sp = new SystemParams(repo);

        sp.set("flag", "true", ParamDataType.BOOLEAN, "SYSTEM", 1, "a flag");
        sp.update("flag", "false");

        assertEquals("false", sp.get("flag"));
        SystemParamDto stored = repo.findByKey("flag");
        assertEquals("a flag", stored.getDescription(), "update should not wipe metadata");
        assertEquals("SYSTEM", stored.getTagCode());
        assertEquals(1, stored.getDisplayPriority());
        assertEquals(ParamDataType.BOOLEAN, stored.getType());
    }

    @Test
    void update_missing_throws() {
        SystemParams sp = new SystemParams(new InMemoryRepo());
        assertThrows(ParamNotFoundException.class, () -> sp.update("missing", "x"));
    }

    @Test
    void update_blank_key_throws() {
        SystemParams sp = new SystemParams(new InMemoryRepo());
        assertThrows(IllegalArgumentException.class, () -> sp.update("   ", "x"));
    }

    @Test
    void getDuration_parses_ok() {
        InMemoryRepo repo = new InMemoryRepo();
        SystemParams sp = new SystemParams(repo);

        sp.set("timeout", "PT15M", ParamDataType.TEXT, "SYSTEM", 1, "timeout");
        assertEquals(Duration.ofMinutes(15), sp.getDuration("timeout"));
    }

    @Test
    void ensureParam_does_not_override_existing_value_but_updates_metadata() {
        InMemoryRepo repo = new InMemoryRepo();
        SystemParams sp = new SystemParams(repo);

        sp.set("max_retry", "5", ParamDataType.NUMBER, "SYSTEM", 1, "old desc");

        sp.ensureParam("max_retry", "999", "new desc", ParamDataType.NUMBER, "SYSTEM2", 7);

        SystemParamDto stored = repo.findByKey("max_retry");
        assertNotNull(stored);

        assertEquals("5", stored.getValue(), "value must not be overridden");
        assertEquals("new desc", stored.getDescription());
        assertEquals("SYSTEM2", stored.getTagCode());
        assertEquals(7, stored.getDisplayPriority());
    }

    @Test
    void ensureParam_creates_when_missing() {
        InMemoryRepo repo = new InMemoryRepo();
        SystemParams sp = new SystemParams(repo);

        sp.ensureParam("k", "v", "desc", ParamDataType.TEXT, "A", 3);

        SystemParamDto stored = repo.findByKey("k");
        assertNotNull(stored);
        assertEquals("v", stored.getValue());
        assertEquals("desc", stored.getDescription());
        assertEquals(ParamDataType.TEXT, stored.getType());
        assertEquals("A", stored.getTagCode());
        assertEquals(3, stored.getDisplayPriority());
    }

    @Test
    void ensureParam_blank_tag_becomes_ungrouped() {
        InMemoryRepo repo = new InMemoryRepo();
        SystemParams sp = new SystemParams(repo);

        sp.ensureParam("k", "v", null, ParamDataType.TEXT, "   ", null);

        SystemParamDto stored = repo.findByKey("k");
        assertNotNull(stored);
        assertEquals("UNGROUPED", stored.getTagCode());
    }

    @Test
    void ensureParam_noop_does_not_save() {
        CountingRepo repo = new CountingRepo();
        SystemParams sp = new SystemParams(repo);

        sp.set("k", "v", ParamDataType.TEXT, "A", 1, "d");
        int before = repo.saveParamCalls;

        sp.ensureParam("k", "SHOULD_NOT_OVERRIDE", "d", ParamDataType.TEXT, "A", 1);

        assertEquals(before, repo.saveParamCalls, "should not call save when nothing changed");
        assertEquals("v", sp.get("k"), "ensureParam must not override value");
    }

    @Test
    void ensureTag_inserts_or_updates_metadata() {
        InMemoryRepo repo = new InMemoryRepo();
        SystemParams sp = new SystemParams(repo);

        sp.ensureTag("SYSTEM", "System Config", "desc1", 1);
        assertEquals(1, repo.findAllTags().size());

        sp.ensureTag("SYSTEM", "System Config v2", "desc2", 9);
        assertEquals(1, repo.findAllTags().size());

        TagGroupDto tag = repo.findAllTags().get(0);
        assertEquals("SYSTEM", tag.getTagCode());
        assertEquals("System Config v2", tag.getTagName());
        assertEquals("desc2", tag.getDescription());
        assertEquals(9, tag.getPriority());
    }

    @Test
    void getMapOrDefault_returns_default_when_missing_or_invalid_json() {
        InMemoryRepo repo = new InMemoryRepo();
        SystemParams sp = new SystemParams(repo);

        Map<String, Object> def = Map.of("x", 1);

        assertEquals(def, sp.getMapOrDefault("missing", def), "missing key should return default");

        sp.set("bad", "{not-json", ParamDataType.JSON, "A", 1, null);
        assertEquals(def, sp.getMapOrDefault("bad", def), "invalid JSON should return default");
    }

    @Test
    void getMapListOrDefault_returns_default_when_invalid_json() {
        InMemoryRepo repo = new InMemoryRepo();
        SystemParams sp = new SystemParams(repo);

        List<Map<String, Object>> def = List.of(Map.of("x", 1));

        sp.set("badArr", "{not-json", ParamDataType.JSON, "A", 1, null);
        assertEquals(def, sp.getMapListOrDefault("badArr", def));
    }

    @Test
    void grouping_by_tag_creates_ungrouped_and_sorts_params() {
        InMemoryRepo repo = new InMemoryRepo();
        SystemParams sp = new SystemParams(repo);

        sp.ensureTag("A", "Group A", null, 1);
        sp.ensureTag("B", "Group B", null, 2);

        sp.set("k2", "v", ParamDataType.TEXT, "A", 20, null);
        sp.set("k1", "v", ParamDataType.TEXT, "A", 10, null);
        sp.set("k3", "v", ParamDataType.TEXT, "", 1, null); // UNGROUPED

        List<TagGroupViewDto> groups = sp.getAllGroupedByTag();
        assertNotNull(groups);
        assertTrue(groups.size() >= 2);

        TagGroupViewDto groupA = groups.stream()
                .filter(g -> "A".equals(g.getTagCode()))
                .findFirst().orElseThrow();

        assertEquals(2, groupA.getParams().size());
        assertEquals("k1", groupA.getParams().get(0).getKey());
        assertEquals("k2", groupA.getParams().get(1).getKey());

        TagGroupViewDto ungrouped = groups.stream()
                .filter(g -> "UNGROUPED".equals(g.getTagCode()))
                .findFirst().orElseThrow();

        assertEquals(1, ungrouped.getParams().size());
        assertEquals("k3", ungrouped.getParams().get(0).getKey());
    }

    @Test
    void getByTag_returns_group_when_metadata_exists_even_if_no_params() {
        InMemoryRepo repo = new InMemoryRepo();
        SystemParams sp = new SystemParams(repo);

        sp.ensureTag("A", "Group A", "desc", 1);

        TagGroupViewDto g = sp.getByTag("A");
        assertNotNull(g);
        assertEquals("A", g.getTagCode());
        assertEquals("Group A", g.getTagName());
        assertEquals(0, g.getParams().size());
    }

    @Test
    void getByTag_returns_group_when_params_exist_even_without_metadata() {
        InMemoryRepo repo = new InMemoryRepo();
        SystemParams sp = new SystemParams(repo);

        sp.set("k", "v", ParamDataType.TEXT, "X", 5, null);

        TagGroupViewDto g = sp.getByTag("X");
        assertNotNull(g);
        assertEquals("X", g.getTagCode());
        assertEquals(1, g.getParams().size());
        assertEquals("k", g.getParams().get(0).getKey());
    }

    @Test
    void getByTag_returns_null_when_no_metadata_and_no_params() {
        SystemParams sp = new SystemParams(new InMemoryRepo());
        assertNull(sp.getByTag("NOPE"));
    }

    @Test
    void service_interface_polymorphism() {
        SystemParamService service = new SystemParams(new InMemoryRepo());
        assertNotNull(service);
    }
}
