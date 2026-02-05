package com.example.systemparam.converter;

import com.example.systemparam.domain.ParamDataType;
import com.example.systemparam.domain.SystemParamDto;
import com.example.systemparam.exception.ParamTypeMismatchException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ParamTypeConverterTest {

    private static SystemParamDto dto(String key, String value, ParamDataType type) {
        SystemParamDto d = new SystemParamDto();
        d.setKey(key);
        d.setValue(value);
        d.setType(type);
        return d;
    }

    @Test
    void number_conversions() {
        SystemParamDto d = dto("n", "42", ParamDataType.NUMBER);
        assertEquals(42, ParamTypeConverter.convert(d, Integer.class));
        assertEquals(42L, ParamTypeConverter.convert(d, Long.class));
        assertEquals(42.0d, ParamTypeConverter.convert(d, Double.class));
        assertEquals(new BigDecimal("42"), ParamTypeConverter.convert(d, BigDecimal.class));
    }

    @Test
    void number_trim_and_invalid() {
        assertEquals(7, ParamTypeConverter.convert(dto("n", "  7  ", ParamDataType.NUMBER), Integer.class));
        assertThrows(ParamTypeMismatchException.class,
                () -> ParamTypeConverter.convert(dto("n", "7.5", ParamDataType.NUMBER), Integer.class));
        assertThrows(ParamTypeMismatchException.class,
                () -> ParamTypeConverter.convert(dto("n", "abc", ParamDataType.NUMBER), Integer.class));
    }

    @Test
    void boolean_conversions() {
        assertTrue(ParamTypeConverter.convert(dto("b", "true", ParamDataType.BOOLEAN), Boolean.class));
        assertTrue(ParamTypeConverter.convert(dto("b", "1", ParamDataType.BOOLEAN), Boolean.class));
        assertFalse(ParamTypeConverter.convert(dto("b", "false", ParamDataType.BOOLEAN), Boolean.class));
        assertFalse(ParamTypeConverter.convert(dto("b", "0", ParamDataType.BOOLEAN), Boolean.class));
    }

    @Test
    void boolean_invalid_throws() {
        assertThrows(ParamTypeMismatchException.class,
                () -> ParamTypeConverter.convert(dto("b", "yes", ParamDataType.BOOLEAN), Boolean.class));
    }

    @Test
    void duration_conversions_iso_and_short_forms() {
        assertEquals(Duration.ofMinutes(15),
                ParamTypeConverter.convert(dto("t", "PT15M", ParamDataType.TEXT), Duration.class));

        assertEquals(Duration.ofSeconds(5),
                ParamTypeConverter.convert(dto("t", "5s", ParamDataType.TEXT), Duration.class));

        assertEquals(Duration.ofMinutes(2),
                ParamTypeConverter.convert(dto("t", "2m", ParamDataType.TEXT), Duration.class));

        assertEquals(Duration.ofMillis(200),
                ParamTypeConverter.convert(dto("t", "200ms", ParamDataType.TEXT), Duration.class));
    }

    @Test
    void duration_invalid_throws() {
        assertThrows(ParamTypeMismatchException.class,
                () -> ParamTypeConverter.convert(dto("t", "abc", ParamDataType.TEXT), Duration.class));
    }

    enum Color { RED, GREEN }

    @Test
    void enum_is_case_insensitive() {
        assertEquals(Color.GREEN,
                ParamTypeConverter.convertEnum(dto("c", "green", ParamDataType.TEXT), Color.class));
        assertEquals(Color.RED,
                ParamTypeConverter.convertEnum(dto("c", "ReD", ParamDataType.TEXT), Color.class));
    }

    @Test
    void enum_invalid_throws() {
        assertThrows(ParamTypeMismatchException.class,
                () -> ParamTypeConverter.convertEnum(dto("c", "blue", ParamDataType.TEXT), Color.class));
    }

    @Test
    void list_parsing_comma_separated() {
        SystemParamDto d = dto("list", "a,b, c", ParamDataType.TEXT);
        List<String> out = ParamTypeConverter.convertList(d, String.class);
        assertEquals(List.of("a", "b", "c"), out);
    }

    @Test
    void json_toMap_ok_and_invalid_throws() {
        Map<String, Object> m = ParamTypeConverter.toMap("k", "{\"a\":1,\"b\":\"x\"}");
        assertEquals(1, ((Number) m.get("a")).intValue());
        assertEquals("x", m.get("b"));

        assertThrows(ParamTypeMismatchException.class,
                () -> ParamTypeConverter.toMap("k", "{not-json"));
    }

    @Test
    void json_toMapList_ok_and_invalid_throws() {
        List<Map<String, Object>> arr = ParamTypeConverter.toMapList("k", "[{\"a\":1},{\"a\":2}]");
        assertEquals(2, arr.size());
        assertEquals(1, ((Number) arr.get(0).get("a")).intValue());

        assertThrows(ParamTypeMismatchException.class,
                () -> ParamTypeConverter.toMapList("k", "{not-json"));
    }

    @Test
    void json_list_typed_parsing() {
        SystemParamDto d = dto("nums", "[1,2,3]", ParamDataType.JSON);
        List<Integer> out = ParamTypeConverter.convertList(d, Integer.class);
        assertEquals(List.of(1, 2, 3), out);
    }

    @Test
    void mismatch_throws() {
        SystemParamDto d = dto("n", "not-a-number", ParamDataType.NUMBER);
        assertThrows(ParamTypeMismatchException.class, () -> ParamTypeConverter.convert(d, Integer.class));
    }
}
