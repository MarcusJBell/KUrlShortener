package urlshortner

/**
 * Util class used to convert database id's to keys and back
 */
object UrlEncoderUtil {

    const val alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    const val base = alphabet.length

    /**
     * Creates a encoded string from the database id to be used as a key
     */
    fun encode(i: Int): String {
        var index = i
        if (index == 0) return alphabet[0].toString()
        return buildString {
            while (index > 0) {
                append(alphabet[index % base])
                index /= base
            }
        }.reversed()
    }

    /**
     * Converts key back into database id
     */
    fun decode(str: String): Int {
        var num = 0
        for (i in 0..str.length) {
            num = num * base + alphabet.indexOf(str[i])
        }
        return num
    }
}