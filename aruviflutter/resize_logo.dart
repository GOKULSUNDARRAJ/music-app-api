import 'dart:io';
import 'package:image/image.dart';

void main() {
  // Read the original image
  final bytes = File('assets/images/arivumusiclogo.png').readAsBytesSync();
  final original = decodeImage(bytes);
  
  if (original == null) {
    print('Failed to decode image');
    return;
  }

  // Resize the image so its width is at most 500 pixels (reasonable for xxhdpi)
  final resized = copyResize(original, width: 500);

  // Save it as launch_image.png in mipmap-xxhdpi
  final outFile = File('android/app/src/main/res/mipmap-xxhdpi/launch_image.png');
  outFile.writeAsBytesSync(encodePng(resized));
  
  print('Successfully created scaled launch_image.png!');
}
