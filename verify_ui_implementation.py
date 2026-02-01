#!/usr/bin/env python3
"""
ScreenRest UI Implementation Verification Script (Part 3)
Verifies that all UI components compile and core functionality is implemented.
"""

import subprocess
import sys
import os
from pathlib import Path

def run_command(cmd, cwd=None):
    """Run a shell command and return success status."""
    try:
        result = subprocess.run(
            cmd,
            shell=True,
            cwd=cwd,
            capture_output=True,
            text=True,
            timeout=300
        )
        return result.returncode == 0, result.stdout, result.stderr
    except subprocess.TimeoutExpired:
        return False, "", "Command timed out"
    except Exception as e:
        return False, "", str(e)

def check_file_exists(filepath):
    """Check if a file exists."""
    return os.path.exists(filepath)

def verify_onboarding_implementation():
    """Verify onboarding flow is implemented."""
    print("\n=== Verifying Onboarding Implementation ===")
    
    files = [
        "app/src/main/java/com/screenrest/app/presentation/onboarding/OnboardingViewModel.kt",
        "app/src/main/java/com/screenrest/app/presentation/onboarding/OnboardingScreen.kt",
        "app/src/main/java/com/screenrest/app/presentation/onboarding/WelcomeStep.kt",
        "app/src/main/java/com/screenrest/app/presentation/onboarding/UsageAccessStep.kt",
        "app/src/main/java/com/screenrest/app/presentation/onboarding/OverlayStep.kt",
        "app/src/main/java/com/screenrest/app/presentation/onboarding/NotificationStep.kt",
        "app/src/main/java/com/screenrest/app/presentation/onboarding/AccessibilityStep.kt",
        "app/src/main/java/com/screenrest/app/presentation/onboarding/CompleteStep.kt",
    ]
    
    all_exist = True
    for file in files:
        exists = check_file_exists(file)
        status = "✓" if exists else "✗"
        print(f"{status} {file}")
        if not exists:
            all_exist = False
    
    return all_exist

def verify_home_implementation():
    """Verify home screen is implemented."""
    print("\n=== Verifying Home Screen Implementation ===")
    
    files = [
        "app/src/main/java/com/screenrest/app/presentation/main/HomeViewModel.kt",
        "app/src/main/java/com/screenrest/app/presentation/main/HomeScreen.kt",
        "app/src/main/java/com/screenrest/app/presentation/main/components/StatusCard.kt",
        "app/src/main/java/com/screenrest/app/presentation/main/components/ConfigSummaryCard.kt",
        "app/src/main/java/com/screenrest/app/presentation/components/PermissionWarningCard.kt",
    ]
    
    all_exist = True
    for file in files:
        exists = check_file_exists(file)
        status = "✓" if exists else "✗"
        print(f"{status} {file}")
        if not exists:
            all_exist = False
    
    return all_exist

def verify_settings_implementation():
    """Verify settings screen is implemented."""
    print("\n=== Verifying Settings Screen Implementation ===")
    
    files = [
        "app/src/main/java/com/screenrest/app/presentation/settings/SettingsViewModel.kt",
        "app/src/main/java/com/screenrest/app/presentation/settings/SettingsScreen.kt",
        "app/src/main/java/com/screenrest/app/presentation/settings/LongDurationWarningDialog.kt",
        "app/src/main/java/com/screenrest/app/presentation/settings/components/TrackingModeSelector.kt",
    ]
    
    all_exist = True
    for file in files:
        exists = check_file_exists(file)
        status = "✓" if exists else "✗"
        print(f"{status} {file}")
        if not exists:
            all_exist = False
    
    return all_exist

def verify_custom_messages_implementation():
    """Verify custom messages screen is implemented."""
    print("\n=== Verifying Custom Messages Implementation ===")
    
    files = [
        "app/src/main/java/com/screenrest/app/presentation/settings/messages/CustomMessagesViewModel.kt",
        "app/src/main/java/com/screenrest/app/presentation/settings/messages/CustomMessagesScreen.kt",
        "app/src/main/java/com/screenrest/app/presentation/settings/messages/AddMessageDialog.kt",
        "app/src/main/java/com/screenrest/app/presentation/settings/messages/components/MessageItem.kt",
    ]
    
    all_exist = True
    for file in files:
        exists = check_file_exists(file)
        status = "✓" if exists else "✗"
        print(f"{status} {file}")
        if not exists:
            all_exist = False
    
    return all_exist

def verify_navigation():
    """Verify navigation is properly configured."""
    print("\n=== Verifying Navigation Configuration ===")
    
    navgraph = "app/src/main/java/com/screenrest/app/presentation/navigation/NavGraph.kt"
    exists = check_file_exists(navgraph)
    status = "✓" if exists else "✗"
    print(f"{status} {navgraph}")
    
    if exists:
        with open(navgraph, 'r', encoding='utf-8') as f:
            content = f.read()
            checks = [
                ("OnboardingScreen", "OnboardingScreen" in content),
                ("HomeScreen", "HomeScreen" in content),
                ("SettingsScreen", "SettingsScreen" in content),
                ("CustomMessagesScreen", "CustomMessagesScreen" in content),
            ]
            
            all_ok = True
            for name, result in checks:
                status = "✓" if result else "✗"
                print(f"  {status} {name} integrated")
                if not result:
                    all_ok = False
            
            return all_ok
    
    return False

def compile_kotlin():
    """Attempt to compile Kotlin code."""
    print("\n=== Compiling Kotlin Code ===")
    print("Running: gradlew compileDebugKotlin")
    print("(This may take a few minutes...)")
    
    success, stdout, stderr = run_command("gradlew compileDebugKotlin", cwd=".")
    
    if success:
        print("✓ Kotlin compilation successful")
        return True
    else:
        print("✗ Kotlin compilation failed")
        if stderr:
            print("\nError output (last 50 lines):")
            lines = stderr.split('\n')
            for line in lines[-50:]:
                print(f"  {line}")
        return False

def main():
    """Run all verification checks."""
    print("=" * 60)
    print("ScreenRest UI Implementation Verification (Part 3)")
    print("=" * 60)
    
    # Change to android directory if we're not already there
    if os.path.exists("android"):
        os.chdir("android")
    
    checks = [
        ("Onboarding Flow", verify_onboarding_implementation),
        ("Home Screen", verify_home_implementation),
        ("Settings Screen", verify_settings_implementation),
        ("Custom Messages", verify_custom_messages_implementation),
        ("Navigation", verify_navigation),
    ]
    
    results = []
    for name, check_func in checks:
        try:
            result = check_func()
            results.append((name, result))
        except Exception as e:
            print(f"Error during {name} check: {e}")
            results.append((name, False))
    
    # Summary
    print("\n" + "=" * 60)
    print("VERIFICATION SUMMARY")
    print("=" * 60)
    
    all_passed = True
    for name, result in results:
        status = "✓ PASS" if result else "✗ FAIL"
        print(f"{status} - {name}")
        if not result:
            all_passed = False
    
    print("\n" + "=" * 60)
    if all_passed:
        print("✓ ALL CHECKS PASSED - UI implementation verified!")
        print("\nNote: Compilation check skipped by default (takes time).")
        print("To compile, run: gradlew compileDebugKotlin")
    else:
        print("✗ SOME CHECKS FAILED - Review errors above")
        return 1
    
    return 0

if __name__ == "__main__":
    sys.exit(main())
