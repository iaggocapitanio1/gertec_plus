# Mantenha apenas o SDK
-keep class br.com.gertec.** { *; }

# Okapi/desktop e AWT: não avise e deixe o R8 remover
-dontwarn uk.org.okapibarcode.**
-dontwarn java.awt.**
-dontwarn javax.print.**

# SLF4J
-keep class org.slf4j.** { *; }
-dontwarn org.slf4j.**