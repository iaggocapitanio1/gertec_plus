import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:gertec_plus/gertec_plus.dart';

void main() => runApp(const App());

class App extends StatelessWidget {
  const App({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Gertec Plus Demo',
      theme: ThemeData(useMaterial3: true, colorSchemeSeed: Colors.blue),
      home: const HomePage(),
      debugShowCheckedModeBanner: false,
    );
  }
}

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  final _gertec = GertecPlus();
  String _platformVersion = '—';
  bool _loading = false;

  @override
  void initState() {
    super.initState();
    _fetchVersion();
  }

  Future<void> _fetchVersion() async {
    setState(() => _loading = true);
    try {
      final v = await _gertec.getPlatformVersion();
      setState(() => _platformVersion = v ?? 'desconhecida');
    } on PlatformException catch (e) {
      setState(() => _platformVersion = 'erro');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Falha ao obter versão: ${e.code}')),
        );
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Plugin example app'),
        actions: [
          IconButton(
            onPressed: _loading ? null : _fetchVersion,
            tooltip: 'Atualizar',
            icon: _loading
                ? const Padding(
                    padding: EdgeInsets.all(12),
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.refresh),
          ),
        ],
      ),
      body: Center(
        child: Card(
          margin: const EdgeInsets.all(16),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: ListTile(
              title: const Text('Versão da plataforma'),
              subtitle: Text(_platformVersion),
              trailing: ElevatedButton(
                onPressed: _loading ? null : _fetchVersion,
                child: const Text('Obter'),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
