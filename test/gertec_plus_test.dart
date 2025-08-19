import 'package:flutter_test/flutter_test.dart';
import 'package:gertec_plus/gertec_plus.dart';
import 'package:gertec_plus/gertec_plus_platform_interface.dart';
import 'package:gertec_plus/gertec_plus_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockGertecPlusPlatform
    with MockPlatformInterfaceMixin
    implements GertecPlusPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final GertecPlusPlatform initialPlatform = GertecPlusPlatform.instance;

  test('$MethodChannelGertecPlus is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelGertecPlus>());
  });

  test('getPlatformVersion', () async {
    GertecPlus gertecPlusPlugin = GertecPlus();
    MockGertecPlusPlatform fakePlatform = MockGertecPlusPlatform();
    GertecPlusPlatform.instance = fakePlatform;

    expect(await gertecPlusPlugin.getPlatformVersion(), '42');
  });
}
