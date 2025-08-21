import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:gertec_plus/gertec_plus.dart';
import 'package:gertec_plus_example/utils.dart';

class HomePage extends StatefulWidget {
  const HomePage({super.key});
  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  final api = GertecPlus();

  // assets to preview/print
  static const imgA = 'assets/a.jpg';
  static const imgB = 'assets/b.jpg';

  bool _busy = false;
  String _platform = '—';
  String _sn = '—';
  String _printerStatus = '—';
  int? _paperUsage;

  late final TextEditingController _textCtrl;
  late final TextEditingController _barcodeCtrl;
  String _barcodeType = 'QR_CODE';


  Future<T?> _wrap<T>(Future<T> Function() fn, {String? ok}) async {
    setState(() => _busy = true);
    try {
      final r = await callNoBusy(fn);
      if (ok != null) _toast(ok);
      return r;
    } on PlatformException catch (e) {
      _toast('Fail: ${e.code}${e.message != null ? ' — ${e.message}' : ''}');
    } catch (e) {
      _toast('Error: $e');
    } finally {
      if (mounted) setState(() => _busy = false);
    }
    return null;
  }


  @override
  void initState() {
    super.initState();
    _textCtrl = TextEditingController(text: 'Olá, Gertec!');
    _barcodeCtrl = TextEditingController(text: 'TEXTO');

    WidgetsBinding.instance.addPostFrameCallback((_) {
      api.warmup(); // fire-and-forget, does not block UI
      if (mounted) _loadInfo();
    });
  }


  @override
  void dispose() {
    _textCtrl.dispose();
    _barcodeCtrl.dispose();
    super.dispose();
  }



  void _toast(String msg) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(msg), behavior: SnackBarBehavior.floating),
    );
  }

  Future<void> _loadInfo() async {
    setState(() => _busy = true);
    try {
      final results = await Future.wait([
        callNoBusy(() => api.getPlatformVersion()),
        callNoBusy(() => api.infoSN()),
        callNoBusy(() => api.prnStatus()),
        callNoBusy(() => api.prnPaperUsage()),
      ]);
      if (!mounted) return;
      setState(() {
        _platform = (results[0] as String?) ?? '—';
        _sn = (results[1] as String?) ?? '—';
        _printerStatus = (results[2] as String?) ?? '—';
        _paperUsage = results[3] as int?;
      });
    } on PlatformException catch (e) {
      _toast('Fail: ${e.code}${e.message != null ? ' — ${e.message}' : ''}');
    } catch (e) {
      _toast('Error: $e');
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }


  @override
  Widget build(BuildContext context) {
    final padBottom = MediaQuery.of(context).padding.bottom;
    final kb = MediaQuery.of(context).viewInsets.bottom;

    return Scaffold(
      resizeToAvoidBottomInset: true,
      appBar: AppBar(
        title: const Text('Gertec Plus'),
        actions: [
          IconButton(
            tooltip: 'Refresh',
            onPressed: _busy ? null : _loadInfo,
            icon: _busy
                ? const Padding(
              padding: EdgeInsets.all(12),
              child: CircularProgressIndicator(strokeWidth: 2),
            )
                : const Icon(Icons.refresh),
          ),
        ],
      ),
      body: SafeArea(
        child: ListView(
          keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
          padding: EdgeInsets.fromLTRB(16, 16, 16, 24 + padBottom + kb),
          children: [
            _Section(
              title: 'Device',
              subtitle: 'Platform and serial',
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  _InfoRow(label: 'Platform', value: _platform),
                  const SizedBox(height: 8),
                  _InfoRow(label: 'SN', value: _sn),
                ],
              ),
            ),

            _Section(
              title: 'Audio',
              subtitle: 'Beep',
              child: _Btn.full(
                icon: Icons.volume_up,
                label: 'Beep',
                onTap: _busy ? null : () => _wrap(api.beep, ok: 'Beep ok'),
              ),
            ),

            _Section(
              title: 'LED',
              subtitle: 'Contactless LED',
              child: Row(
                children: [
                  Expanded(
                    child: _Btn(
                      icon: Icons.light_mode,
                      label: 'On',
                      onTap: _busy ? null : () => _wrap(api.ledOn, ok: 'LED on'),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: _Btn(
                      icon: Icons.lightbulb_outline,
                      label: 'Off',
                      onTap: _busy ? null : () => _wrap(api.ledOff, ok: 'LED off'),
                    ),
                  ),
                ],
              ),
            ),

            _Section(
              title: 'Printer',
              subtitle: 'Text and barcode',
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  _InfoRow(label: 'Status', value: _printerStatus),
                  const SizedBox(height: 4),
                  _InfoRow(label: 'Paper usage', value: _paperUsage?.toString() ?? '—'),
                  const SizedBox(height: 12),
                  Row(
                    children: [

                      Expanded(
                        child: _Btn(
                          icon: Icons.stacked_bar_chart,
                          label: 'Usage',
                          onTap: _busy
                              ? null
                              : () async {
                            final u = await _wrap(() => api.prnPaperUsage());
                            if (u != null) setState(() => _paperUsage = u);
                            _toast('Usage: ${u ?? '—'}');
                          },
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: _textCtrl,
                    decoration: const InputDecoration(
                      labelText: 'Text to print',
                      border: OutlineInputBorder(),
                      isDense: true,
                    ),
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Expanded(
                        child: DropdownButtonFormField<String>(
                          value: _barcodeType,
                          decoration: const InputDecoration(
                            labelText: 'Barcode type',
                            border: OutlineInputBorder(),
                            isDense: true,
                          ),
                          items: const [
                            DropdownMenuItem(value: 'QR_CODE', child: Text('QR_CODE')),
                            DropdownMenuItem(value: 'PDF_417', child: Text('PDF_417')),
                            DropdownMenuItem(value: 'CODE128', child: Text('CODE128')),
                          ],
                          onChanged: (v) => setState(() => _barcodeType = v ?? 'QR_CODE'),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: TextField(
                          controller: _barcodeCtrl,
                          decoration: const InputDecoration(
                            labelText: 'Barcode data',
                            border: OutlineInputBorder(),
                            isDense: true,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Wrap(
                    spacing: 12,
                    runSpacing: 12,
                    children: [
                      _Btn(
                        icon: Icons.short_text,
                        label: 'Print text',
                        onTap: _busy
                            ? null
                            : () => _wrap(() => api.prnText(_textCtrl.text), ok: 'Text ok'),
                      ),
                      _Btn(
                        icon: Icons.qr_code,
                        label: 'Print code',
                        onTap: _busy
                            ? null
                            : () => _wrap(
                              () => api.prnBarcode(_barcodeType, _barcodeCtrl.text),
                          ok: 'Code ok',
                        ),
                      ),
                      _Btn(
                        icon: Icons.forward_to_inbox,
                        label: 'Output',
                        onTap: _busy ? null : () => _wrap(api.prnOutput, ok: 'Output ok'),
                      ),
                    ],
                  ),
                ],
              ),
            ),

            // New image preview + print
            _Section(
              title: 'Images',
              subtitle: 'Preview and print assets',
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Row(
                    children: [
                      Expanded(child: Image.asset(imgA, height: 80, fit: BoxFit.contain)),
                      const SizedBox(width: 12),
                      Expanded(child: Image.asset(imgB, height: 80, fit: BoxFit.contain)),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Expanded(
                        child: _Btn(
                          icon: Icons.print,
                          label: 'Print A',
                          onTap: _busy
                              ? null
                              : () => _wrap(
                                () => api.prnImageAsset(imgA, maxWidth: 384, align: 'CENTER'),
                            ok: 'Image A ok',
                          ),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: _Btn(
                          icon: Icons.print,
                          label: 'Print B',
                          onTap: _busy
                              ? null
                              : () => _wrap(
                                () => api.prnImageAsset(imgB, maxWidth: 384, align: 'CENTER'),
                            ok: 'Image B ok',
                          ),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),

            _Section(
              title: 'MSR',
              subtitle: 'Read tracks',
              child: _Btn.full(
                icon: Icons.credit_card,
                label: 'Read',
                onTap: _busy
                    ? null
                    : () async {
                  final m = await _wrap(api.msrRead);
                  _toast(m == null ? 'No data' : 'MSR: $m');
                },
              ),
            ),

            _Section(
              title: 'Contactless',
              subtitle: 'Power control',
              child: Row(
                children: [
                  Expanded(
                    child: _Btn(
                      icon: Icons.contactless,
                      label: 'Power ON',
                      onTap: _busy ? null : () => _wrap(api.clPowerOn, ok: 'CL on'),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: _Btn(
                      icon: Icons.power_settings_new,
                      label: 'Power OFF',
                      onTap: _busy ? null : () => _wrap(api.clPowerOff, ok: 'CL off'),
                    ),
                  ),
                ],
              ),
            ),

            _Section(
              title: 'Smartcard',
              subtitle: 'Status',
              child: _Btn.full(
                icon: Icons.sim_card,
                label: 'Get status',
                onTap: _busy
                    ? null
                    : () async {
                  final s = await _wrap(api.smartStatus);
                  _toast('Smart: ${s ?? '—'}');
                },
              ),
            ),

            const SizedBox(height: 8),
            Builder(
              builder: (context) => Text(
                'Tip: pull to dismiss keyboard. Buttons stay visible.',
                style: TextStyle(color: Theme.of(context).colorScheme.outline),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _Section extends StatelessWidget {
  final String title;
  final String? subtitle;
  final Widget child;
  const _Section({required this.title, this.subtitle, required this.child});

  @override
  Widget build(BuildContext context) {
    final th = Theme.of(context);
    return Card(
      elevation: 0,
      margin: const EdgeInsets.only(bottom: 16),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(
            children: [
              Expanded(child: Text(title, style: th.textTheme.titleMedium)),
              if (subtitle != null)
                Text(subtitle!, style: th.textTheme.bodySmall?.copyWith(color: th.colorScheme.outline)),
            ],
          ),
          const Divider(height: 16),
          child,
        ]),
      ),
    );
  }
}

class _Btn extends StatelessWidget {
  final IconData icon;
  final String label;
  final VoidCallback? onTap;
  const _Btn({required this.icon, required this.label, required this.onTap});

  const _Btn.full({required this.icon, required this.label, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return ConstrainedBox(
      constraints: const BoxConstraints(minWidth: 140, minHeight: 44),
      child: FilledButton.icon(
        onPressed: onTap,
        icon: Icon(icon),
        label: Text(label),
        style: FilledButton.styleFrom(
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        ),
      ),
    );
  }
}

class _InfoRow extends StatelessWidget {
  final String label;
  final String value;
  const _InfoRow({required this.label, required this.value});
  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Row(
      children: [
        Text('$label: ', style: TextStyle(color: cs.outline)),
        Expanded(child: Text(value, style: const TextStyle(fontWeight: FontWeight.w600))),
      ],
    );
  }
}