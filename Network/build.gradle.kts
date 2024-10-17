import buildtypes.BuildTypeConfig
import flavors.CountryFlavor
import flavors.MarketFlavor
import flavors.SegmentFlavor

plugins {
    alias(libs.plugins.space.android.library)
}

buildConfig {
    flavors = setOf(CountryFlavor(), SegmentFlavor(), MarketFlavor())
    buildTypeConfig = BuildTypeConfig.Default
}

dependencies {
    implementation(libs.retrofitScalarConverter)
    implementation(libs.retrofit)
    implementation(libs.loggingInterceptor)
    implementation(libs.bundles.coroutines)
    implementation(libs.kotlinStdLib)
    implementation(libs.androidx.ktxCore)
    implementation(libs.koin)
    api(libs.gson)

    implementation(projects.spaceCore.common)
    api(projects.spaceCore.ui)

    testImplementation(projects.spaceCore.test)
    api(libs.test.pact) {
        exclude("org.apache.tika","tika-core")
    }
    api(projects.spaceCore.test)
}

android {
    namespace = getPropertyValue("NAME_SPACE")
}
