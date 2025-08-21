import 'package:flutter/services.dart';

Future<T?> callNoBusy<T>(Future<T> Function() fn) async {
  const delays = [Duration(milliseconds: 200), Duration(milliseconds: 400), Duration(milliseconds: 800)];
  PlatformException? lastInit;
  for (final d in delays) {
    try { return await fn(); }
    on PlatformException catch (e) {
      if (e.code != 'GEDI_INIT') rethrow;
      lastInit = e;
      await Future.delayed(d);
    }
  }
  // final attempt; surface any error
  try { return await fn(); } on PlatformException catch (e) {
    if (e.code == 'GEDI_INIT') throw lastInit ?? e;
    rethrow;
  }
}