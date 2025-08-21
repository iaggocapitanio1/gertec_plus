import 'dart:typed_data';
import 'gertec_plus_platform_interface.dart';

class GertecPlus {
  GertecPlusPlatform get _p => GertecPlusPlatform.instance;

  // Lifecycle
  Future<void> warmup() => _p.warmup();

  // Device / Info
  Future<String?> getPlatformVersion() => _p.getPlatformVersion();
  Future<String?> infoSN() => _p.infoSN();

  // Audio
  Future<void> beep() => _p.beep();

  // LED
  Future<void> ledSet(String id, bool on) => _p.ledSet(id, on);
  Future<void> ledOn() => _p.ledOn();
  Future<void> ledOff() => _p.ledOff();

  // Printer
  Future<String?> prnStatus() => _p.prnStatus();
  Future<void> prnText(String text) => _p.prnText(text);
  Future<void> prnBarcode(String type, String data) => _p.prnBarcode(type, data);
  Future<void> prnBlank(int h) => _p.prnBlank(h);
  Future<void> prnOutput() => _p.prnOutput();
  Future<int?> prnPaperUsage() => _p.prnPaperUsage();
  Future<void> prnResetPaper() => _p.prnResetPaper();

  // Images
  Future<void> prnImageBytes(Uint8List bytes, {int maxWidth = 384, String align = 'CENTER'}) =>
      _p.prnImageBytes(bytes, maxWidth: maxWidth, align: align);
  Future<void> prnImageAsset(String assetPath, {int maxWidth = 384, String align = 'CENTER'}) =>
      _p.prnImageAsset(assetPath, maxWidth: maxWidth, align: align);
  Future<void> prnImageDrawable(String drawableName, {int maxWidth = 384, String align = 'CENTER'}) =>
      _p.prnImageDrawable(drawableName, maxWidth: maxWidth, align: align);
  Future<void> prnImageFile(String path, {int maxWidth = 384, String align = 'CENTER'}) =>
      _p.prnImageFile(path, maxWidth: maxWidth, align: align);

  // Clock
  Future<Map<String, dynamic>?> clockGet() => _p.clockGet();

  // Contactless
  Future<void> clPowerOn() => _p.clPowerOn();
  Future<void> clPowerOff() => _p.clPowerOff();
  Future<Map<String, dynamic>?> clIsoPolling({int timeoutMs = 3000}) =>
      _p.clIsoPolling(timeoutMs: timeoutMs);

  // MSR
  Future<Map<String, dynamic>?> msrRead() => _p.msrRead();

  // Smartcard
  Future<String?> smartStatus({String slot = 'USER'}) => _p.smartStatus(slot: slot);
  Future<void> smartPowerOff({String slot = 'USER'}) => _p.smartPowerOff(slot: slot);

  // Demo
  Future<String?> prnDemo() => _p.prnDemo();
}