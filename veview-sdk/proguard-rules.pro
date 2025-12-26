# Keep all public classes and members of the SDK's public API
-keep public class com.veview.veview_sdk.presentation.VeViewSDK { *; }
-keep public class com.veview.veview_sdk.domain.reviewer.VoiceReviewer { *; }
-keep public class com.veview.veview_sdk.configs.** { *; }
-keep public class com.veview.veview_sdk.model.** { *; }

# If your analysis engine uses a library like Gson/Moshi for parsing JSON,
# you must keep the data model classes that are used for serialization.
# For example:
# -keep class com.veview.veview_sdk.analysis.network.ApiResponseModel { *; }

# Keep the names of any methods annotated for use by libraries like Retrofit.
# (This is just an example, adapt as needed)
# -keepclassmembers interface * {
#     @retrofit2.http.* <methods>;
# }
