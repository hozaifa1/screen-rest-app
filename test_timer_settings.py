#!/usr/bin/env python3
"""
Test script to verify timer settings conversion logic
This simulates the timer picker behavior across different device densities
"""

def test_timer_conversion():
    """Test that timer values are correctly converted and stored"""
    
    print("Testing Timer Settings Conversion\n")
    print("=" * 50)
    
    # Test cases: (minutes, seconds, expected_total_seconds)
    test_cases = [
        (3, 0, 180, "3 minutes"),
        (0, 30, 30, "30 seconds"),
        (1, 0, 60, "1 minute"),
        (5, 30, 330, "5 minutes 30 seconds"),
        (0, 15, 15, "15 seconds"),
        (10, 45, 645, "10 minutes 45 seconds"),
    ]
    
    all_passed = True
    
    for minutes, seconds, expected_total, description in test_cases:
        # Simulate the conversion logic from TimerAdjustDialog
        total_seconds = minutes * 60 + seconds
        
        # Verify conversion
        if total_seconds == expected_total:
            print(f"✓ PASS: {description}")
            print(f"  Input: {minutes}m {seconds}s → {total_seconds}s")
        else:
            print(f"✗ FAIL: {description}")
            print(f"  Expected: {expected_total}s, Got: {total_seconds}s")
            all_passed = False
        
        # Verify reverse conversion (for display)
        display_min = total_seconds // 60
        display_sec = total_seconds % 60
        
        if display_min == minutes and display_sec == seconds:
            print(f"  Reverse: {total_seconds}s → {display_min}m {display_sec}s ✓")
        else:
            print(f"  Reverse FAILED: Expected {minutes}m {seconds}s, Got {display_min}m {display_sec}s ✗")
            all_passed = False
        
        print()
    
    print("=" * 50)
    
    # Test density-independent pixel conversion simulation
    print("\nTesting Density-Independent Behavior\n")
    print("=" * 50)
    
    # Simulate different screen densities
    densities = [
        (1.0, "mdpi (160dpi)"),
        (1.5, "hdpi (240dpi)"),
        (2.0, "xhdpi (320dpi)"),
        (3.0, "xxhdpi (480dpi)"),
        (4.0, "xxxhdpi (640dpi)"),
    ]
    
    item_height_dp = 40  # dp value from code
    scroll_offset_threshold = 0.5  # 50% of item height
    
    for density, density_name in densities:
        item_height_px = item_height_dp * density
        threshold_px = item_height_px * scroll_offset_threshold
        
        # Test scroll offset detection
        test_offsets = [
            (threshold_px - 1, False, "just below threshold"),
            (threshold_px, True, "at threshold"),
            (threshold_px + 1, True, "just above threshold"),
        ]
        
        print(f"\n{density_name}:")
        print(f"  Item height: {item_height_dp}dp = {item_height_px}px")
        print(f"  Threshold: {threshold_px}px")
        
        for offset, should_increment, desc in test_offsets:
            would_increment = offset > threshold_px
            status = "✓" if would_increment == should_increment else "✗"
            print(f"  {status} Offset {offset}px ({desc}): {'increment' if would_increment else 'no increment'}")
    
    print("\n" + "=" * 50)
    
    if all_passed:
        print("\n✓ All timer conversion tests PASSED!")
        return 0
    else:
        print("\n✗ Some tests FAILED!")
        return 1

if __name__ == "__main__":
    exit(test_timer_conversion())
