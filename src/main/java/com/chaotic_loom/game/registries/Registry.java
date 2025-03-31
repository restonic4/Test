package com.chaotic_loom.game.registries;

import com.chaotic_loom.game.core.Environment;
import com.chaotic_loom.game.networking.components.Packet;
import com.chaotic_loom.game.registries.built_in.RegistryKeys;
import com.chaotic_loom.game.registries.components.Identifier;
import com.chaotic_loom.game.registries.components.Registration;
import com.chaotic_loom.game.registries.components.RegistryKey;
import com.chaotic_loom.game.registries.components.RegistryObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

public class Registry {
    public static final Logger logger = LogManager.getLogger("Registry");
    private static final Map<RegistryKey<?>, Map<Identifier, ?>> registries = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static <T extends RegistryObject> T register(RegistryKey<T> registryKey, Identifier identifier, T object) {
        Map<Identifier, T> registry = getOrCreateRegistrySet(registryKey);

        logger.debug("Registering {} on {}!", identifier, registryKey.key());

        if (registry.containsKey(identifier)) {
            throw new IllegalArgumentException("Duplicate identifier: " + identifier);
        }

        identifier.setRegistryKey(registryKey);

        object.setIdentifier(identifier);
        object.onPopulate();

        registry.put(identifier, object);

        return object;
    }

    public static <T extends RegistryObject> T getRegistryObject(RegistryKey<T> registryKey, Identifier identifier) {
        Map<Identifier, T> registry = getRegistrySet(registryKey);

        if (registry == null) return null;

        return registry.get(identifier);
    }

    @SuppressWarnings("unchecked")
    private static <T extends RegistryObject> Map<Identifier, T> getOrCreateRegistrySet(RegistryKey<T> registryKey) {
        return (Map<Identifier, T>) registries.computeIfAbsent(registryKey, k -> {
            logger.debug("Creating new registry map for {}", registryKey);

            return new HashMap<>();
        });
    }

    @SuppressWarnings("unchecked")
    public static <T extends RegistryObject> Map<Identifier, T> getRegistrySet(RegistryKey<T> registryKey) {
        return (Map<Identifier, T>) registries.get(registryKey);
    }

    public static <T extends RegistryObject> boolean isNamespaceLoaded(String id) {
        for (Map.Entry<RegistryKey<?>, Map<Identifier, ?>> data : registries.entrySet()) {
            Map<Identifier, ?> map = data.getValue();

            for (Map.Entry<Identifier, ?> registryData : map.entrySet()) {
                Identifier identifier = registryData.getKey();

                if (Objects.equals(identifier.getNamespace(), id)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static Map<RegistryKey<?>, Map<Identifier, ?>> getRegistries() {
        return registries;
    }

    // Annotation

    public static void startRegistrationAnnotationCollection(Environment environment) {
        logger.info("Starting registration annotation collection for {}", environment);

        Reflections reflections = new Reflections(createConfigBuilder());
        Set<Class<?>> registrarsFound = reflections.getTypesAnnotatedWith(Registration.class);

        logger.debug("Total registrars found: {}", registrarsFound.size());

        List<Class<?>> sortedRegistrars = registrarsFound.stream()
                .filter(registrar -> {
                    Registration annotation = registrar.getAnnotation(Registration.class);
                    return annotation != null && annotation.environment() == environment;
                })
                .sorted((registrar1, registrar2) -> {
                    Registration annotation1 = registrar1.getAnnotation(Registration.class);
                    Registration annotation2 = registrar2.getAnnotation(Registration.class);
                    return Integer.compare(annotation2.priority(), annotation1.priority());
                })
                .toList();

        logger.debug("Valid registrars found: {}", sortedRegistrars.size());

        for (Class<?> registrar : sortedRegistrars) {
            Registration annotation = registrar.getAnnotation(Registration.class);
            if (annotation != null) {
                try {
                    Method registerMethod = registrar.getDeclaredMethod("register");

                    if (Modifier.isStatic(registerMethod.getModifiers())) {
                        logger.info("Executing registrar: {} with priority {}", registrar.getName(), annotation.priority());
                        registerMethod.invoke(null);
                    } else {
                        logger.error("Method 'register' in {} is not static!", registrar.getSimpleName());
                    }
                } catch (NoSuchMethodException e) {
                    logger.error("No 'register' method found in: {}", registrar.getSimpleName());
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static ConfigurationBuilder createConfigBuilder() {
        ConfigurationBuilder configBuilder = new ConfigurationBuilder();

        // Explicitly gather URLs from standard locations
        logger.info("Attempting to gather URLs for Reflections scanning...");
        Set<URL> urls = new java.util.HashSet<>();
        urls.addAll(ClasspathHelper.forClassLoader()); // Context class loader
        urls.addAll(ClasspathHelper.forJavaClassPath()); // System property java.class.path

        // --- Add specific class loader if known ---
        // urls.addAll(ClasspathHelper.forClassLoader(KnownClassLoaderWithLib.class.getClassLoader()));
        // -----------------------------------------------------

        configBuilder.addUrls(urls);
        configBuilder.setScanners(Scanners.TypesAnnotated);

        logger.debug("Reflections scanning configured URLs ({}):", configBuilder.getUrls().size());
        for(URL url : configBuilder.getUrls()) {
            logger.debug(" -> {}", url);
        }

        return configBuilder;
    }
}
