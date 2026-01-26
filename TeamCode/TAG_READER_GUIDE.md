# 📷 TAG READER - Limelight Testing Guide

## Purpose
The **TagReader** program is a diagnostic tool to test your Limelight camera and see exactly what AprilTags it's detecting.

---

## 🚀 How to Use

### Step 1: Deploy and Run
1. Build and deploy your code to the robot
2. On Driver Station, select **"Tag Reader"** from TeleOp menu
3. Press **INIT**
4. Press **START**

### Step 2: Point at AprilTag
1. Hold an AprilTag in front of the Limelight camera
2. Make sure tag is:
   - **Flat** (not wrinkled or bent)
   - **Well-lit** (good lighting, no glare)
   - **In focus** (not too close, not too far)
   - **Facing camera** (tag visible to camera)

### Step 3: Read Telemetry
Watch the Driver Station telemetry screen - it will show:

---

## 📊 What You'll See

### ✅ If Everything Works:
```
=================================
  TAG READER - LIVE DATA
=================================
Limelight: ✓ Connected

Tags Detected: ✓ 1 tag(s)

---------------------------------

Tag #1: ID: 20
  Position X: 12.34 inches
  Position Y: 5.67 inches
  Position Z: 24.00 inches
  Distance: 27.85 inches
  Corners: 4 detected

---------------------------------

tx (Horizontal): -2.34°
ty (Vertical): 1.23°
Latency: 15.2 ms

=================================

LOOKING FOR TAGS 20 & 24:
Tag 20: ✓ VISIBLE
Tag 24: ✗ Not found

=================================
```

### ❌ If Limelight Not Connected:
```
=================================
  TAG READER - LIVE DATA
=================================

❌ LIMELIGHT NOT CONNECTED

Troubleshooting:
1. Check USB cable
2. Check Robot Configuration
3. Verify device name: 'limelight'
4. Restart Robot Controller
=================================
```

### ⚠️ If No Tags Detected:
```
=================================
  TAG READER - LIVE DATA
=================================
Limelight: ✓ Connected

⚠️ NO TARGETS DETECTED

Make sure:
• Pipeline 0 is AprilTag mode
• AprilTag is in camera view
• Good lighting conditions
• Tag is not too far away
• Tag is flat and not damaged
=================================
```

---

## 🔧 Troubleshooting

### Problem: "LIMELIGHT NOT CONNECTED"

**Possible Causes:**
1. USB cable not connected
2. Limelight not powered on
3. Wrong device name in Robot Configuration
4. Limelight firmware issue

**Solutions:**
1. Check USB cable is firmly connected
2. Look for Limelight LED indicators (should be lit)
3. Open Robot Configuration on Driver Station
   - Go to: Configure → Edit
   - Look for device named **"limelight"**
   - Type should be **Limelight 3A**
   - If missing, scan for devices and add it
4. Restart Robot Controller
5. Try a different USB port
6. Check Limelight web interface (connect via WiFi)

---

### Problem: "NO TARGETS DETECTED"

**Possible Causes:**
1. Wrong pipeline (not AprilTag mode)
2. Tag not in camera view
3. Poor lighting
4. Tag too far away
5. Tag damaged or wrinkled

**Solutions:**

**Check Pipeline:**
1. Connect to Limelight web interface
2. Go to camera settings
3. Verify Pipeline 0 is set to **"AprilTag"** mode
4. Save settings

**Check Tag Visibility:**
1. Make sure camera is pointed at tag
2. Tag should be centered in view
3. Not too close (minimum 6 inches)
4. Not too far (maximum depends on tag size)
   - 6" tag: ~15 feet max
   - 8" tag: ~20 feet max

**Check Lighting:**
1. Ensure good, even lighting
2. Avoid direct sunlight on tag
3. Avoid dark shadows
4. Adjust Limelight exposure if needed

**Check Tag Quality:**
1. Tag should be printed clearly
2. Black squares should be solid black
3. White areas should be clean white
4. Tag should be flat (not wrinkled)
5. Tag should not be damaged

---

### Problem: "Wrong Tag ID Detected"

**Example:** You're holding tag 20, but telemetry shows tag 11

**Explanation:**
- Different AprilTag families have different IDs
- INTO THE DEEP uses tags 11-16 for field perimeter
- Your custom tags might be IDs 20 and 24
- Make sure you printed the correct tag ID!

**Solution:**
1. Verify the tag you printed
2. Check the number printed on the tag itself
3. If wrong, print the correct tag ID
4. Use tags from: **DECODE_Printable_AT_USLetter - V1**

---

### Problem: "Tag 20 shows as Not Found" even though tag is visible

**Possible Causes:**
1. You're holding tag with different ID (not 20)
2. Tag is detected but ID is different
3. LimelightManager not checking correctly

**Solutions:**
1. Look at telemetry section **"Tags Detected"**
2. Check what ID is actually being reported
3. If it shows "Tag #1: ID: 11" then you have tag 11, not 20
4. Print and use the correct tag (ID 20 or 24)

---

## 📋 What Each Value Means

### Tag Data:
- **ID**: The AprilTag number (should be 20 or 24 for your use)
- **Position X**: Horizontal distance (left/right) from camera
- **Position Y**: Vertical distance (up/down) from camera  
- **Position Z**: Forward distance from camera
- **Distance**: Total straight-line distance to tag
- **Corners**: Number of tag corners detected (should be 4)

### Limelight Data:
- **tx (Horizontal)**: Angle offset from camera center (degrees)
  - Positive = tag is to the right
  - Negative = tag is to the left
  - Zero = tag centered horizontally
  
- **ty (Vertical)**: Vertical angle offset from camera center (degrees)
  - Positive = tag is above center
  - Negative = tag is below center
  - Zero = tag centered vertically

- **Latency**: Time delay for processing (milliseconds)
  - Lower is better
  - Typical: 10-30 ms

---

## 🎯 Testing Checklist

Use this to verify everything works:

### Hardware Check:
- [ ] Limelight shows "✓ Connected"
- [ ] Limelight LEDs are lit
- [ ] USB cable secure
- [ ] Robot Configuration has "limelight" device

### Tag Detection Check:
- [ ] Hold tag 20 in front of camera
- [ ] "Tags Detected: ✓ 1 tag(s)" appears
- [ ] "Tag #1: ID: 20" shows correct ID
- [ ] Position data appears
- [ ] "Tag 20: ✓ VISIBLE" at bottom

- [ ] Switch to tag 24
- [ ] "Tag #1: ID: 24" shows correct ID
- [ ] "Tag 24: ✓ VISIBLE" at bottom

### Movement Check:
- [ ] Move tag left → tx becomes negative
- [ ] Move tag right → tx becomes positive
- [ ] Move tag up → ty becomes positive
- [ ] Move tag down → ty becomes negative
- [ ] Move tag closer → distance decreases
- [ ] Move tag farther → distance increases

---

## 🔍 Common Issues & Fixes

### Issue: Multiple tags detected when only holding one

**Explanation:** 
- Limelight sees other AprilTags in the background
- Field perimeter tags (11-16) might be visible
- Someone else's tag might be in view

**Solution:**
- This is normal and OK
- TagReader shows ALL detected tags
- Check "LOOKING FOR TAGS 20 & 24" section
- That section specifically checks for your tags

---

### Issue: Tag detected but no position data

**Explanation:**
- Tag is detected but too far/blurry for 3D position
- Limelight needs clear view for position calculation

**Solution:**
- Move tag closer to camera
- Ensure tag is flat and facing camera
- Improve lighting
- Use larger tag size if possible

---

### Issue: Latency very high (>50ms)

**Explanation:**
- Limelight processing is slow
- Might be overloaded or wrong settings

**Solution:**
- Check Limelight web interface settings
- Reduce resolution if too high
- Ensure only AprilTag pipeline active
- Update Limelight firmware

---

## ✅ Success Indicators

You know everything is working when:

1. ✓ Limelight: Connected
2. ✓ Tags Detected: 1 or more
3. ✓ Tag ID matches what you're holding (20 or 24)
4. ✓ Position data appears
5. ✓ tx/ty values change when moving tag
6. ✓ "Tag 20: ✓ VISIBLE" or "Tag 24: ✓ VISIBLE"

**If all above are true → Limelight is working perfectly!** ✅

---

## 🎯 Next Steps After Successful Test

Once TagReader shows your tags correctly:

1. **Note the detected Tag IDs** (should be 20 and 24)
2. **Verify tx/ty values** change when moving tag
3. **Note typical detection distance** for your tag size
4. **Run AutoTurret** program to test turret tracking
5. **Fine-tune** AutoTurret PID values if needed

---

## 📝 Recording Test Results

Fill this out during testing:

```
Date: _______________

LIMELIGHT CONNECTION:
[ ] Connected  [ ] Not Connected

TAGS PRINTED:
Tag 1 ID: _____  Size: _____"
Tag 2 ID: _____  Size: _____"

DETECTION TEST:
Tag 20:
  [ ] Detected  [ ] Not Detected
  Max Distance: _____ inches
  
Tag 24:
  [ ] Detected  [ ] Not Detected
  Max Distance: _____ inches

tx/ty RANGE:
  tx: _____ to _____ degrees
  ty: _____ to _____ degrees

ISSUES FOUND:
_________________________________
_________________________________
_________________________________

RESOLUTION:
_________________________________
_________________________________
_________________________________
```

---

## 🆘 Still Having Issues?

### Check These Common Mistakes:

1. **Wrong Pipeline**
   - Limelight has multiple pipelines
   - Must be on AprilTag detection pipeline
   - Check web interface

2. **Tag Family Mismatch**
   - AprilTags have different families (16h5, 36h11, etc.)
   - Limelight must be configured for correct family
   - INTO THE DEEP uses 36h11 family

3. **Upside Down Tag**
   - AprilTag has orientation
   - Make sure tag is right-side up
   - Look for orientation marker on tag

4. **Glossy/Reflective Tag**
   - If tag is laminated or glossy
   - Light reflects and camera can't see it
   - Use matte finish only

5. **Tag Too Small**
   - Very small tags (< 4") hard to detect from distance
   - Use largest tag size possible
   - 6-8" recommended for competition

---

## 📞 Support

If TagReader still doesn't detect your tags after checking everything:

1. Take screenshot of telemetry
2. Note exact error message
3. Check if ANY tags are detected (even wrong IDs)
4. Verify tag is printed correctly
5. Try known-good AprilTag from game manual

---

**Good luck testing! This tool will help you verify Limelight is working before using it with AutoTurret.** 🎯📷


