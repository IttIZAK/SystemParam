package com.example.systemparam.port;

import com.example.systemparam.domain.SystemParamDto;
import com.example.systemparam.domain.TagGroupDto;

import java.util.List;

public interface SystemParamRepository {
    SystemParamDto findByKey(String key);
    List<SystemParamDto> findAllParams();
    List<TagGroupDto> findAllTags();
    void save(SystemParamDto param);
    void save(TagGroupDto tag);
}
