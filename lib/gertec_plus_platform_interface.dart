import 'package:plugin_platform_interface/plugin_platform_interface.dart';

abstract class GertecPlusPlatform extends PlatformInterface {
  GertecPlusPlatform() : super(token: _token);
  static final Object _token = Object();

  static GertecPlusPlatform _instance = throw UnimplementedError('Set instance');
  static GertecPlusPlatform get instance => _instance;
  static set instance(GertecPlusPlatform i) {
    PlatformInterface.verifyToken(i, _token);
    _instance = i;
  }

  // INFO
  Future<String> getSN();

  // PRINTER
  Future<void> prnInit();
  Future<String> prnStatus();
  Future<int?> prnPaperUsage();
  Future<void> prnResetPaper();
  Future<void> prnText(String text);
  Future<void> prnBlank(int h);
  Future<void> prnBarcode(String type, String data);
  Future<void> prnOutput();
  Future<String> prnDemo();

  // AUDIO
  Future<void> beep();

  // LED
  Future<void> ledSet(String id, bool on);

  // CLOCK
  Future<Map?> clockRtc();

  // CL
  Future<void> clPowerOn();
  Future<void> clPowerOff();
  Future<Map?> clIsoPolling(int timeoutMs);

  // SMART
  Future<String> smartStatus(String slot);
  Future<void> smartPowerOff(String slot);

  // MSR
  Future<Map?> msrRead();
}
