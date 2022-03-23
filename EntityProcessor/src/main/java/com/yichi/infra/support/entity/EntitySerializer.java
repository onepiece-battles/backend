package com.yichi.infra.support.entity;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class EntitySerializer {
  private static EntitySerializer instance = new EntitySerializer();
  private final ObjectMapper objectMapper;

  private EntitySerializer() {
    SimpleModule module = new SimpleModule("DefaultEntityModule", Version.unknownVersion());
    module.setAbstractTypes(new EntityTypeResolver());
    this.objectMapper = new ObjectMapper();
    this.objectMapper.registerModule(module);
    this.objectMapper.setSerializationInclusion(Include.NON_NULL);
    this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    this.objectMapper.configure(MapperFeature.USE_BASE_TYPE_AS_DEFAULT_IMPL, true);
  }

  public static EntitySerializer get() {
    return instance;
  }

  public ObjectMapper getObjectMapper() {
    return this.objectMapper;
  }

  public <T> T deserialize(String json, Class<T> type) {
    try {
      return this.getObjectMapper().readValue(json, type);
    } catch (IOException var4) {
      throw new RuntimeException(var4);
    }
  }

  public <T> T deserialize(String json, TypeReference<T> type) {
    try {
      return this.getObjectMapper().readValue(json, type);
    } catch (IOException var4) {
      throw new RuntimeException(var4);
    }
  }

  public <T> T deserialize(InputStream stream, Class<T> type) throws IOException {
    return this.getObjectMapper().readValue(stream, type);
  }

  public <T> List<T> deserializeList(String json, Class<T[]> type) {
    try {
      return (List<T>) Arrays.asList((Object[]) this.getObjectMapper().readValue(json, type));
    } catch (IOException var4) {
      throw new RuntimeException(var4);
    }
  }

  public <T> List<T> deserializeList(InputStream stream, Class<T[]> type) throws IOException {
    return (List<T>) Arrays.asList((Object[]) this.getObjectMapper().readValue(stream, type));
  }

  public String serialize(Object obj) {
    try {
      return this.getObjectMapper().writeValueAsString(obj);
    } catch (IOException var3) {
      throw new RuntimeException(var3);
    }
  }

  public byte[] serializeToBytes(Object obj) {
    try {
      return this.getObjectMapper().writeValueAsBytes(obj);
    } catch (IOException var3) {
      throw new RuntimeException(var3);
    }
  }
}
