#!/usr/bin/env python3
"""
Verification script for Part 2 implementation
Checks that all required files exist and manifest is properly configured
"""

import os
import sys
import xml.etree.ElementTree as ET

def check_file_exists(filepath, description):
    """Check if a file exists and print status"""
    if os.path.exists(filepath):
        print(f"✓ {description}: {filepath}")
        return True
    else:
        print(f"✗ MISSING {description}: {filepath}")
        return False

def check_manifest_contains(manifest_path, search_text, description):
    """Check if manifest contains specific text"""
    try:
        with open(manifest_path, 'r', encoding='utf-8') as f:
            content = f.read()
            if search_text in content:
                print(f"✓ Manifest contains {description}")
                return True
            else:
                print(f"✗ Manifest MISSING {description}")
                return False
    except Exception as e:
        print(f"✗ Error checking manifest: {e}")
        return False

def main():
    print("=" * 70)
    print("PART 2 IMPLEMENTATION VERIFICATION")
    print("=" * 70)
    
    base_path = os.path.dirname(os.path.abspath(__file__))
    app_path = os.path.join(base_path, "app", "src", "main", "java", "com", "screenrest", "app")
    res_path = os.path.join(base_path, "app", "src", "main", "res")
    
    all_checks_passed = True
    
    # Phase 3: Usage Tracking Service
    print("\n--- PHASE 3: Usage Tracking Service ---")
    all_checks_passed &= check_file_exists(
        os.path.join(app_path, "service", "UsageCalculator.kt"),
        "UsageCalculator"
    )
    all_checks_passed &= check_file_exists(
        os.path.join(app_path, "service", "UsageTrackingService.kt"),
        "UsageTrackingService"
    )
    all_checks_passed &= check_file_exists(
        os.path.join(app_path, "service", "ServiceController.kt"),
        "ServiceController"
    )
    all_checks_passed &= check_file_exists(
        os.path.join(app_path, "receiver", "BootReceiver.kt"),
        "BootReceiver"
    )
    
    # Phase 4: Block Screen
    print("\n--- PHASE 4: Block Screen ---")
    all_checks_passed &= check_file_exists(
        os.path.join(app_path, "presentation", "block", "BlockActivity.kt"),
        "BlockActivity"
    )
    all_checks_passed &= check_file_exists(
        os.path.join(app_path, "presentation", "block", "BlockViewModel.kt"),
        "BlockViewModel"
    )
    all_checks_passed &= check_file_exists(
        os.path.join(app_path, "presentation", "block", "BlockScreen.kt"),
        "BlockScreen"
    )
    all_checks_passed &= check_file_exists(
        os.path.join(app_path, "presentation", "components", "AyahDisplay.kt"),
        "AyahDisplay"
    )
    all_checks_passed &= check_file_exists(
        os.path.join(app_path, "service", "BlockAccessibilityService.kt"),
        "BlockAccessibilityService"
    )
    all_checks_passed &= check_file_exists(
        os.path.join(app_path, "receiver", "BlockCompleteReceiver.kt"),
        "BlockCompleteReceiver"
    )
    
    # Phase 5: Navigation & Theme
    print("\n--- PHASE 5: Navigation & Theme ---")
    all_checks_passed &= check_file_exists(
        os.path.join(app_path, "presentation", "theme", "Color.kt"),
        "Theme Color"
    )
    all_checks_passed &= check_file_exists(
        os.path.join(app_path, "presentation", "theme", "Type.kt"),
        "Theme Typography"
    )
    all_checks_passed &= check_file_exists(
        os.path.join(app_path, "presentation", "theme", "Theme.kt"),
        "Theme"
    )
    all_checks_passed &= check_file_exists(
        os.path.join(app_path, "presentation", "navigation", "Screen.kt"),
        "Navigation Screen"
    )
    all_checks_passed &= check_file_exists(
        os.path.join(app_path, "presentation", "navigation", "NavGraph.kt"),
        "Navigation Graph"
    )
    
    # Resources
    print("\n--- RESOURCES ---")
    all_checks_passed &= check_file_exists(
        os.path.join(res_path, "values", "themes.xml"),
        "Themes XML"
    )
    all_checks_passed &= check_file_exists(
        os.path.join(res_path, "values", "strings.xml"),
        "Strings XML"
    )
    all_checks_passed &= check_file_exists(
        os.path.join(res_path, "xml", "accessibility_service_config.xml"),
        "Accessibility Service Config"
    )
    all_checks_passed &= check_file_exists(
        os.path.join(res_path, "drawable", "ic_notification.xml"),
        "Notification Icon"
    )
    
    # Manifest checks
    print("\n--- MANIFEST VERIFICATION ---")
    manifest_path = os.path.join(base_path, "app", "src", "main", "AndroidManifest.xml")
    all_checks_passed &= check_manifest_contains(
        manifest_path,
        "FOREGROUND_SERVICE",
        "Foreground Service Permission"
    )
    all_checks_passed &= check_manifest_contains(
        manifest_path,
        "PACKAGE_USAGE_STATS",
        "Usage Stats Permission"
    )
    all_checks_passed &= check_manifest_contains(
        manifest_path,
        "UsageTrackingService",
        "UsageTrackingService Registration"
    )
    all_checks_passed &= check_manifest_contains(
        manifest_path,
        "BlockActivity",
        "BlockActivity Registration"
    )
    all_checks_passed &= check_manifest_contains(
        manifest_path,
        "BootReceiver",
        "BootReceiver Registration"
    )
    all_checks_passed &= check_manifest_contains(
        manifest_path,
        "BlockAccessibilityService",
        "Accessibility Service Registration"
    )
    
    # Summary
    print("\n" + "=" * 70)
    if all_checks_passed:
        print("✓ ALL CHECKS PASSED - Part 2 implementation is complete!")
        print("\nNext steps:")
        print("1. Build the project: gradle assembleDebug (or via Android Studio)")
        print("2. Install on device: adb install -r app/build/outputs/apk/debug/app-debug.apk")
        print("3. Grant permissions manually via Settings")
        print("4. Test the tracking service and block screen")
        return 0
    else:
        print("✗ SOME CHECKS FAILED - Please review the output above")
        return 1

if __name__ == "__main__":
    sys.exit(main())
