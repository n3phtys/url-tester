package de.usu.usickaag.urltester

data class CSVLine(val inputUrl: String, val temporaryUrl: String? = null, val httpResult: String? = null) {

    override fun toString() : String {
        return "$inputUrl;${temporaryUrl?:""};${(httpResult?:"").replace("\r", "").replace("\n", " ").replace("\t", " ").replace(";", ".,")}"
    }
}