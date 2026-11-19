# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class de.lernen.** { *** Companion; }
-keepclasseswithmembers class de.lernen.** { kotlinx.serialization.KSerializer serializer(...); }
