package de.usu.usickaag.urltester

data class CSVLine(val inputUrl: String, val temporaryUrl: String? = null, val resultUrl: String? = null) {

    override fun toString() : String {
        return "$inputUrl;$temporaryUrl;$resultUrl"
    }
}