# R8 rules for the release build.
# AndroidX, Compose, Hilt and coroutines ship their own consumer rules. Add a rule here only for
# code R8 cannot see being used (reflection, serialization), with a comment saying why.

# Keep line numbers for readable crash stack traces, without the original source file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
