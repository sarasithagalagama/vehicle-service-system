package com.vehicleservice.controller;

import com.vehicleservice.service.SingletonVerificationService;
import com.vehicleservice.strategy.PricingStrategyManager;
import com.vehicleservice.util.SingletonManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

// Test controller for design pattern verification
@Controller
@RequestMapping("/test")
public class PatternTestController {

    @Autowired
    private SingletonVerificationService singletonVerificationService;

    @Autowired
    private PricingStrategyManager pricingStrategyManager;

    @Autowired
    private SingletonManager singletonManager;

    @Autowired
    private ApplicationContext applicationContext;

    // Test Strategy Pattern
    @GetMapping("/strategy/{serviceType}")
    public ResponseEntity<Map<String, Object>> testStrategy(@PathVariable String serviceType) {
        Map<String, Object> result = new HashMap<>();

        try {
            BigDecimal basePrice = pricingStrategyManager.calculateBasePrice(serviceType);
            BigDecimal additionalCharges = pricingStrategyManager.calculateAdditionalCharges(serviceType, basePrice);
            BigDecimal totalPrice = pricingStrategyManager.calculateTotalPrice(serviceType, basePrice,
                    additionalCharges);

            result.put("serviceType", serviceType);
            result.put("basePrice", basePrice);
            result.put("additionalCharges", additionalCharges);
            result.put("totalPrice", totalPrice);
            result.put("strategyWorking", true);

        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("strategyWorking", false);
        }

        return ResponseEntity.ok(result);
    }

    // Test Singleton Pattern
    @GetMapping("/singleton-verification")
    public ResponseEntity<Map<String, Object>> verifySingletons() {
        Map<String, Object> stats = singletonVerificationService.getVerificationStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/singleton-bean/{beanName}")
    public ResponseEntity<Map<String, Object>> verifyBean(@PathVariable String beanName) {
        Map<String, Object> result = singletonVerificationService.verifyBean(beanName);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/singleton-manager")
    public ResponseEntity<Map<String, Object>> testSingletonManager() {
        Map<String, Object> result = new HashMap<>();

        SingletonManager manager1 = SingletonManager.getInstance();
        SingletonManager manager2 = SingletonManager.getInstance();

        boolean isSingleton = manager1 == manager2;
        boolean verificationPassed = singletonManager.verifySingletonBehavior();

        result.put("isSingleton", isSingleton);
        result.put("verificationPassed", verificationPassed);
        result.put("instance1", manager1.toString());
        result.put("instance2", manager2.toString());
        result.put("sameInstance", manager1 == manager2);

        return ResponseEntity.ok(result);
    }

    // Test Factory Pattern
    @GetMapping("/factory-beans")
    public ResponseEntity<Map<String, Object>> testFactoryPattern() {
        Map<String, Object> result = new HashMap<>();

        // Test dependency injection
        result.put("pricingStrategyManager", pricingStrategyManager != null);
        result.put("singletonVerificationService", singletonVerificationService != null);
        result.put("singletonManager", singletonManager != null);
        result.put("applicationContext", applicationContext != null);

        // Test bean creation
        result.put("totalBeans", applicationContext.getBeanDefinitionCount());
        result.put("beanNames", applicationContext.getBeanDefinitionNames());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/spring-context")
    public ResponseEntity<Map<String, Object>> analyzeSpringContext() {
        Map<String, Object> analysis = new HashMap<>();

        String[] beanNames = applicationContext.getBeanDefinitionNames();
        analysis.put("totalBeans", beanNames.length);
        analysis.put("beanNames", beanNames);

        // Analyze bean scopes
        Map<String, Integer> scopeCount = new HashMap<>();
        org.springframework.context.ConfigurableApplicationContext configurableContext = (org.springframework.context.ConfigurableApplicationContext) applicationContext;
        for (String beanName : beanNames) {
            try {
                String scope = configurableContext.getBeanFactory().getBeanDefinition(beanName).getScope();
                scopeCount.put(scope, scopeCount.getOrDefault(scope, 0) + 1);
            } catch (Exception e) {
                scopeCount.put("unknown", scopeCount.getOrDefault("unknown", 0) + 1);
            }
        }
        analysis.put("scopeDistribution", scopeCount);

        return ResponseEntity.ok(analysis);
    }

    // Test all patterns together
    @GetMapping("/all-patterns")
    public ResponseEntity<Map<String, Object>> testAllPatterns() {
        Map<String, Object> result = new HashMap<>();

        // Strategy Pattern Test
        try {
            BigDecimal oilChangePrice = pricingStrategyManager.calculateBasePrice("Oil Change");
            result.put("strategyPattern", Map.of(
                    "working", true,
                    "oilChangePrice", oilChangePrice,
                    "message", "Strategy pattern working correctly"));
        } catch (Exception e) {
            result.put("strategyPattern", Map.of(
                    "working", false,
                    "error", e.getMessage()));
        }

        // Singleton Pattern Test
        try {
            Map<String, Object> singletonStats = singletonVerificationService.getVerificationStats();
            boolean singletonManagerVerified = singletonManager.verifySingletonBehavior();
            result.put("singletonPattern", Map.of(
                    "working", true,
                    "stats", singletonStats,
                    "singletonManagerVerified", singletonManagerVerified,
                    "message", "Singleton pattern working correctly"));
        } catch (Exception e) {
            result.put("singletonPattern", Map.of(
                    "working", false,
                    "error", e.getMessage()));
        }

        // Factory Pattern Test
        try {
            int totalBeans = applicationContext.getBeanDefinitionCount();
            result.put("factoryPattern", Map.of(
                    "working", true,
                    "totalBeans", totalBeans,
                    "message", "Factory pattern (Spring IoC) working correctly"));
        } catch (Exception e) {
            result.put("factoryPattern", Map.of(
                    "working", false,
                    "error", e.getMessage()));
        }

        return ResponseEntity.ok(result);
    }
}
