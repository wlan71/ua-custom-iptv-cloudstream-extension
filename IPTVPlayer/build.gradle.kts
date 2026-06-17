// use an integer for version numbers
version = 15

dependencies {
    val implementation by configurations
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
}

cloudstream {
    // All of these properties are optional, you can safely remove them
    language = "uk"
    description = "Ukrainian IPTV channels"
    authors = listOf("wlan71")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 1 // will be 3 if unspecified
    tvTypes = listOf(
        "Live",
    )

    iconUrl = "https://raw.githubusercontent.com/wlan71/ua-custom-iptv-cloudstream-extension/master/IPTVPlayer/icon.png"

}
