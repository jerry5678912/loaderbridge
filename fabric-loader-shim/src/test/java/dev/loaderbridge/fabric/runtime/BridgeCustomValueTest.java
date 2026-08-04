package dev.loaderbridge.fabric.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import net.fabricmc.loader.api.metadata.CustomValue;
import org.junit.jupiter.api.Test;

class BridgeCustomValueTest {
    @Test
    void preservesFabricTypesValuesAndContainerIterationOrder() {
        CustomValue object = BridgeCustomValue.parse(
                "{\"first\":1,\"second\":[true,null,\"value\"]}");

        assertThat(object.getType()).isEqualTo(CustomValue.CvType.OBJECT);
        assertThat(object.getAsObject()).isSameAs(object.getAsObject());
        assertThat(object.getAsObject()).extracting(entry -> entry.getKey())
                .containsExactly("first", "second");
        assertThat(object.getAsObject().get("first").getAsNumber())
                .isInstanceOf(Double.class).isEqualTo(1.0d);
        var array = object.getAsObject().get("second").getAsArray();
        assertThat(array).isSameAs(object.getAsObject().get("second").getAsArray());
        assertThat(array.get(0).getAsBoolean()).isTrue();
        assertThat(array.get(1).getType()).isEqualTo(CustomValue.CvType.NULL);
        assertThat(array.get(2).getAsString()).isEqualTo("value");

        var objectIterator = object.getAsObject().iterator();
        var firstEntry = objectIterator.next();
        assertThatThrownBy(objectIterator::remove).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> firstEntry.setValue(BridgeCustomValue.parse("2")))
                .isInstanceOf(UnsupportedOperationException.class);
        var arrayIterator = array.iterator();
        arrayIterator.next();
        assertThatThrownBy(arrayIterator::remove).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsEveryWrongTypeConversionWithFabricClassCastSemantics() {
        assertWrongType("{}", "getAsArray", value -> value.getAsArray(), "OBJECT", "Array");
        assertWrongType("[]", "getAsObject", value -> value.getAsObject(), "ARRAY", "Object");
        assertWrongType("true", "getAsString", value -> value.getAsString(), "BOOLEAN", "String");
        assertWrongType("\"value\"", "getAsBoolean", value -> value.getAsBoolean(), "STRING", "Boolean");
        assertWrongType("42", "getAsObject", value -> value.getAsObject(), "NUMBER", "Object");
        assertWrongType("null", "getAsNumber", value -> value.getAsNumber(), "NULL", "Number");
    }

    private static void assertWrongType(String json, String operation,
            java.util.function.Consumer<CustomValue> conversion, String source, String target) {
        CustomValue value = BridgeCustomValue.parse(json);
        assertThatThrownBy(() -> conversion.accept(value))
                .as(operation)
                .isInstanceOf(ClassCastException.class)
                .hasMessage("can't convert " + source + " to " + target);
    }
}
