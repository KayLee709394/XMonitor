package com.XMonitor.curator.zk;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class JsonValues {
  public Map<String, Object> values;

  public JsonValues(Map<String, Object> values) {
    this.values = values;
  }

  public static <T> JsonValues of(Map<String, T> rawValues) {
    return new JsonValues(
      Maps.transformValues(
              rawValues, new Function<T, Object>() {
                @Override
                public Object apply(T input) {
                  return (Object) input;
                }
              }
      )
    );
  }

  public static <T> JsonValues of(Map<String, T> parseVals, String... keys) {
    return of(parseVals, Lists.newArrayList(keys));
  }

  public static <T> JsonValues of(Map<String, T> parseVals, List<String> keys) {
    JsonValues values = new JsonValues(Maps.<String, Object>newLinkedHashMap());
    for (String key : keys) {
      T val = parseVals.get(key);
      if (val == null) {
        values.values.put(key, "unknown");
      } else {
        values.values.put(key, val);
      }
    }
    return values;
  }

  public static Iterable<Map<String, Object>> toMaps(Iterable<JsonValues> values) {
    return Iterables.transform(
            values, new Function<JsonValues, Map<String, Object>>() {
              @Override
              public Map<String, Object> apply(JsonValues input) {
                return input.values;
              }
            }
    );
  }


}
