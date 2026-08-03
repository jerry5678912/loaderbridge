package dev.loaderbridge.fabric.remap;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Bounded Tiny-v2 lookup used for non-class resources such as Mixin refmaps. */
final class TinyMappingIndex {
    private static final int MAX_LINES = 500_000;
    private static final Pattern TYPE = Pattern.compile("L([^;]+);");
    private final Map<String, String> classes;
    private final Map<String, String> reverseClasses;
    private final Map<Member, String> fields;
    private final Map<Member, String> methods;

    private TinyMappingIndex(Map<String, String> classes, Map<Member, String> fields,
            Map<Member, String> methods) {
        this.classes = Map.copyOf(classes);
        Map<String, String> reverse = new LinkedHashMap<>();
        classes.forEach((source, target) -> reverse.put(target, source));
        this.reverseClasses = Map.copyOf(reverse);
        this.fields = Map.copyOf(fields);
        this.methods = Map.copyOf(methods);
    }

    static TinyMappingIndex read(Path path) throws IOException {
        Map<String, String> classes = new LinkedHashMap<>();
        Map<Member, String> fields = new LinkedHashMap<>();
        Map<Member, String> methods = new LinkedHashMap<>();
        String owner = null;
        int count = 0;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            if (!"tiny\t2\t0\tintermediary\tnamed".equals(reader.readLine())) {
                throw new IOException("LB-MIXIN-REFMAP-001: unsupported mapping namespaces");
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (++count > MAX_LINES) {
                    throw new IOException("LB-MIXIN-REFMAP-001: mapping line limit exceeded");
                }
                String[] parts = line.split("\t", -1);
                if (parts.length == 3 && parts[0].equals("c")) {
                    owner = parts[1];
                    classes.put(parts[1], parts[2]);
                } else if (parts.length == 5 && parts[0].isEmpty() && owner != null) {
                    Member member = new Member(owner, parts[3], parts[2]);
                    if (parts[1].equals("f")) fields.put(member, parts[4]);
                    else if (parts[1].equals("m")) methods.put(member, parts[4]);
                }
            }
        }
        return new TinyMappingIndex(classes, fields, methods);
    }

    String translateReference(String reference) {
        return translateReference(reference, null);
    }

    String translateReference(String reference, String inferredOwner) {
        if (!reference.startsWith("L") && inferredOwner != null) {
            int methodDescriptor = reference.indexOf('(');
            if (methodDescriptor > 0) {
                String name = reference.substring(0, methodDescriptor);
                String descriptor = reference.substring(methodDescriptor);
                return mapMethod(inferredOwner, name, descriptor) + mapDescriptor(descriptor);
            }
            int fieldDescriptor = reference.indexOf(':');
            if (fieldDescriptor > 0) {
                String name = reference.substring(0, fieldDescriptor);
                String descriptor = reference.substring(fieldDescriptor + 1);
                return mapField(inferredOwner, name, descriptor) + ":" + mapDescriptor(descriptor);
            }
        }
        if (!reference.startsWith("L")) return mapDescriptor(reference);
        int separator = reference.indexOf(';');
        if (separator < 2) return reference;
        String owner = reference.substring(1, separator);
        String remainder = reference.substring(separator + 1);
        String targetOwner = classes.getOrDefault(owner, owner);
        if (remainder.isEmpty()) return "L" + targetOwner + ";";
        int methodDescriptor = remainder.indexOf('(');
        if (methodDescriptor > 0) {
            String name = remainder.substring(0, methodDescriptor);
            String descriptor = remainder.substring(methodDescriptor);
            String targetName = methods.getOrDefault(new Member(owner, name, descriptor), name);
            return "L" + targetOwner + ";" + targetName + mapDescriptor(descriptor);
        }
        int fieldDescriptor = remainder.indexOf(':');
        if (fieldDescriptor > 0) {
            String name = remainder.substring(0, fieldDescriptor);
            String descriptor = remainder.substring(fieldDescriptor + 1);
            String targetName = fields.getOrDefault(new Member(owner, name, descriptor), name);
            return "L" + targetOwner + ";" + targetName + ":" + mapDescriptor(descriptor);
        }
        return "L" + targetOwner + ";" + remainder;
    }

    String mapClass(String name) {
        return classes.getOrDefault(name, name);
    }

    String sourceClass(String runtimeName) {
        return reverseClasses.getOrDefault(runtimeName, runtimeName);
    }

    String sourceDescriptor(String runtimeDescriptor) {
        Matcher matcher = TYPE.matcher(runtimeDescriptor);
        StringBuilder output = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(output, Matcher.quoteReplacement(
                    "L" + reverseClasses.getOrDefault(matcher.group(1), matcher.group(1)) + ";"));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    String mapField(String owner, String name, String descriptor) {
        return fields.getOrDefault(new Member(owner, name, descriptor), name);
    }

    String mapMethod(String owner, String name, String descriptor) {
        return methods.getOrDefault(new Member(owner, name, descriptor), name);
    }

    static String unqualified(String reference) {
        int separator = reference.indexOf(';');
        return reference.startsWith("L") && separator >= 0
                ? reference.substring(separator + 1) : reference;
    }

    private String mapDescriptor(String descriptor) {
        Matcher matcher = TYPE.matcher(descriptor);
        StringBuilder output = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(output, Matcher.quoteReplacement(
                    "L" + classes.getOrDefault(matcher.group(1), matcher.group(1)) + ";"));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private record Member(String owner, String name, String descriptor) {}
}
