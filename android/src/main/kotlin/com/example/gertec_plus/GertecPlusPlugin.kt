package com.example.gertec_plus
import android.content.Context
import android.os.Build

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

import java.util.Locale


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

private fun isGertecHint(): Boolean =
    Build.MANUFACTURER.equals("Gertec", true) ||
            Build.BRAND.equals("Gertec", true) ||
            Build.MODEL.startsWith("GPOS", true)

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

    private lateinit var channel: MethodChannel
    private var activity: Activity? = null
    private var engineBinding: FlutterPlugin.FlutterPluginBinding? = null
    private var gedi: IGEDI? = null

    override fun onAttachedToEngine(b: FlutterPlugin.FlutterPluginBinding) {
        engineBinding = b
        channel = MethodChannel(b.binaryMessenger, "gertec_plus")
        channel.setMethodCallHandler(this)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        val ctx = binding.activity.applicationContext
        if (isEmulator()) { gedi = null; return }
        gedi = tryInitGedi(ctx)
    }




    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) =
        onAttachedToActivity(binding)

    override fun onDetachedFromActivityForConfigChanges() { activity = null }

    override fun onDetachedFromActivity() {
        activity = null
        // SDK não expõe release estático. Apenas solte a referência.
        gedi = null
    }


    private fun requireGedi(result: MethodChannel.Result): IGEDI? {
        val g = gedi
        if (g == null) {
            result.error("GEDI_NOT_AVAILABLE", "Runtime GEDI não disponível neste dispositivo.", null)
            return null
        }
        return g
    }


    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        try {
            when (call.method) {

                // INFO
                "info.getSN" -> {
                    val g = requireGedi(result) ?: return
                    val sn = g.getINFO().ControlNumberGet(GEDI_INFO_e_ControlNumber.SN)
                    result.success(sn)
                }

                // IMAGEM
                "prn.image" -> {
                    val g = requireGedi(result) ?: return
                    val maxW = call.argument<Int>("maxWidth") ?: 384
                    val align = call.argument<String>("align")?.let { GEDI_PRNTR_e_Alignment.valueOf(it) }
                        ?: GEDI_PRNTR_e_Alignment.CENTER
                    val act = activity ?: return result.error("NO_ACTIVITY", "Plugin not attached to an Activity", null)
                    val source = call.argument<String>("source") ?: "bytes"

                    val bitmap: Bitmap? = when (source) {
                        "drawable" -> {
                            val name = call.argument<String>("name") ?: return result.error("BAD_ARGS", "name required", null)
                            val res = act.resources
                            val id = res.getIdentifier(name, "drawable", act.packageName)
                            if (id == 0) return result.error("NOT_FOUND", "drawable $name not found", null)
                            BitmapFactory.decodeResource(res, id)
                        }
                        "asset" -> {
                            val assetKey = call.argument<String>("asset") ?: return result.error("BAD_ARGS", "asset required", null)
                            val path = (engineBinding?.flutterAssets
                                ?: return result.error("NO_ENGINE", "Flutter engine binding missing", null)
                                    ).getAssetFilePathByName(assetKey)
                            act.applicationContext.assets.open(path).use(BitmapFactory::decodeStream)
                        }
                        "bytes" -> {
                            val bytes = call.argument<ByteArray>("bytes") ?: return result.error("BAD_ARGS", "bytes required", null)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        }
                        "file" -> {
                            val path = call.argument<String>("path") ?: return result.error("BAD_ARGS", "path required", null)
                            BitmapFactory.decodeFile(path)
                        }
                        else -> return result.error("BAD_ARGS", "unknown source $source", null)
                    }

                    if (bitmap == null) return result.error("DECODE_FAIL", "bitmap decode failed", null)
                    val scaled = scaleToWidth(bitmap, maxW)
                    printBitmap(g, scaled, align)
                    if (scaled != bitmap && !bitmap.isRecycled) bitmap.recycle()
                    result.success(true)
                }

                // PRINTER
                "prn.status" -> { val g = requireGedi(result) ?: return; result.success(g.getPRNTR().Status().name) }
                "prn.paperUsage" -> { val g = requireGedi(result) ?: return; result.success(g.getPRNTR().GetPaperUsage()) }
                "prn.resetPaper" -> { val g = requireGedi(result) ?: return; g.getPRNTR().ResetPaperUsage(); result.success(true) }
                "prn.text" -> {
                    val g = requireGedi(result) ?: return
                    val text = call.argument<String>("text") ?: ""
                    val paint = Paint().apply { textSize = 10f }
                    val cfg = GEDI_PRNTR_st_StringConfig().apply { lineSpace = 1; offset = 1; this.paint = paint }
                    g.getPRNTR().DrawStringExt(cfg, text)
                    result.success(true)
                }
                "prn.blank" -> { val g = requireGedi(result) ?: return; g.getPRNTR().DrawBlankLine(call.argument<Int>("h") ?: 9); result.success(true) }
                "prn.barcode" -> {
                    val g = requireGedi(result) ?: return
                    val type = GEDI_PRNTR_e_BarCodeType.valueOf(call.argument<String>("type")!!)
                    val data = call.argument<String>("data") ?: "TEXTO"
                    val cfg = GEDI_PRNTR_st_BarCodeConfig().apply {
                        barCodeType = type; height = if (type == GEDI_PRNTR_e_BarCodeType.QR_CODE) 100 else 100
                        width = if (type == GEDI_PRNTR_e_BarCodeType.QR_CODE) 150 else 100
                    }
                    g.getPRNTR().DrawBarCode(cfg, data)
                    result.success(true)
                }
                "prn.output" -> { val g = requireGedi(result) ?: return; g.getPRNTR().Output(); result.success(true) }

                // AUDIO
                "audio.beep" -> { val g = requireGedi(result) ?: return; g.getAUDIO().Beep(); result.success(true) }

                // LED
                "led.set" -> {
                    val g = requireGedi(result) ?: return
                    val id = GEDI_LED_e_Id.valueOf(call.argument<String>("id")!!)
                    val on = call.argument<Boolean>("on") ?: false
                    g.getLED().Set(id, on); result.success(true)
                }

                // CLOCK
                "clock.rtc" -> {
                    val g = requireGedi(result) ?: return
                    val rtc = g.getCLOCK().RTCFGet()
                    result.success(mapOf(
                        "hour" to rtc.bHour.toInt(), "minute" to rtc.bMinute.toInt(), "second" to rtc.bSecond.toInt(),
                        "day" to rtc.bDay.toInt(), "month" to rtc.bMonth.toInt(), "year" to rtc.bYear.toInt(), "dow" to rtc.bDoW.toInt()
                    ))
                }

                // CONTACTLESS
                "cl.powerOn" -> { val g = requireGedi(result) ?: return; g.getCL().PowerOn(); result.success(true) }
                "cl.powerOff" -> { val g = requireGedi(result) ?: return; g.getCL().PowerOff(); result.success(true) }
                "cl.isoPolling" -> {
                    val g = requireGedi(result) ?: return
                    val info = g.getCL().ISO_Polling(call.argument<Int>("timeoutMs") ?: 3000)
                    val uid = info.abUID.joinToString("") { String.format(Locale.US, "%02X", it) }
                    result.success(mapOf("type" to info.peType.name, "uidHex" to uid))
                }

                // SMART
                "smart.status" -> { val g = requireGedi(result) ?: return; val slot = GEDI_SMART_e_Slot.valueOf(call.argument<String>("slot")!!); result.success(g.getSMART().Status(slot).name) }
                "smart.powerOff" -> { val g = requireGedi(result) ?: return; val slot = GEDI_SMART_e_Slot.valueOf(call.argument<String>("slot")!!); g.getSMART().PowerOff(slot); result.success(true) }

                // MSR
                "msr.read" -> {
                    val g = requireGedi(result) ?: return
                    val t = g.getMSR().Read()
                    fun bytesToHex(b: ByteArray?) = b?.joinToString("") { String.format("%02X", it) } ?: ""
                    result.success(mapOf("tk1" to bytesToHex(t.abTk1Buf), "tk2" to bytesToHex(t.abTk2Buf), "tk3" to bytesToHex(t.abTk3Buf)))
                }

                // DEMO
                "prn.demo" -> {
                    val g = requireGedi(result) ?: return
                    val p = g.getPRNTR()
                    val st = p.Status()
                    if (st != GEDI_PRNTR_e_Status.OK) { result.success("status=$st"); return }
                    p.Init()
                    val paint = Paint().apply { textSize = 10f }
                    val strCfg = GEDI_PRNTR_st_StringConfig().apply { lineSpace = 1; offset = 1; this.paint = paint }
                    p.DrawStringExt(strCfg, "TEXTO"); p.DrawBlankLine(9); p.Output()

                    p.Init()
                    p.DrawStringExt(strCfg, "PDF_417")
                    p.DrawBarCode(GEDI_PRNTR_st_BarCodeConfig().apply { barCodeType = GEDI_PRNTR_e_BarCodeType.PDF_417; height = 100; width = 100 }, "TEXTO")
                    p.DrawBlankLine(9)
                    p.DrawStringExt(strCfg, "QR_CODE")
                    p.DrawBarCode(GEDI_PRNTR_st_BarCodeConfig().apply { barCodeType = GEDI_PRNTR_e_BarCodeType.QR_CODE; height = 100; width = 150 }, "TEXTO")
                    p.DrawBlankLine(9)
                    p.Output()

                    val before = runCatching { p.GetPaperUsage() }.getOrNull()
                    runCatching { p.ResetPaperUsage() }
                    val after = runCatching { p.GetPaperUsage() }.getOrNull()
                    result.success("status=$st; paperBefore=$before; paperAfter=$after")
                }

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
    }
}