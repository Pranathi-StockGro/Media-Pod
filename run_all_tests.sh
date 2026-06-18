# Exit immediately if a command exits with a non-zero status.
set -e
echo "--------------------------------------------------------"
echo "🚀 Starting Media-Pod Test Suite"
echo "--------------------------------------------------------"
# 1. Core Logic & Conversion Tests
echo "📦 Testing mediapod-coil conversion..."
./gradlew :mediapod-coil:testAndroidHostTest
echo "📦 Testing mediapod-glide conversion..."
./gradlew :mediapod-glide:testAndroidHostTest
# 2. Shared Logic Tests
echo "📦 Testing shared module logic..."
./gradlew :shared:testAndroidHostTest
# 3. Sample App & UI Tests (Roborazzi)
echo "️ Running Sample App UI and Roborazzi Snapshot tests..."
# This runs both standard unit tests and Roborazzi verification
./gradlew :androidsampleapp:testDebugUnitTest
echo "--------------------------------------------------------"
echo "✅ All tests passed successfully!"
echo "--------------------------------------------------------"