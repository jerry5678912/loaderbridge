package net.fabricmc.loader.api.metadata;

import java.util.Map;

public interface CustomValue {
    CvType getType();
    CvObject getAsObject();
    CvArray getAsArray();
    String getAsString();
    Number getAsNumber();
    boolean getAsBoolean();

    enum CvType { OBJECT, ARRAY, STRING, NUMBER, BOOLEAN, NULL }

    interface CvObject extends Iterable<Map.Entry<String, CustomValue>>, CustomValue {
        int size();
        boolean containsKey(String key);
        CustomValue get(String key);
    }

    interface CvArray extends Iterable<CustomValue>, CustomValue {
        int size();
        CustomValue get(int index);
    }
}
