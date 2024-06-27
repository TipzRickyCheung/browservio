package tipz.viola.webview

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import tipz.viola.download.InternalDownloadProvider
import tipz.viola.utils.UrlUtils
import java.io.IOException

class VJavaScriptInterface(private val context: Context) {
    @JavascriptInterface
    @Throws(IOException::class)
    fun getBase64FromBlobData(uriString: String, mimeType: String) {
        Log.i(LOG_TAG, "getBase64FromBlobData(): mimeType=${mimeType}")
        InternalDownloadProvider.apply {
            byteArrayToFile(context, base64StringToByteArray(getRawDataFromDataUri(uriString)),
                UrlUtils.changeExtension("${System.currentTimeMillis()}.bin", mimeType))
        }
    }

    companion object {
        val LOG_TAG = "VJavaScriptInterface"
        val INTERFACE_NAME = "ViolaBrowser"

        /**
         * Method to convert blobUrl to Blob, then process Base64 data on native side
         *
         * 1. Download Blob URL as Blob object
         * 2. Convert Blob object to Base64 data
         * 3. Pass Base64 data to Android layer for processing
         */
        fun getBase64StringFromBlobUrl(blobUrl: String, mimeType: String): String {
            // Script to convert blob URL to Base64 data in Web layer, then process it in Android layer
            return "javascript: var xhr = new XMLHttpRequest();" +
                    "xhr.open('GET', '${blobUrl}', true);" +
                    "xhr.setRequestHeader('Content-type','${mimeType}');" +
                    "xhr.responseType = 'blob';" +
                    "xhr.onload = function(e) {" +
                    "if (this.status == 200) {" +
                    "var blobFile = this.response;" +
                    "var reader = new FileReader();" +
                    "reader.readAsDataURL(blobFile);" +
                    "reader.onloadend = function() {" +
                    "uriString = reader.result;" +
                    "${INTERFACE_NAME}.getBase64FromBlobData(uriString, '${mimeType}');" +
                    "}" +
                    "}" +
                    "};" +
                    "xhr.send();"
        }
    }
}