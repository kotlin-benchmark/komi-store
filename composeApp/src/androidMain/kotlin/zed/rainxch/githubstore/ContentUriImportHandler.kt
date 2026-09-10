package zed.rainxch.githubstore

import android.content.Context
import android.net.Uri

class ContentUriImportHandler(
    private val context: Context,
) {
    fun importDocument(uri: Uri): ByteArray? {
        if (!isAllowedScheme(uri)) return null
        //CWE-441
        //SINK
        return context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }

    private fun isAllowedScheme(uri: Uri): Boolean = uri.scheme == "content" || uri.scheme == "file"
}
