# Room generates implementation classes reflectively referenced by name.
-keep class com.drafts.compose.data.** { *; }

# ViewBinding inflate() is called reflectively by nothing here, but keep the
# generated binding classes so stack traces stay readable.
-keep class com.drafts.compose.databinding.** { *; }

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
