package com.vehicleservice.util;

// Import statements for singleton management and logging
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// Singleton Manager - Custom Singleton Pattern Implementation
@Component
@org.springframework.context.annotation.Scope("singleton")
public class SingletonManager {

    private static final Logger logger = LoggerFactory.getLogger(SingletonManager.class);

    // Singleton instance with volatile for thread safety
    private static volatile SingletonManager instance;

    // Registry for managing other singletons
    private final ConcurrentHashMap<String, Object> singletonInstances = new ConcurrentHashMap<>();

    // Track instance creation count
    private final AtomicInteger instanceCount = new AtomicInteger(0);

    // Private constructor prevents external instantiation
    private SingletonManager() {
        logger.info("SingletonManager instance created - Instance #{}", instanceCount.incrementAndGet());
    }

    // Get singleton instance using double-checked locking
    public static SingletonManager getInstance() {
        if (instance == null) {
            synchronized (SingletonManager.class) {
                if (instance == null) {
                    instance = new SingletonManager();
                    logger.info("New SingletonManager instance created with Double-Checked Locking");
                }
            }
        }
        return instance;
    }

    // Register a singleton instance
    public <T> T registerSingleton(String key, T instance) {
        if (key == null || instance == null) {
            throw new IllegalArgumentException("Key and instance cannot be null");
        }

        @SuppressWarnings("unchecked")
        T result = (T) singletonInstances.computeIfAbsent(key, k -> {
            logger.info("Registered singleton instance for key: {}", k);
            return instance;
        });
        return result;
    }

    // Get registered singleton by key
    @SuppressWarnings("unchecked")
    public <T> T getSingleton(String key, Class<T> clazz) {
        Object instance = singletonInstances.get(key);
        if (instance != null && clazz.isInstance(instance)) {
            return (T) instance;
        }
        return null;
    }

    // Check if singleton exists for key
    public boolean hasSingleton(String key) {
        return singletonInstances.containsKey(key);
    }

    // Get count of registered singletons
    public int getSingletonCount() {
        return singletonInstances.size();
    }

    // Get instance creation count
    public int getInstanceCreationCount() {
        return instanceCount.get();
    }

    // Verify singleton behavior
    public boolean verifySingletonBehavior() {
        SingletonManager instance1 = getInstance();
        SingletonManager instance2 = getInstance();

        boolean isSingleton = instance1 == instance2;
        logger.info("Singleton verification: {} (instances are same: {})",
                isSingleton ? "PASSED" : "FAILED", isSingleton);

        return isSingleton;
    }

    // Log singleton status for debugging
    public void logSingletonStatus() {
        logger.info("=== Singleton Status ===");
        logger.info("SingletonManager instances created: {}", instanceCount.get());
        logger.info("Registered singletons: {}", singletonInstances.size());
        logger.info("Registered singleton keys: {}", singletonInstances.keySet());
        logger.info("========================");
    }

    // Clear all singletons (testing only)
    public void clearAllSingletons() {
        logger.warn("Clearing all registered singletons - this should only be used in tests");
        singletonInstances.clear();
    }
}
