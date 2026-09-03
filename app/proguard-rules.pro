# Firestore model classes are (de)serialized by reflection — keep their fields and no-arg constructors.
-keepclassmembers class kz.qlms.app.data.model.** {
    <init>();
    <fields>;
}
-keep class kz.qlms.app.data.model.** { *; }

# Firebase / Play Services already ship consumer rules; nothing extra needed here.
