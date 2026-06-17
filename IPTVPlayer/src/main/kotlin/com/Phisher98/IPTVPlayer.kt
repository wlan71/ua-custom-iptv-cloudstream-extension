package com.phisher98

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.INFER_TYPE
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newDrmExtractorLink
import com.lagradost.cloudstream3.utils.newExtractorLink
import java.io.InputStream
import java.util.UUID


class IPTVPlayer(private val sharedPref: android.content.SharedPreferences? = null) : MainAPI() {
    private val defaultUrl = "https://iptv-org.github.io/iptv/countries/ua.m3u"
    override var lang = "uk"
    override var mainUrl = "https://iptv-org.github.io/iptv/countries/ua.m3u"
    override var name = "Ukraine IPTV"

    companion object {
        private var logoCache: Map<String, String>? = null

        private fun normalizeName(name: String): String {
            return name
                .replace(Regex("\\s+(UHD|FHD|FHD\\+|HD|SD|4K)(\\s+\\d+)?", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\s+\\+\\d+"), "")
                .replace(Regex("\\s+\\(.*?\\)"), "")
                .replace(Regex("\\s+(UK|US|RU|UA|DE|FR|NL|AL|PL|IT|ES|AT|BE|CZ|HU|RO|BG|TR|IN|AU|CA|AR|BR|SE|NO|DK|FI|GR|IL|SA|AE|JP|KR|CN)$", RegexOption.IGNORE_CASE), "")
                .replace("-", " ")
                .replace("!", "")
                .replace(Regex("\\s+"), " ")
                .trim()
                .lowercase()
        }

        suspend fun getLogoMap(): Map<String, String> {
            logoCache?.let { return it }
            return try {
                val m3u = app.get("https://iptv-org.github.io/iptv/countries/ua.m3u").text
                val playlist = IptvPlaylistParser().parseM3U(m3u)
                val map = mutableMapOf<String, String>()
                playlist.items.forEach { item ->
                    val logo = item.attributes["tvg-logo"]?.takeIf { it.isNotBlank() } ?: return@forEach
                    item.title?.takeIf { it.isNotBlank() }?.let { map[normalizeName(it)] = logo }
                    item.attributes["tvg-name"]?.takeIf { it.isNotBlank() }?.let { map[normalizeName(it)] = logo }
                }
                logoCache = map
                map
            } catch (e: Exception) {
                emptyMap()
            }
        }

        fun findLogo(channelName: String, logoMap: Map<String, String>): String? {
            val normalized = normalizeName(channelName)
            logoMap[normalized]?.let { return it }
            logoMap.entries.firstOrNull { normalized.contains(it.key) && it.key.length > 3 }?.let { return it.value }
            return null
        }
    }

    private fun getPlaylistUrl(): String {
        val custom = sharedPref?.getString("playlist_url", "")?.trim()
        return if (!custom.isNullOrBlank()) custom else defaultUrl
    }
    override val hasMainPage = true
    override val hasChromecastSupport = true
    override val supportedTypes = setOf(
        TvType.Live,
    )

    override suspend fun getMainPage(
        page: Int,
        request : MainPageRequest
    ): HomePageResponse {
        val data = IptvPlaylistParser().parseM3U(app.get(getPlaylistUrl()).text)
        val logoMap = getLogoMap()
        return newHomePageResponse(data.items.distinctBy { it.title }.groupBy{it.attributes["group-title"]}.map { group ->
            val title = group.key ?: ""
            val show = group.value.map { channel ->
                val streamurl = channel.url ?: ""
                val channelname = channel.title ?: ""
                val posterurl = channel.attributes["tvg-logo"]?.takeIf { it.isNotBlank() }
                    ?: findLogo(channelname, logoMap)
                val nation = channel.attributes["group-title"] ?: ""
                val key = channel.attributes["key"] ?: ""
                val keyid = channel.attributes["keyid"] ?: ""
                newLiveSearchResponse(channelname, LoadData(streamurl, channelname, posterurl ?: "", nation, key, keyid).toJson(), TvType.Live)
                {
                    this.posterUrl = posterurl
                    this.lang = nation
                }
            }
            HomePageList(
                title,
                show,
                isHorizontalImages = true
            )
        })
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val data = IptvPlaylistParser().parseM3U(app.get(getPlaylistUrl()).text)
        val logoMap = getLogoMap()
        return data.items.distinctBy { it.title }.filter { it.title?.contains(query, ignoreCase = true) ?: false }.map { channel ->
                val streamurl = channel.url ?: ""
                val channelname = channel.title ?: ""
                val posterurl = channel.attributes["tvg-logo"]?.takeIf { it.isNotBlank() }
                    ?: findLogo(channelname, logoMap)
                val nation = channel.attributes["group-title"] ?: ""
                val key = channel.attributes["key"] ?: ""
                val keyid = channel.attributes["keyid"] ?: ""
            newLiveSearchResponse(channelname, LoadData(streamurl, channelname, posterurl ?: "", nation, key, keyid).toJson(), TvType.Live)
            {
                this.posterUrl = posterurl
                this.lang = nation
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val data = if (url.trimStart().startsWith("{")) {
            parseJson<LoadData>(url)
        } else {
            val playlist = IptvPlaylistParser().parseM3U(app.get(getPlaylistUrl()).text)
            val logoMap = getLogoMap()
            val channel = playlist.items.firstOrNull { it.url == url }
            if (channel != null) {
                val channelname = channel.title ?: ""
                val posterurl = channel.attributes["tvg-logo"]?.takeIf { it.isNotBlank() }
                    ?: findLogo(channelname, logoMap)
                LoadData(url, channelname, posterurl ?: "", channel.attributes["group-title"] ?: "", channel.attributes["key"] ?: "", channel.attributes["keyid"] ?: "")
            } else {
                LoadData(url, "", "", "", "", "")
            }
        }
        val dataJson = if (url.trimStart().startsWith("{")) url else data.toJson()
        return newLiveStreamLoadResponse(data.title, data.url, dataJson) {
            this.posterUrl = data.poster
            this.plot = data.nation
        }
    }
    data class LoadData(
        val url: String,
        val title: String,
        val poster: String,
        val nation: String,
        val key: String,
        val keyid: String,
    )
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val loadData = parseJson<LoadData>(data)
        if (loadData.url.contains("mpd"))
        {
            callback.invoke(
                newDrmExtractorLink(
                    this.name,
                    this.name,
                    loadData.url,
                    INFER_TYPE,
                    UUID.randomUUID()
                )
                {
                    this.quality=Qualities.Unknown.value
                    this.key=loadData.key.trim()
                    this.kid=loadData.keyid.trim()
                }
            )
        }
            else
        if(loadData.url.contains("&e=.m3u"))
            {
                callback.invoke(
                    newExtractorLink(
                        this.name,
                        this.name,
                        url = loadData.url,
                        ExtractorLinkType.M3U8
                    ) {
                        this.referer = ""
                        this.quality = Qualities.Unknown.value
                    }
                )

            }
        else
        {
            callback.invoke(
                newExtractorLink(
                    this.name,
                    loadData.title,
                    url = loadData.url,
                    INFER_TYPE
                ) {
                    this.referer = ""
                    this.quality = Qualities.Unknown.value
                }
            )

        }
        return true
    }
}


data class Playlist(
    val items: List<PlaylistItem> = emptyList(),
)

data class PlaylistItem(
    val title: String? = null,
    val attributes: Map<String, String> = emptyMap(),
    val headers: Map<String, String> = emptyMap(),
    val url: String? = null,
    val userAgent: String? = null,
    val key: String? = null,
    val keyid: String? = null,
)


class IptvPlaylistParser {


    /**
     * Parse M3U8 string into [Playlist]
     *
     * @param content M3U8 content string.
     * @throws PlaylistParserException if an error occurs.
     */
    fun parseM3U(content: String): Playlist {
        return parseM3U(content.byteInputStream())
    }

    /**
     * Parse M3U8 content [InputStream] into [Playlist]
     *
     * @param input Stream of input data.
     * @throws PlaylistParserException if an error occurs.
     */
    @Throws(PlaylistParserException::class)
    fun parseM3U(input: InputStream): Playlist {
        val reader = input.bufferedReader()

        if (!reader.readLine().isExtendedM3u()) {
            throw PlaylistParserException.InvalidHeader()
        }

        val playlistItems = mutableListOf<PlaylistItem>()

        var currentTitle: String? = null
        var currentAttributes: Map<String, String> = emptyMap()
        var currentUserAgent: String? = null
        var currentReferrer: String? = null
        var currentHeaders: Map<String, String> = emptyMap()
        var currentGroup: String? = null

        reader.forEachLine { line ->
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) return@forEachLine

            when {
                trimmedLine.startsWith(EXT_INF) -> {
                    currentTitle = trimmedLine.getTitle()
                    currentAttributes = trimmedLine.getAttributes()
                    currentUserAgent = null
                    currentReferrer = null
                    currentHeaders = emptyMap()
                }

                trimmedLine.startsWith(EXT_GRP) -> {
                    currentGroup = trimmedLine.removePrefix(EXT_GRP).removePrefix(":").trim()
                }

                trimmedLine.startsWith(EXT_VLC_OPT) -> {
                    val userAgent = trimmedLine.getTagValue("http-user-agent")
                    val referrer = trimmedLine.getTagValue("http-referrer")
                    currentUserAgent = userAgent ?: currentUserAgent
                    currentReferrer = referrer ?: currentReferrer
                    if (currentReferrer != null) {
                        currentHeaders = currentHeaders + mapOf("referrer" to currentReferrer!!)
                    }
                }

                !trimmedLine.startsWith("#") -> {
                    val url = trimmedLine.getUrl()
                    val uaParam = trimmedLine.getUrlParameter("user-agent")
                    val refParam = trimmedLine.getUrlParameter("referer")
                    val key = trimmedLine.getUrlParameter("key")
                    val keyid = trimmedLine.getUrlParameter("keyid")

                    val combinedUserAgent = uaParam ?: currentUserAgent
                    val ref = refParam ?: currentReferrer

                    val urlHeaders = if (ref != null) {
                        currentHeaders + mapOf("referrer" to ref)
                    } else currentHeaders

                    if (currentTitle != null) {
                        val attrs = if (currentAttributes["group-title"].isNullOrBlank() && currentGroup != null) {
                            currentAttributes + mapOf("group-title" to currentGroup!!)
                        } else currentAttributes
                        playlistItems.add(
                            PlaylistItem(
                                currentTitle ?: "",
                                attributes = attrs,
                                url = url,
                                userAgent = combinedUserAgent,
                                headers = urlHeaders,
                                key = key,
                                keyid = keyid
                            )
                        )
                    }

                    currentTitle = null
                    currentAttributes = emptyMap()
                    currentUserAgent = null
                    currentReferrer = null
                    currentHeaders = emptyMap()
                    currentGroup = null
                }
            }
        }

        return Playlist(playlistItems)
    }
    /**
     * Replace "" (quotes) from given string.
     */
    private fun String.replaceQuotesAndTrim(): String {
        return replace("\"", "").trim()
    }

    /**
     * Check if given content is valid M3U8 playlist.
     */
    private fun String.isExtendedM3u(): Boolean = startsWith(EXT_M3U)

    /**
     * Get title of media.
     *
     * Example:-
     *
     * Input:
     * ```
     * #EXTINF:-1 tvg-id="1234" group-title="Kids" tvg-logo="url/to/logo", Title
     * ```
     * Result: Title
     */
    private fun String.getTitle(): String? {
        return split(",").lastOrNull()?.replaceQuotesAndTrim()
    }

    /**
     * Get media url.
     *
     * Example:-
     *
     * Input:
     * ```
     * https://example.com/sample.m3u8|user-agent="Custom"
     * ```
     * Result: https://example.com/sample.m3u8
     */
    private fun String.getUrl(): String? {
        return split("|").firstOrNull()?.replaceQuotesAndTrim()
    }

    /**
     * Get url parameters.
     *
     * Example:-
     *
     * Input:
     * ```
     * http://192.54.104.122:8080/d/abcdef/video.mp4|User-Agent=Mozilla&Referer=CustomReferrer
     * ```
     * Result will be equivalent to kotlin map:
     * ```Kotlin
     * mapOf(
     *   "User-Agent" to "Mozilla",
     *   "Referer" to "CustomReferrer"
     * )
     * ```
     */
  /*  private fun String.getUrlParameters(): Map<String, String> {
        val urlRegex = Regex("^(.*)\\|", RegexOption.IGNORE_CASE)
        val headersString = replace(urlRegex, "").replaceQuotesAndTrim()
        return headersString.split("&").mapNotNull {
            val pair = it.split("=")
            if (pair.size == 2) pair.first() to pair.last() else null
        }.toMap()
    }

   */

    /**
     * Get url parameter with key.
     *
     * Example:-
     *
     * Input:
     * ```
     * http://192.54.104.122:8080/d/abcdef/video.mp4|User-Agent=Mozilla&Referer=CustomReferrer
     * ```
     * If given key is `user-agent`, then
     *
     * Result: Mozilla
     */
    private fun String.getUrlParameter(key: String): String? {
        val urlRegex = Regex("^(.*)\\|", RegexOption.IGNORE_CASE)
        val keyRegex = Regex("$key=(\\w[^&]*)", RegexOption.IGNORE_CASE)
        val paramsString = replace(urlRegex, "").replaceQuotesAndTrim()
        return keyRegex.find(paramsString)?.groups?.get(1)?.value
    }

    /**
     * Get attributes from `#EXTINF` tag as Map<String, String>.
     *
     * Example:-
     *
     * Input:
     * ```
     * #EXTINF:-1 tvg-id="1234" group-title="Kids" tvg-logo="url/to/logo", Title
     * ```
     * Result will be equivalent to kotlin map:
     * ```Kotlin
     * mapOf(
     *   "tvg-id" to "1234",
     *   "group-title" to "Kids",
     *   "tvg-logo" to "url/to/logo"
     *)
     * ```
     */
    private fun String.getAttributes(): Map<String, String> {
        val extInfRegex = Regex("(#EXTINF:.?[0-9]+)", RegexOption.IGNORE_CASE)
        val attributesString = replace(extInfRegex, "").replaceQuotesAndTrim().split(",").first()
        return attributesString.split(Regex("\\s")).mapNotNull {
            val pair = it.split("=")
            if (pair.size == 2) pair.first() to pair.last()
                .replaceQuotesAndTrim() else null
        }.toMap()
    }

    /**
     * Get value from a tag.
     *
     * Example:-
     *
     * Input:
     * ```
     * #EXTVLCOPT:http-referrer=http://example.com/
     * ```
     * Result: http://example.com/
     */
    private fun String.getTagValue(key: String): String? {
        val keyRegex = Regex("$key=(.*)", RegexOption.IGNORE_CASE)
        return keyRegex.find(this)?.groups?.get(1)?.value?.replaceQuotesAndTrim()
    }

    companion object {
        const val EXT_M3U = "#EXTM3U"
        const val EXT_INF = "#EXTINF"
        const val EXT_GRP = "#EXTGRP"
        const val EXT_VLC_OPT = "#EXTVLCOPT"
    }

}

/**
 * Exception thrown when an error occurs while parsing playlist.
 */
sealed class PlaylistParserException(message: String) : Exception(message) {

    /**
     * Exception thrown if given file content is not valid.
     */
    class InvalidHeader :
        PlaylistParserException("Invalid file header. Header doesn't start with #EXTM3U")

}
