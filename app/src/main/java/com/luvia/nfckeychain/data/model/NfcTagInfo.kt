package com.luvia.nfckeychain.data.model

data class NfcTagInfo(
    val tagId: String,
    val tagType: String,
    val technologies: List<String>,
    val maxTransceiveLength: Int,
    val isWritable: Boolean,
    val ndefMessage: String? = null,
    val rawData: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NfcTagInfo

        if (tagId != other.tagId) return false
        if (tagType != other.tagType) return false
        if (technologies != other.technologies) return false
        if (maxTransceiveLength != other.maxTransceiveLength) return false
        if (isWritable != other.isWritable) return false
        if (ndefMessage != other.ndefMessage) return false
        if (rawData != null) {
            if (other.rawData == null) return false
            if (!rawData.contentEquals(other.rawData)) return false
        } else if (other.rawData != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = tagId.hashCode()
        result = 31 * result + tagType.hashCode()
        result = 31 * result + technologies.hashCode()
        result = 31 * result + maxTransceiveLength
        result = 31 * result + isWritable.hashCode()
        result = 31 * result + (ndefMessage?.hashCode() ?: 0)
        result = 31 * result + (rawData?.contentHashCode() ?: 0)
        return result
    }
}
