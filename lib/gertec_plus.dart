import 'package:flutter/services.dart';
import 'gertec_plus_platform_interface.dart';

class MethodChannelGertecPlus extends GertecPlusPlatform {
  final _ch = const MethodChannel('gertec_plus');

  // INFO
  @override
  Future<String> getSN() async => await _ch.invokeMethod('info.getSN');

  // PRINTER
  @override
  Future<void> prnInit() => _ch.invokeMethod('prn.init');
  @override
  Future<String> prnStatus() async => await _ch.invokeMethod('prn.status');
  @override
  Future<int?> prnPaperUsage() async => await _ch.invokeMethod('prn.paperUsage');
  @override
  Future<void> prnResetPaper() => _ch.invokeMethod('prn.resetPaper');
  @override
  Future<void> prnText(String text) => _ch.invokeMethod('prn.text', {'text': text});
  @override
  Future<void> prnBlank(int h) => _ch.invokeMethod('prn.blank', {'h': h});
  @override
  Future<void> prnBarcode(String type, String data) =>
      _ch.invokeMethod('prn.barcode', {'type': type, 'data': data});
  @override
  Future<void> prnOutput() => _ch.invokeMethod('prn.output');
  @override
  Future<String> prnDemo() async => await _ch.invokeMethod('prn.demo');

  // AUDIO
  @override
  Future<void> beep() => _ch.invokeMethod('audio.beep');

  // LED
  @override
  Future<void> ledSet(String id, bool on) =>
      _ch.invokeMethod('led.set', {'id': id, 'on': on});

  // CLOCK
  @override
  Future<Map?> clockRtc() async => await _ch.invokeMethod('clock.rtc');

  // CL
  @override
  Future<void> clPowerOn() => _ch.invokeMethod('cl.powerOn');
  @override
  Future<void> clPowerOff() => _ch.invokeMethod('cl.powerOff');
  @override
  Future<Map?> clIsoPolling(int timeoutMs) async =>
      await _ch.invokeMethod('cl.isoPolling', {'timeoutMs': timeoutMs});

  // SMART
  @override
  Future<String> smartStatus(String slot) async =>
      await _ch.invokeMethod('smart.status', {'slot': slot});
  @override
  Future<void> smartPowerOff(String slot) =>
      _ch.invokeMethod('smart.powerOff', {'slot': slot});

  // MSR
  @override
  Future<Map?> msrRead() async => await _ch.invokeMethod('msr.read');
}
