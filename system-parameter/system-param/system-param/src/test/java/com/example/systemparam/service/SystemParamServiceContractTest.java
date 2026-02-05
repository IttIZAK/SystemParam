package com.example.systemparam.service;

import com.example.systemparam.domain.ParamDataType;
import com.example.systemparam.domain.SystemParamDto;
import com.example.systemparam.domain.TagGroupDto;
import com.example.systemparam.port.SystemParamRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class SystemParamServiceContractTest {

    static class InMemoryRepo implements SystemParamRepository {
        private final Map<String, SystemParamDto> params = new ConcurrentHashMap<>();
        private final Map<String, TagGroupDto> tags = new ConcurrentHashMap<>();

        @Override public SystemParamDto findByKey(String key) { return params.get(key); }
        @Override public List<SystemParamDto> findAllParams() { return new ArrayList<>(params.values()); }
        @Override public List<TagGroupDto> findAllTags() { return new ArrayList<>(tags.values()); }
        @Override public void save(SystemParamDto param) { params.put(param.getKey(), param); }
        @Override public void save(TagGroupDto tag) { tags.put(tag.getTagCode(), tag); }
    }

    @Test
    void can_use_through_interface() {
        SystemParamService svc = new SystemParams(new InMemoryRepo());
        svc.set("hello", "world", ParamDataType.TEXT, "SYSTEM", 1, "desc");
        assertEquals("world", svc.get("hello"));
    }
}
