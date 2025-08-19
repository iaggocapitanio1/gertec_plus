import 'gertec_plus_platform_interface.dart';

class GertecPlus {
  GertecPlusPlatform get _p => GertecPlusPlatform.instance;

  // Device / Info
  Future<String?> getPlatformVersion() => _p.getPlatformVersion();
  Future<String?> infoSN() => _p.infoSN();

  // Audio
  Future<void> beep() => _p.beep();

  // LED
  Future<void> ledOn() => _p.ledOn();
  Future<void> ledOff() => _p.ledOff();

  // Printer
  Future<void> prnInit() => _p.prnInit();
  Future<String?> prnStatus() => _p.prnStatus();
  Future<void> prnText(String text) => _p.prnText(text);
  Future<void> prnBarcode(String type, String data) => _p.prnBarcode(type, data);
  Future<void> prnImage() => _p.prnImage();
  Future<void> prnOutput() => _p.prnOutput();
  Future<int?> prnPaperUsage() => _p.prnPaperUsage();

  // Clock
  Future<Map?> clockGet() => _p.clockGet();

  // Contactless
  Future<void> clPowerOn() => _p.clPowerOn();
  Future<void> clPowerOff() => _p.clPowerOff();

  // MSR
  Future<Map?> msrRead() => _p.msrRead();

  // Smartcard
  Future<String?> smartStatus() => _p.smartStatus();
}