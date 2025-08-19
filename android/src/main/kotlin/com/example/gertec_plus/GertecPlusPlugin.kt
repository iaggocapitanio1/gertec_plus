package com.example.gertec_plus

import android.app.Activity
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

class GertecPlusPlugin :
    FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {

    private lateinit var channel: MethodChannel
    private var activity: Activity? = null

    override fun onAttachedToEngine(b: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(b.binaryMessenger, "gertec_plus")
        channel.setMethodCallHandler(this)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        GEDI.init(activity!!.applicationContext)
    }
    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) = onAttachedToActivity(binding)
    override fun onDetachedFromActivityForConfigChanges() { activity = null }
    override fun onDetachedFromActivity() { activity = null }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        try {
            when (call.method) {

                // INFO
                "info.getSN" -> {
                    val sn = GEDI.getInstance().iNFO.ControlNumberGet(GEDI_INFO_e_ControlNumber.SN)
                    result.success(sn)
                }

                // PRINTER
                "prn.init" -> { GEDI.getInstance().pRNTR.Init(); result.success(true) }
                "prn.status" -> { result.success(GEDI.getInstance().pRNTR.Status().name) }
                "prn.paperUsage" -> { result.success(GEDI.getInstance().pRNTR.GetPaperUsage()) }
                "prn.resetPaper" -> { GEDI.getInstance().pRNTR.ResetPaperUsage(); result.success(true) }
                "prn.text" -> {
                    val text = call.argument<String>("text") ?: ""
                    val paint = Paint().apply { textSize = 10f }
                    val cfg = GEDI_PRNTR_st_StringConfig().apply { lineSpace = 1; offset = 1; this.paint = paint }
                    GEDI.getInstance().pRNTR.DrawStringExt(cfg, text)
                    result.success(true)
                }
                "prn.blank" -> {
                    val h = call.argument<Int>("h") ?: 9
                    GEDI.getInstance().pRNTR.DrawBlankLine(h)
                    result.success(true)
                }
                "prn.barcode" -> {
                    val type = GEDI_PRNTR_e_BarCodeType.valueOf(call.argument<String>("type")!!)
                    val data = call.argument<String>("data") ?: "TEXTO"
                    val cfg = GEDI_PRNTR_st_BarCodeConfig().apply {
                        barCodeType = type; height = if (type == GEDI_PRNTR_e_BarCodeType.QR_CODE) 100 else 100
                        width  = if (type == GEDI_PRNTR_e_BarCodeType.QR_CODE) 150 else 100
                    }
                    GEDI.getInstance().pRNTR.DrawBarCode(cfg, data)
                    result.success(true)
                }
                "prn.output" -> { GEDI.getInstance().pRNTR.Output(); result.success(true) }

                // AUDIO
                "audio.beep" -> { GEDI.getInstance().aUDIO.Beep(); result.success(true) }

                // LED
                "led.set" -> {
                    val id = GEDI_LED_e_Id.valueOf(call.argument<String>("id")!!)
                    val on = call.argument<Boolean>("on") ?: false
                    GEDI.getInstance().lED.Set(id, on)
                    result.success(true)
                }

                // CLOCK
                "clock.rtc" -> {
                    val rtc = GEDI.getInstance().cLOCK.RTCFGet()
                    val m = mapOf(
                        "hour" to rtc.bHour.toInt(), "minute" to rtc.bMinute.toInt(), "second" to rtc.bSecond.toInt(),
                        "day" to rtc.bDay.toInt(), "month" to rtc.bMonth.toInt(), "year" to rtc.bYear.toInt(), "dow" to rtc.bDoW.toInt()
                    )
                    result.success(m)
                }

                // CONTACTLESS (CL) — básico
                "cl.powerOn" -> { GEDI.getInstance().cL.PowerOn(); result.success(true) }
                "cl.powerOff" -> { GEDI.getInstance().cL.PowerOff(); result.success(true) }
                "cl.isoPolling" -> {
                    val timeoutMs = call.argument<Int>("timeoutMs") ?: 3000
                    val info = GEDI.getInstance().cL.ISO_Polling(timeoutMs)
                    val uid = info.abUID.joinToString("") { String.format(Locale.US, "%02X", it) }
                    result.success(mapOf("type" to info.peType.name, "uidHex" to uid))
                }

                // SMART — status e power off
                "smart.status" -> {
                    val slot = GEDI_SMART_e_Slot.valueOf(call.argument<String>("slot")!!)
                    result.success(GEDI.getInstance().sMART.Status(slot).name)
                }
                "smart.powerOff" -> {
                    val slot = GEDI_SMART_e_Slot.valueOf(call.argument<String>("slot")!!)
                    GEDI.getInstance().sMART.PowerOff(slot)
                    result.success(true)
                }

                // MSR — leitura simples
                "msr.read" -> {
                    val t = GEDI.getInstance().mSR.Read()
                    fun bytesToHex(b: ByteArray?) = b?.joinToString("") { String.format("%02X", it) } ?: ""
                    result.success(mapOf(
                        "tk1" to bytesToHex(t.abTk1Buf),
                        "tk2" to bytesToHex(t.abTk2Buf),
                        "tk3" to bytesToHex(t.abTk3Buf)
                    ))
                }

                // DEMO equivalente ao seu btnIPRNTR
                "prn.demo" -> {
                    val p = GEDI.getInstance().pRNTR
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
        channel.setMethodCallHandler(null)
    }
}
