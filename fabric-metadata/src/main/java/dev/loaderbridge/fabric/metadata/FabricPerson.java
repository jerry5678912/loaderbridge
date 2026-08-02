package dev.loaderbridge.fabric.metadata;

import java.util.Map;

public record FabricPerson(String name, Map<String, String> contact) {
    public FabricPerson {
        contact = Map.copyOf(contact);
    }
}
