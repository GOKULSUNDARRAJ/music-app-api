import 'package:barcode/barcode.dart';

void main() {
  final bc = Barcode.code128();
  final data = 'aruvi://playlist/123';
  final svg = bc.toSvg(data, width: 200, height: 80);
  final boolIterable = bc.make(data, width: 200, height: 80);
  
  int barCount = 0;
  for (var bar in boolIterable) {
    if (bar.black) barCount++;
  }
  print('Total elements: \');
  print('Total black elements: \');
}
