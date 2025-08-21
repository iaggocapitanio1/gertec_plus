package com.example.gertec_plus
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import br.com.gertec.gedi.GEDI
import br.com.gertec.gedi.enums.*
import br.com.gertec.gedi.interfaces.*
import br.com.gertec.gedi.structs.*
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import android.os.Handler
import android.os.Looper
import java.util.Locale

private const val INIT_COOLDOWN_MS = 2000L

private fun isEmulator(): Boolean {
    val fp = Build.FINGERPRINT.lowercase()
    val model = Build.MODEL.lowercase()
    val product = Build.PRODUCT.lowercase()
    val brand = Build.BRAND.lowercase()
    val device = Build.DEVICE.lowercase()
    val hw = Build.HARDWARE.lowercase()
    return fp.contains("generic") || fp.contains("emulator") ||
            model.contains("emulator") || model.contains("android sdk built for x86") ||
            product.contains("sdk") || product.contains("sdk_gphone") ||
            brand.startsWith("generic") || device.startsWith("generic") ||
            hw.contains("goldfish") || hw.contains("ranchu") || hw.contains("qemu")
}



private fun tryInitGedi(ctx: Context): IGEDI? =
    runCatching { GEDI.getInstance(ctx) }.getOrNull()
        ?: runCatching { GEDI.init(ctx); GEDI.getInstance() }.getOrNull()

private fun scaleToWidth(src: Bitmap, maxW: Int): Bitmap {
    if (src.width <= maxW) return src
    val ratio = maxW.toFloat() / src.width.toFloat()
    val h = (src.height * ratio).toInt()
    return Bitmap.createScaledBitmap(src, maxW, h, true)
}

private fun printBitmap(gedi: IGEDI, bm: Bitmap, align: GEDI_PRNTR_e_Alignment = GEDI_PRNTR_e_Alignment.CENTER) {
    val p = gedi.getPRNTR()
    val b = if (bm.config != Bitmap.Config.ARGB_8888) bm.copy(Bitmap.Config.ARGB_8888, false) else bm
    val cfg = GEDI_PRNTR_st_PictureConfig().apply {
        alignment = align
        width = b.width
        height = b.height
        offset = 0
    }
    p.Init()
    p.DrawPictureExt(cfg, b)
    p.DrawBlankLine(9)
    p.Output()
}

class GertecPlusPlugin :
    FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {


    private val main = Handler(Looper.getMainLooper())
    private lateinit var channel: MethodChannel
    private var activity: Activity? = null
    private var appCtx: Context? = null
    private var engineBinding: FlutterPlugin.FlutterPluginBinding? = null
    @Volatile private var gedi: IGEDI? = null
    private val exec = java.util.concurrent.Executors.newSingleThreadExecutor()
    @Volatile private var initStarted = false
    @Volatile private var lastInitAt = 0L


    override fun onAttachedToEngine(b: FlutterPlugin.FlutterPluginBinding) {
        engineBinding = b
        channel = MethodChannel(b.binaryMessenger, "gertec_plus")
        channel.setMethodCallHandler(this)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        appCtx = binding.activity.applicationContext
        // Do NOT init here. Avoid cold start freeze.
    }

    override fun onDetachedFromActivityForConfigChanges() { activity = null }
    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) = onAttachedToActivity(binding)
    override fun onDetachedFromActivity() {
        activity = null
        gedi = null
        initStarted = false
        lastInitAt = 0L
    }



    private fun startInitIfNeeded() {
        if (initStarted || appCtx == null) return
        if (SystemClock.uptimeMillis() - lastInitAt < INIT_COOLDOWN_MS) return
        if (isEmulator()) return
        initStarted = true
        lastInitAt = SystemClock.uptimeMillis()
        exec.submit {
            gedi = tryInitGedi(appCtx!!)
            initStarted = false
        }
    }

    private inline fun <T> ioCall(
        crossinline block: (IGEDI) -> T,
        result: MethodChannel.Result
    ) {
        val g = gedi
        if (g == null) { startInitIfNeeded(); result.error("GEDI_INIT","GEDI not ready. Try again shortly.",null); return }
        exec.submit {
            try {
                val out = block(g)
                main.post { result.success(out) }
            } catch (e: Exception) {
                main.post { result.error("GEDI_ERROR", e.message, null) }
            }
        }
    }


    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        try {
            when (call.method) {
                "warmup" -> { startInitIfNeeded(); result.success(true) }
                // INFO
                "platformVersion" -> result.success("Android ${Build.VERSION.RELEASE}")
                "info.getSN" ->
                    ioCall({ it.getINFO().ControlNumberGet(GEDI_INFO_e_ControlNumber.SN) }, result)

                // IMAGEM
                "prn.image" -> ioCall({ g ->
                    val act = activity ?: error("NO_ACTIVITY")
                    val maxW = call.argument<Int>("maxWidth") ?: 384
                    val align = call.argument<String>("align")?.let(GEDI_PRNTR_e_Alignment::valueOf)
                        ?: GEDI_PRNTR_e_Alignment.CENTER
                    val source = call.argument<String>("source") ?: "bytes"
                    val bm: Bitmap = when (source) {
                        "drawable" -> {
                            val name = call.argument<String>("name") ?: error("BAD_ARGS name")
                            val id = act.resources.getIdentifier(name, "drawable", act.packageName)
                            if (id == 0) error("NOT_FOUND $name")
                            BitmapFactory.decodeResource(act.resources, id)
                        }
                        "asset" -> {
                            val assetKey = call.argument<String>("asset") ?: error("BAD_ARGS asset")
                            val path = engineBinding?.flutterAssets?.getAssetFilePathByName(assetKey)
                                ?: error("NO_ENGINE")

                            act.assets.open(path).use(BitmapFactory::decodeStream)
                        }
                        "bytes" -> {
                            val bytes = call.argument<ByteArray>("bytes") ?: error("BAD_ARGS bytes")
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        }
                        "file" -> {
                            val path = call.argument<String>("path") ?: error("BAD_ARGS path")
                            BitmapFactory.decodeFile(path)
                        }
                        else -> error("BAD_ARGS source")
                    }
                    val scaled = scaleToWidth(bm, maxW)
                    printBitmap(g, scaled, align)
                    if (scaled != bm && !bm.isRecycled) bm.recycle()
                    true
                }, result)

                // PRINTER

                "prn.status" ->
                    ioCall({ it.getPRNTR().Status().name }, result)
                "prn.paperUsage" -> ioCall({ it.getPRNTR().GetPaperUsage() }, result)
                "prn.resetPaper" -> ioCall({ it.getPRNTR().ResetPaperUsage(); true }, result)
                "prn.text" -> ioCall({
                    val p = it.getPRNTR()
                    p.Init()
                    val paint = Paint().apply { textSize = 10f }
                    val cfg = GEDI_PRNTR_st_StringConfig().apply { lineSpace = 1; offset = 1; this.paint = paint }
                    p.DrawStringExt(cfg, call.argument<String>("text") ?: "")
                    true
                }, result)

                "prn.blank" -> ioCall({
                    val p = it.getPRNTR()
                    p.Init()
                    p.DrawBlankLine(call.argument<Int>("h") ?: 9); true
                }, result)

                "prn.barcode" -> ioCall({
                    val p = it.getPRNTR()
                    p.Init()
                    val type = GEDI_PRNTR_e_BarCodeType.valueOf(call.argument<String>("type")!!)
                    val data = call.argument<String>("data") ?: "TEXTO"
                    val cfg = GEDI_PRNTR_st_BarCodeConfig().apply {
                        barCodeType = type; height = 100; width = if (type == GEDI_PRNTR_e_BarCodeType.QR_CODE) 150 else 100
                    }
                    p.DrawBarCode(cfg, data); true
                }, result)

                "prn.output" -> ioCall({ it.getPRNTR().Output(); true }, result)

                "audio.beep" -> ioCall({ it.getAUDIO().Beep(); true }, result)

                "led.set" -> ioCall({
                    val id = GEDI_LED_e_Id.valueOf(call.argument<String>("id")!!)
                    val on = call.argument<Boolean>("on") ?: false
                    it.getLED().Set(id, on); true
                }, result)

                "clock.rtc" -> ioCall({
                    val rtc = it.getCLOCK().RTCFGet()
                    mapOf("hour" to rtc.bHour.toInt(), "minute" to rtc.bMinute.toInt(), "second" to rtc.bSecond.toInt(),
                        "day" to rtc.bDay.toInt(), "month" to rtc.bMonth.toInt(), "year" to rtc.bYear.toInt(), "dow" to rtc.bDoW.toInt())
                }, result)

                "cl.powerOn"  -> ioCall({ it.getCL().PowerOn(); true }, result)
                "cl.powerOff" -> ioCall({ it.getCL().PowerOff(); true }, result)
                "cl.isoPolling" -> ioCall({
                    val info = it.getCL().ISO_Polling(call.argument<Int>("timeoutMs") ?: 3000)
                    val uid = info.abUID.joinToString("") { b -> String.format(Locale.US, "%02X", b) }
                    mapOf("type" to info.peType.name, "uidHex" to uid)
                }, result)

                "smart.status" -> ioCall({
                    val slot = GEDI_SMART_e_Slot.valueOf(call.argument<String>("slot")!!)
                    it.getSMART().Status(slot).name
                }, result)
                "smart.powerOff" -> ioCall({
                    val slot = GEDI_SMART_e_Slot.valueOf(call.argument<String>("slot")!!)
                    it.getSMART().PowerOff(slot); true
                }, result)

                "msr.read" -> ioCall({
                    val t = it.getMSR().Read()
                    fun hx(b: ByteArray?) = b?.joinToString("") { x -> String.format("%02X", x) } ?: ""
                    mapOf("tk1" to hx(t.abTk1Buf), "tk2" to hx(t.abTk2Buf), "tk3" to hx(t.abTk3Buf))
                }, result)

                "prn.demo" -> ioCall({
                    val p = it.getPRNTR()
                    val st = p.Status()
                    if (st != GEDI_PRNTR_e_Status.OK) return@ioCall "status=$st"
                    p.Init()
                    val paint = Paint().apply { textSize = 10f }
                    val str = GEDI_PRNTR_st_StringConfig().apply { lineSpace = 1; offset = 1; this.paint = paint }
                    p.DrawStringExt(str, "TEXTO"); p.DrawBlankLine(9); p.Output()
                    p.Init()
                    p.DrawStringExt(str, "PDF_417")
                    p.DrawBarCode(GEDI_PRNTR_st_BarCodeConfig().apply { barCodeType = GEDI_PRNTR_e_BarCodeType.PDF_417; height = 100; width = 100 }, "TEXTO")
                    p.DrawBlankLine(9)
                    p.DrawStringExt(str, "QR_CODE")
                    p.DrawBarCode(GEDI_PRNTR_st_BarCodeConfig().apply { barCodeType = GEDI_PRNTR_e_BarCodeType.QR_CODE; height = 100; width = 150 }, "TEXTO")
                    p.DrawBlankLine(9)
                    p.Output()
                    val before = runCatching { p.GetPaperUsage() }.getOrNull()
                    runCatching { p.ResetPaperUsage() }
                    val after = runCatching { p.GetPaperUsage() }.getOrNull()
                    "status=$st; paperBefore=$before; paperAfter=$after"
                }, result)


                else -> result.notImplemented()
            }
        } catch (e: Exception) {
            result.error("GEDI_ERROR", e.message, null)
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        engineBinding = null
        channel.setMethodCallHandler(null)
        gedi = null
        exec.shutdownNow()
    }
}