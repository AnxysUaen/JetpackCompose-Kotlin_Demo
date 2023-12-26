package top.whscat.elevator

import android.graphics.Bitmap
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.journeyapps.barcodescanner.BarcodeEncoder
import top.whscat.elevator.ui.theme.ElevatorTheme
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class MainActivity : ComponentActivity() {
    private val viewModel: QRCodeViewModel by viewModels()
    override fun onStart() {
        super.onStart()
        setContent {
            ElevatorTheme {
                QRCodeView(viewModel)
            }
        }
    }
}

class QRCodeViewModel : ViewModel() {
    private val _content = MutableLiveData("init")
    val content: LiveData<String> = _content

    fun updateContent(newContent: String) {
        _content.value = newContent
    }
}

@Composable
fun QRCodeView(viewModel: QRCodeViewModel) {
    val content by viewModel.content.observeAsState(getCipherText())
    val bitmap = remember(content) { generateQRCode(content) }
    // 当从后台切换至前台时触发重新绘制二维码
    viewModel.updateContent(getCipherText())
    QRCodeView(bitmap)
}

@Composable
fun QRCodeView(bitmap: Bitmap) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        AndroidView(factory = {
            val imageView = ImageView(it).apply {
                setImageBitmap(bitmap)
            }
            imageView
        },
            update = {
                it.setImageBitmap(bitmap)
            },
            modifier = Modifier.size(200.dp).padding(16.dp)
        )
    }
}

private fun generateQRCode(content: String): Bitmap {
    val hints = hashMapOf<EncodeHintType, Any>()
    hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512, hints)
    val barcodeEncoder = BarcodeEncoder()
    return barcodeEncoder.createBitmap(bitMatrix)
}

@Composable
private fun getCipherText(): String {
    val digest = MessageDigest.getInstance("SHA-256")

    val levelWords = intArrayOf(103,111,103,101,110,105,117,115)
    val level = digest.digest(levelWords.map { it.toByte() }.toByteArray())

    val ivWords = intArrayOf(103,111,103,101,110,105,117,115,49,50,51,52,53,54,55,56)
    val iv = ivWords.map { it.toByte() }.toByteArray()

    val timeKey = DateTimeFormatter.ofPattern("yyMMddHHmmss").format(LocalDateTime.now())
    val publicStr = """01${timeKey}0057996b4072b611ed4836069bb4abca0e2641597300000000"""

    val cipher = Cipher.getInstance("AES/CBC/NoPadding")
    val keySpec = SecretKeySpec(level, "AES")
    val ivSpec = IvParameterSpec(iv)

    cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
    val encryptedBytes = cipher.doFinal(publicStr.chunked(2).map { it.toInt(16).toByte() }.toByteArray())
    return Base64.getEncoder().encodeToString(encryptedBytes)
}

@Composable
fun QRCodeView(content: String) {
    val bitmap = generateQRCode(content)
    QRCodeView(bitmap)
}

@Preview
@Composable
fun PreviewQRCodeView() {
    MaterialTheme {
        QRCodeView(getCipherText())
    }
}