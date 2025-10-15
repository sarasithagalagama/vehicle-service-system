package com.vehicleservice.service;

import com.vehicleservice.util.SingletonManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

// Singleton Verification Service - Verifies Spring-managed singletons
@Service
@org.springframework.context.annotation.Scope("singleton")
public class SingletonVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(SingletonVerificationService.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SingletonManager singletonManager;

    // Track bean instances for verification
    private final Map<String, Object> beanInstances = new HashMap<>();

    // Verify all Spring beans are singletons using instance comparison
    @PostConstruct
    public void verifyAllSingletons() {
        logger.info("Starting singleton verification for all Spring beans...");

        // List of critical beans to verify
        String[] criticalBeans = {
                "userService",
                "bookingService",
                "inventoryService",
                "assignmentService",
                "feedbackService",
                "dataInitializationService",
                "userRepository",
                "bookingRepository",
                "inventoryItemRepository",
                "technicianRepository",
                "technicianAssignmentRepository",
                "roleRepository",
                "inventoryTransactionRepository",
                "feedbackRepository",
                "adminController",
                "authController",
                "bookingController",
                "customerController",
                "feedbackController",
                "inventoryController",
                "managerController",
                "staffController",
                "securityConfig"
        };

        boolean allSingletons = true;

        for (String beanName : criticalBeans) {
            try {
                if (applicationContext.containsBean(beanName)) {
                    Object bean1 = applicationContext.getBean(beanName);
                    Object bean2 = applicationContext.getBean(beanName);

                    boolean isSingleton = bean1 == bean2;
                    beanInstances.put(beanName, bean1);

                    String scope = "singleton"; // Default scope for Spring beans
                    try {
                        if (applicationContext instanceof org.springframework.context.support.GenericApplicationContext) {
                            scope = ((org.springframework.context.support.GenericApplicationContext) applicationContext)
                                    .getBeanDefinition(beanName).getScope();
                        }
                    } catch (Exception e) {
                        // Use default scope if unable to determine
                    }

                    logger.info("Bean '{}': {} (Scope: {})",
                            beanName,
                            isSingleton ? "SINGLETON ✓" : "NOT SINGLETON ✗",
                            scope);

                    if (!isSingleton) {
                        allSingletons = false;
                    }
                } else {
                    logger.warn("Bean '{}' not found in application context", beanName);
                }
            } catch (Exception e) {
                logger.error("Error verifying bean '{}': {}", beanName, e.getMessage());
                allSingletons = false;
            }
        }

        // Verify SingletonManager
        boolean singletonManagerVerified = singletonManager.verifySingletonBehavior();

        // Log overall verification result
        if (allSingletons && singletonManagerVerified) {
            logger.info("✅ ALL SINGLETON VERIFICATIONS PASSED - Application follows Singleton pattern correctly");
        } else {
            logger.error("❌ SINGLETON VERIFICATION FAILED - Some beans are not properly configured as singletons");
        }

        // Log singleton status
        singletonManager.logSingletonStatus();
        logBeanScopeSummary();
    }

    // Log summary of bean scopes in application
    private void logBeanScopeSummary() {
        logger.info("=== Bean Scope Summary ===");

        String[] allBeanNames = applicationContext.getBeanDefinitionNames();
        Map<String, Integer> scopeCount = new HashMap<>();

        for (String beanName : allBeanNames) {
            try {
                String scope = "singleton"; // Default scope for Spring beans
                if (applicationContext instanceof org.springframework.context.support.GenericApplicationContext) {
                    scope = ((org.springframework.context.support.GenericApplicationContext) applicationContext)
                            .getBeanDefinition(beanName).getScope();
                }
                scopeCount.put(scope, scopeCount.getOrDefault(scope, 0) + 1);
            } catch (Exception e) {
                logger.debug("Could not determine scope for bean '{}': {}", beanName, e.getMessage());
                scopeCount.put("unknown", scopeCount.getOrDefault("unknown", 0) + 1);
            }
        }

        scopeCount.forEach((scope, count) -> logger.info("Scope '{}': {} beans", scope, count));

        logger.info("Total beans: {}", allBeanNames.length);
        logger.info("========================");
    }

    // Get verification results for specific bean
    public Map<String, Object> verifyBean(String beanName) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (!applicationContext.containsBean(beanName)) {
                result.put("exists", false);
                result.put("error", "Bean not found");
                return result;
            }

            Object bean1 = applicationContext.getBean(beanName);
            Object bean2 = applicationContext.getBean(beanName);

            boolean isSingleton = bean1 == bean2;
            String scope = "singleton"; // Default scope for Spring beans
            try {
                if (applicationContext instanceof org.springframework.context.support.GenericApplicationContext) {
                    scope = ((org.springframework.context.support.GenericApplicationContext) applicationContext)
                            .getBeanDefinition(beanName).getScope();
                }
            } catch (Exception e) {
                // Use default scope if unable to determine
            }

            result.put("exists", true);
            result.put("isSingleton", isSingleton);
            result.put("scope", scope);
            result.put("instance1", bean1.toString());
            result.put("instance2", bean2.toString());
            result.put("sameInstance", bean1 == bean2);

        } catch (Exception e) {
            result.put("exists", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    // Get all registered bean instances
    public Map<String, Object> getBeanInstances() {
        return new HashMap<>(beanInstances);
    }

    // Check if bean is properly configured as singleton
    public boolean isBeanSingleton(String beanName) {
        try {
            if (!applicationContext.containsBean(beanName)) {
                return false;
            }

            Object bean1 = applicationContext.getBean(beanName);
            Object bean2 = applicationContext.getBean(beanName);

            return bean1 == bean2;
        } catch (Exception e) {
            logger.error("Error checking singleton status for bean '{}': {}", beanName, e.getMessage());
            return false;
        }
    }

    // Get singleton verification statistics
    public Map<String, Object> getVerificationStats() {
        Map<String, Object> stats = new HashMap<>();

        int totalBeans = beanInstances.size();
        long singletonCount = beanInstances.entrySet().stream()
                .filter(entry -> isBeanSingleton(entry.getKey()))
                .count();

        stats.put("totalBeans", totalBeans);
        stats.put("singletonCount", singletonCount);
        stats.put("nonSingletonCount", totalBeans - singletonCount);
        stats.put("singletonPercentage", totalBeans > 0 ? (singletonCount * 100.0 / totalBeans) : 0);
        stats.put("singletonManagerVerified", singletonManager.verifySingletonBehavior());

        return stats;
    }
}
