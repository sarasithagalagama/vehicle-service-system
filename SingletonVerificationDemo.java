package com.vehicleservice.controller;

import com.vehicleservice.service.SingletonVerificationService;
import com.vehicleservice.util.SingletonManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

// Demo controller for Singleton Verification
@Controller
public class SingletonVerificationDemo {

    @Autowired
    private SingletonVerificationService singletonVerificationService;

    @Autowired
    private SingletonManager singletonManager;

    // Demo endpoint to show singleton verification
    @GetMapping("/demo/singleton-verification")
    @ResponseBody
    public Map<String, Object> demonstrateSingletonVerification() {
        System.out.println("\n=== SINGLETON VERIFICATION DEMO ===");

        // 1. Verify SingletonManager behavior
        System.out.println("1. Testing SingletonManager...");
        boolean singletonManagerResult = singletonManager.verifySingletonBehavior();
        System.out.println("   SingletonManager verification: " + (singletonManagerResult ? "PASSED ✓" : "FAILED ✗"));

        // 2. Test multiple instances of SingletonManager
        System.out.println("2. Testing multiple getInstance() calls...");
        SingletonManager instance1 = SingletonManager.getInstance();
        SingletonManager instance2 = SingletonManager.getInstance();
        boolean sameInstance = instance1 == instance2;
        System.out.println("   Instance 1: " + instance1.toString());
        System.out.println("   Instance 2: " + instance2.toString());
        System.out.println("   Same instance: " + (sameInstance ? "YES ✓" : "NO ✗"));

        // 3. Test Spring-managed singletons
        System.out.println("3. Testing Spring-managed singletons...");
        boolean bookingServiceSingleton = singletonVerificationService.isBeanSingleton("bookingService");
        boolean userServiceSingleton = singletonVerificationService.isBeanSingleton("userService");
        boolean inventoryServiceSingleton = singletonVerificationService.isBeanSingleton("inventoryService");

        System.out.println("   BookingService singleton: " + (bookingServiceSingleton ? "YES ✓" : "NO ✗"));
        System.out.println("   UserService singleton: " + (userServiceSingleton ? "YES ✓" : "NO ✗"));
        System.out.println("   InventoryService singleton: " + (inventoryServiceSingleton ? "YES ✓" : "NO ✗"));

        // 4. Get verification statistics
        System.out.println("4. Verification Statistics:");
        Map<String, Object> stats = singletonVerificationService.getVerificationStats();
        System.out.println("   Total beans verified: " + stats.get("totalBeans"));
        System.out.println("   Singleton beans: " + stats.get("singletonCount"));
        System.out.println("   Non-singleton beans: " + stats.get("nonSingletonCount"));
        System.out.println("   Singleton percentage: " + stats.get("singletonPercentage") + "%");

        // 5. Log singleton status
        System.out.println("5. SingletonManager Status:");
        singletonManager.logSingletonStatus();

        System.out.println("=== END SINGLETON VERIFICATION DEMO ===\n");

        return stats;
    }
}
