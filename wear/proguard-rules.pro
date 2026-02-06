# Wear OS ProGuard Rules

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Gson models
-keep class com.quranmedia.player.wear.data.model.** { *; }
-keep class com.quranmedia.player.wear.domain.model.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }
