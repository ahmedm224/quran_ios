# Add project specific ProGuard rules here.

# CRITICAL: Don't obfuscate or shrink - ensures everything works
-dontobfuscate
-dontshrink

# Keep all app classes and members
-keep class com.quranmedia.player.** { *; }
-keepclassmembers class com.quranmedia.player.** { *; }
-keepnames class com.quranmedia.player.** { *; }

# Keep all attributes for proper functionality
-keepattributes *

# Keep ExoPlayer classes
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep Room classes - CRITICAL
-keep class * extends androidx.room.RoomDatabase {
    *;
}
-keep @androidx.room.Entity class * {
    *;
}
-keep @androidx.room.Dao interface * {
    *;
}
-keep interface * extends androidx.room.Dao {
    *;
}
-keepclassmembers class * {
    @androidx.room.* *;
}
-keep class com.quranmedia.player.data.database.dao.** { *; }
-dontwarn androidx.room.paging.**

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel
-keepclassmembers class * extends dagger.hilt.android.lifecycle.HiltViewModel {
    <init>(...);
}

# Keep MediaBrowserService
-keep class * extends androidx.media.MediaBrowserServiceCompat { *; }
-keep class androidx.media.** { *; }

# Gson - CRITICAL for API responses
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep all fields with @SerializedName annotation
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep all data classes used in API responses
-keep class com.quranmedia.player.data.api.model.** { *; }
-keepclassmembers class com.quranmedia.player.data.api.model.** {
    *;
}

# OkHttp & Retrofit - CRITICAL
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn okio.**
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keep interface retrofit2.** { *; }

# Keep API interfaces and models
-keep interface com.quranmedia.player.data.api.** { *; }
-keep class com.quranmedia.player.data.api.** { *; }

# For Retrofit with Kotlin serialization/GSON
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
  @retrofit2.http.* <methods>;
}

# Protobuf
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }

# Keep app data models - CRITICAL
-keep class com.quranmedia.player.data.api.model.** { *; }
-keep class com.quranmedia.player.domain.model.** { *; }
-keep class com.quranmedia.player.data.database.entity.** { *; }

# Keep all Kotlin extension functions - CRITICAL for entity mapping
-keepclassmembers class com.quranmedia.player.data.database.entity.** {
    public ** toDomainModel();
    public ** toEntity();
}
-keep class com.quranmedia.player.data.database.entity.**Kt { *; }

# Keep Kotlin metadata
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Keep Kotlin coroutines - CRITICAL
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepclassmembers class kotlin.coroutines.SafeContinuation {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# Keep Flow and suspend functions
-keep class kotlinx.coroutines.flow.** { *; }
-keepclassmembers class * {
    kotlinx.coroutines.flow.Flow *;
}

# Keep Kotlin reflection
-keep class kotlin.reflect.** { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Keep all repositories - CRITICAL
-keep class com.quranmedia.player.data.repository.** { *; }
-keep interface com.quranmedia.player.domain.repository.** { *; }

# Keep all presentation layer - ViewModels and UI state
-keep class com.quranmedia.player.presentation.** { *; }
-keepclassmembers class com.quranmedia.player.presentation.** {
    *;
}

# Keep WorkManager - CRITICAL
-keep class * extends androidx.work.Worker {
    *;
}
-keep class * extends androidx.work.CoroutineWorker {
    *;
}
-keep class androidx.work.** { *; }
-keep @androidx.hilt.work.HiltWorker class * {
    *;
}
-keep class com.quranmedia.player.data.worker.** { *; }

# Keep Timber logging
-keep class timber.log.** { *; }
-dontwarn timber.log.**

# Keep DataStore
-keep class androidx.datastore.** { *; }
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# Keep Media3 Session
-keep class androidx.media3.session.** { *; }
-keep class androidx.media3.common.Player$Listener { *; }

# Keep all ViewModels
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(...);
}

# Keep Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Serializable classes
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep line numbers for debugging crashes
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
