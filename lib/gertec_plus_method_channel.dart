import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'gertec_plus_platform_interface.dart';
import 'dart:typed_data';

class MethodChannelGertecPlus extends GertecPlusPlatform {
  @visibleForTesting
  final MethodChannel _ch = const MethodChannel('gertec_plus');

  // Device / Info
  @override
  Future<String?> getPlatformVersion() =>
      _ch.invokeMethod<String>('getPlatformVersion');

  @override
  Future<String?> infoSN() => _ch.invokeMethod<String>('info.getSN');

  @override
  Future<void> beep() => _ch.invokeMethod('audio.beep');

  // LED
  Future<void> ledSet(String id, bool on) =>
      _ch.invokeMethod('led.set', {'id': id, 'on': on});
  // Helpers opcionais
  @override
  Future<void> ledOn() => ledSet('GEDI_LED_ID_CONTACTLESS_GREEN', true);
  @override
  Future<void> ledOff() => ledSet('GEDI_LED_ID_CONTACTLESS_GREEN', false);





  // Printer
  @override
  Future<String?> prnStatus() => _ch.invokeMethod<String>('prn.status');
  @override
  Future<int?> prnPaperUsage() => _ch.invokeMethod<int>('prn.paperUsage');
  Future<void> prnResetPaper() => _ch.invokeMethod('prn.resetPaper');
  @override
  Future<void> prnText(String text) =>
      _ch.invokeMethod('prn.text', {'text': text});
  @override
  Future<void> prnBarcode(String type, String data) =>
      _ch.invokeMethod('prn.barcode', {'type': type, 'data': data});
  @override
  Future<void> prnOutput() => _ch.invokeMethod('prn.output');

  // Contactless
  @override
  Future<void> clPowerOn() => _ch.invokeMethod('cl.powerOn');
  @override
  Future<void> clPowerOff() => _ch.invokeMethod('cl.powerOff');
  Future<Map?> clIsoPolling({int timeoutMs = 3000}) =>
      _ch.invokeMethod<Map>('cl.isoPolling', {'timeoutMs': timeoutMs});

  // MSR
  @override
  Future<Map?> msrRead() => _ch.invokeMethod<Map>('msr.read');

  // Smartcard (obrigatório informar slot)
  @override
  Future<String?> smartStatus({String slot = 'USER'}) =>
      _ch.invokeMethod<String>('smart.status', {'slot': slot});
  Future<void> smartPowerOff({String slot = 'USER'}) =>
      _ch.invokeMethod('smart.powerOff', {'slot': slot});
  // Image printing
  @override
  Future<void> prnImageBytes(
      Uint8List bytes, {
        int maxWidth = 384,
        String align = 'CENTER',
      }) =>
      _ch.invokeMethod('prn.image', {
        'source': 'bytes',
        'bytes': bytes,
        'maxWidth': maxWidth,
        'align': align,
      });

  @override
  Future<void> prnImageAsset(
      String assetPath, {
        int maxWidth = 384,
        String align = 'CENTER',
      }) =>
      _ch.invokeMethod('prn.image', {
        'source': 'asset',
        'asset': assetPath,
        'maxWidth': maxWidth,
        'align': align,
      });

  @override
  Future<void> prnImageDrawable(
      String drawableName, {
        int maxWidth = 384,
        String align = 'CENTER',
      }) =>
      _ch.invokeMethod('prn.image', {
        'source': 'drawable',
        'name': drawableName,
        'maxWidth': maxWidth,
        'align': align,
      });

  @override
  Future<void> prnImageFile(
      String path, {
        int maxWidth = 384,
        String align = 'CENTER',
      }) =>
      _ch.invokeMethod('prn.image', {
        'source': 'file',
        'path': path,
        'maxWidth': maxWidth,
        'align': align,
      });


}