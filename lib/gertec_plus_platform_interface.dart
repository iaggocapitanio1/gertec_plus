// lib/gertec_plus_platform_interface.dart
import 'dart:typed_data';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';
import 'gertec_plus_method_channel.dart';

abstract class GertecPlusPlatform extends PlatformInterface {
  GertecPlusPlatform() : super(token: _token);
  static final Object _token = Object();

  static GertecPlusPlatform _instance = MethodChannelGertecPlus();
  static GertecPlusPlatform get instance => _instance;
  static set instance(GertecPlusPlatform i) {
    PlatformInterface.verifyToken(i, _token);
    _instance = i;
  }

  // Lifecycle
  Future<void> warmup() => _unimpl('warmup');

  // Device / Info
  Future<String?> getPlatformVersion() => _unimpl('getPlatformVersion');
  Future<String?> infoSN() => _unimpl('infoSN');

  // Audio
  Future<void> beep() => _unimpl('beep');

  // LED
  Future<void> ledSet(String id, bool on) => _unimpl('ledSet');
  // Convenience
  Future<void> ledOn() => _unimpl('ledOn');
  Future<void> ledOff() => _unimpl('ledOff');

  // Printer
  Future<String?> prnStatus() => _unimpl('prnStatus');
  Future<void> prnText(String text) => _unimpl('prnText');
  Future<void> prnBarcode(String type, String data) => _unimpl('prnBarcode');
  Future<void> prnBlank(int h) => _unimpl('prnBlank');
  Future<void> prnOutput() => _unimpl('prnOutput');
  Future<int?> prnPaperUsage() => _unimpl('prnPaperUsage');
  Future<void> prnResetPaper() => _unimpl('prnResetPaper');

  // Images
  Future<void> prnImageBytes(Uint8List bytes, {int maxWidth = 384, String align = 'CENTER'}) =>
      _unimpl('prnImageBytes');
  Future<void> prnImageAsset(String assetPath, {int maxWidth = 384, String align = 'CENTER'}) =>
      _unimpl('prnImageAsset');
  Future<void> prnImageDrawable(String drawableName, {int maxWidth = 384, String align = 'CENTER'}) =>
      _unimpl('prnImageDrawable');
  Future<void> prnImageFile(String path, {int maxWidth = 384, String align = 'CENTER'}) =>
      _unimpl('prnImageFile');

  // Clock
  Future<Map<String, dynamic>?> clockGet() => _unimpl('clockGet');

  // Contactless
  Future<void> clPowerOn() => _unimpl('clPowerOn');
  Future<void> clPowerOff() => _unimpl('clPowerOff');
  Future<Map<String, dynamic>?> clIsoPolling({int timeoutMs = 3000}) => _unimpl('clIsoPolling');

  // MSR
  Future<Map<String, dynamic>?> msrRead() => _unimpl('msrRead');

  // Smartcard
  Future<String?> smartStatus({String slot = 'SLOT0'}) => _unimpl('smartStatus');
  Future<void> smartPowerOff({String slot = 'SLOT0'}) => _unimpl('smartPowerOff');

  // Demo
  Future<String?> prnDemo() => _unimpl('prnDemo');

  static Never _unimpl(String name) =>
      throw UnimplementedError('$name() not implemented by the platform.');
}