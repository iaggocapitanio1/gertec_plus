import 'package:plugin_platform_interface/plugin_platform_interface.dart';
import 'gertec_plus_method_channel.dart';

abstract class GertecPlusPlatform extends PlatformInterface {
  GertecPlusPlatform() : super(token: _token);
  static final Object _token = Object();

  static GertecPlusPlatform _instance = MethodChannelGertecPlus(); // default
  static GertecPlusPlatform get instance => _instance;
  static set instance(GertecPlusPlatform i) {
    PlatformInterface.verifyToken(i, _token);
    _instance = i;
  }

  // Device / Info
  Future<String?> getPlatformVersion() =>
      throw UnimplementedError('getPlatformVersion() not implemented.');
  Future<String?> infoSN() =>
      throw UnimplementedError('infoSN() not implemented.');

  // Audio
  Future<void> beep() => throw UnimplementedError('beep() not implemented.');

  // LED
  Future<void> ledOn() => throw UnimplementedError('ledOn() not implemented.');
  Future<void> ledOff() => throw UnimplementedError('ledOff() not implemented.');

  // Printer
  Future<void> prnInit() => throw UnimplementedError('prnInit() not implemented.');
  Future<String?> prnStatus() => throw UnimplementedError('prnStatus() not implemented.');
  Future<void> prnText(String text) => throw UnimplementedError('prnText() not implemented.');
  Future<void> prnBarcode(String type, String data) => throw UnimplementedError('prnBarcode() not implemented.');
  Future<void> prnImage() => throw UnimplementedError('prnImage() not implemented.');
  Future<void> prnOutput() => throw UnimplementedError('prnOutput() not implemented.');
  Future<int?> prnPaperUsage() => throw UnimplementedError('prnPaperUsage() not implemented.');

  // Clock
  Future<Map?> clockGet() => throw UnimplementedError('clockGet() not implemented.');

  // Contactless
  Future<void> clPowerOn() => throw UnimplementedError('clPowerOn() not implemented.');
  Future<void> clPowerOff() => throw UnimplementedError('clPowerOff() not implemented.');

  // MSR
  Future<Map?> msrRead() => throw UnimplementedError('msrRead() not implemented.');

  // Smartcard
  Future<String?> smartStatus() => throw UnimplementedError('smartStatus() not implemented.');
}